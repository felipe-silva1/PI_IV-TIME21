package com.guiavest.backend_api.controller;

import br.edu.time21.comunicacao.RespostaDeConta;
import com.guiavest.backend_api.model.AuthRequest;
import com.guiavest.backend_api.model.AuthSessionResponse;
import com.guiavest.backend_api.model.SessionUser;
import com.guiavest.backend_api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest
{
    @Test
    void cadastroRecusaEmailInvalidoAntesDeAcessarServidor()
    {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);

        ResponseEntity<?> resposta = controller.cadastrar(
                new AuthRequest("Estudante", "email-invalido", "senha-segura-123"));

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        verifyNoInteractions(authService);
    }

    @Test
    void cadastroRecusaEmailJaCadastrado()
    {
        AuthService authService = mock(AuthService.class);
        when(authService.cadastrar("Estudante", "estudante@example.com",
                "senha-segura-123")).thenReturn(new RespostaDeConta(
                        RespostaDeConta.EMAIL_EM_USO, null, null, null));
        AuthController controller = new AuthController(authService);

        ResponseEntity<?> resposta = controller.cadastrar(new AuthRequest(
                "Estudante", "estudante@example.com", "senha-segura-123"));

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals("Já existe uma conta com este e-mail.",
                ((Map<?, ?>) resposta.getBody()).get("message"));
    }

    @Test
    void sessaoBearerReconheceUsuarioNasProximasRequisicoes()
    {
        AuthService authService = mock(AuthService.class);
        when(authService.usuarioDaSessao("token-valido")).thenReturn(
                Optional.of(new SessionUser("id-1", "Estudante")));
        AuthController controller = new AuthController(authService);

        ResponseEntity<?> resposta = controller.sessao("Bearer token-valido");

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(new SessionUser("id-1", "Estudante"), resposta.getBody());
        verify(authService).usuarioDaSessao("token-valido");
    }

    @Test
    void loginRetornaTokenDeSessao()
    {
        AuthService authService = mock(AuthService.class);
        AuthSessionResponse sessao =
                new AuthSessionResponse("token-valido", "id-1", "Estudante");
        when(authService.entrar("estudante@example.com", "senha-segura-123"))
                .thenReturn(sessao);
        AuthController controller = new AuthController(authService);

        AuthSessionResponse resposta = controller.entrar(new AuthRequest(
                null, "estudante@example.com", "senha-segura-123"));

        assertEquals(sessao, resposta);
    }

    @Test
    void erroDeLoginDevolveMensagemLegivel()
    {
        AuthController controller = new AuthController(mock(AuthService.class));

        ResponseEntity<Map<String, String>> resposta = controller.erroDeAutenticacao(
                new AuthService.AuthException(401, "E-mail ou senha incorretos."));

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertEquals("E-mail ou senha incorretos.", resposta.getBody().get("message"));
    }
}
