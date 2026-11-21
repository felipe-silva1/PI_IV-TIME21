package com.guiavest.backend_api.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.guiavest.backend_api.model.BlocoDeEstudo;
import com.guiavest.backend_api.service.RotinaService.RotinaException;

import br.edu.time21.comunicacao.Bloco;
import br.edu.time21.comunicacao.Cronograma;
import br.edu.time21.comunicacao.PedidoParaBuscarCronograma;
import br.edu.time21.comunicacao.PedidoParaSalvarCronograma;
import br.edu.time21.comunicacao.Resposta;
import br.edu.time21.comunicacao.RespostaDeCronograma;
import br.edu.time21.comunicacao.RespostaDeErro;
import br.edu.time21.comunicacao.Rotina;
import br.edu.time21.comunicacao.Topico;

@Service
public class CronogramaService
{
    private final ClienteSocket clienteSocket;
    private final RotinaService rotinaService;
    private final GeradorDeCronograma geradorDeCronograma;

    public CronogramaService(
            ClienteSocket clienteSocket,
            RotinaService rotinaService,
            GeradorDeCronograma geradorDeCronograma)
    {
        this.clienteSocket = clienteSocket;
        this.rotinaService = rotinaService;
        this.geradorDeCronograma = geradorDeCronograma;
    }

    public Optional<Cronograma> buscarDaSemana(String usuarioId, String semanaTexto)
    {
        LocalDate segunda = resolverSegundaFeira(semanaTexto);
        Resposta resposta = this.clienteSocket.enviar(
                new PedidoParaBuscarCronograma(usuarioId, segunda.toString()));

        if (resposta instanceof RespostaDeCronograma respCronograma)
            return Optional.ofNullable(respCronograma.getCronograma());

        if (resposta instanceof RespostaDeErro erro)
            throw traduzirErro(erro);

        throw new CronogramaException(503, "Não foi possível consultar o cronograma no momento.");
    }

    public void gerarPrimeiroMes(String usuarioId)
    {
        LocalDate segunda = resolverSegundaFeira(null);

        for(int indice = 0; indice < 4; indice++) {
            String semana = segunda.plusWeeks(indice).toString();

            if(buscarDaSemana(usuarioId, semana).isEmpty())
            {
                gerarESalvar(usuarioId, semana);
            }
        }
    }

    public Cronograma gerarESalvar(String usuarioId, String semanaTexto)
    {
        LocalDate segunda = resolverSegundaFeira(semanaTexto);

        Rotina rotina;
        try
        {
            rotina = this.rotinaService.buscarPorUsuario(usuarioId)
                    .orElseThrow(() -> new CronogramaException(
                            400, "Cadastre sua rotina antes de gerar o cronograma."));
        }
        catch (RotinaException erro)
        {
            throw new CronogramaException(erro.getStatus(), erro.getMessage());
        }

        List<BlocoDeEstudo> blocosGerados = this.geradorDeCronograma.gerar(rotina, segunda);
        if (blocosGerados.isEmpty())
            throw new CronogramaException(
                    400, "Sua rotina não possui turnos ou matérias suficientes para gerar o cronograma.");

        ArrayList<Bloco> blocosProtocolo = new ArrayList<>(blocosGerados.size());
        for (BlocoDeEstudo b : blocosGerados)
        {
            ArrayList<Topico> topicos = new ArrayList<>();
            if (b.topicos() != null)
            {
                for (BlocoDeEstudo.Topico t : b.topicos())
                    topicos.add(new Topico(t.nome(), t.concluido()));
            }
            blocosProtocolo.add(new Bloco(
                    b.data(),
                    b.turno(),
                    b.materia(),
                    b.duracaoMin(),
                    topicos
            ));
        }

        Cronograma novoCronograma = new Cronograma(
                usuarioId,
                segunda.toString(),
                blocosProtocolo
        );

        Resposta resposta = this.clienteSocket.enviar(new PedidoParaSalvarCronograma(novoCronograma));

        if (resposta instanceof RespostaDeCronograma respCronograma && respCronograma.getCronograma() != null)
            return respCronograma.getCronograma();

        if (resposta instanceof RespostaDeErro erro)
            throw traduzirErro(erro);

        throw new CronogramaException(503, "Não foi possível salvar o cronograma gerado.");
    }

    private static LocalDate resolverSegundaFeira(String semanaTexto)
    {
        if (semanaTexto == null || semanaTexto.isBlank())
            return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        try
        {
            LocalDate data = LocalDate.parse(semanaTexto.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        catch (DateTimeParseException erro)
        {
            throw new CronogramaException(400, "Data da semana inválida. Use o formato AAAA-MM-DD.");
        }
    }

    private static CronogramaException traduzirErro(RespostaDeErro erro)
    {
        if (RespostaDeErro.PEDIDO_INVALIDO.equals(erro.getCodigo()))
            return new CronogramaException(400, "Dados do cronograma inválidos.");
        if (RespostaDeErro.BANCO_INDISPONIVEL.equals(erro.getCodigo()))
            return new CronogramaException(503, "Não foi possível acessar o banco de dados.");
        return new CronogramaException(503, "Não foi possível acessar o serviço de cronogramas.");
    }

    public static class CronogramaException extends RuntimeException
    {
        private final int status;

        public CronogramaException(int status, String mensagem)
        {
            super(mensagem);
            this.status = status;
        }

        public int getStatus() { return this.status; }
    }
}
