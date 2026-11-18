package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.Bloco;
import br.edu.time21.comunicacao.PedidoParaAtualizarTarefa;
import br.edu.time21.comunicacao.PedidoParaBuscarTarefas;
import br.edu.time21.comunicacao.Resposta;
import br.edu.time21.comunicacao.RespostaDeErro;
import br.edu.time21.comunicacao.RespostaDeTarefa;
import br.edu.time21.comunicacao.RespostaDeTarefas;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class TarefaService
{
    private final ClienteSocket clienteSocket;

    public TarefaService(ClienteSocket clienteSocket)
    {
        this.clienteSocket = clienteSocket;
    }

    public List<Bloco> buscarPorDia(String usuarioId, String data)
    {
        validarUsuario(usuarioId);

        if (!dataValida(data))
            throw new TarefaException(400, "Data inválida.");

        Resposta resposta =
                this.clienteSocket.enviar(new PedidoParaBuscarTarefas(usuarioId, data));

        if (resposta instanceof RespostaDeTarefas respostaDeTarefas)
            return respostaDeTarefas.getTarefas() == null
                    ? List.of()
                    : List.copyOf(respostaDeTarefas.getTarefas());

        if (resposta instanceof RespostaDeErro erro)
            throw traduzirErro(erro);

        throw new TarefaException(503, "Não foi possível consultar as tarefas.");
    }

    public Bloco atualizarStatus(String usuarioId, String tarefaId, Boolean concluido)
    {
        validarUsuario(usuarioId);

        if (tarefaId == null || tarefaId.isBlank())
            throw new TarefaException(400, "Tarefa inválida.");

        if (concluido == null)
            throw new TarefaException(400, "Informe o novo status da tarefa.");

        Resposta resposta =
                this.clienteSocket.enviar(
                        new PedidoParaAtualizarTarefa(usuarioId, tarefaId, concluido));

        if (resposta instanceof RespostaDeTarefa respostaDeTarefa &&
            respostaDeTarefa.getTarefa() != null)
            return respostaDeTarefa.getTarefa();

        if (resposta instanceof RespostaDeErro erro)
            throw traduzirErro(erro);

        throw new TarefaException(503, "Não foi possível atualizar a tarefa.");
    }

    private static void validarUsuario(String usuarioId)
    {
        if (usuarioId == null || usuarioId.isBlank())
            throw new TarefaException(401, "Sessão inválida ou expirada.");
    }

    private static boolean dataValida(String data)
    {
        if (data == null)
            return false;

        try
        {
            LocalDate.parse(data);
            return true;
        }
        catch (DateTimeParseException erro)
        {
            return false;
        }
    }

    private static TarefaException traduzirErro(RespostaDeErro erro)
    {
        if (RespostaDeErro.PEDIDO_INVALIDO.equals(erro.getCodigo()))
            return new TarefaException(400, "Dados da tarefa inválidos.");
        if (RespostaDeErro.BANCO_INDISPONIVEL.equals(erro.getCodigo()))
            return new TarefaException(503, "Não foi possível acessar o banco de dados.");
        return new TarefaException(503, "Não foi possível acessar o serviço de tarefas.");
    }

    public static class TarefaException extends RuntimeException
    {
        private final int status;

        public TarefaException(int status, String mensagem)
        {
            super(mensagem);
            this.status = status;
        }

        public int getStatus()
        {
            return this.status;
        }
    }
}
