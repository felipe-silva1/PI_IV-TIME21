package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.TurnoLivre;
import com.guiavest.backend_api.model.BlocoDeEstudo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeradorDeCronogramaTest
{
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 16);

    @Test
    void geraCronogramaNoCasoNormal()
    {
        List<BlocoDeEstudo> blocos = new GeradorDeCronograma().gerar(
                "usuario-1",
                SEGUNDA,
                List.of(
                        new TurnoLivre(2, TurnoLivre.MANHA),
                        new TurnoLivre(1, TurnoLivre.NOITE),
                        new TurnoLivre(3, TurnoLivre.TARDE)),
                List.of("Matematica", "Fisica", "Quimica"));

        assertEquals(3, blocos.size());
        assertEquals("2026-11-16", blocos.get(0).data());
        assertEquals("2026-11-17", blocos.get(1).data());
        assertEquals("2026-11-18", blocos.get(2).data());
        assertEquals(List.of("Matematica", "Fisica", "Quimica"),
                blocos.stream().map(BlocoDeEstudo::materia).toList());
        assertTrue(blocos.stream().allMatch(bloco -> bloco.duracaoMin() == 90));
    }

    @Test
    void usaAsMateriasMaisPrioritariasQuandoHaPoucosTurnos()
    {
        List<BlocoDeEstudo> blocos = new GeradorDeCronograma().gerar(
                "usuario-1",
                SEGUNDA,
                List.of(new TurnoLivre(1, TurnoLivre.MANHA)),
                List.of("Matematica", "Fisica"));

        assertEquals(1, blocos.size());
        assertEquals("Matematica", blocos.get(0).materia());
    }

    @Test
    void naoGeraCronogramaSemMateriasPrioritarias()
    {
        List<BlocoDeEstudo> blocos = new GeradorDeCronograma().gerar(
                "usuario-1",
                SEGUNDA,
                List.of(new TurnoLivre(1, TurnoLivre.MANHA)),
                List.of());

        assertTrue(blocos.isEmpty());
    }

    @Test
    void distribuiUmBlocoParaCadaMateriaQueCabeNosTurnos()
    {
        List<BlocoDeEstudo> blocos = new GeradorDeCronograma().gerar(
                "usuario-1",
                SEGUNDA,
                List.of(
                        new TurnoLivre(1, TurnoLivre.MANHA),
                        new TurnoLivre(2, TurnoLivre.MANHA)),
                List.of("Matematica", "Fisica", "Quimica", "Biologia"));

        assertEquals(2, blocos.size());
        assertEquals(List.of("Matematica", "Fisica"),
                blocos.stream().map(BlocoDeEstudo::materia).toList());
    }
}
