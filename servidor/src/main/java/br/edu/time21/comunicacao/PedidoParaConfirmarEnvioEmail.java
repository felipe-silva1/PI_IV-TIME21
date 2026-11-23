package br.edu.time21.comunicacao;

public class PedidoParaConfirmarEnvioEmail extends Pedido
{
    private static final long serialVersionUID = 1L;

    private String usuarioId;
    private String blocoId;
    private String token;

    public PedidoParaConfirmarEnvioEmail(String usuarioId, String blocoId, String token)
    {
        this.usuarioId = usuarioId;
        this.blocoId = blocoId;
        this.token = token;
    }

    public String getUsuarioId() { return this.usuarioId; }
    public String getBlocoId() { return this.blocoId; }
    public String getToken() { return this.token; }
}
