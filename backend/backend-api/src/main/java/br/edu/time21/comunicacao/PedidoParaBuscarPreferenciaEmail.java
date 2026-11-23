package br.edu.time21.comunicacao;

public class PedidoParaBuscarPreferenciaEmail extends Pedido
{
    private static final long serialVersionUID = 1L;

    private String usuarioId;
    private String token;

    public PedidoParaBuscarPreferenciaEmail(String usuarioId, String token)
    {
        this.usuarioId = usuarioId;
        this.token = token;
    }

    public String getUsuarioId() { return this.usuarioId; }
    public String getToken() { return this.token; }
}
