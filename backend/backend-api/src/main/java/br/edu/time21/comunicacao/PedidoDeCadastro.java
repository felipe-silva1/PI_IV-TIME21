package br.edu.time21.comunicacao;

public class PedidoDeCadastro extends Pedido
{
    private static final long serialVersionUID = 1L;

    private String nome;
    private String email;
    private String senhaHash;

    public PedidoDeCadastro(String nome, String email, String senhaHash)
    {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    public String getNome() { return this.nome; }
    public String getEmail() { return this.email; }
    public String getSenhaHash() { return this.senhaHash; }
}
