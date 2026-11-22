// Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

import java.util.ArrayList;
import java.util.List;

/** Consulta o intervalo [inicio, fim) em milissegundos e os vínculos das tarefas do cronograma. */
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
