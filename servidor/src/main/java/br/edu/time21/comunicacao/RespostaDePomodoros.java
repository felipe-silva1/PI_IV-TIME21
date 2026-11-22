// Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Devolve os minutos do período e os IDs de tarefas que já têm tempo de foco medido. */
public final class RespostaDePomodoros extends Resposta
{
    private static final long serialVersionUID = 1L;

    private final long minutosDeFoco;
    private final ArrayList<String> tarefasComFoco;

    public RespostaDePomodoros(long minutosDeFoco, Collection<String> tarefasComFoco)
    {
        this.minutosDeFoco = minutosDeFoco;
        this.tarefasComFoco = new ArrayList<>(tarefasComFoco);
    }

    public long getMinutosDeFoco() { return minutosDeFoco; }
    public List<String> getTarefasComFoco() { return List.copyOf(tarefasComFoco); }
}
