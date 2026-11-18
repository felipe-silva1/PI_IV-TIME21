//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

// grava o cronograma da semana cronograma.getSemanaInicio() do usuario cronograma.getUsuarioId(): apaga os blocos que ele jatinha nessa semana 
// e insere os novos (regra de "regerar" do dicionario do banco). respondido com RespostaDeCronograma (o cronograma como ficou gravado, com
// os ids dos blocos) ou RespostaDeErro (PEDIDO_INVALIDO, BANCO_INDISPONIVEL ou ERRO_INTERNO)
public class PedidoParaSalvarCronograma extends Pedido{
    private static final long serialVersionUID = 1L;

    private Cronograma cronograma;

    public PedidoParaSalvarCronograma (Cronograma cronograma)
    {
        this.cronograma = cronograma;
    }

    public Cronograma getCronograma() { return this.cronograma; }

    @Override
    public String toString()
    {
        return "cronograma=" + this.cronograma;
    }
}
