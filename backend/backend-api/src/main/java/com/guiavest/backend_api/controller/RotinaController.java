//Arthur Berto de Castro RA: 25009917
package com.guiavest.backend_api.controller;

import br.edu.time21.comunicacao.Rotina;
import com.guiavest.backend_api.model.RotinaRequest;
import com.guiavest.backend_api.model.SessionUser;
import com.guiavest.backend_api.service.AuthService;
import com.guiavest.backend_api.service.RotinaService;
import com.guiavest.backend_api.service.RotinaService.RotinaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rotinas")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class RotinaController
{
    // Serviços responsáveis pela autenticação e pelas operações da rotina.
    private final AuthService authService;
    private final RotinaService rotinaService;

    // Injeta os serviços usados pelos endpoints.
    public RotinaController(AuthService authService, RotinaService rotinaService)
    {
        this.authService = authService;
        this.rotinaService = rotinaService;
    }

    // Retorna a rotina cadastrada pelo usuário autenticado.
    @GetMapping
    public ResponseEntity<?> buscarRotina(
            @RequestHeader(value = "Authorization", required = false) String authorization)
    {
        SessionUser usuario = autenticar(authorization);
        Optional<Rotina> rotina = this.rotinaService.buscarPorUsuario(usuario.usuarioId());
        return rotina.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Rotina ainda não cadastrada.")));
    }

    // Cria ou atualiza a rotina do usuário autenticado.
    @PutMapping
    public ResponseEntity<Rotina> salvarRotina(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) RotinaRequest request)
    {
        SessionUser usuario = autenticar(authorization);
        return ResponseEntity.ok(this.rotinaService.salvar(usuario.usuarioId(), request));
    }

    // Mantém o POST como alternativa para salvar ou atualizar a rotina.
    @PostMapping
    public ResponseEntity<Rotina> criarRotina(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) RotinaRequest request)
    {
        return salvarRotina(authorization, request);
    }

    // Converte erros específicos da rotina em respostas HTTP.
    @ExceptionHandler(RotinaException.class)
    public ResponseEntity<?> erroDeRotina(RotinaException erro)
    {
        return ResponseEntity.status(erro.getStatus())
                .body(Map.of("message", erro.getMessage()));
    }

    // Valida o token Bearer e identifica o usuário da sessão.
    private SessionUser autenticar(String authorization)
    {
        String token = (authorization != null && authorization.startsWith("Bearer "))
                ? authorization.substring("Bearer ".length()).trim()
                : null;

        return this.authService.usuarioDaSessao(token)
                .orElseThrow(() -> new RotinaException(401, "Sessão inválida ou expirada."));
    }
}