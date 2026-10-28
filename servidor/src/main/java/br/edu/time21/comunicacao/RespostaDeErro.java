package br.edu.time21.comunicacao;

// resposta padrao para qualquer pedido que o servidor nao conseguiu atender.
// o backend decide pelo codigo o que devolver ao frontend; a mensagem e so
// para log e diagnostico, nunca para ser mostrada crua ao usuario final.
public class RespostaDeErro extends Resposta
{
    private static final long serialVersionUID = 1L;

    public static final String PEDIDO_DESCONHECIDO = "PEDIDO_DESCONHECIDO";
    public static final String PEDIDO_INVALIDO     = "PEDIDO_INVALIDO";
    public static final String BANCO_INDISPONIVEL  = "BANCO_INDISPONIVEL";
    public static final String ERRO_INTERNO        = "ERRO_INTERNO";

    private String codigo;
    private String mensagem;

    public RespostaDeErro (String codigo, String mensagem)
    {
        this.codigo   = codigo;
        this.mensagem = mensagem;
    }

    public String getCodigo   () { return this.codigo;   }
    public String getMensagem () { return this.mensagem; }

    public String toString ()
    {
        return "codigo="      + this.codigo +
               ", mensagem="  + this.mensagem;
    }
}
