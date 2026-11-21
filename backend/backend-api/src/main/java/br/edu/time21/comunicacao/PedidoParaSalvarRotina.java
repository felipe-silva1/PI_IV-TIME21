//Max Thomazini Barbosa 25003934
package br.edu.time21.comunicacao;

// grava a rotina do usuario rotina.getUsuarioId(): cria se ele ainda nao
// tem, substitui inteira se ja tem. respondido com RespostaDeRotina (a
// rotina como ficou gravada) ou RespostaDeErro (PEDIDO_INVALIDO,
// BANCO_INDISPONIVEL ou ERRO_INTERNO)
// Envia ao servidor os dados da rotina que devem ser cadastrados ou atualizados.
public class PedidoParaSalvarRotina extends Pedido
{
    private static final long serialVersionUID = 1L;

    private Rotina rotina;

    public PedidoParaSalvarRotina (Rotina rotina)
    {
        this.rotina = rotina;
    }

    public Rotina getRotina () { return this.rotina; }

    public String toString ()
    {
        return "rotina=" + this.rotina;
    }
}
