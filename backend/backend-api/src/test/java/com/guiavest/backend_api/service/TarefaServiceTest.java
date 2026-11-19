//Autor: Edson Luan Rocha RA: 25897620
package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.Bloco;
import br.edu.time21.comunicacao.PedidoParaAtualizarTarefa;
import br.edu.time21.comunicacao.PedidoParaBuscarTarefas;
import br.edu.time21.comunicacao.RespostaDeTarefa;
import br.edu.time21.comunicacao.RespostaDeTarefas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TarefaServiceTest
{
    @Test
    void buscarPorDiaEnviaPedidoComUsuarioEData()
    {
        ClienteSocket clienteSocket = mock(ClienteSocket.class);
        when(clienteSocket.enviar(any(PedidoParaBuscarTarefas.class)))
                .thenReturn(new RespostaDeTarefas(List.of()));
        TarefaService servico = new TarefaService(clienteSocket);

        List<Bloco> tarefas =
                servico.buscarPorDia("65f1c2a4e1b2c3d4e5f60718", "2026-11-18");

        assertTrue(tarefas.isEmpty());
        var pedido = forClass(PedidoParaBuscarTarefas.class);
        verify(clienteSocket).enviar(pedido.capture());
        assertEquals("65f1c2a4e1b2c3d4e5f60718", pedido.getValue().getUsuarioId());
        assertEquals("2026-11-18", pedido.getValue().getData());
    }

    @Test
    void atualizarStatusEnviaPedidoDeConclusao()
    {
        ClienteSocket clienteSocket = mock(ClienteSocket.class);
        Bloco tarefa = new Bloco(
                "65f1c4d9e1b2c3d4e5f6071a",
                "2026-11-18", "NOITE", "Matematica", 60, List.of(),
                true, 1784476800000L);
        when(clienteSocket.enviar(any(PedidoParaAtualizarTarefa.class)))
                .thenReturn(new RespostaDeTarefa(tarefa));
        TarefaService servico = new TarefaService(clienteSocket);

        Bloco atualizada = servico.atualizarStatus(
                "65f1c2a4e1b2c3d4e5f60718",
                "65f1c4d9e1b2c3d4e5f6071a",
                true);

        assertTrue(atualizada.isConcluido());
        var pedido = forClass(PedidoParaAtualizarTarefa.class);
        verify(clienteSocket).enviar(pedido.capture());
        assertEquals("65f1c4d9e1b2c3d4e5f6071a", pedido.getValue().getTarefaId());
        assertTrue(pedido.getValue().isConcluido());
    }
}
