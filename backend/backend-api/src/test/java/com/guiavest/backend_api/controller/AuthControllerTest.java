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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest
{
    @Test
    void cadastroValidoCriaContaESessao()
    {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        RespostaDeConta conta =
                new RespostaDeConta(RespostaDeConta.OK, "usuario-1", "Estudante", null);
        AuthSessionResponse sessao =
                new AuthSessionResponse("token", "usuario-1", "Estudante");
        when(authService.cadastrar("Estudante", "estudante@example.com", "senha-segura"))
                .thenReturn(conta);
        when(authService.criarSessao(conta)).thenReturn(sessao);

        ResponseEntity<?> resposta = controller.cadastrar(
                new AuthRequest(" Estudante ", " Estudante@Example.com ",
                                "senha-segura"));

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertEquals(sessao, resposta.getBody());
        verify(authService).cadastrar("Estudante", "estudante@example.com", "senha-segura");
        verify(authService).criarSessao(conta);
    }

    @Test
    void cadastroInvalidoDevolveMensagemSemChamarServico()
    {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);

        ResponseEntity<?> resposta = controller.cadastrar(
                new AuthRequest("Estudante", "email-invalido", "senha-segura"));

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertInstanceOf(Map.class, resposta.getBody());
        assertEquals("Confira nome, e-mail e senha (mínimo de 8 caracteres).",
                ((Map<?, ?>) resposta.getBody()).get("message"));
        verify(authService, never()).cadastrar(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void cadastroSemCorpoDevolveErroDeValidacao()
    {
        AuthController controller = new AuthController(mock(AuthService.class));

        ResponseEntity<?> resposta = controller.cadastrar(null);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
    }

    @Test
    void emailJaCadastradoDevolveConflito()
    {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        when(authService.cadastrar("Estudante", "estudante@example.com", "senha-segura"))
                .thenReturn(new RespostaDeConta(
                        RespostaDeConta.EMAIL_EM_USO, null, null, null));

        ResponseEntity<?> resposta = controller.cadastrar(
                new AuthRequest("Estudante", "estudante@example.com", "senha-segura"));

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
    }

    @Test
    void loginValidoNormalizaEmailEDevolveSessao()
    {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        AuthSessionResponse sessao =
                new AuthSessionResponse("token", "usuario-1", "Estudante");
        when(authService.entrar("estudante@example.com", "senha-segura"))
                .thenReturn(sessao);

        AuthSessionResponse resposta = controller.entrar(
                new AuthRequest(null, "Estudante@Example.com", "senha-segura"));

        assertEquals(sessao, resposta);
        verify(authService).entrar("estudante@example.com", "senha-segura");
    }

    @Test
    void consultaSessaoUsaTokenBearer()
    {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        SessionUser usuario = new SessionUser("usuario-1", "Estudante");
        when(authService.usuarioDaSessao("token"))
                .thenReturn(Optional.of(usuario));

        ResponseEntity<?> resposta = controller.sessao("Bearer token");

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(usuario, resposta.getBody());
    }

    @Test
    void consultaSessaoInvalidaDevolveNaoAutorizado()
    {
        AuthController controller = new AuthController(mock(AuthService.class));

        ResponseEntity<?> resposta = controller.sessao(null);

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertTrue(((Map<?, ?>) resposta.getBody()).containsKey("message"));
    }
}
