//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

// resposta de PedidoParaSalvarCronograma e de PedidoParaBuscarCronograma.
// cronograma e null quando a busca nao encontrou blocos na semana: semana
// ainda nao gerada e um caso normal, nao um erro.
public class RespostaDeCronograma extends Resposta{
    private static final long serialVersionUID = 1L;
    private Cronograma cronograma;
    
    public RespostaDeCronograma(Cronograma cronograma)
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
