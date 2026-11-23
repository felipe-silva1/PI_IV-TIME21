//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package servidor;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.*;
import java.time.format.*;
import java.util.*;

import br.edu.time21.comunicacao.*;

// atende os pedidos de cronograma lendo e gravando na colecao
// blocos_de_estudo (ver docs/dicionario-banco.md). nao existe colecao
// "cronogramas": o cronograma de uma semana e o conjunto de blocos com o
// mesmo usuarioId e semanaInicio. nao guarda estado alem da colecao, que e
// thread-safe, entao uma unica instancia e compartilhada por todas as
// SupervisorasDeConexao.
public class OperacoesDeCronograma {
    public static final String COLECAO = "blocos_de_estudo";
    private static final int MINUTOS_POR_POMODORO = 25;

    private static final Set<String> TURNOS = Set.of (TurnoLivre.MANHA, TurnoLivre.TARDE, TurnoLivre.NOITE);
    
    // ordem dos turnos dentro de um dia, para devolver o cronograma na
    // ordem em que vai ser estudado (em ordem alfabetica, NOITE viria
    // antes de TARDE)
    private static final List<String> ORDEM_DOS_TURNOS = List.of (TurnoLivre.MANHA, TurnoLivre.TARDE, TurnoLivre.NOITE);
    private MongoCollection<Document> colecao;
    private final MongoCollection<Document> pomodoros;

    public OperacoesDeCronograma (MongoDatabase banco) throws Exception
    {
        if (banco==null)
            throw new Exception ("Banco ausente");

        this.colecao = banco.getCollection (OperacoesDeCronograma.COLECAO);
        this.pomodoros = banco.getCollection ("sessoes_pomodoro");
    }

    // os dois indices previstos no dicionario: (usuarioId, semanaInicio)
    // para o cronograma e o progresso da semana, (usuarioId, data) para o
    // checklist diario. nao sao unicos: uma semana tem varios blocos.
    // createIndex nao faz nada se o indice ja existe; se o banco estiver
    // fora do ar, so avisa no console
    public void garantirIndices()
    {
        try
        {
            this.colecao.createIndex (Indexes.ascending ("usuarioId", "semanaInicio"));
            this.colecao.createIndex (Indexes.ascending ("usuarioId", "data"));
            this.pomodoros.createIndex (Indexes.ascending ("usuarioId", "concluidoEm"));
            this.pomodoros.createIndex (Indexes.ascending ("usuarioId", "tarefaId"));
        }
        catch (Exception erro)
        {
            System.err.println("Falha ao criar indices em "+ OperacoesDeCronograma.COLECAO + ": " + erro.getMessage());
        }
    }

    public Resposta salvarPomodoro(PedidoParaSalvarPomodoro pedido)
    {
        String usuario = pedido.getUsuarioId();
        String sessao = pedido.getSessaoId();
        String tarefa = pedido.getTarefaId();
        long concluidoEm = pedido.getConcluidoEm();

        if (usuario == null || !ObjectId.isValid(usuario) || !uuidValido(sessao)
                || (tarefa != null && !ObjectId.isValid(tarefa))
                || concluidoEm <= 0 || concluidoEm > System.currentTimeMillis() + 60_000)
            return new RespostaDeErro(RespostaDeErro.PEDIDO_INVALIDO, "Sessao Pomodoro invalida.");

        try
        {
            ObjectId usuarioId = new ObjectId(usuario);
            Bson chave = Filters.eq("_id", usuarioId.toHexString() + ":" + sessao);
            ObjectId tarefaId = tarefa == null ? null : new ObjectId(tarefa);
            if (tarefaId != null && colecao.find(Filters.and(
                    Filters.eq("_id", tarefaId), Filters.eq("usuarioId", usuarioId)))
                    .projection(Projections.include("_id")).first() == null)
                tarefaId = null; // Preserva o tempo mesmo se o cronograma tiver sido regerado durante o foco.

            // A chave única garante que a mesma sessão seja contabilizada uma vez.
            Document sessaoSalva = pomodoros.findOneAndUpdate(chave, Updates.combine(
                    Updates.setOnInsert("usuarioId", usuarioId),
                    Updates.setOnInsert("tarefaId", tarefaId),
                    Updates.setOnInsert("concluidoEm", new Date(concluidoEm)),
                    Updates.setOnInsert("duracaoMin", MINUTOS_POR_POMODORO)),
                    new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));

            // Use o vínculo persistido para que um reenvio avalie a mesma tarefa.
            ObjectId tarefaSalva = sessaoSalva.getObjectId("tarefaId");
            if (tarefaSalva != null)
                concluirTarefaPorPomodoro(usuarioId, tarefaSalva);

            return new RespostaDePomodoroSalvo();
        }
        catch (Exception erro)
        {
            return erroDeBanco("salvar Pomodoro", erro);
        }
    }

    private void concluirTarefaPorPomodoro(ObjectId usuarioId, ObjectId tarefaId)
    {
        Document total = pomodoros.aggregate(List.of(
                Aggregates.match(Filters.and(
                        Filters.eq("usuarioId", usuarioId), Filters.eq("tarefaId", tarefaId))),
                Aggregates.group(null,
                        Accumulators.sum("minutos", "$duracaoMin"),
                        Accumulators.max("ultimoFoco", "$concluidoEm"))
        )).first();
        if (total == null) return;

        long minutos = ((Number) total.get("minutos")).longValue();
        Bson tarefaComMetaAtingida = Filters.and(
                Filters.eq("_id", tarefaId),
                Filters.eq("usuarioId", usuarioId),
                Filters.lte("duracaoMin", minutos),
                Filters.ne("metaPomodoroAtingida", true));

        // Aplica a conclusão uma vez por tarefa, preservando uma reabertura manual.
        Document atualizacao = new Document("metaPomodoroAtingida", true)
                .append("concluido", true)
                .append("concluidoEm", new Document("$cond", List.of(
                        new Document("$eq", List.of("$concluido", true)),
                        "$concluidoEm",
                        total.getDate("ultimoFoco"))));
        colecao.updateOne(tarefaComMetaAtingida, List.of(new Document("$set", atualizacao)));
    }

    public Resposta buscarPomodoros(PedidoParaBuscarPomodoros pedido)
    {
        String usuario = pedido.getUsuarioId();
        long inicio = pedido.getInicio();
        long fim = pedido.getFim();
        if (usuario == null || !ObjectId.isValid(usuario) || inicio >= fim)
            return new RespostaDeErro(RespostaDeErro.PEDIDO_INVALIDO, "Periodo de Pomodoro invalido.");

        try
        {
            List<ObjectId> tarefas = pedido.getTarefas().stream()
                    .filter(ObjectId::isValid).map(ObjectId::new).toList();
            Bson periodo = Filters.and(Filters.gte("concluidoEm", new Date(inicio)),
                    Filters.lt("concluidoEm", new Date(fim)));
            // Sessões de outras semanas também indicam que a tarefa já tem tempo medido.
            Bson consulta = tarefas.isEmpty() ? periodo : Filters.or(periodo, Filters.in("tarefaId", tarefas));
            Bson filtro = Filters.and(Filters.eq("usuarioId", new ObjectId(usuario)), consulta);
            long minutos = 0;
            Set<String> tarefasComFoco = new HashSet<>();

            for (Document sessao : pomodoros.find(filtro)
                    .projection(Projections.include("concluidoEm", "duracaoMin", "tarefaId")))
            {
                long conclusao = sessao.getDate("concluidoEm").getTime();
                if (conclusao >= inicio && conclusao < fim)
                    minutos += sessao.getInteger("duracaoMin");

                ObjectId tarefa = sessao.getObjectId("tarefaId");
                if (tarefa != null)
                    tarefasComFoco.add(tarefa.toHexString());
            }

            return new RespostaDePomodoros(minutos, tarefasComFoco);
        }
        catch (Exception erro)
        {
            return erroDeBanco("buscar Pomodoros", erro);
        }
    }

    private static boolean uuidValido(String valor)
    {
        if (valor == null) return false;
        try
        {
            return UUID.fromString(valor).toString().equals(valor);
        }
        catch (IllegalArgumentException erro)
        {
            return false;
        }
    }

    // busca os blocos do usuario na semana, ordenados por dia e turno.
    // semana sem nenhum bloco devolve RespostaDeCronograma com cronograma null:
    // semana ainda nao gerada e um caso normal, nao um erro
    public Resposta buscar (PedidoParaBuscarCronograma pedido)
    {
        String usuarioId    = pedido.getUsuarioId();
        String semanaInicio = pedido.getSemanaInicio();

        String problema = OperacoesDeCronograma.problemaNaSemana (usuarioId, semanaInicio);
        if (problema!=null)
            return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO, problema);

        try
        {
            ArrayList<Bloco> blocos = new ArrayList<Bloco> ();
            for (Document documento : this.colecao.find (
                     OperacoesDeCronograma.daSemana (new ObjectId (usuarioId), semanaInicio)))
                blocos.add (OperacoesDeCronograma.deDocumento (documento));

            if (blocos.isEmpty())
                return new RespostaDeCronograma (null);

            // data em yyyy-MM-dd ordena certo como texto
            blocos.sort (Comparator.comparing (Bloco::getData)
                                   .thenComparingInt (bloco ->
                                       OperacoesDeCronograma.ORDEM_DOS_TURNOS.indexOf (bloco.getTurno())));

            return new RespostaDeCronograma (
                new Cronograma (usuarioId, semanaInicio, blocos));
        }
        catch (Exception erro)
        {
            return OperacoesDeCronograma.erroDeBanco ("buscar", erro);
        }
    }

    // retorna os blocos do usuario que estao na data informada. no checklist
    // uma data sem tarefas e um resultado normal (lista vazia).
    public Resposta buscarTarefas (PedidoParaBuscarTarefas pedido)
    {
        String usuarioId = pedido.getUsuarioId();
        String data = pedido.getData();

        if (usuarioId==null || !ObjectId.isValid (usuarioId))
            return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                       "usuarioId invalido: " + usuarioId);

        if (OperacoesDeCronograma.dataOuNull (data)==null)
            return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                       "data invalida: " + data);

        try
        {
            ArrayList<Bloco> tarefas = new ArrayList<Bloco> ();
            for (Document documento : this.colecao.find (
                     Filters.and (Filters.eq ("usuarioId", new ObjectId (usuarioId)),
                                  Filters.eq ("data", data))))
                tarefas.add (OperacoesDeCronograma.deDocumento (documento));

            tarefas.sort (Comparator.comparingInt (
                tarefa -> OperacoesDeCronograma.ORDEM_DOS_TURNOS.indexOf (tarefa.getTurno())));

            return new RespostaDeTarefas (tarefas);
        }
        catch (Exception erro)
        {
            return OperacoesDeCronograma.erroDeBanco ("buscar tarefas", erro);
        }
    }

    // atualiza somente o status do bloco identificado, garantindo que ele
    // pertence ao usuario que fez o pedido.
    public Resposta atualizarTarefa (PedidoParaAtualizarTarefa pedido)
    {
        String usuarioId = pedido.getUsuarioId();
        String tarefaId = pedido.getTarefaId();

        if (usuarioId==null || !ObjectId.isValid (usuarioId))
            return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                       "usuarioId invalido: " + usuarioId);

        if (tarefaId==null || !ObjectId.isValid (tarefaId))
            return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                       "tarefaId invalido: " + tarefaId);

        try
        {
            Bson filtro = Filters.and (Filters.eq ("_id", new ObjectId (tarefaId)),
                                       Filters.eq ("usuarioId", new ObjectId (usuarioId)));

            if (pedido.isAtualizacaoCompleta ())
            {
                String data = pedido.getData ();
                String turno = pedido.getTurno ();
                String materia = pedido.getMateria ();
                int duracaoMin = pedido.getDuracaoMin ();

                if (OperacoesDeCronograma.dataOuNull (data)==null)
                    return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                               "data invalida");
                if (turno==null || !OperacoesDeCronograma.TURNOS.contains (turno))
                    return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                               "turno invalido");
                if (OperacoesDeCronograma.vazia (materia))
                    return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                               "materia ausente");
                if (duracaoMin<=0)
                    return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                               "duracaoMin deve ser maior que zero");

                String descricao = pedido.getDescricao ();
                if (descricao!=null && descricao.length()>500)
                    return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                               "descricao excede 500 caracteres");

                    Document existente = this.colecao.find(filtro).first();
                    if (existente==null)
                        return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                                   "tarefa nao encontrada");

                    LocalDate semanaInicio =
                        OperacoesDeCronograma.dataOuNull (existente.getString ("semanaInicio"));
                    LocalDate novaData = OperacoesDeCronograma.dataOuNull (data);
                    if (semanaInicio==null || novaData==null ||
                        novaData.isBefore(semanaInicio) ||
                        novaData.isAfter(semanaInicio.plusDays(6)))
                        return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                                   "data fora da semana da tarefa");

                Document atual = this.colecao.findOneAndUpdate (
                    filtro,
                    Updates.combine (
                        Updates.set ("data", data),
                        Updates.set ("turno", turno),
                        Updates.set ("materia", materia.trim()),
                        Updates.set ("duracaoMin", duracaoMin),
                        Updates.set ("descricao", descricao==null ? "" : descricao.trim()),
                        Updates.set ("concluido", pedido.isConcluido()),
                        pedido.isConcluido ()
                            ? Updates.set ("concluidoEm", new Date ())
                            : Updates.set ("concluidoEm", null)),
                    new com.mongodb.client.model.FindOneAndUpdateOptions ()
                        .returnDocument (com.mongodb.client.model.ReturnDocument.AFTER));

                if (atual==null)
                    return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                               "tarefa nao encontrada");

                return new RespostaDeTarefa (OperacoesDeCronograma.deDocumento (atual));
            }

            Document atual = pedido.isConcluido ()
                ? this.colecao.findOneAndUpdate (
                    filtro,
                    Updates.combine (Updates.set ("concluido", true),
                                     Updates.set ("concluidoEm", new Date ())),
                    new com.mongodb.client.model.FindOneAndUpdateOptions ()
                        .returnDocument (com.mongodb.client.model.ReturnDocument.AFTER))
                : this.colecao.findOneAndUpdate (
                    filtro,
                    Updates.combine (Updates.set ("concluido", false),
                                     Updates.set ("concluidoEm", null)),
                    new com.mongodb.client.model.FindOneAndUpdateOptions ()
                        .returnDocument (com.mongodb.client.model.ReturnDocument.AFTER));

            if (atual==null)
                return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                           "tarefa nao encontrada");

            return new RespostaDeTarefa (OperacoesDeCronograma.deDocumento (atual));
        }
        catch (Exception erro)
        {
            return OperacoesDeCronograma.erroDeBanco ("atualizar tarefa", erro);
        }
    }

    // valida a chave de um cronograma (usuario + semana); usado tanto no
    // salvar quanto no buscar. devolve null se esta ok
    private static String problemaNaSemana (String usuarioId, String semanaInicio)
    {
        if (usuarioId==null || !ObjectId.isValid (usuarioId))
            return "usuarioId invalido: " + usuarioId;

        LocalDate segunda = OperacoesDeCronograma.dataOuNull (semanaInicio);
        if (segunda==null)
            return "semanaInicio invalida: " + semanaInicio;

        if (segunda.getDayOfWeek()!=DayOfWeek.MONDAY)
            return "semanaInicio nao e segunda-feira: " + semanaInicio;

        return null;
    }

    // regra de "regerar" do dicionario: apaga os blocos que o usuario ja
    // tinha na semana e insere os novos. os ids antigos deixam de existir;
    // a resposta traz os ids gerados agora.
    //
    // deleteMany + insertMany NAO sao atomicos: se o banco cair entre os
    // dois, a semana fica vazia e o backend precisa pedir para salvar de
    // novo. transacao resolveria, mas exige replica set (o Atlas tem; um
    // mongod local avulso nao).
    public Resposta salvar (PedidoParaSalvarCronograma pedido)
    {
        Cronograma cronograma = pedido.getCronograma();
        String problema = OperacoesDeCronograma.problemaEm (cronograma);
        if (problema!=null)
            return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO, problema);

        try
        {
            ObjectId usuarioId = new ObjectId (cronograma.getUsuarioId());
            String semanaInicio = cronograma.getSemanaInicio();

            ArrayList<Document> documentos = new ArrayList<Document> ();
            for(Bloco bloco : cronograma.getBlocos()) 
                documentos.add (OperacoesDeCronograma.paraDocumento (bloco, usuarioId, semanaInicio));

            this.colecao.deleteMany (OperacoesDeCronograma.daSemana (usuarioId, semanaInicio));
            this.colecao.insertMany (documentos);

            // insertMany preenche o _id de cada Document da lista, entao da
            // para montar a resposta sem ler de volta do banco
            ArrayList<Bloco> gravados = new ArrayList<Bloco> ();
            for (Document documento : documentos)
                gravados.add (OperacoesDeCronograma.deDocumento (documento));

            return new RespostaDeCronograma (
                new Cronograma (usuarioId.toHexString(), semanaInicio, gravados));
        }
        catch (Exception erro)
        {
            return OperacoesDeCronograma.erroDeBanco ("salvar", erro);
        }
    }
    // regras do dicionario que o servidor garante antes de gravar; devolve
    // null se o cronograma esta ok. a validacao voltada ao usuario (materias
    // do vestibular, turnos dentro da rotina) e do backend
    private static String problemaEm (Cronograma cronograma)
    {
        if (cronograma==null)
            return "cronograma ausente";

        String problema = OperacoesDeCronograma.problemaNaSemana (cronograma.getUsuarioId(),
                                                                  cronograma.getSemanaInicio());
        if (problema!=null)
            return problema;

        LocalDate segunda = OperacoesDeCronograma.dataOuNull (cronograma.getSemanaInicio());
        List<Bloco> blocos = cronograma.getBlocos();
        if (blocos==null || blocos.isEmpty())
            return "cronograma sem nenhum bloco";

        LocalDate domingo = segunda.plusDays (6);
        for (Bloco bloco : blocos)
        {
            if (bloco==null)
                return "bloco ausente no cronograma";

            LocalDate data = OperacoesDeCronograma.dataOuNull (bloco.getData());
            if(data==null)
                return "data invalida: "+ bloco.getData();

            if(data.isBefore(segunda) || data.isAfter(domingo))
                return "data fora da semana " + cronograma.getSemanaInicio() + ": " + bloco.getData();

            // Set.of(...).contains(null) lanca NullPointerException
            if (bloco.getTurno()==null || !OperacoesDeCronograma.TURNOS.contains (bloco.getTurno()))
                return "turno invalido: "+ bloco.getTurno();
            
            if(OperacoesDeCronograma.vazia (bloco.getMateria()))
                return "materia ausente";

            if(bloco.getDuracaoMin()<=0)
                return "duracaoMin deve ser maior que zero: " + bloco.getDuracaoMin();

            List<Topico> topicos = bloco.getTopicos();
            if(topicos==null)
                return "topicos ausentes (use lista vazia)";

            for (Topico topico : topicos)
                if(topico==null || OperacoesDeCronograma.vazia (topico.getNome()))
                    return "topico sem nome";
        }
        return null;
    }

    // so aceita yyyy-MM-dd com data existente (ISO_LOCAL_DATE e estrito:
    // recusa 2026-02-30); devolve null em vez de lancar excecao
    private static LocalDate dataOuNull (String texto)
    {
        if (texto==null)
            return null;

        try
        {
            return LocalDate.parse (texto, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        catch (DateTimeParseException erro)
        {
            return null;
        }
    }

    private static boolean vazia (String texto)
    {
        return texto==null || texto.trim().isEmpty();
    }

    private static Bson daSemana (ObjectId usuarioId, String semanaInicio)
    {
        return Filters.and (Filters.eq ("usuarioId",    usuarioId),
                            Filters.eq ("semanaInicio", semanaInicio));
    }

    // o id que vier no Bloco e ignorado: o bloco e sempre inserido como novo
    // e o MongoDB gera o _id
    private static Document paraDocumento (Bloco bloco, ObjectId usuarioId, String semanaInicio)
    {
        ArrayList<Document> topicos = new ArrayList<Document> ();
        for (Topico topico : bloco.getTopicos())
            topicos.add (new Document ("nome",      topico.getNome())
                             .append  ("concluido", topico.isConcluido()));

        Date concluidoEm = bloco.isConcluido() && bloco.getConcluidoEm()>0
                         ? new Date (bloco.getConcluidoEm())
                         : null;

        return new Document ("usuarioId",    usuarioId)
                   .append  ("semanaInicio", semanaInicio)
                   .append  ("data",         bloco.getData())
                   .append  ("turno",        bloco.getTurno())
                   .append  ("materia",      bloco.getMateria())
                   .append  ("duracaoMin",   bloco.getDuracaoMin())
                   .append  ("descricao",    bloco.getDescricao())
                   .append  ("topicos",      topicos)
                   .append  ("concluido",    bloco.isConcluido())
                   .append  ("concluidoEm",  concluidoEm);
    }

    private static Bloco deDocumento (Document documento)
    {
        ArrayList<Topico> topicos = new ArrayList<Topico> ();
        for (Document topico : documento.getList ("topicos", Document.class, List.of()))
            topicos.add (new Topico (topico.getString ("nome"),
                                     Boolean.TRUE.equals (topico.getBoolean ("concluido"))));

        Date concluidoEm = documento.getDate ("concluidoEm");

        // Number: um documento editado a mao (Compass, mongosh) pode ter
        // guardado duracaoMin como double em vez de int
        return new Bloco (documento.getObjectId ("_id").toHexString(),
                          documento.getString ("data"),
                          documento.getString ("turno"),
                          documento.getString ("materia"),
                          documento.get ("duracaoMin", Number.class).intValue(),
                          documento.getString ("descricao"),
                          topicos,
                          Boolean.TRUE.equals (documento.getBoolean ("concluido")),
                          concluidoEm==null ? 0L : concluidoEm.getTime());
    }

    private static RespostaDeErro erroDeBanco (String operacao, Exception erro)
    {
        System.err.println ("Falha ao " + operacao + " cronograma no MongoDB: " + erro.getMessage());

        // nenhum servidor do MongoDB ao alcance: fora do ar, sem rede ou IP
        // nao liberado no Atlas
        if (erro instanceof MongoTimeoutException || erro instanceof MongoSocketException)
            return new RespostaDeErro (RespostaDeErro.BANCO_INDISPONIVEL,
                                       "MongoDB indisponivel");

        return new RespostaDeErro (RespostaDeErro.ERRO_INTERNO,
                                   "Falha ao " + operacao + " o cronograma");
    }
}
