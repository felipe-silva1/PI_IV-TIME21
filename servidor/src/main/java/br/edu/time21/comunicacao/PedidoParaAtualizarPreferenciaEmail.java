package br.edu.time21.comunicacao;

public class PedidoParaAtualizarPreferenciaEmail extends Pedido
{
    private static final long serialVersionUID = 1L;

    private String usuarioId;
    private String token;
    private boolean emailAtivo;

    public PedidoParaAtualizarPreferenciaEmail(String usuarioId, boolean emailAtivo, String token)
    {
        this.usuarioId = usuarioId;
        this.emailAtivo = emailAtivo;
        this.token = token;
    }

    public String getUsuarioId() { return this.usuarioId; }
    public String getToken() { return this.token; }
    public boolean isEmailAtivo() { return this.emailAtivo; }
}
