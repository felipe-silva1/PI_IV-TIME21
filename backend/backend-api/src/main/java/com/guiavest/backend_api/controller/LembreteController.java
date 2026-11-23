package com.guiavest.backend_api.controller;

import com.guiavest.backend_api.model.PreferenciaEmailRequest;
import com.guiavest.backend_api.model.PreferenciaLembreteResponse;
import com.guiavest.backend_api.model.SessionUser;
import com.guiavest.backend_api.service.AuthService;
import com.guiavest.backend_api.service.LembreteService;
import com.guiavest.backend_api.service.LembreteService.LembreteException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/lembretes")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class LembreteController
{
    private final AuthService authService;
    private final LembreteService lembreteService;

    public LembreteController(AuthService authService, LembreteService lembreteService)
    {
        this.authService = authService;
        this.lembreteService = lembreteService;
    }

    @GetMapping("/preferencias")
    public PreferenciaLembreteResponse buscarPreferencias(
            @RequestHeader(value = "Authorization", required = false) String authorization)
    {
        SessionUser usuario = autenticar(authorization);
        return this.lembreteService.buscarPreferenciaEmail(usuario.usuarioId());
    }

    @PutMapping("/preferencias/email")
    public PreferenciaLembreteResponse atualizarPreferenciaEmail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) PreferenciaEmailRequest request)
    {
        SessionUser usuario = autenticar(authorization);
        Boolean emailAtivo = request == null ? null : request.emailAtivo();
        return this.lembreteService.atualizarPreferenciaEmail(usuario.usuarioId(), emailAtivo);
    }

    @ExceptionHandler(LembreteException.class)
    public ResponseEntity<?> erroDeLembrete(LembreteException erro)
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
                .orElseThrow(() -> new LembreteException(401, "Sessão inválida ou expirada."));
    }
}
