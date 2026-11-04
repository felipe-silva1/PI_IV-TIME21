package br.edu.time21.comunicacao;

// busca a rotina de um usuario pelo id. respondido com RespostaDeRotina
// (com rotina null se o usuario ainda nao cadastrou a dele) ou
// RespostaDeErro (PEDIDO_INVALIDO, BANCO_INDISPONIVEL ou ERRO_INTERNO)
public class PedidoParaBuscarRotina extends Pedido
{
    private static final long serialVersionUID = 1L;

    private String usuarioId;   // ObjectId do usuario em hexadecimal (24 caracteres)

    public PedidoParaBuscarRotina (String usuarioId)
    {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioId () { return this.usuarioId; }

    public String toString ()
    {
        return "usuarioId=" + this.usuarioId;
    }
}
