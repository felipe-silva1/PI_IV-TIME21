package br.edu.time21.comunicacao;

import java.util.ArrayList;
import java.util.List;

public final class PedidoParaBuscarPomodoros extends Pedido
{
    private static final long serialVersionUID = 1L;

    private final String usuarioId;
    private final long inicio;
    private final long fim;
    private final ArrayList<String> tarefas;

    public PedidoParaBuscarPomodoros(String usuarioId, long inicio, long fim, List<String> tarefas)
    {
        this.usuarioId = usuarioId;
        this.inicio = inicio;
        this.fim = fim;
        this.tarefas = new ArrayList<>(tarefas);
    }

    public String getUsuarioId() { return usuarioId; }
    public long getInicio() { return inicio; }
    public long getFim() { return fim; }
    public List<String> getTarefas() { return List.copyOf(tarefas); }
}
