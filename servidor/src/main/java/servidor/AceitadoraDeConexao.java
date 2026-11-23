//Autor: Arthur Berto de Castro RA: 25009917
package servidor;

import java.net.*;
import java.util.*;

public class AceitadoraDeConexao extends Thread
{
    // Socket que recebe novas conexoes.
    private ServerSocket           pedido;
    private ArrayList<Parceiro>    usuarios;
    private RepositorioDeChamadas  repositorio;
    private OperacoesDeRotina      operacoesDeRotina;
    private OperacoesDeCronograma  operacoesDeCronograma;

    // Valida os recursos e prepara a porta do servidor.
    public AceitadoraDeConexao
    (String porta, ArrayList<Parceiro> usuarios, RepositorioDeChamadas repositorio,
     OperacoesDeRotina operacoesDeRotina, OperacoesDeCronograma operacoesDeCronograma)
    throws Exception
    {
        // Confere se a porta foi informada.
        if (porta==null)
            throw new Exception ("Porta ausente");

        // Converte a porta e abre o socket de escuta.
        try
        {
            this.pedido =
            new ServerSocket (Integer.parseInt(porta));
        }
        catch (Exception erro)
        {
            throw new Exception ("Porta invalida");
        }

        // Confere se os componentes necessarios foram informados.
        if (usuarios==null)
            throw new Exception ("Usuarios ausentes");

        if (repositorio==null)
            throw new Exception ("Repositorio ausente");

        if (operacoesDeRotina==null)
            throw new Exception ("Operacoes de rotina ausentes");

        if (operacoesDeCronograma==null)
            throw new Exception ("Operacoes de cronograma ausentes");

        // Guarda os componentes compartilhados pelas conexoes.
        this.usuarios          = usuarios;
        this.repositorio       = repositorio;
        this.operacoesDeRotina = operacoesDeRotina;
        this.operacoesDeCronograma = operacoesDeCronograma;
    }

    // Aceita conexoes continuamente enquanto o servidor estiver ativo.
    public void run ()
    {
        for(;;)
        {
            Socket conexao=null;

            // Aguarda a chegada de um cliente.
            try
            {
                conexao = this.pedido.accept();
            }
            catch (Exception erro)
            {
                // Termina se o socket foi fechado; senao, tenta novamente.
                if (this.pedido.isClosed())
                    return;

                continue;
            }

            // Cria uma thread supervisora para atender a conexao.
            try
            {
                SupervisoraDeConexao supervisoraDeConexao =
                new SupervisoraDeConexao (conexao, this.usuarios, this.repositorio,
                                          this.operacoesDeRotina, this.operacoesDeCronograma);
                supervisoraDeConexao.start();
            }
            catch (Exception erro)
            {
                // Fecha a conexao se o atendimento nao puder ser iniciado.
                try { conexao.close(); } catch (Exception falha) {}
            }
        }
    }
}