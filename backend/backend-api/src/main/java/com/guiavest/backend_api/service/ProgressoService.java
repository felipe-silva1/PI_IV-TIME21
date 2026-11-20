package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.Cronograma;
import com.guiavest.backend_api.model.ProgressoSemanal;
import com.guiavest.backend_api.service.CronogramaService.CronogramaException;
import org.springframework.stereotype.Service;

@Service
public class ProgressoService
{
    private final CronogramaService cronogramaService;
    private final CalculadoraDeProgressoSemanal calculadora;

    public ProgressoService(
            CronogramaService cronogramaService,
            CalculadoraDeProgressoSemanal calculadora)
    {
        this.cronogramaService = cronogramaService;
        this.calculadora = calculadora;
    }

    public ProgressoSemanal calcularDaSemana(String usuarioId, String semana)
    {
        if (usuarioId == null || usuarioId.isBlank())
            throw new ProgressoException(401, "Sessão inválida ou expirada.");

        Cronograma cronograma;
        try
        {
            cronograma = cronogramaService.buscarDaSemana(usuarioId, semana)
                    .orElse(null);
        }
        catch (CronogramaException erro)
        {
            throw new ProgressoException(erro.getStatus(), erro.getMessage());
        }

        if (cronograma == null)
            throw new ProgressoException(
                    404, "Cronograma ainda não gerado para esta semana.");

        return calculadora.calcular(cronograma.getBlocos());
    }

    public static class ProgressoException extends RuntimeException
    {
        private final int status;

        public ProgressoException(int status, String mensagem)
        {
            super(mensagem);
            this.status = status;
        }

        public int getStatus() { return status; }
    }
}
