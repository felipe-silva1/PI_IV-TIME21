package servidor;

import java.net.*;
import java.util.*;

public class AceitadoraDeConexao extends Thread
{
    private ServerSocket           pedido;
    private ArrayList<Parceiro>    usuarios;
    private RepositorioDeChamadas  repositorio;
    private OperacoesDeRotina      operacoesDeRotina;
    private OperacoesDeCronograma  operacoesDeCronograma;
    private OperacoesDeLembretes   operacoesDeLembretes;

    public AceitadoraDeConexao
    (String porta, ArrayList<Parceiro> usuarios, RepositorioDeChamadas repositorio,
     OperacoesDeRotina operacoesDeRotina, OperacoesDeCronograma operacoesDeCronograma,
     OperacoesDeLembretes operacoesDeLembretes)
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

        if (operacoesDeRotina==null)
            throw new Exception ("Operacoes de rotina ausentes");

        if (operacoesDeCronograma==null)
            throw new Exception ("Operacoes de cronograma ausentes");

        if (operacoesDeLembretes==null)
            throw new Exception ("Operacoes de lembretes ausentes");

        this.usuarios          = usuarios;
        this.repositorio       = repositorio;
        this.operacoesDeRotina = operacoesDeRotina;
        this.operacoesDeCronograma = operacoesDeCronograma;
        this.operacoesDeLembretes = operacoesDeLembretes;
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
                new SupervisoraDeConexao (conexao, this.usuarios, this.repositorio,
                                          this.operacoesDeRotina, this.operacoesDeCronograma,
                                          this.operacoesDeLembretes);
                supervisoraDeConexao.start();
            }
            catch (Exception erro)
            {
                try { conexao.close(); } catch (Exception falha) {}
            }
        }
    }
}
