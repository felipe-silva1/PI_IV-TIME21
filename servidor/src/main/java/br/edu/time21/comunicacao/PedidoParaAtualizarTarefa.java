//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

// atualiza o status de uma tarefa (bloco de estudo).
// respondido com RespostaDeTarefa ou RespostaDeErro.
public class PedidoParaAtualizarTarefa extends Pedido
{
    private static final long serialVersionUID = 1L;

    private String usuarioId;
    private String tarefaId;
    private boolean concluido;

    public PedidoParaAtualizarTarefa (String usuarioId, String tarefaId, boolean concluido)
    {
        this.usuarioId = usuarioId;
        this.tarefaId = tarefaId;
        this.concluido = concluido;
    }

    public String getUsuarioId () { return this.usuarioId; }
    public String getTarefaId () { return this.tarefaId; }
    public boolean isConcluido () { return this.concluido; }

    @Override
    public String toString ()
    {
        return "usuarioId=" + this.usuarioId +
               ", tarefaId=" + this.tarefaId +
               ", concluido=" + this.concluido;
    }
}
