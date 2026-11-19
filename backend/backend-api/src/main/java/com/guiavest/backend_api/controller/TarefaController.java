//Autor: Edson Luan Rocha RA: 25897620
package com.guiavest.backend_api.controller;

import br.edu.time21.comunicacao.Bloco;
import com.guiavest.backend_api.model.SessionUser;
import com.guiavest.backend_api.model.TarefaStatusRequest;
import com.guiavest.backend_api.service.AuthService;
import com.guiavest.backend_api.service.TarefaService;
import com.guiavest.backend_api.service.TarefaService.TarefaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tarefas")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class TarefaController
{
    private final AuthService authService;
    private final TarefaService tarefaService;

    public TarefaController(AuthService authService, TarefaService tarefaService)
    {
        this.authService = authService;
        this.tarefaService = tarefaService;
    }

    @GetMapping
    public ResponseEntity<List<Bloco>> buscarPorDia(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("data") String data)
    {
        SessionUser usuario = autenticar(authorization);
        return ResponseEntity.ok(this.tarefaService.buscarPorDia(usuario.usuarioId(), data));
    }

    @PatchMapping("/{tarefaId}/status")
    public ResponseEntity<Bloco> atualizarStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("tarefaId") String tarefaId,
            @RequestBody(required = false) TarefaStatusRequest request)
    {
        SessionUser usuario = autenticar(authorization);
        Boolean concluido = request == null ? null : request.concluido();
        return ResponseEntity.ok(
                this.tarefaService.atualizarStatus(
                        usuario.usuarioId(), tarefaId, concluido));
    }

    @ExceptionHandler(TarefaException.class)
    public ResponseEntity<?> erroDeTarefa(TarefaException erro)
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
                .orElseThrow(() -> new TarefaException(401, "Sessão inválida ou expirada."));
    }
}
