package servidor;

import java.io.*;
import java.net.*;
import java.util.*;

import br.edu.time21.comunicacao.*;

public class SupervisoraDeConexao extends Thread
{
    // so as classes do protocolo podem ser desserializadas (protege contra
    // objetos maliciosos). o padrao casa o nome TOTALMENTE QUALIFICADO, por
    // isso o pacote precisa aparecer aqui; "!*" rejeita todo o resto.
    // String nao precisa ser liberada (vai no stream como TC_STRING, sem
    // descritor de classe), mas qualquer novo tipo de campo (Date, List,
    // classe de modelo) tem de ser acrescentado a este filtro.
    private static final String FILTRO =
        "br.edu.time21.comunicacao.*;!*";

    private Parceiro               usuario;
    private Socket                 conexao;
    private ArrayList<Parceiro>    usuarios;
    private RepositorioDeChamadas  repositorio;

    public SupervisoraDeConexao
    (Socket conexao, ArrayList<Parceiro> usuarios, RepositorioDeChamadas repositorio)
    throws Exception
    {
        if (conexao==null)
            throw new Exception ("Conexao ausente");

        if (usuarios==null)
            throw new Exception ("Usuarios ausentes");

        if (repositorio==null)
            throw new Exception ("Repositorio ausente");

        this.conexao     = conexao;
        this.usuarios    = usuarios;
        this.repositorio = repositorio;
    }

    public void run ()
    {
        // 1) o ObjectOutputStream e criado e recebe flush() ANTES do ObjectInputStream
        ObjectOutputStream transmissor;
        try
        {
            transmissor =
            new ObjectOutputStream(
            this.conexao.getOutputStream());

            transmissor.flush();
        }
        catch (Exception erro)
        {
            try { this.conexao.close(); } catch (Exception falha) {}
            return;
        }

        // 2) so depois o ObjectInputStream
        ObjectInputStream receptor=null;
        try
        {
            receptor=
            new ObjectInputStream(
            this.conexao.getInputStream());

            receptor.setObjectInputFilter (
                ObjectInputFilter.Config.createFilter (SupervisoraDeConexao.FILTRO));
        }
        catch (Exception err0)
        {
            try
            {
                transmissor.close();
                this.conexao.close();
            }
            catch (Exception falha)
            {} // so tentando fechar antes de acabar a thread

            return;
        }

        try
        {
            this.usuario =
            new Parceiro (this.conexao,
                          receptor,
                          transmissor);
        }
        catch (Exception erro)
        {} // sei que passei os parametros corretos

        String origem = this.usuario.getEndereco();

        try
        {
            synchronized (this.usuarios)
            {
                this.usuarios.add (this.usuario);
            }

            this.repositorio.registrar ("CONEXAO_ABERTA", origem, null);

            for(;;)
            {
                Comunicado comunicado = this.usuario.envie ();

                if (comunicado==null)
                    break;
                else if (comunicado instanceof PedidoDeStatus)
                {
                    this.repositorio.registrar ("PEDIDO_DE_STATUS", origem, null);

                    int ativas;
                    synchronized (this.usuarios)
                    {
                        ativas = this.usuarios.size();
                    }

                    this.usuario.receba (
                        new RespostaDeStatus ("ATIVO",
                                              ativas,
                                              System.currentTimeMillis(),
                                              this.repositorio.estaDisponivel()));
                }
                else if (comunicado instanceof PedidoParaSair)
                    break;
                else
                {
                    // classe do protocolo, mas sem tratamento nesta versao
                    this.repositorio.registrar ("PEDIDO_DESCONHECIDO", origem,
                                                comunicado.getClass().getName());

                    this.usuario.receba (
                        new RespostaDeErro (RespostaDeErro.PEDIDO_DESCONHECIDO,
                                            "O servidor nao trata " +
                                            comunicado.getClass().getSimpleName()));
                }
            }
        }
        catch (Exception erro)
        {
            // cliente encerrou/derrubou a conexao (ou enviou algo invalido):
            // so esta thread termina; o servidor e as demais conexoes seguem
        }
        finally
        {
            synchronized (this.usuarios)
            {
                this.usuarios.remove (this.usuario);
            }

            try
            {
                this.usuario.adeus();
            }
            catch (Exception falha)
            {} // so tentando fechar antes de acabar a thread

            this.repositorio.registrar ("CONEXAO_ENCERRADA", origem, null);
        }
    }
}
