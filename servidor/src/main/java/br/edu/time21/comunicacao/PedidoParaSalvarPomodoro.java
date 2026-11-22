package br.edu.time21.comunicacao;

public final class PedidoParaSalvarPomodoro extends Pedido
{
    private static final long serialVersionUID = 1L;

    private final String usuarioId;
    private final String sessaoId;
    private final String tarefaId;
    private final long concluidoEm;

    public PedidoParaSalvarPomodoro(String usuarioId, String sessaoId, String tarefaId, long concluidoEm)
    {
        this.usuarioId = usuarioId;
        this.sessaoId = sessaoId;
        this.tarefaId = tarefaId;
        this.concluidoEm = concluidoEm;
    }

    public String getUsuarioId() { return usuarioId; }
    public String getSessaoId() { return sessaoId; }
    public String getTarefaId() { return tarefaId; }
    public long getConcluidoEm() { return concluidoEm; }
}
