//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// resposta de PedidoParaBuscarTarefas.
public class RespostaDeTarefas extends Resposta
{
    private static final long serialVersionUID = 1L;

    private ArrayList<Bloco> tarefas;

    public RespostaDeTarefas (List<Bloco> tarefas)
    {
        this.tarefas = tarefas == null ? null : new ArrayList<Bloco> (tarefas);
    }

    public List<Bloco> getTarefas ()
    {
        return this.tarefas == null ? null : Collections.unmodifiableList (this.tarefas);
    }

    @Override
    public String toString ()
    {
        return "tarefas=" + this.tarefas;
    }
}
