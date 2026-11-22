//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package com.guiavest.backend_api.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.time21.comunicacao.Bloco;
import br.edu.time21.comunicacao.Cronograma;
import br.edu.time21.comunicacao.PedidoParaBuscarCronograma;
import br.edu.time21.comunicacao.PedidoParaSalvarCronograma;
import br.edu.time21.comunicacao.RespostaDeCronograma;
import br.edu.time21.comunicacao.Rotina;
import br.edu.time21.comunicacao.TurnoLivre;

class CronogramaServiceTest
{
    @Test
    void geraApenasSabadoEDomingoNaPrimeiraSemanaQuandoRotinaComecaNoSabado()
    {
        String usuarioId = "usuario";
        ClienteSocket socket = mock(ClienteSocket.class);
        RotinaService rotinaService = mock(RotinaService.class);
        CronogramaService servico =
                new CronogramaService(socket, rotinaService, new GeradorDeCronograma());

        List<TurnoLivre> turnos = IntStream.rangeClosed(1, 7)
                .mapToObj(dia -> new TurnoLivre(dia, TurnoLivre.MANHA))
                .toList();

        when(rotinaService.buscarPorUsuario(usuarioId))
                .thenReturn(Optional.of(
                        new Rotina(usuarioId, "ENEM", turnos, List.of("Matemática"))));
        when(socket.enviar(any(PedidoParaBuscarCronograma.class)))
                .thenReturn(new RespostaDeCronograma(null));
        when(socket.enviar(any(PedidoParaSalvarCronograma.class)))
                .thenAnswer(chamada -> {
                    PedidoParaSalvarCronograma pedido = chamada.getArgument(0);
                    return new RespostaDeCronograma(pedido.getCronograma());
                });

        servico.gerarPrimeiroMes(usuarioId, LocalDate.of(2026, 11, 21));

        var gravacoes = forClass(PedidoParaSalvarCronograma.class);
        verify(socket, atLeastOnce()).enviar(gravacoes.capture());

        Cronograma primeiraSemana = gravacoes.getAllValues().get(0).getCronograma();
        assertEquals("2026-11-16", primeiraSemana.getSemanaInicio());
        assertEquals(
                List.of("2026-11-21", "2026-11-22"),
                primeiraSemana.getBlocos().stream().map(Bloco::getData).toList()
        );
    }
    @Test
    void pulaPrimeiraSemanaSemTurnosNoPeriodoEContinuaNasSeguintes()
    {
        String usuarioId = "usuario";
        ClienteSocket socket = mock(ClienteSocket.class);
        RotinaService rotinaService = mock(RotinaService.class);
        CronogramaService servico =
                new CronogramaService(socket, rotinaService, new GeradorDeCronograma());

        Rotina rotina = new Rotina(
                usuarioId,
                "ENEM",
                List.of(new TurnoLivre(1, TurnoLivre.MANHA)),
                List.of("Matemática")
        );

        when(rotinaService.buscarPorUsuario(usuarioId)).thenReturn(Optional.of(rotina));
        when(socket.enviar(any(PedidoParaBuscarCronograma.class)))
                .thenReturn(new RespostaDeCronograma(null));
        when(socket.enviar(any(PedidoParaSalvarCronograma.class)))
                .thenAnswer(chamada -> {
                    PedidoParaSalvarCronograma pedido = chamada.getArgument(0);
                    return new RespostaDeCronograma(pedido.getCronograma());
                });

        servico.gerarPrimeiroMes(usuarioId, LocalDate.of(2026, 11, 21));

        var gravacoes = forClass(PedidoParaSalvarCronograma.class);
        verify(socket, atLeastOnce()).enviar(gravacoes.capture());

        List<String> semanasGravadas = gravacoes.getAllValues().stream()
                .map(PedidoParaSalvarCronograma::getCronograma)
                .map(Cronograma::getSemanaInicio)
                .toList();

        assertEquals(
                List.of("2026-11-23", "2026-11-30", "2026-12-07", "2026-12-14"),
                semanasGravadas
        );
    }

    @Test
    void retomaPeriodoOriginalDepoisDeSalvarRotinaNovamente()
    {
        String usuarioId = "usuario";
        ClienteSocket socket = mock(ClienteSocket.class);
        RotinaService rotinaService = mock(RotinaService.class);
        CronogramaService servico =
                new CronogramaService(socket, rotinaService, new GeradorDeCronograma());

        LocalDate inicio = LocalDate.of(2026, 11, 21);
        ZoneId fuso = ZoneId.systemDefault();
        List<TurnoLivre> turnos = IntStream.rangeClosed(1, 7)
                .mapToObj(dia -> new TurnoLivre(dia, TurnoLivre.MANHA))
                .toList();
        Rotina rotina = new Rotina(
                usuarioId, "ENEM", turnos, List.of("Matemática"),
                inicio.plusDays(1).atStartOfDay(fuso).toInstant().toEpochMilli(),
                inicio.atStartOfDay(fuso).toInstant().toEpochMilli()
        );
        Cronograma semanaExistente = new Cronograma(usuarioId, "2026-11-16", List.of());

        when(rotinaService.buscarPorUsuario(usuarioId)).thenReturn(Optional.of(rotina));
        when(socket.enviar(any(PedidoParaBuscarCronograma.class)))
                .thenAnswer(chamada -> {
                    PedidoParaBuscarCronograma pedido = chamada.getArgument(0);
                    Cronograma encontrado = semanaExistente.getSemanaInicio().equals(pedido.getSemanaInicio())
                            ? semanaExistente : null;
                    return new RespostaDeCronograma(encontrado);
                });
        when(socket.enviar(any(PedidoParaSalvarCronograma.class)))
                .thenAnswer(chamada -> {
                    PedidoParaSalvarCronograma pedido = chamada.getArgument(0);
                    return new RespostaDeCronograma(pedido.getCronograma());
                });

        servico.gerarPrimeiroMes(usuarioId);

        var gravacoes = forClass(PedidoParaSalvarCronograma.class);
        verify(socket, atLeastOnce()).enviar(gravacoes.capture());

        List<String> datasGravadas = gravacoes.getAllValues().stream()
                .flatMap(pedido -> pedido.getCronograma().getBlocos().stream())
                .map(Bloco::getData)
                .toList();

        assertEquals(
                LocalDate.of(2026, 11, 23).datesUntil(LocalDate.of(2026, 12, 21))
                        .map(LocalDate::toString).toList(),
                datasGravadas
        );
    }
}
