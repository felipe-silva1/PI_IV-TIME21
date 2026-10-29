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
import java.util.concurrent.TimeUnit;

// Classe que interage com o MongoDB
public class RepositorioDeChamadas
{
    public static String BANCO_PADRAO    = "servidor";
    public static String COLECAO_PADRAO  = "chamadas";

    private MongoClient               cliente;
    private MongoDatabase             banco;
    private MongoCollection<Document> colecao;
    private MongoCollection<Document> usuarios;

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
                    b.serverSelectionTimeout (10, TimeUnit.SECONDS))
                .build();

            // conexao nao falha se banco estiver fora do ar, usar estaDisponivel() para pegar status do banco
            this.cliente = MongoClients.create (configuracao);
            this.banco   = this.cliente.getDatabase (nomeDoBanco);
            this.colecao = this.banco.getCollection (RepositorioDeChamadas.COLECAO_PADRAO);
            this.usuarios = this.banco.getCollection ("usuarios");
        }
        catch (Exception erro)
        {
            throw new Exception ("URI do MongoDB invalida");
        }
    }

    public String cadastrarUsuario(String nome, String email, String senhaHash)
    {
        this.usuarios.createIndex(Indexes.ascending("email"),
                                  new IndexOptions().unique(true));
        Document usuario = new Document("nome", nome)
                .append("email", email)
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
        return this.usuarios.find(new Document("email", email)).first();
    }

    public void registrar (String tipo, String origem, String detalhes)
    {
        try
        {
            this.colecao.insertOne (
                new Document ("tipo",     tipo)
                    .append  ("origem",   origem)
                    .append  ("detalhes", detalhes)
                    .append  ("dataHora", new Date()));
                    
            // DEBUG: Print no console para todo dado enviado ao mongoDB com sucesso    
            System.out.println ("Dado enviado com sucesso para o MongoDB: tipo=" + tipo + ", origem=" + origem);
        }
        catch (Exception erro)
        {
            System.err.println ("Falha ao gravar no MongoDB: " + erro.getMessage());
        }
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

    public void fechar ()
    {
        try
        {
            this.cliente.close();
        }
        catch (Exception erro)
        {}
    }
}
