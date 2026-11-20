package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.Bloco;
import com.guiavest.backend_api.model.ProgressoSemanal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CalculadoraDeProgressoSemanal
{
    public ProgressoSemanal calcular(List<Bloco> blocos)
    {
        if (blocos == null)
            blocos = List.of();

        int totalTarefas = blocos.size();
        int tarefasConcluidas = 0;
        int minutosPlanejados = 0;
        int minutosCumpridos = 0;

        for (Bloco bloco : blocos)
        {
            if (bloco == null)
            {
                totalTarefas--;
                continue;
            }

            int duracao = Math.max(0, bloco.getDuracaoMin());
            minutosPlanejados += duracao;
            if (bloco.isConcluido())
            {
                tarefasConcluidas++;
                minutosCumpridos += duracao;
            }
        }

        double horasPlanejadas = arredondar(minutosPlanejados / 60.0);
        double horasCumpridas = arredondar(minutosCumpridos / 60.0);
        double percentualTarefas = totalTarefas == 0
                ? 0.0
                : arredondar(tarefasConcluidas * 100.0 / totalTarefas);
        double percentualHoras = minutosPlanejados == 0
                ? 0.0
                : arredondar(minutosCumpridos * 100.0 / minutosPlanejados);

        return new ProgressoSemanal(
                tarefasConcluidas,
                totalTarefas,
                percentualTarefas,
                horasCumpridas,
                horasPlanejadas,
                percentualHoras);
    }

    private static double arredondar(double valor)
    {
        return BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
