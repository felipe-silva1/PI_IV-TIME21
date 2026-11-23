//Autor: Arthur Berto de Castro RA: 25009917
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
    // descritor de classe), mas qualquer novo tipo de campo (Date, enum,
    // String[], outra colecao) tem de ser acrescentado a este filtro.
    //
    // java.util.ArrayList entra por causa das listas da Rotina. ele sozinho
    // nao basta: o readObject do ArrayList valida um Object[] interno, que o
    // filtro checa como java.lang.Object. liberar Object nao abre brecha,
    // porque cada elemento da lista continua passando pelo filtro.
    private static final String FILTRO =
        "br.edu.time21.comunicacao.*;java.util.ArrayList;java.lang.Object;!*";

    private Parceiro               usuario;
    private Socket                 conexao;
    private ArrayList<Parceiro>    usuarios;
    private RepositorioDeChamadas  repositorio;
    private OperacoesDeRotina      operacoesDeRotina;
    private OperacoesDeCronograma  operacoesDeCronograma;

    // Valida os recursos necessários antes de iniciar a supervisora.
    public SupervisoraDeConexao
    (Socket conexao, ArrayList<Parceiro> usuarios, RepositorioDeChamadas repositorio,
     OperacoesDeRotina operacoesDeRotina, OperacoesDeCronograma operacoesDeCronograma)
    throws Exception
    {
        if (conexao==null)
            throw new Exception ("Conexao ausente");

        if (usuarios==null)
            throw new Exception ("Usuarios ausentes");

        if (repositorio==null)
            throw new Exception ("Repositorio ausente");

        if (operacoesDeRotina==null)
            throw new Exception ("Operacoes de rotina ausentes");

        if (operacoesDeCronograma==null)
            throw new Exception ("Operacoes de cronograma ausentes");

        this.conexao           = conexao;
        this.usuarios          = usuarios;
        this.repositorio       = repositorio;
        this.operacoesDeRotina = operacoesDeRotina;
        this.operacoesDeCronograma = operacoesDeCronograma;
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
            // Registra o cliente na lista de conexões ativas.
            synchronized (this.usuarios)
            {
                this.usuarios.add (this.usuario);
            }

            this.repositorio.registrar ("CONEXAO_ABERTA", origem, null);

            for(;;)
            {
                // Aguarda e identifica o próximo pedido do cliente.
                Comunicado comunicado = this.usuario.envie ();

                if (comunicado==null)
                    break;
                else if (comunicado instanceof PedidoDeStatus)
                {
                    // Retorna estado do servidor e da conexão atual.
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
                else if (comunicado instanceof PedidoDeCadastro pedido)
                {
                    // Valida os dados antes de cadastrar o usuário.
                    if (pedido.getNome() == null || pedido.getNome().isBlank()
                            || pedido.getEmail() == null || pedido.getEmail().isBlank()
                            || pedido.getSenhaHash() == null || pedido.getSenhaHash().isBlank())
                    {
                        this.usuario.receba(new RespostaDeErro(
                                RespostaDeErro.PEDIDO_INVALIDO,
                                "Dados de cadastro invalidos"));
                        continue;
                    }

                    try
                    {
                        String usuarioId = this.repositorio.cadastrarUsuario(
                                pedido.getNome(), pedido.getEmail(), pedido.getSenhaHash());
                        this.usuario.receba(new RespostaDeConta(
                                usuarioId == null ? RespostaDeConta.EMAIL_EM_USO
                                                  : RespostaDeConta.OK,
                                usuarioId, pedido.getNome(), null));
                    }
                    catch (Exception erro)
                    {
                        System.err.println("Falha ao criar conta no MongoDB: "
                                + erro.getMessage());
                        this.usuario.receba(new RespostaDeConta(
                                RespostaDeConta.BANCO_INDISPONIVEL, null, null, null));
                    }
                }
                else if (comunicado instanceof PedidoDeLogin pedido)
                {
                    // Valida e consulta as credenciais no banco.
                    if (pedido.getEmail() == null || pedido.getEmail().isBlank())
                    {
                        this.usuario.receba(new RespostaDeErro(
                                RespostaDeErro.PEDIDO_INVALIDO,
                                "E-mail de login invalido"));
                        continue;
                    }

                    try
                    {
                        org.bson.Document usuario =
                                this.repositorio.buscarUsuario(pedido.getEmail());
                        if (usuario == null)
                        {
                            this.usuario.receba(new RespostaDeConta(
                                    RespostaDeConta.NAO_ENCONTRADO, null, null, null));
                        }
                        else
                        {
                            this.usuario.receba(new RespostaDeConta(
                                    RespostaDeConta.OK,
                                    ((org.bson.types.ObjectId) usuario.get("_id")).toHexString(),
                                    usuario.getString("nome"),
                                    usuario.getString("senhaHash")));
                        }
                    }
                    catch (Exception erro)
                    {
                        System.err.println("Falha ao consultar conta no MongoDB: "
                                + erro.getMessage());
                        this.usuario.receba(new RespostaDeConta(
                                RespostaDeConta.BANCO_INDISPONIVEL, null, null, null));
                    }
                }
                else if (comunicado instanceof PedidoParaSalvarRotina pedido)
                {
                    // Persiste uma rotina enviada pelo cliente.
                    this.repositorio.registrar ("PEDIDO_PARA_SALVAR_ROTINA", origem,
                                                pedido.toString());
                    this.usuario.receba (this.operacoesDeRotina.salvar (pedido));
                }
                else if (comunicado instanceof PedidoParaBuscarRotina pedido)
                {
                    // Busca uma rotina existente no banco.
                    this.repositorio.registrar ("PEDIDO_PARA_BUSCAR_ROTINA", origem,
                                                pedido.toString());
                    this.usuario.receba (this.operacoesDeRotina.buscar (pedido));
                }
                else if (comunicado instanceof PedidoParaSalvarCronograma pedido)
                {
                    // Persiste o cronograma enviado pelo cliente.
                    this.repositorio.registrar ("PEDIDO_PARA_SALVAR_CRONOGRAMA", origem,
                                                pedido.toString());
                    this.usuario.receba (this.operacoesDeCronograma.salvar (pedido));
                }
                else if (comunicado instanceof PedidoParaBuscarCronograma pedido)
                {
                    // Busca o cronograma associado ao pedido.
                    this.repositorio.registrar ("PEDIDO_PARA_BUSCAR_CRONOGRAMA", origem,
                                                pedido.toString());
                    this.usuario.receba (this.operacoesDeCronograma.buscar (pedido));
                }
                else if (comunicado instanceof PedidoParaBuscarTarefas pedido)
                {
                    // Recupera as tarefas do cronograma.
                    this.repositorio.registrar ("PEDIDO_PARA_BUSCAR_TAREFAS", origem,
                                                pedido.toString());
                    this.usuario.receba (this.operacoesDeCronograma.buscarTarefas (pedido));
                }
                else if (comunicado instanceof PedidoParaAtualizarTarefa pedido)
                {
                    // Atualiza o estado de uma tarefa existente.
                    this.repositorio.registrar ("PEDIDO_PARA_ATUALIZAR_TAREFA", origem,
                                                pedido.toString());
                    this.usuario.receba (this.operacoesDeCronograma.atualizarTarefa (pedido));
                }
                else if (comunicado instanceof PedidoParaSalvarPomodoro pedido)
                    // Persiste os dados de uma sessão Pomodoro.
                    this.usuario.receba (this.operacoesDeCronograma.salvarPomodoro (pedido));
                else if (comunicado instanceof PedidoParaBuscarPomodoros pedido)
                    // Recupera as sessões Pomodoro registradas.
                    this.usuario.receba (this.operacoesDeCronograma.buscarPomodoros (pedido));
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
            // Remove o cliente e encerra os recursos da conexão.
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
