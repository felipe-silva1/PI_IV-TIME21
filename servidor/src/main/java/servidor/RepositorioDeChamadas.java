package servidor;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

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
        }
        catch (Exception erro)
        {
            throw new Exception ("URI do MongoDB invalida");
        }
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
