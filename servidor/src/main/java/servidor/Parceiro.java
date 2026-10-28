package servidor;

import java.io.*;
import java.net.*;
import java.util.concurrent.Semaphore;

import br.edu.time21.comunicacao.Comunicado;

public class Parceiro
{
    private Socket             conexao;
    private ObjectInputStream  receptor;
    private ObjectOutputStream transmissor;

    private Comunicado proximoComunicado=null;

    private Semaphore mutEx = new Semaphore (1,true);

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
