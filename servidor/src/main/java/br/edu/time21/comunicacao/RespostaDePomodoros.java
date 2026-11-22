package br.edu.time21.comunicacao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
