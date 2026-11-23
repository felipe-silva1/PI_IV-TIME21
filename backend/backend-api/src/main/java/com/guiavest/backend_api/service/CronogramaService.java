//Arthur Berto de Castro RA: 25009917
package com.guiavest.backend_api.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    // Cliente responsável pela comunicação com o servidor Java.
    private final ClienteSocket clienteSocket;
    // Serviço usado para obter os dados da rotina configurada pelo estudante.
    private final RotinaService rotinaService;
    // Componente que distribui matérias e tópicos nos blocos de estudo.
    private final GeradorDeCronograma geradorDeCronograma;

    // Recebe as dependências necessárias para consultar rotina, gerar cronograma e persistir dados.
    public CronogramaService(
            ClienteSocket clienteSocket,
            RotinaService rotinaService,
            GeradorDeCronograma geradorDeCronograma)
    {
        this.clienteSocket = clienteSocket;
        this.rotinaService = rotinaService;
        this.geradorDeCronograma = geradorDeCronograma;
    }

    // Busca o cronograma correspondente à segunda-feira informada pelo usuário.
    // Quando nenhuma semana é fornecida, o método usa automaticamente a semana atual.
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

    /**
     * Gera os blocos dos 30 dias a partir da criação da rotina
     * e preserva os cronogramas já gravados.
     *
     * O período começa na data em que a rotina foi criada. Se essa informação
     * não estiver disponível, a data atual é usada como ponto inicial.
     */
    public void gerarPrimeiroMes(String usuarioId)
    {
        Rotina rotina = buscarRotina(usuarioId);
        long criadoEm = rotina.getCriadoEm();
        LocalDate inicio = criadoEm > 0
                ? Instant.ofEpochMilli(criadoEm).atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        gerarPrimeiroMes(rotina, inicio);
    }

    // Sobrecarga usada por cenários internos que precisam informar uma data inicial.
    void gerarPrimeiroMes(String usuarioId, LocalDate inicio)
    {
        gerarPrimeiroMes(buscarRotina(usuarioId), inicio);
    }

    // Percorre as semanas do período de 30 dias e cria somente as semanas ainda inexistentes.
    private void gerarPrimeiroMes(Rotina rotina, LocalDate inicio)
    {
        String usuarioId = rotina.getUsuarioId();
        LocalDate fim = inicio.plusDays(29);
        LocalDate primeiraSegunda = inicio.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (LocalDate segunda = primeiraSegunda;
                !segunda.isAfter(fim);
                segunda = segunda.plusWeeks(1))
        {
            String semana = segunda.toString();

            // Preserva cronogramas já salvos, evitando sobrescrever dados do usuário.
            if (buscarDaSemana(usuarioId, semana).isPresent())
                continue;

            // Gera os blocos da semana e filtra apenas os dias dentro do período permitido.
            List<BlocoDeEstudo> blocosDoPeriodo = gerarBlocosDaSemana(rotina, segunda)
                    .stream()
                    .filter(bloco -> {
                        LocalDate data = LocalDate.parse(bloco.data());
                        return !data.isBefore(inicio) && !data.isAfter(fim);
                    })
                    .toList();

            if (!blocosDoPeriodo.isEmpty())
                salvarSemana(usuarioId, segunda, blocosDoPeriodo);
        }
    }

    // Gera uma semana específica novamente e envia o resultado para persistência.
    public Cronograma gerarESalvar(String usuarioId, String semanaTexto)
    {
        LocalDate segunda = resolverSegundaFeira(semanaTexto);
        List<BlocoDeEstudo> blocosGerados = gerarBlocosDaSemana(buscarRotina(usuarioId), segunda);
        return salvarSemana(usuarioId, segunda, blocosGerados);
    }

    // Consulta a rotina do usuário antes da geração.
    // A rotina é obrigatória porque define turnos disponíveis e matérias prioritárias.
    private Rotina buscarRotina(String usuarioId)
    {
        try
        {
            return this.rotinaService.buscarPorUsuario(usuarioId)
                    .orElseThrow(() -> new CronogramaException(
                            400, "Cadastre sua rotina antes de gerar o cronograma."));
        }
        catch (RotinaException erro)
        {
            throw new CronogramaException(erro.getStatus(), erro.getMessage());
        }
    }

    // Solicita ao gerador os blocos da semana e valida se alguma produção foi possível.
    private List<BlocoDeEstudo> gerarBlocosDaSemana(Rotina rotina, LocalDate segunda)
    {
        List<BlocoDeEstudo> blocosGerados = this.geradorDeCronograma.gerar(rotina, segunda);
        if (blocosGerados.isEmpty())
            throw new CronogramaException(
                    400, "Sua rotina não possui turnos ou matérias suficientes para gerar o cronograma.");

        return blocosGerados;
    }

    // Converte os modelos usados pela API para as classes do protocolo de comunicação.
    // Em seguida, cria o cronograma da semana e envia o pedido de salvamento ao servidor.
    private Cronograma salvarSemana(
            String usuarioId, LocalDate segunda, List<BlocoDeEstudo> blocosGerados)
    {
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

    // Normaliza o texto recebido para uma segunda-feira no formato ISO.
    // Datas inválidas são rejeitadas antes de qualquer consulta ou gravação.
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

    // Traduz os códigos retornados pelo servidor em erros HTTP compreensíveis pela API.
    private static CronogramaException traduzirErro(RespostaDeErro erro)
    {
        if (RespostaDeErro.PEDIDO_INVALIDO.equals(erro.getCodigo()))
            return new CronogramaException(400, "Dados do cronograma inválidos.");
        if (RespostaDeErro.BANCO_INDISPONIVEL.equals(erro.getCodigo()))
            return new CronogramaException(503, "Não foi possível acessar o banco de dados.");
        return new CronogramaException(503, "Não foi possível acessar o serviço de cronogramas.");
    }

    // Exceção específica do domínio que associa uma falha a um status HTTP.
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
