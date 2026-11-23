//Arthur Berto de Castro RA: 25009917
package com.guiavest.backend_api.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.guiavest.backend_api.model.SessionUser;
import com.guiavest.backend_api.service.AuthService;
import com.guiavest.backend_api.service.CronogramaService;
import com.guiavest.backend_api.service.CronogramaService.CronogramaException;

import br.edu.time21.comunicacao.Cronograma;

@RestController
@RequestMapping("/api/cronogramas")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class CronogramaController
{
    // Dependências responsáveis por validar a sessão e gerar os cronogramas.
    private final AuthService authService;
    private final CronogramaService cronogramaService;

    // Configura os serviços usados por todos os endpoints desta classe.
    public CronogramaController(AuthService authService, CronogramaService cronogramaService)
    {
        this.authService = authService;
        this.cronogramaService = cronogramaService;
    }

    // Busca o cronograma da semana informada para o usuário autenticado.
    // Quando o parâmetro "semana" não é enviado, o serviço usa a semana atual.
    @GetMapping
    public ResponseEntity<?> buscarCronograma(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "semana", required = false) String semana)
    {
        SessionUser usuario = autenticar(authorization);
        Optional<Cronograma> cronograma =
                this.cronogramaService.buscarDaSemana(usuario.usuarioId(), semana);

        return cronograma.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Cronograma ainda não gerado para esta semana.")));
    }

    /**
     * Gera o primeiro mês do usuário autenticado após a rotina ser cadastrada.
     * Retorna HTTP 204 para indicar que o processamento terminou sem conteúdo adicional.
     */
    @PostMapping("/gerar-primeiro-mes")
    public ResponseEntity<Void> gerarPrimeiroMes(
                @RequestHeader(value = "Authorization", required = false)
                String authorization) {
        SessionUser usuario = autenticar(authorization);
        cronogramaService.gerarPrimeiroMes(usuario.usuarioId());
        return ResponseEntity.noContent().build();
}

    // Gera e salva o cronograma da semana especificada.
    @PostMapping("/gerar")
    public ResponseEntity<Cronograma> gerarCronograma(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "semana", required = false) String semana)
    {
        SessionUser usuario = autenticar(authorization);
        return ResponseEntity.ok(this.cronogramaService.gerarESalvar(usuario.usuarioId(), semana));
    }

    // Permite criar um cronograma usando o método POST na rota base.
    @PostMapping
    public ResponseEntity<Cronograma> criarCronograma(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "semana", required = false) String semana)
    {
        return gerarCronograma(authorization, semana);
    }

    // Permite regerar ou substituir o cronograma pela rota PUT.
    @PutMapping
    public ResponseEntity<Cronograma> regerarCronograma(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "semana", required = false) String semana)
    {
        return gerarCronograma(authorization, semana);
    }

    // Traduz exceções do domínio em respostas HTTP padronizadas com status e mensagem.
    @ExceptionHandler(CronogramaException.class)
    public ResponseEntity<?> erroDeCronograma(CronogramaException erro)
    {
        return ResponseEntity.status(erro.getStatus())
                .body(Map.of("message", erro.getMessage()));
    }

    // Extrai o token Bearer do cabeçalho e verifica se ele pertence a uma sessão ativa.
    // Um token ausente ou expirado interrompe a requisição com uma exceção 401.
    private SessionUser autenticar(String authorization)
    {
        String token = (authorization != null && authorization.startsWith("Bearer "))
                ? authorization.substring("Bearer ".length()).trim()
                : null;

        return this.authService.usuarioDaSessao(token)
                .orElseThrow(() -> new CronogramaException(401, "Sessão inválida ou expirada."));
    }
}
