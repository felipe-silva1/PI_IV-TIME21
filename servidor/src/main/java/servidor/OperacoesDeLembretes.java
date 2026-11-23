package servidor;

import br.edu.time21.comunicacao.LembreteDeEmail;
import br.edu.time21.comunicacao.PedidoParaAtualizarPreferenciaEmail;
import br.edu.time21.comunicacao.PedidoParaBuscarLembretesEmail;
import br.edu.time21.comunicacao.PedidoParaBuscarPreferenciaEmail;
import br.edu.time21.comunicacao.PedidoParaConfirmarEnvioEmail;
import br.edu.time21.comunicacao.Resposta;
import br.edu.time21.comunicacao.RespostaDeConfirmacaoEmail;
import br.edu.time21.comunicacao.RespostaDeErro;
import br.edu.time21.comunicacao.RespostaDeLembretesEmail;
import br.edu.time21.comunicacao.RespostaDePreferenciaEmail;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class OperacoesDeLembretes
{
    public static final String COLECAO_PREFERENCIAS = "preferencias_lembrete";
    private static final String CAMPO_ENVIO_EMAIL = "emailLembreteEnviadoEm";
    private static final Set<String> TURNOS_VALIDOS =
            Set.of("MANHA", "TARDE", "NOITE");

    private final MongoCollection<Document> preferencias;
    private final MongoCollection<Document> blocos;
    private final MongoCollection<Document> usuarios;
    private final String tokenBackend;

    public OperacoesDeLembretes(MongoDatabase banco, String tokenBackend)
    {
        if (banco == null)
            throw new IllegalArgumentException("Banco ausente");

        this.preferencias = banco.getCollection(COLECAO_PREFERENCIAS);
        this.blocos = banco.getCollection(OperacoesDeCronograma.COLECAO);
        this.usuarios = banco.getCollection("usuarios");
        this.tokenBackend = tokenBackend;
    }

    public void garantirIndices()
    {
        try
        {
            this.preferencias.createIndex(
                    Indexes.ascending("usuarioId"),
                    new IndexOptions().unique(true));
        }
        catch (Exception erro)
        {
            System.err.println("Falha ao criar índice de preferências de lembrete: " +
                    erro.getMessage());
        }
    }

    public Resposta buscarPreferenciaEmail(PedidoParaBuscarPreferenciaEmail pedido)
    {
        if (!autorizado(pedido.getToken()))
            return acessoNegado();

        ObjectId usuarioId = idValido(pedido.getUsuarioId());
        if (usuarioId == null)
            return new RespostaDeErro(RespostaDeErro.PEDIDO_INVALIDO, "usuarioId invalido");

        try
        {
            Document preferencia = this.preferencias.find(
                    Filters.eq("usuarioId", usuarioId)).first();
            return new RespostaDePreferenciaEmail(
                    preferencia != null && Boolean.TRUE.equals(preferencia.getBoolean("emailAtivo")));
        }
        catch (Exception erro)
        {
            return erroDeBanco("buscar preferencia de email", erro);
        }
    }

    public Resposta atualizarPreferenciaEmail(PedidoParaAtualizarPreferenciaEmail pedido)
    {
        if (!autorizado(pedido.getToken()))
            return acessoNegado();

        ObjectId usuarioId = idValido(pedido.getUsuarioId());
        if (usuarioId == null)
            return new RespostaDeErro(RespostaDeErro.PEDIDO_INVALIDO, "usuarioId invalido");

        try
        {
            this.preferencias.updateOne(
                    Filters.eq("usuarioId", usuarioId),
                    Updates.combine(
                            Updates.set("emailAtivo", pedido.isEmailAtivo()),
                            Updates.set("atualizadoEm", new Date())),
                    new UpdateOptions().upsert(true));
            return new RespostaDePreferenciaEmail(pedido.isEmailAtivo());
        }
        catch (Exception erro)
        {
            return erroDeBanco("atualizar preferencia de email", erro);
        }
    }

    public Resposta buscarLembretesEmail(PedidoParaBuscarLembretesEmail pedido)
    {
        if (!autorizado(pedido.getToken()))
            return acessoNegado();

        if (!dataValida(pedido.getData()) || pedido.getTurnos() == null ||
            pedido.getTurnos().isEmpty() ||
            pedido.getTurnos().stream().anyMatch(turno -> !TURNOS_VALIDOS.contains(turno)))
            return new RespostaDeErro(RespostaDeErro.PEDIDO_INVALIDO,
                    "data ou turnos invalidos");

        try
        {
            Bson filtroBlocos = Filters.and(
                    Filters.eq("data", pedido.getData()),
                    Filters.in("turno", pedido.getTurnos()),
                    Filters.ne("concluido", true),
                    Filters.or(
                            Filters.exists(CAMPO_ENVIO_EMAIL, false),
                            Filters.eq(CAMPO_ENVIO_EMAIL, null)));

            ArrayList<LembreteDeEmail> lembretes = new ArrayList<>();
            for (Document bloco : this.blocos.find(filtroBlocos))
            {
                ObjectId usuarioId = bloco.getObjectId("usuarioId");
                Document preferencia = this.preferencias.find(
                        Filters.and(
                                Filters.eq("usuarioId", usuarioId),
                                Filters.eq("emailAtivo", true))).first();
                if (preferencia == null)
                    continue;

                Document usuario = this.usuarios.find(Filters.eq("_id", usuarioId)).first();
                String email = usuario == null ? null : usuario.getString("email");
                if (email == null || email.isBlank())
                    continue;

                lembretes.add(new LembreteDeEmail(
                        bloco.getObjectId("_id").toHexString(),
                        usuarioId.toHexString(),
                        email,
                        bloco.getString("data"),
                        bloco.getString("turno"),
                        bloco.getString("materia")));
            }
            return new RespostaDeLembretesEmail(lembretes);
        }
        catch (Exception erro)
        {
            return erroDeBanco("buscar lembretes de email", erro);
        }
    }

    public Resposta confirmarEnvioEmail(PedidoParaConfirmarEnvioEmail pedido)
    {
        if (!autorizado(pedido.getToken()))
            return acessoNegado();

        ObjectId usuarioId = idValido(pedido.getUsuarioId());
        ObjectId blocoId = idValido(pedido.getBlocoId());
        if (usuarioId == null || blocoId == null)
            return new RespostaDeErro(RespostaDeErro.PEDIDO_INVALIDO,
                    "usuarioId ou blocoId invalido");

        try
        {
            Bson filtroPendente = Filters.and(
                    Filters.eq("_id", blocoId),
                    Filters.eq("usuarioId", usuarioId),
                    Filters.ne("concluido", true),
                    Filters.or(
                            Filters.exists(CAMPO_ENVIO_EMAIL, false),
                            Filters.eq(CAMPO_ENVIO_EMAIL, null)));
            boolean confirmado = this.blocos.updateOne(
                    filtroPendente,
                    Updates.set(CAMPO_ENVIO_EMAIL, new Date())).getMatchedCount() == 1;
            return new RespostaDeConfirmacaoEmail(confirmado);
        }
        catch (Exception erro)
        {
            return erroDeBanco("confirmar envio de email", erro);
        }
    }

    private static ObjectId idValido(String texto)
    {
        return texto != null && ObjectId.isValid(texto) ? new ObjectId(texto) : null;
    }

    private boolean autorizado(String token)
    {
        if (this.tokenBackend == null || this.tokenBackend.isBlank() ||
            token == null || token.isBlank())
            return false;
        return MessageDigest.isEqual(
                this.tokenBackend.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private static RespostaDeErro acessoNegado()
    {
        return new RespostaDeErro(
                RespostaDeErro.PEDIDO_INVALIDO, "operacao nao autorizada");
    }

    private static boolean dataValida(String texto)
    {
        if (texto == null)
            return false;
        try
        {
            return LocalDate.parse(texto, DateTimeFormatter.ISO_LOCAL_DATE) != null;
        }
        catch (DateTimeParseException erro)
        {
            return false;
        }
    }

    private static RespostaDeErro erroDeBanco(String operacao, Exception erro)
    {
        System.err.println("Falha ao " + operacao + ": " + erro.getMessage());
        if (erro instanceof MongoTimeoutException || erro instanceof MongoSocketException)
            return new RespostaDeErro(
                    RespostaDeErro.BANCO_INDISPONIVEL, "MongoDB indisponivel");
        return new RespostaDeErro(
                RespostaDeErro.ERRO_INTERNO, "Falha ao " + operacao);
    }
}
