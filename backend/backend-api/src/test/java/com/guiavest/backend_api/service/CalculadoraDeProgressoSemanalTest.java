package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.Bloco;
import com.guiavest.backend_api.model.ProgressoSemanal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraDeProgressoSemanalTest
{
    @Test
    void calculaPercentualDeTarefasEHorasCumpridas()
    {
        CalculadoraDeProgressoSemanal calculadora =
                new CalculadoraDeProgressoSemanal();
        Bloco concluido = new Bloco(
                "id-concluido",
                "2026-11-17",
                "TARDE",
                "Fisica",
                90,
                List.of(),
                true,
                1784476800000L);
        List<Bloco> blocos = List.of(
                new Bloco("2026-11-16", "MANHA", "Matematica", 60, List.of()),
                concluido);

        ProgressoSemanal progresso = calculadora.calcular(blocos);

        assertEquals(1, progresso.getTarefasConcluidas());
        assertEquals(2, progresso.getTotalTarefas());
        assertEquals(50.0, progresso.getPercentualTarefasConcluidas());
        assertEquals(2.5, progresso.getHorasPlanejadas());
        assertEquals(1.5, progresso.getHorasCumpridas());
        assertEquals(60.0, progresso.getPercentualHorasCumpridas());
    }

    @Test
    void retornaZerosQuandoNaoHaTarefas()
    {
        ProgressoSemanal progresso =
                new CalculadoraDeProgressoSemanal().calcular(List.of());

        assertEquals(0, progresso.getTotalTarefas());
        assertEquals(0.0, progresso.getPercentualTarefasConcluidas());
        assertEquals(0.0, progresso.getHorasPlanejadas());
        assertEquals(0.0, progresso.getHorasCumpridas());
        assertEquals(0.0, progresso.getPercentualHorasCumpridas());
    }
}
