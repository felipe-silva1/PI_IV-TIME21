package servidor;

import br.edu.time21.comunicacao.Bloco;
import br.edu.time21.comunicacao.Cronograma;
import br.edu.time21.comunicacao.PedidoParaSalvarCronograma;
import br.edu.time21.comunicacao.Resposta;
import br.edu.time21.comunicacao.RespostaDeCronograma;
import br.edu.time21.comunicacao.RespostaDeErro;
import br.edu.time21.comunicacao.Topico;
import br.edu.time21.comunicacao.TurnoLivre;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OperacoesDeCronogramaInterclasseTest
{
    private static final String USUARIO_ID = "65f1c2a4e1b2c3d4e5f60718";
    private static final String SEGUNDA = "2026-11-02";

    @SuppressWarnings("unchecked")
    private MongoCollection<Document> mockColecao()
    {
        return mock(MongoCollection.class);
    }

    @Test
    void salvaCronogramaValidoEDevolveBlocosComIdsGerados() throws Exception
    {
        MongoDatabase banco = mock(MongoDatabase.class);
        MongoCollection<Document> blocos = mockColecao();
        MongoCollection<Document> pomodoros = mockColecao();
        when(banco.getCollection(OperacoesDeCronograma.COLECAO)).thenReturn(blocos);
        when(banco.getCollection("sessoes_pomodoro")).thenReturn(pomodoros);

        ObjectId idGerado = new ObjectId();
        doAnswer(chamada -> {
            List<Document> docs = chamada.getArgument(0);
            for (Document doc : docs)
                doc.append("_id", idGerado);
            return null;
        }).when(blocos).insertMany(anyList());

        OperacoesDeCronograma operacoes = new OperacoesDeCronograma(banco);
        Topico topico = new Topico("Funcoes", false);
        Bloco bloco = new Bloco("2026-11-03", TurnoLivre.NOITE, "Matematica", 90, List.of(topico));
        Cronograma cronograma = new Cronograma(USUARIO_ID, SEGUNDA, List.of(bloco));

        Resposta resposta = operacoes.salvar(new PedidoParaSalvarCronograma(cronograma));

        verify(blocos).deleteMany(any(Bson.class));
        verify(blocos).insertMany(anyList());

        RespostaDeCronograma ok = assertInstanceOf(RespostaDeCronograma.class, resposta);
        Cronograma salvo = ok.getCronograma();
        assertEquals(USUARIO_ID, salvo.getUsuarioId());
        assertEquals(SEGUNDA, salvo.getSemanaInicio());
        assertEquals(1, salvo.getBlocos().size());
        assertEquals(idGerado.toHexString(), salvo.getBlocos().get(0).getId());
        assertEquals("Matematica", salvo.getBlocos().get(0).getMateria());
        assertEquals("Funcoes", salvo.getBlocos().get(0).getTopicos().get(0).getNome());
        assertFalse(salvo.getBlocos().get(0).isConcluido());
    }

    @Test
    void recusaBlocoComDataForaDaSemana() throws Exception
    {
        MongoDatabase banco = mock(MongoDatabase.class);
        MongoCollection<Document> blocos = mockColecao();
        when(banco.getCollection(OperacoesDeCronograma.COLECAO)).thenReturn(blocos);
        when(banco.getCollection("sessoes_pomodoro")).thenReturn(mockColecao());

        OperacoesDeCronograma operacoes = new OperacoesDeCronograma(banco);
        Bloco blocoFora = new Bloco("2026-11-15", TurnoLivre.MANHA, "Fisica", 90, List.of());
        Cronograma cronograma = new Cronograma(USUARIO_ID, SEGUNDA, List.of(blocoFora));

        Resposta resposta = operacoes.salvar(new PedidoParaSalvarCronograma(cronograma));

        RespostaDeErro erro = assertInstanceOf(RespostaDeErro.class, resposta);
        assertEquals(RespostaDeErro.PEDIDO_INVALIDO, erro.getCodigo());
        assertTrue(erro.getMensagem().contains("data fora da semana"));
        verifyNoInteractions(blocos);
    }

    @Test
    void devolveBancoIndisponivelQuandoMongoDaTimeout() throws Exception
    {
        MongoDatabase banco = mock(MongoDatabase.class);
        MongoCollection<Document> blocos = mockColecao();
        when(banco.getCollection(OperacoesDeCronograma.COLECAO)).thenReturn(blocos);
        when(banco.getCollection("sessoes_pomodoro")).thenReturn(mockColecao());
        when(blocos.deleteMany(any(Bson.class)))
                .thenThrow(new MongoTimeoutException("timeout"));

        OperacoesDeCronograma operacoes = new OperacoesDeCronograma(banco);
        Bloco bloco = new Bloco("2026-11-03", TurnoLivre.MANHA, "Quimica", 60, List.of());
        Cronograma cronograma = new Cronograma(USUARIO_ID, SEGUNDA, List.of(bloco));

        Resposta resposta = operacoes.salvar(new PedidoParaSalvarCronograma(cronograma));

        RespostaDeErro erro = assertInstanceOf(RespostaDeErro.class, resposta);
        assertEquals(RespostaDeErro.BANCO_INDISPONIVEL, erro.getCodigo());
    }

    @Test
    void lancaExcecaoQuandoBancoEstaAusente()
    {
        Exception erro = assertThrows(Exception.class, () -> new OperacoesDeCronograma(null));
        assertEquals("Banco ausente", erro.getMessage());
    }
}