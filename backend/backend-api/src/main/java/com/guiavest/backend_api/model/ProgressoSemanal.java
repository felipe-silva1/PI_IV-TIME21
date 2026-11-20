package com.guiavest.backend_api.model;

public class ProgressoSemanal
{
    private final int tarefasConcluidas;
    private final int totalTarefas;
    private final double percentualTarefasConcluidas;
    private final double horasCumpridas;
    private final double horasPlanejadas;
    private final double percentualHorasCumpridas;

    public ProgressoSemanal(
            int tarefasConcluidas,
            int totalTarefas,
            double percentualTarefasConcluidas,
            double horasCumpridas,
            double horasPlanejadas,
            double percentualHorasCumpridas)
    {
        this.tarefasConcluidas = tarefasConcluidas;
        this.totalTarefas = totalTarefas;
        this.percentualTarefasConcluidas = percentualTarefasConcluidas;
        this.horasCumpridas = horasCumpridas;
        this.horasPlanejadas = horasPlanejadas;
        this.percentualHorasCumpridas = percentualHorasCumpridas;
    }

    public int getTarefasConcluidas() { return tarefasConcluidas; }
    public int getTotalTarefas() { return totalTarefas; }
    public int getTarefasTotais() { return totalTarefas; }
    public double getPercentualTarefasConcluidas() { return percentualTarefasConcluidas; }
    public double getPercentualConclusao() { return percentualTarefasConcluidas; }
    public double getHorasCumpridas() { return horasCumpridas; }
    public double getHorasPlanejadas() { return horasPlanejadas; }
    public double getPercentualHorasCumpridas() { return percentualHorasCumpridas; }
}
