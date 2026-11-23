//Autor: Arthur Berto de Castro RA: 25009917
package servidor;

import java.io.*;
import java.net.*;
import java.util.concurrent.Semaphore;

import br.edu.time21.comunicacao.Comunicado;

public class Parceiro
{
    // Recursos usados para a comunicação com o cliente.
    private Socket             conexao;
    private ObjectInputStream  receptor;
    private ObjectOutputStream transmissor;

    // Armazena temporariamente o próximo comunicado recebido.
    private Comunicado proximoComunicado=null;

    // Garante acesso seguro às operações de leitura.
    private Semaphore mutEx = new Semaphore (1,true);

    // Valida e configura os recursos da conexão.
    public Parceiro (Socket             conexao,
                     ObjectInputStream  receptor,
                     ObjectOutputStream transmissor)
                     throws Exception // se parametro nulos
    {
        if (conexao==null)
            throw new Exception ("Conexao ausente");

        if (receptor==null)
            throw new Exception ("Receptor ausente");

        if (transmissor==null)
            throw new Exception ("Transmissor ausente");

        this.conexao     = conexao;
        this.receptor    = receptor;
        this.transmissor = transmissor;
    }

    public String getEndereco ()
    {
        return String.valueOf (this.conexao.getRemoteSocketAddress());
    }

    // Envia um comunicado ao cliente de forma sincronizada.
    // synchronized: a supervisora e o Servidor (comando "desativar")
    // podem escrever no mesmo transmissor ao mesmo tempo
    public void receba (Comunicado x) throws Exception
    {
        try
        {
            synchronized (this.transmissor)
            {
                this.transmissor.writeObject (x);
                this.transmissor.flush       ();
            }
        }
        catch (IOException erro)
        {
            throw new Exception ("Erro de transmissao");
        }
    }

    // Consulta o próximo comunicado sem removê-lo da fila.
    public Comunicado espie () throws Exception
    {
        this.mutEx.acquireUninterruptibly();
        try
        {
            if (this.proximoComunicado==null)
                this.proximoComunicado = (Comunicado)this.receptor.readObject();

            return this.proximoComunicado;
        }
        catch (Exception erro)
        {
            throw new Exception ("Erro de recepcao");
        }
        finally
        {
            this.mutEx.release();
        }
    }

    // Lê e remove o próximo comunicado disponível.
    public Comunicado envie () throws Exception
    {
        this.mutEx.acquireUninterruptibly();
        try
        {
            if (this.proximoComunicado==null)
                this.proximoComunicado = (Comunicado)this.receptor.readObject();

            Comunicado ret         = this.proximoComunicado;
            this.proximoComunicado = null;
            return ret;
        }
        catch (Exception erro)
        {
            throw new Exception ("Erro de recepcao");
        }
        finally
        {
            this.mutEx.release();
        }
    }

    // fecha cada recurso separadamente: se um falhar, os outros ainda sao fechados
    public void adeus () throws Exception
    {
        boolean falhou=false;

        try { this.transmissor.close(); } catch (Exception erro) { falhou=true; }
        try { this.receptor   .close(); } catch (Exception erro) { falhou=true; }
        try { this.conexao    .close(); } catch (Exception erro) { falhou=true; }

        if (falhou)
            throw new Exception ("Erro de desconexao");
    }
}
