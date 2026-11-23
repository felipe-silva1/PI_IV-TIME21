package com.guiavest.backend_api.service;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LembreteServiceTest
{
    @Test
    void associaOsTurnosAosHorariosDefinidosEPermiteReenvioBreve()
    {
        assertEquals(List.of("MANHA"),
                LembreteService.turnosNaJanelaDeEnvio(LocalTime.of(5, 0)));
        assertEquals(List.of("MANHA"),
                LembreteService.turnosNaJanelaDeEnvio(LocalTime.of(5, 4)));
        assertEquals(List.of(),
                LembreteService.turnosNaJanelaDeEnvio(LocalTime.of(5, 5)));
        assertEquals(List.of("TARDE"),
                LembreteService.turnosNaJanelaDeEnvio(LocalTime.of(14, 0)));
        assertEquals(List.of("NOITE"),
                LembreteService.turnosNaJanelaDeEnvio(LocalTime.of(18, 0)));
    }
}
