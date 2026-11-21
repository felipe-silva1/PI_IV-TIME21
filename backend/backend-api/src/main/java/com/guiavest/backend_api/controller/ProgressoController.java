//Max Thomazini Barbosa 25003934
package com.guiavest.backend_api.controller;

import com.guiavest.backend_api.model.ProgressoSemanal;
import com.guiavest.backend_api.model.SessionUser;
import com.guiavest.backend_api.service.AuthService;
import com.guiavest.backend_api.service.ProgressoService;
import com.guiavest.backend_api.service.ProgressoService.ProgressoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/progresso", "/api/tarefas/progresso-semanal"})
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class ProgressoController
{
    // Serviços usados para validar a sessão e calcular o progresso da semana.
    private final AuthService authService;
    private final ProgressoService progressoService;

    // O Spring injeta as dependências necessárias por construtor.
    public ProgressoController(AuthService authService, ProgressoService progressoService)
    {
        this.authService = authService;
        this.progressoService = progressoService;
    }

    @GetMapping({"", "/semanal", "/semana"})
    public ResponseEntity<ProgressoSemanal> calcularDaSemana(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "semana", required = false) String semana)
    {
        // Primeiro valida o token enviado pelo cliente.
        SessionUser usuario = autenticar(authorization);
        // Depois calcula e devolve o progresso associado ao usuário da sessão.
        return ResponseEntity.ok(
                progressoService.calcularDaSemana(usuario.usuarioId(), semana));
    }

    @ExceptionHandler(ProgressoException.class)
    public ResponseEntity<?> erroDeProgresso(ProgressoException erro)
    {
        // Converte a exceção da camada de negócio em resposta HTTP com status e mensagem.
        return ResponseEntity.status(erro.getStatus())
                .body(Map.of("message", erro.getMessage()));
    }

    private SessionUser autenticar(String authorization)
    {
        // Remove o prefixo Bearer e deixa apenas o token da sessão.
        String token = (authorization != null && authorization.startsWith("Bearer "))
                ? authorization.substring("Bearer ".length()).trim()
                : null;

        return authService.usuarioDaSessao(token)
                .orElseThrow(() ->
                        new ProgressoException(401, "Sessão inválida ou expirada."));
    }
}
