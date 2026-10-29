package br.edu.time21.comunicacao;

public class RespostaDeConta extends Resposta
{
    private static final long serialVersionUID = 1L;

    public static final String OK = "OK";
    public static final String EMAIL_EM_USO = "EMAIL_EM_USO";
    public static final String NAO_ENCONTRADO = "NAO_ENCONTRADO";
    public static final String BANCO_INDISPONIVEL = "BANCO_INDISPONIVEL";

    private String codigo;
    private String usuarioId;
    private String nome;
    private String senhaHash;

    public RespostaDeConta(String codigo, String usuarioId, String nome, String senhaHash)
    {
        this.codigo = codigo;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.senhaHash = senhaHash;
    }

    public String getCodigo() { return this.codigo; }
    public String getUsuarioId() { return this.usuarioId; }
    public String getNome() { return this.nome; }
    public String getSenhaHash() { return this.senhaHash; }
}
