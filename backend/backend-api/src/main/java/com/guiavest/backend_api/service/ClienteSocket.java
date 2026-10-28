package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.Comunicado;
import br.edu.time21.comunicacao.Pedido;
import br.edu.time21.comunicacao.PedidoParaSair;
import br.edu.time21.comunicacao.Resposta;
import br.edu.time21.comunicacao.RespostaDeErro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Service
public class ClienteSocket
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClienteSocket.class);

    private final String endereco;
    private final int porta;
    private final int timeoutEmMilissegundos;

    public ClienteSocket(
            @Value("${servidor.endereco:localhost}") String endereco,
            @Value("${servidor.porta:3000}") int porta,
            @Value("${servidor.timeout-ms:5000}") int timeoutEmMilissegundos)
    {
        if (endereco == null || endereco.isBlank())
            throw new IllegalArgumentException("Endereco do servidor ausente");
        if (porta < 1 || porta > 65535)
            throw new IllegalArgumentException("Porta do servidor invalida");
        if (timeoutEmMilissegundos < 1)
            throw new IllegalArgumentException("Timeout do servidor invalido");

        this.endereco = endereco;
        this.porta = porta;
        this.timeoutEmMilissegundos = timeoutEmMilissegundos;
    }

    public Resposta enviar(Pedido pedido)
    {
        if (pedido == null)
            throw new IllegalArgumentException("Pedido ausente");

        try (Socket conexao = new Socket())
        {
            conexao.connect(new java.net.InetSocketAddress(this.endereco, this.porta),
                            this.timeoutEmMilissegundos);
            conexao.setSoTimeout(this.timeoutEmMilissegundos);

            try (ObjectOutputStream transmissor =
                         new ObjectOutputStream(conexao.getOutputStream()))
            {
                transmissor.flush();

                try (ObjectInputStream receptor =
                             new ObjectInputStream(conexao.getInputStream()))
                {
                    transmissor.writeObject(pedido);
                    transmissor.flush();

                    Comunicado comunicado = (Comunicado) receptor.readObject();
                    if (!(comunicado instanceof Resposta resposta))
                    {
                        String tipo = comunicado == null
                                ? "nulo"
                                : comunicado.getClass().getName();
                        LOGGER.warn("Servidor devolveu um comunicado inesperado: {}",
                                    tipo);
                        return erroDeComunicacao();
                    }

                    transmissor.writeObject(new PedidoParaSair());
                    transmissor.flush();
                    return resposta;
                }
            }
        }
        catch (IOException | ClassNotFoundException | ClassCastException erro)
        {
            LOGGER.warn("Nao foi possivel comunicar com o servidor {}:{}",
                        this.endereco, this.porta, erro);
            return erroDeComunicacao();
        }
    }

    private RespostaDeErro erroDeComunicacao()
    {
        return new RespostaDeErro(
                RespostaDeErro.ERRO_INTERNO,
                "Nao foi possivel comunicar com o servidor");
    }
}
