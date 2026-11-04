package servidor;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

import br.edu.time21.comunicacao.*;

// atende os pedidos de rotina lendo e gravando na colecao perfis (ver
// docs/dicionario-banco.md): um documento por usuario, achado por usuarioId.
// nao guarda estado alem da colecao, que e thread-safe, entao uma unica
// instancia e compartilhada por todas as SupervisorasDeConexao.
public class OperacoesDeRotina
{
    public static final String COLECAO = "perfis";

    private static final Set<String> TURNOS =
        Set.of (TurnoLivre.MANHA, TurnoLivre.TARDE, TurnoLivre.NOITE);

    private MongoCollection<Document> colecao;

    public OperacoesDeRotina (MongoDatabase banco) throws Exception
    {
        if (banco==null)
            throw new Exception ("Banco ausente");

        this.colecao = banco.getCollection (OperacoesDeRotina.COLECAO);
    }

    // o indice unico em usuarioId garante um perfil por usuario mesmo que
    // dois pedidos de salvar cheguem juntos. createIndex nao faz nada se o
    // indice ja existe; se o banco estiver fora do ar, so avisa no console
    public void garantirIndices ()
    {
        try
        {
            this.colecao.createIndex (Indexes.ascending ("usuarioId"),
                                      new IndexOptions().unique (true));
        }
        catch (Exception erro)
        {
            System.err.println ("Falha ao criar indice em " + OperacoesDeRotina.COLECAO +
                                ": " + erro.getMessage());
        }
    }

    public Resposta salvar (PedidoParaSalvarRotina pedido)
    {
        Rotina rotina   = pedido.getRotina();
        String problema = OperacoesDeRotina.problemaEm (rotina);
        if (problema!=null)
            return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO, problema);

        try
        {
            ObjectId usuarioId = new ObjectId (rotina.getUsuarioId());
            Date     agora     = new Date ();

            // upsert: insere se o usuario ainda nao tem perfil; se ja tem,
            // substitui o documento inteiro (o _id e mantido)
            this.colecao.replaceOne (Filters.eq ("usuarioId", usuarioId),
                                     OperacoesDeRotina.paraDocumento (rotina, usuarioId, agora),
                                     new ReplaceOptions().upsert (true));

            return new RespostaDeRotina (
                new Rotina (usuarioId.toHexString(),
                            rotina.getVestibular(),
                            rotina.getDisponibilidade(),
                            rotina.getMateriasPrioritarias(),
                            agora.getTime()));
        }
        catch (Exception erro)
        {
            return OperacoesDeRotina.erroDeBanco ("salvar", erro);
        }
    }

    public Resposta buscar (PedidoParaBuscarRotina pedido)
    {
        String usuarioId = pedido.getUsuarioId();
        if (usuarioId==null || !ObjectId.isValid (usuarioId))
            return new RespostaDeErro (RespostaDeErro.PEDIDO_INVALIDO,
                                       "usuarioId invalido: " + usuarioId);

        try
        {
            Document documento =
                this.colecao.find (Filters.eq ("usuarioId", new ObjectId (usuarioId))).first();

            return new RespostaDeRotina (
                documento==null ? null : OperacoesDeRotina.deDocumento (documento));
        }
        catch (Exception erro)
        {
            return OperacoesDeRotina.erroDeBanco ("buscar", erro);
        }
    }

    // regras do dicionario que o servidor garante antes de gravar; devolve
    // null se a rotina esta ok. a validacao voltada ao usuario (vestibular
    // existente, materias do vestibular, mensagens para a tela) e do backend
    private static String problemaEm (Rotina rotina)
    {
        if (rotina==null)
            return "rotina ausente";

        if (rotina.getUsuarioId()==null || !ObjectId.isValid (rotina.getUsuarioId()))
            return "usuarioId invalido: " + rotina.getUsuarioId();

        if (OperacoesDeRotina.vazia (rotina.getVestibular()))
            return "vestibular ausente";

        List<TurnoLivre> disponibilidade = rotina.getDisponibilidade();
        if (disponibilidade==null || disponibilidade.isEmpty())
            return "disponibilidade sem nenhum turno";

        Set<TurnoLivre> turnosVistos = new HashSet<TurnoLivre> ();
        for (TurnoLivre turno : disponibilidade)
        {
            if (turno==null)
                return "turno ausente na disponibilidade";

            if (turno.getDiaSemana()<1 || turno.getDiaSemana()>7)
                return "diaSemana fora de 1 a 7: " + turno.getDiaSemana();

            // Set.of(...).contains(null) lanca NullPointerException
            if (turno.getTurno()==null || !OperacoesDeRotina.TURNOS.contains (turno.getTurno()))
                return "turno invalido: " + turno.getTurno();

            if (!turnosVistos.add (turno))
                return "turno repetido na disponibilidade: " + turno;
        }

        List<String> materias = rotina.getMateriasPrioritarias();
        if (materias==null || materias.isEmpty())
            return "nenhuma materia prioritaria";

        Set<String> materiasVistas = new HashSet<String> ();
        for (String materia : materias)
        {
            if (OperacoesDeRotina.vazia (materia))
                return "materia ausente";

            if (!materiasVistas.add (materia))
                return "materia repetida: " + materia;
        }

        return null;
    }

    private static boolean vazia (String texto)
    {
        return texto==null || texto.trim().isEmpty();
    }

    private static Document paraDocumento (Rotina rotina, ObjectId usuarioId, Date agora)
    {
        ArrayList<Document> disponibilidade = new ArrayList<Document> ();
        for (TurnoLivre turno : rotina.getDisponibilidade())
            disponibilidade.add (new Document ("diaSemana", turno.getDiaSemana())
                                     .append  ("turno",     turno.getTurno()));

        return new Document ("usuarioId",            usuarioId)
                   .append  ("vestibular",           rotina.getVestibular())
                   .append  ("disponibilidade",      disponibilidade)
                   .append  ("materiasPrioritarias", rotina.getMateriasPrioritarias())
                   .append  ("atualizadoEm",         agora);
    }

    private static Rotina deDocumento (Document documento)
    {
        ArrayList<TurnoLivre> disponibilidade = new ArrayList<TurnoLivre> ();
        for (Document turno : documento.getList ("disponibilidade", Document.class, List.of()))
            // Number: um documento editado a mao (Compass, mongosh) pode
            // ter guardado diaSemana como double em vez de int
            disponibilidade.add (new TurnoLivre (turno.get ("diaSemana", Number.class).intValue(),
                                                 turno.getString ("turno")));

        Date atualizadoEm = documento.getDate ("atualizadoEm");

        return new Rotina (documento.getObjectId ("usuarioId").toHexString(),
                           documento.getString ("vestibular"),
                           disponibilidade,
                           documento.getList ("materiasPrioritarias", String.class),
                           atualizadoEm==null ? 0L : atualizadoEm.getTime());
    }

    private static RespostaDeErro erroDeBanco (String operacao, Exception erro)
    {
        System.err.println ("Falha ao " + operacao + " rotina no MongoDB: " + erro.getMessage());

        // nenhum servidor do MongoDB ao alcance: fora do ar, sem rede ou IP
        // nao liberado no Atlas
        if (erro instanceof MongoTimeoutException || erro instanceof MongoSocketException)
            return new RespostaDeErro (RespostaDeErro.BANCO_INDISPONIVEL,
                                       "MongoDB indisponivel");

        return new RespostaDeErro (RespostaDeErro.ERRO_INTERNO,
                                   "Falha ao " + operacao + " a rotina");
    }
}
