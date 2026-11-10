package servidor;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.MongoWriteException;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.*;

// Classe que interage com o MongoDB
public class RepositorioDeChamadas
{
    public static String BANCO_PADRAO    = "servidor";
    public static String COLECAO_PADRAO  = "chamadas";

    // quanto uma operacao espera o MongoDB antes de desistir. com o banco
    // fora do ar, toda operacao espera esse tempo inteiro, entao ele tem de
    // ficar abaixo do timeout do backend (SERVIDOR_TIMEOUT_MS, 5 s por
    // padrao): so assim o backend recebe a RespostaDeErro/RespostaDeStatus
    // antes de desistir da conexao
    public static final int TIMEOUT_DO_BANCO_EM_SEGUNDOS = 3;

    // limite de registros esperando para ser gravados (banco fora do ar por
    // muito tempo); passando disso, os novos sao descartados
    private static final int TAMANHO_DA_FILA = 1000;

    private MongoClient               cliente;
    private MongoDatabase             banco;
    private MongoCollection<Document> colecao;
    private MongoCollection<Document> usuarios;
    private boolean indiceEmailCriado;

    // grava os registros em segundo plano, um por vez e na ordem em que
    // chegaram, para que uma gravacao lenta nao atrase a resposta ao backend
    private ThreadPoolExecutor        gravador;

    public RepositorioDeChamadas (String uri, String nomeDoBanco)
    throws Exception
    {
        if (uri==null)
            throw new Exception ("URI do MongoDB ausente");

        if (nomeDoBanco==null)
            throw new Exception ("Nome do banco ausente");

        try
        {
            MongoClientSettings configuracao =
            MongoClientSettings.builder()
                .applyConnectionString (new ConnectionString (uri))
                .applyToClusterSettings (b ->
                    b.serverSelectionTimeout (RepositorioDeChamadas.TIMEOUT_DO_BANCO_EM_SEGUNDOS,
                                              TimeUnit.SECONDS))
                .build();

            // conexao nao falha se banco estiver fora do ar, usar estaDisponivel() para pegar status do banco
            this.cliente = MongoClients.create (configuracao);
            this.banco   = this.cliente.getDatabase (nomeDoBanco);
            this.colecao = this.banco.getCollection (RepositorioDeChamadas.COLECAO_PADRAO);
            this.usuarios = this.cliente.getDatabase(Servidor.BANCO_GUIAVEST).getCollection("usuarios");
        }
        catch (Exception erro)
        {
            throw new Exception ("URI do MongoDB invalida");
        }

        // uma unica thread, daemon para nunca impedir o fim do programa
        this.gravador =
        new ThreadPoolExecutor (1, 1, 0L, TimeUnit.MILLISECONDS,
                                new ArrayBlockingQueue<Runnable> (RepositorioDeChamadas.TAMANHO_DA_FILA),
                                tarefa ->
                                {
                                    Thread thread = new Thread (tarefa, "gravador-de-chamadas");
                                    thread.setDaemon (true);
                                    return thread;
                                });
    }

    public String cadastrarUsuario(String nome, String email, String senhaHash)
    {
        this.criarIndiceUnicoDeEmail();
        String emailNormalizado = normalizarEmail(email);
        Document usuario = new Document("nome", nome)
                .append("email", emailNormalizado)
                .append("senhaHash", senhaHash)
                .append("criadoEm", new Date());

        try
        {
            this.usuarios.insertOne(usuario);
            return ((ObjectId) usuario.get("_id")).toHexString();
        }
        catch (MongoWriteException erro)
        {
            if (erro.getError().getCode() == 11000)
                return null;
            throw erro;
        }
    }

    public Document buscarUsuario(String email)
    {
        return this.usuarios.find(new Document("email", normalizarEmail(email))).first();
    }

    private synchronized void criarIndiceUnicoDeEmail()
    {
        if (this.indiceEmailCriado)
            return;

        this.usuarios.createIndex(Indexes.ascending("email"),
                                  new IndexOptions().unique(true));
        this.indiceEmailCriado = true;
    }

    private static String normalizarEmail(String email)
    {
        if (email == null)
            throw new IllegalArgumentException("E-mail ausente");

        return email.trim().toLowerCase(Locale.ROOT);
    }

    public void registrar (String tipo, String origem, String detalhes)
    {
        // dataHora e o momento do evento, nao o da gravacao
        Document registro =
            new Document ("tipo",     tipo)
                .append  ("origem",   origem)
                .append  ("detalhes", detalhes)
                .append  ("dataHora", new Date());

        try
        {
            this.gravador.execute (() -> this.gravar (registro));
        }
        catch (RejectedExecutionException erro)
        {
            // fila cheia, ou repositorio ja fechado
            System.err.println ("Registro descartado: tipo=" + tipo + ", origem=" + origem);
        }
    }

    private void gravar (Document registro)
    {
        try
        {
            this.colecao.insertOne (registro);

            // DEBUG: Print no console para todo dado enviado ao mongoDB com sucesso
            System.out.println ("Dado enviado com sucesso para o MongoDB: tipo=" + registro.getString ("tipo") +
                                ", origem=" + registro.getString ("origem"));
        }
        catch (Exception erro)
        {
            System.err.println ("Falha ao gravar no MongoDB: " + erro.getMessage());
        }
    }

    // os outros bancos (como o da aplicacao, usado por OperacoesDeRotina)
    // sao abertos pelo mesmo cliente, para compartilhar o pool de conexoes
    public MongoDatabase getBanco (String nome)
    {
        return this.cliente.getDatabase (nome);
    }

    public boolean estaDisponivel ()
    {
        try
        {
            this.banco.runCommand (new Document ("ping", 1));
            return true;
        }
        catch (Exception erro)
        {
            return false;
        }
    }

    // da um tempo para os registros pendentes (como SERVIDOR_DESATIVADO)
    // serem gravados; o que nao der tempo e descartado antes de fechar
    public void fechar ()
    {
        this.gravador.shutdown();
        try
        {
            if (!this.gravador.awaitTermination (RepositorioDeChamadas.TIMEOUT_DO_BANCO_EM_SEGUNDOS + 2,
                                                 TimeUnit.SECONDS))
                this.gravador.shutdownNow();
        }
        catch (InterruptedException erro)
        {
            this.gravador.shutdownNow();
        }

        try
        {
            this.cliente.close();
        }
        catch (Exception erro)
        {}
    }
}
