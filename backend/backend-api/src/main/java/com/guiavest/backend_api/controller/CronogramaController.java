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
    private final AuthService authService;
    private final CronogramaService cronogramaService;

    public CronogramaController(AuthService authService, CronogramaService cronogramaService)
    {
        this.authService = authService;
        this.cronogramaService = cronogramaService;
    }

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

    @PostMapping("/gerar")
    public ResponseEntity<Cronograma> gerarCronograma(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "semana", required = false) String semana)
    {
        SessionUser usuario = autenticar(authorization);
        return ResponseEntity.ok(this.cronogramaService.gerarESalvar(usuario.usuarioId(), semana));
    }

    @PostMapping
    public ResponseEntity<Cronograma> criarCronograma(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "semana", required = false) String semana)
    {
        return gerarCronograma(authorization, semana);
    }

    @PutMapping
    public ResponseEntity<Cronograma> regerarCronograma(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "semana", required = false) String semana)
    {
        return gerarCronograma(authorization, semana);
    }

    @ExceptionHandler(CronogramaException.class)
    public ResponseEntity<?> erroDeCronograma(CronogramaException erro)
    {
        return ResponseEntity.status(erro.getStatus())
                .body(Map.of("message", erro.getMessage()));
    }

    private SessionUser autenticar(String authorization)
    {
        String token = (authorization != null && authorization.startsWith("Bearer "))
                ? authorization.substring("Bearer ".length()).trim()
                : null;

        return this.authService.usuarioDaSessao(token)
                .orElseThrow(() -> new CronogramaException(401, "Sessão inválida ou expirada."));
    }
}