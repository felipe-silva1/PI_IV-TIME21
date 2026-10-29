package br.edu.time21.comunicacao;

public class PedidoDeLogin extends Pedido
{
    private static final long serialVersionUID = 1L;

    private String email;

    public PedidoDeLogin(String email)
    {
        this.email = email;
    }

    public String getEmail() { return this.email; }
}
