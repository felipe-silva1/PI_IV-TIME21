package servidor;

import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class SupervisoraDeConexao extends Thread
{
    // so estas classes podem ser desserializadas (protege contra objetos maliciosos)
    private static final String FILTRO =
    "servidor.Comunicado;servidor.PedidoDeStatus;servidor.PedidoParaSair;" +
    "servidor.PedidoDeInsercao;servidor.PedidoDeBusca;!*";

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
                else if (comunicado instanceof PedidoDeInsercao)
                {
                    PedidoDeInsercao p = (PedidoDeInsercao)comunicado;
                    try
                    {
                        this.repositorio.inserir (p.getColecao(), p.getDocumentoJson());
                        this.usuario.receba (new RespostaDeOperacao (true, "ok", null));
                    }
                    catch (Exception erro)
                    {
                        this.usuario.receba (new RespostaDeOperacao (false, erro.getMessage(), null));
                    }
                }
                else if (comunicado instanceof PedidoDeBusca)
                {
                    PedidoDeBusca p = (PedidoDeBusca)comunicado;
                    try
                    {
                        this.usuario.receba (new RespostaDeOperacao (true, "ok",
                            this.repositorio.buscar (p.getColecao(), p.getFiltroJson())));
                    }
                    catch (Exception erro)
                    {
                        this.usuario.receba (new RespostaDeOperacao (false, erro.getMessage(), null));
                    }
                }
                else if (comunicado instanceof PedidoParaSair)
                    break;
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
