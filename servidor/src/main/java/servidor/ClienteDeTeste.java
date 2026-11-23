//Autor: Arthur Berto de Castro RA: 25009917
package servidor;

import java.io.*;
import java.net.*;

import br.edu.time21.comunicacao.Comunicado;
import br.edu.time21.comunicacao.PedidoDeStatus;
import br.edu.time21.comunicacao.PedidoParaSair;

// Simula o backend: java ClienteDeTeste [HOST] [PORTA]
public class ClienteDeTeste
{
    public static void main (String[] args) throws Exception
    {
        // Define o servidor usando argumentos ou valores padrão.
        String host  = args.length>0 ? args[0] : "localhost";
        int    porta = args.length>1 ? Integer.parseInt(args[1]) : 3000;

        try (Socket conexao = new Socket (host, porta))
        {
            // Configura os fluxos para enviar e receber objetos.
            ObjectOutputStream transmissor = new ObjectOutputStream (conexao.getOutputStream());
            transmissor.flush();
            ObjectInputStream  receptor    = new ObjectInputStream  (conexao.getInputStream());

            // Solicita o status atual do servidor.
            transmissor.writeObject (new PedidoDeStatus());
            transmissor.flush();

            // Recebe e exibe a resposta do servidor.
            Comunicado resposta = (Comunicado)receptor.readObject();
            System.out.println ("Resposta: " + resposta);

            // Envia o pedido de encerramento da conexão.
            transmissor.writeObject (new PedidoParaSair());
            transmissor.flush();
        }
    }
}
