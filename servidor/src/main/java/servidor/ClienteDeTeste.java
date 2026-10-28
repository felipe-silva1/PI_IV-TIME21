package servidor;

import java.io.*;
import java.net.*;

// Simula o backend: java ClienteDeTeste [HOST] [PORTA]
public class ClienteDeTeste
{
    public static void main (String[] args) throws Exception
    {
        String host  = args.length>0 ? args[0] : "localhost";
        int    porta = args.length>1 ? Integer.parseInt(args[1]) : 3000;

        try (Socket conexao = new Socket (host, porta))
        {
            ObjectOutputStream transmissor = new ObjectOutputStream (conexao.getOutputStream());
            transmissor.flush();
            ObjectInputStream  receptor    = new ObjectInputStream  (conexao.getInputStream());

            transmissor.writeObject (new PedidoDeStatus());
            transmissor.flush();

            Comunicado resposta = (Comunicado)receptor.readObject();
            System.out.println ("Resposta: " + resposta);

            transmissor.writeObject (new PedidoParaSair());
            transmissor.flush();
        }
    }
}
