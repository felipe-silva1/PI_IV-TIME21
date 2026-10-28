package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.PedidoDeStatus;
import br.edu.time21.comunicacao.PedidoParaSair;
import br.edu.time21.comunicacao.RespostaDeErro;
import br.edu.time21.comunicacao.RespostaDeStatus;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ClienteSocketTest
{
    @Test
    void enviaPedidoEDevolveRespostaDoServidor() throws Exception
    {
        try (ServerSocket servidor = new ServerSocket(0))
        {
            Thread atendente = new Thread(() -> atenderUmaConexao(servidor));
            atendente.start();

            ClienteSocket cliente =
                    new ClienteSocket("localhost", servidor.getLocalPort(), 1000);
            RespostaDeStatus resposta =
                    assertInstanceOf(RespostaDeStatus.class,
                                     cliente.enviar(new PedidoDeStatus()));

            assertEquals("ATIVO", resposta.getSituacao());
            atendente.join(1000);
        }
    }

    @Test
    void servidorIndisponivelDevolveErroTratado()
    {
        try (ServerSocket servidor = new ServerSocket(0))
        {
            int porta = servidor.getLocalPort();
            servidor.close();

            ClienteSocket cliente = new ClienteSocket("localhost", porta, 1000);
            RespostaDeErro resposta =
                    assertInstanceOf(RespostaDeErro.class,
                                     cliente.enviar(new PedidoDeStatus()));

            assertEquals(RespostaDeErro.ERRO_INTERNO, resposta.getCodigo());
        }
        catch (Exception erro)
        {
            throw new AssertionError("Nao foi possivel preparar o teste", erro);
        }
    }

    private void atenderUmaConexao(ServerSocket servidor)
    {
        try (Socket conexao = servidor.accept();
             ObjectOutputStream transmissor =
                     new ObjectOutputStream(conexao.getOutputStream()))
        {
            transmissor.flush();
            try (ObjectInputStream receptor =
                         new ObjectInputStream(conexao.getInputStream()))
            {
                assertInstanceOf(PedidoDeStatus.class, receptor.readObject());
                transmissor.writeObject(new RespostaDeStatus(
                        "ATIVO", 1, 123L, true));
                transmissor.flush();
                assertInstanceOf(PedidoParaSair.class, receptor.readObject());
            }
        }
        catch (Exception erro)
        {
            throw new AssertionError("Servidor de teste falhou", erro);
        }
    }
}
