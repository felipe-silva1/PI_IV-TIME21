package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.LembreteDeEmail;
import br.edu.time21.comunicacao.PedidoParaAtualizarPreferenciaEmail;
import br.edu.time21.comunicacao.PedidoParaBuscarLembretesEmail;
import br.edu.time21.comunicacao.PedidoParaBuscarPreferenciaEmail;
import br.edu.time21.comunicacao.PedidoParaConfirmarEnvioEmail;
import br.edu.time21.comunicacao.Resposta;
import br.edu.time21.comunicacao.RespostaDeConfirmacaoEmail;
import br.edu.time21.comunicacao.RespostaDeErro;
import br.edu.time21.comunicacao.RespostaDeLembretesEmail;
import br.edu.time21.comunicacao.RespostaDePreferenciaEmail;
import com.guiavest.backend_api.model.PreferenciaLembreteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class LembreteService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LembreteService.class);
    private static final DateTimeFormatter HORA_FORMATADA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int JANELA_DE_ENVIO_MINUTOS = 5;
    private static final List<HorarioDeTurno> HORARIOS = List.of(
            new HorarioDeTurno("MANHA", LocalTime.of(5, 0)),
            new HorarioDeTurno("TARDE", LocalTime.of(14, 0)),
            new HorarioDeTurno("NOITE", LocalTime.of(18, 0)));

    private final ClienteSocket clienteSocket;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean emailHabilitado;
    private final String servidorSmtp;
    private final String remetente;
    private final String tokenBackend;
    private final ZoneId fusoHorario;

    public LembreteService(
            ClienteSocket clienteSocket,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.lembretes.email-habilitado:false}") boolean emailHabilitado,
            @Value("${spring.mail.host:}") String servidorSmtp,
            @Value("${app.lembretes.remetente:}") String remetente,
            @Value("${app.lembretes.token:}") String tokenBackend,
            @Value("${app.lembretes.fuso-horario:America/Sao_Paulo}") String fusoHorario)
    {
        this.clienteSocket = clienteSocket;
        this.mailSenderProvider = mailSenderProvider;
        this.emailHabilitado = emailHabilitado;
        this.servidorSmtp = servidorSmtp;
        this.remetente = remetente;
        this.tokenBackend = tokenBackend;
        this.fusoHorario = ZoneId.of(fusoHorario);
    }

    public PreferenciaLembreteResponse buscarPreferenciaEmail(String usuarioId)
    {
        if (!tokenConfigurado())
            return new PreferenciaLembreteResponse(false, false);

        Resposta resposta = this.clienteSocket.enviar(
                new PedidoParaBuscarPreferenciaEmail(usuarioId, this.tokenBackend));
        if (resposta instanceof RespostaDePreferenciaEmail preferencia)
            return new PreferenciaLembreteResponse(
                    preferencia.isEmailAtivo(), emailDisponivel());
        if (resposta instanceof RespostaDeErro erro)
            throw traduzirErro(erro);
        throw new LembreteException(503, "Não foi possível consultar a preferência de e-mail.");
    }

    public PreferenciaLembreteResponse atualizarPreferenciaEmail(
            String usuarioId, Boolean emailAtivo)
    {
        if (emailAtivo == null)
            throw new LembreteException(400, "Informe se os lembretes por e-mail estão ativos.");
        if (emailAtivo && !emailDisponivel())
            throw new LembreteException(
                    503, "O envio por e-mail ainda não está configurado neste ambiente.");
        if (!tokenConfigurado())
            return new PreferenciaLembreteResponse(false, false);

        Resposta resposta = this.clienteSocket.enviar(
                new PedidoParaAtualizarPreferenciaEmail(
                        usuarioId, emailAtivo, this.tokenBackend));
        if (resposta instanceof RespostaDePreferenciaEmail preferencia)
            return new PreferenciaLembreteResponse(
                    preferencia.isEmailAtivo(), emailDisponivel());
        if (resposta instanceof RespostaDeErro erro)
            throw traduzirErro(erro);
        throw new LembreteException(503, "Não foi possível salvar a preferência de e-mail.");
    }

    @Scheduled(
            cron = "0 * * * * *",
            zone = "${app.lembretes.fuso-horario:America/Sao_Paulo}")
    public void enviarLembretesDevidos()
    {
        if (!emailDisponivel())
            return;

        LocalDateTime agora = LocalDateTime.now(this.fusoHorario).withSecond(0).withNano(0);
        List<String> turnos = turnosNaJanelaDeEnvio(agora.toLocalTime());
        if (turnos.isEmpty())
            return;

        Resposta resposta = this.clienteSocket.enviar(
                new PedidoParaBuscarLembretesEmail(
                        agora.toLocalDate().toString(), turnos, this.tokenBackend));
        if (resposta instanceof RespostaDeErro erro)
        {
            LOGGER.warn("Não foi possível buscar os lembretes de e-mail: {}", erro.getCodigo());
            return;
        }
        if (!(resposta instanceof RespostaDeLembretesEmail lembretes))
        {
            LOGGER.warn("O servidor devolveu uma resposta inesperada ao buscar lembretes de e-mail.");
            return;
        }

        if (lembretes.getLembretes() == null)
            return;

        for (LembreteDeEmail lembrete : lembretes.getLembretes())
            enviarLembrete(lembrete);
    }

    static List<String> turnosNaJanelaDeEnvio(LocalTime hora)
    {
        List<String> turnos = new ArrayList<>();
        for (HorarioDeTurno horario : HORARIOS)
        {
            if (!hora.isBefore(horario.hora()) &&
                hora.isBefore(horario.hora().plusMinutes(JANELA_DE_ENVIO_MINUTOS)))
                turnos.add(horario.turno());
        }
        return List.copyOf(turnos);
    }

    private void enviarLembrete(LembreteDeEmail lembrete)
    {
        JavaMailSender mailSender = this.mailSenderProvider.getIfAvailable();
        if (mailSender == null)
        {
            LOGGER.error("SMTP está habilitado, mas não há um remetente de e-mail configurado.");
            return;
        }

        String horario = horarioDoTurno(lembrete.getTurno());
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(this.remetente);
        mensagem.setTo(lembrete.getEmail());
        mensagem.setSubject("Hora de estudar: " + lembrete.getMateria());
        mensagem.setText("Olá! Seu bloco de " + lembrete.getMateria() +
                " está previsto para hoje às " + horario +
                ". Acesse o GuiaVest para acompanhar seu cronograma.");

        try
        {
            mailSender.send(mensagem);
        }
        catch (MailException | IllegalArgumentException erro)
        {
            LOGGER.error("Falha ao enviar lembrete por e-mail para o bloco {}.",
                    lembrete.getBlocoId(), erro);
            return;
        }

        Resposta confirmacao = this.clienteSocket.enviar(
                new PedidoParaConfirmarEnvioEmail(
                        lembrete.getUsuarioId(), lembrete.getBlocoId(), this.tokenBackend));
        if (confirmacao instanceof RespostaDeConfirmacaoEmail respostaConfirmacao &&
            respostaConfirmacao.isConfirmado())
            return;

        if (confirmacao instanceof RespostaDeErro erro)
            LOGGER.error("E-mail enviado, mas não foi possível registrar o envio do lembrete {}: {}",
                    lembrete.getBlocoId(), erro.getCodigo());
        else
            LOGGER.error("E-mail enviado, mas o servidor não confirmou o lembrete {}.",
                    lembrete.getBlocoId());
    }

    private boolean emailDisponivel()
    {
        return tokenConfigurado() && this.emailHabilitado &&
                this.servidorSmtp != null && !this.servidorSmtp.isBlank() &&
                this.remetente != null && !this.remetente.isBlank() &&
                this.mailSenderProvider.getIfAvailable() != null;
    }

    private boolean tokenConfigurado()
    {
        return this.tokenBackend != null && !this.tokenBackend.isBlank();
    }

    private static String horarioDoTurno(String turno)
    {
        return HORARIOS.stream()
                .filter(horario -> horario.turno().equals(turno))
                .map(horario -> horario.hora().format(HORA_FORMATADA))
                .findFirst()
                .orElse("horário programado");
    }

    private static LembreteException traduzirErro(RespostaDeErro erro)
    {
        if (RespostaDeErro.PEDIDO_INVALIDO.equals(erro.getCodigo()))
            return new LembreteException(400, "Dados de lembrete inválidos.");
        if (RespostaDeErro.BANCO_INDISPONIVEL.equals(erro.getCodigo()))
            return new LembreteException(503, "Não foi possível acessar o banco de dados.");
        return new LembreteException(503, "Não foi possível acessar o serviço de lembretes.");
    }

    private record HorarioDeTurno(String turno, LocalTime hora) {}

    public static class LembreteException extends RuntimeException
    {
        private final int status;

        public LembreteException(int status, String mensagem)
        {
            super(mensagem);
            this.status = status;
        }

        public int getStatus() { return this.status; }
    }
}
