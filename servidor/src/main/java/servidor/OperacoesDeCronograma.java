package servidor;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
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

    private static final Set<String> TURNOS = Set.of (TurnoLivre.MANHA, TurnoLivre.TARDE, TurnoLivre.NOITE);
    private MongoCollection<Document> colecao;

    public OperacoesDeCronograma (MongoDatabase banco) throws Exception
    {
        if (banco==null)
            throw new Exception ("Banco ausente");

        this.colecao = banco.getCollection (OperacoesDeCronograma.COLECAO);
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
        }
        catch (Exception erro)
        {
            System.err.println("Falha ao criar indices em "+ OperacoesDeCronograma.COLECAO + ": " + erro.getMessage());
        }
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
        if(cronograma.getUsuarioId()==null || !ObjectId.isValid (cronograma.getUsuarioId()))
            return "usuarioId invalido: " + cronograma.getUsuarioId();

        LocalDate segunda = OperacoesDeCronograma.dataOuNull (cronograma.getSemanaInicio());
        if (segunda==null)
            return "semanaInicio invalida: "+cronograma.getSemanaInicio();

        if (segunda.getDayOfWeek()!=DayOfWeek.MONDAY)
            return "semanaInicio nao e segunda-feira: "+cronograma.getSemanaInicio();

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
