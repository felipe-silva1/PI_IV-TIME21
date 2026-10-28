package servidor;

import java.net.*;
import java.util.*;

public class AceitadoraDeConexao extends Thread
{
    private ServerSocket           pedido;
    private ArrayList<Parceiro>    usuarios;
    private RepositorioDeChamadas  repositorio;

    public AceitadoraDeConexao
    (String porta, ArrayList<Parceiro> usuarios, RepositorioDeChamadas repositorio)
    throws Exception
    {
        if (porta==null)
            throw new Exception ("Porta ausente");

        try
        {
            this.pedido =
            new ServerSocket (Integer.parseInt(porta));
        }
        catch (Exception  erro)
        {
            throw new Exception ("Porta invalida");
        }

        if (usuarios==null)
            throw new Exception ("Usuarios ausentes");

        if (repositorio==null)
            throw new Exception ("Repositorio ausente");

        this.usuarios    = usuarios;
        this.repositorio = repositorio;
    }

    public void run ()
    {
        for(;;)
        {
            Socket conexao=null;
            try
            {
                conexao = this.pedido.accept();
            }
            catch (Exception erro)
            {
                if (this.pedido.isClosed())
                    return;

                continue;
            }

            // cada conexao tem a sua propria thread
            try
            {
                SupervisoraDeConexao supervisoraDeConexao =
                new SupervisoraDeConexao (conexao, this.usuarios, this.repositorio);
                supervisoraDeConexao.start();
            }
            catch (Exception erro)
            {
                try { conexao.close(); } catch (Exception falha) {}
            }
        }
    }
}
