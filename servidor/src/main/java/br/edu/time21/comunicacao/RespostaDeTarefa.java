//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

// resposta de PedidoParaAtualizarTarefa.
public class RespostaDeTarefa extends Resposta
{
    private static final long serialVersionUID = 1L;

    private Bloco tarefa;

    public RespostaDeTarefa (Bloco tarefa)
    {
        this.tarefa = tarefa;
    }

    public Bloco getTarefa () { return this.tarefa; }

    @Override
    public String toString ()
    {
        return "tarefa=" + this.tarefa;
    }
}
