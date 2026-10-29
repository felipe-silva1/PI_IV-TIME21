package com.guiavest.backend_api.controller;

import br.edu.time21.comunicacao.RespostaDeConta;
import com.guiavest.backend_api.model.AuthRequest;
import com.guiavest.backend_api.model.AuthSessionResponse;
import com.guiavest.backend_api.model.SessionUser;
import com.guiavest.backend_api.service.AuthService;
import com.guiavest.backend_api.service.AuthService.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class AuthController
{
    private final AuthService authService;

    public AuthController(AuthService authService)
    {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> cadastrar(@RequestBody AuthRequest request)
    {
        if (request == null || request.nome() == null || request.nome().isBlank()
                || request.email() == null || !emailValido(request.email())
                || request.senha() == null || request.senha().length() < 8
                || !AuthService.senhaValidaParaHash(request.senha()))
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Confira nome, e-mail e senha (mínimo de 8 caracteres)."));

        RespostaDeConta conta = this.authService.cadastrar(
                request.nome().trim(), request.email().trim().toLowerCase(),
                request.senha());
        if (RespostaDeConta.EMAIL_EM_USO.equals(conta.getCodigo()))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Já existe uma conta com este e-mail."));
        if (!RespostaDeConta.OK.equals(conta.getCodigo()))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Não foi possível criar a conta agora."));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.authService.criarSessao(conta));
    }

    @PostMapping("/login")
    public AuthSessionResponse entrar(@RequestBody AuthRequest request)
    {
        if (request == null || request.email() == null || !emailValido(request.email())
                || request.senha() == null || request.senha().isBlank())
            throw new AuthException(400, "Informe um e-mail e uma senha válidos.");

        return this.authService.entrar(request.email().trim().toLowerCase(),
                                       request.senha());
    }

    @GetMapping("/session")
    public ResponseEntity<?> sessao(
            @RequestHeader(value = "Authorization", required = false) String authorization)
    {
        Optional<SessionUser> usuario = this.authService.usuarioDaSessao(token(authorization));
        return usuario.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Sessão inválida ou expirada.")));
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> sair(
            @RequestHeader(value = "Authorization", required = false) String authorization)
    {
        this.authService.encerrarSessao(token(authorization));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> erroDeAutenticacao(AuthException erro)
    {
        return ResponseEntity.status(erro.getStatus())
                .body(Map.of("message", erro.getMessage()));
    }

    private static boolean emailValido(String email)
    {
        return email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private static String token(String authorization)
    {
        if (authorization == null || !authorization.startsWith("Bearer "))
            return null;
        return authorization.substring("Bearer ".length()).trim();
    }
}
