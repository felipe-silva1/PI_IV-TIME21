package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.PedidoParaBuscarRotina;
import br.edu.time21.comunicacao.PedidoParaSalvarRotina;
import br.edu.time21.comunicacao.Resposta;
import br.edu.time21.comunicacao.RespostaDeErro;
import br.edu.time21.comunicacao.RespostaDeRotina;
import br.edu.time21.comunicacao.Rotina;
import br.edu.time21.comunicacao.TurnoLivre;
import com.guiavest.backend_api.model.RotinaRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RotinaService
{
    private static final Map<String, Set<String>> CATALOGO_MATERIAS = Map.of(
            "ENEM", Set.of(
                    "Matemática", "Redação", "Português e Literatura", "Física",
                    "Química", "Biologia", "História", "Geografia",
                    "Filosofia e Sociologia", "Inglês / Espanhol"
            ),
            "FUVEST", Set.of(
                    "Português e Obras Obrigatórias", "Redação", "Matemática", "Física",
                    "Química", "Biologia", "História", "Geografia", "Inglês"
            ),
            "UNICAMP", Set.of(
                    "Português e Literatura", "Redação", "Matemática", "Física",
                    "Química", "Biologia", "História", "Geografia", "Inglês", "Interdisciplinares"
            ),
            "UNESP", Set.of(
                    "Português e Literatura", "Redação", "Matemática", "Física",
                    "Química", "Biologia", "História", "Geografia",
                    "Filosofia e Sociologia", "Inglês"
            ),
            "OUTROS", Set.of(
                    "Matemática", "Português", "Redação", "Física",
                    "Química", "Biologia", "História", "Geografia", "Inglês", "Atualidades"
            )
    );

    private static final Set<String> TURNOS_VALIDOS =
            Set.of(TurnoLivre.MANHA, TurnoLivre.TARDE, TurnoLivre.NOITE);

    private final ClienteSocket clienteSocket;

    public RotinaService(ClienteSocket clienteSocket)
    {
        this.clienteSocket = clienteSocket;
    }

    public Optional<Rotina> buscarPorUsuario(String usuarioId)
    {
        Resposta resposta = this.clienteSocket.enviar(new PedidoParaBuscarRotina(usuarioId));

        if (resposta instanceof RespostaDeRotina respRotina)
            return Optional.ofNullable(respRotina.getRotina());

        if (resposta instanceof RespostaDeErro erro)
            throw traduzirErro(erro);

        throw new RotinaException(503, "Não foi possível consultar a rotina no momento.");
    }

    public Rotina salvar(String usuarioId, RotinaRequest request)
    {
        if (request == null || request.vestibular() == null || request.vestibular().isBlank())
            throw new RotinaException(400, "Escolha um vestibular válido.");

        String vestibular = request.vestibular().trim().toUpperCase(Locale.ROOT);
        Set<String> materiasPermitidas = CATALOGO_MATERIAS.get(vestibular);
        if (materiasPermitidas == null)
            throw new RotinaException(400, "Escolha um vestibular válido.");

        if (request.disponibilidade() == null || request.disponibilidade().isEmpty())
            throw new RotinaException(400, "Selecione pelo menos um turno livre na semana.");

        Set<TurnoLivre> turnosUnicos = new LinkedHashSet<>();
        for (RotinaRequest.TurnoItem item : request.disponibilidade())
        {
            if (item == null || item.turno() == null)
                throw new RotinaException(400, "Turno inválido na disponibilidade.");

            String turnoNormalizado = item.turno().trim().toUpperCase(Locale.ROOT);
            if (item.diaSemana() < 1 || item.diaSemana() > 7 || !TURNOS_VALIDOS.contains(turnoNormalizado))
                throw new RotinaException(400, "Dia da semana ou turno inválido.");

            if (!turnosUnicos.add(new TurnoLivre(item.diaSemana(), turnoNormalizado)))
                throw new RotinaException(400, "Não repita o mesmo turno no mesmo dia.");
        }

        if (request.materiasPrioritarias() == null || request.materiasPrioritarias().isEmpty())
            throw new RotinaException(400, "Selecione pelo menos uma matéria prioritária.");

        Set<String> materiasUnicas = new LinkedHashSet<>();
        for (String materia : request.materiasPrioritarias())
        {
            if (materia == null || materia.isBlank())
                throw new RotinaException(400, "Matéria inválida na lista de prioridades.");

            String materiaLimpa = materia.trim();
            if (!materiasPermitidas.contains(materiaLimpa))
                throw new RotinaException(400, "Matéria inválida para o vestibular escolhido: " + materiaLimpa);

            if (!materiasUnicas.add(materiaLimpa))
                throw new RotinaException(400, "Não repita a mesma matéria na lista de prioridades.");
        }

        Rotina rotina = new Rotina(
                usuarioId,
                vestibular,
                new ArrayList<>(turnosUnicos),
                new ArrayList<>(materiasUnicas)
        );

        Resposta resposta = this.clienteSocket.enviar(new PedidoParaSalvarRotina(rotina));

        if (resposta instanceof RespostaDeRotina respRotina && respRotina.getRotina() != null)
            return respRotina.getRotina();

        if (resposta instanceof RespostaDeErro erro)
            throw traduzirErro(erro);

        throw new RotinaException(503, "Não foi possível salvar a rotina agora.");
    }

    private static RotinaException traduzirErro(RespostaDeErro erro)
    {
        if (RespostaDeErro.PEDIDO_INVALIDO.equals(erro.getCodigo()))
            return new RotinaException(400, "Confira os turnos e as matérias selecionadas.");
        if (RespostaDeErro.BANCO_INDISPONIVEL.equals(erro.getCodigo()))
            return new RotinaException(503, "Não foi possível acessar o banco de dados.");
        return new RotinaException(503, "Não foi possível acessar o serviço de rotinas.");
    }

    public static class RotinaException extends RuntimeException
    {
        private final int status;

        public RotinaException(int status, String mensagem)
        {
            super(mensagem);
            this.status = status;
        }

        public int getStatus() { return this.status; }
    }
}