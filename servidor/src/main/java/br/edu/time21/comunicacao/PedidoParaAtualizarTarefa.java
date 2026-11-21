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
    private boolean atualizacaoCompleta;
    private String data;
    private String turno;
    private String materia;
    private int duracaoMin;
    private String descricao;

    public PedidoParaAtualizarTarefa (String usuarioId, String tarefaId, boolean concluido)
    {
        this.usuarioId = usuarioId;
        this.tarefaId = tarefaId;
        this.concluido = concluido;
    }
    public PedidoParaAtualizarTarefa (
            String usuarioId, String tarefaId, String data, String turno,
            String materia, int duracaoMin, String descricao, boolean concluido)
    {
        this.usuarioId = usuarioId;
        this.tarefaId = tarefaId;
        this.data = data;
        this.turno = turno;
        this.materia = materia;
        this.duracaoMin = duracaoMin;
        this.descricao = descricao;
        this.concluido = concluido;
        this.atualizacaoCompleta = true;
    }

    public String getUsuarioId () { return this.usuarioId; }
    public String getTarefaId () { return this.tarefaId; }
    public boolean isConcluido () { return this.concluido; }
    public boolean isAtualizacaoCompleta () { return this.atualizacaoCompleta; }
    public String getData () { return this.data; }
    public String getTurno () { return this.turno; }
    public String getMateria () { return this.materia; }
    public int getDuracaoMin () { return this.duracaoMin; }
    public String getDescricao () { return this.descricao; }

    @Override
    public String toString ()
    {
        return "usuarioId=" + this.usuarioId +
               ", tarefaId=" + this.tarefaId +
               ", concluido=" + this.concluido;
    }
}
