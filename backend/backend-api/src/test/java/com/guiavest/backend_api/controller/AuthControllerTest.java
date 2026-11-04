package com.guiavest.backend_api.controller;

import br.edu.time21.comunicacao.RespostaDeConta;
import com.guiavest.backend_api.model.AuthRequest;
import com.guiavest.backend_api.model.SessionUser;
import com.guiavest.backend_api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest
{
    @Test
    void cadastroRecusaEmailJaCadastrado()
    {
        AuthService authService = mock(AuthService.class);
        when(authService.cadastrar("Estudante", "estudante@example.com", "senha-segura"))
                .thenReturn(new RespostaDeConta(
                        RespostaDeConta.EMAIL_EM_USO, null, null, null));
        AuthController controller = new AuthController(authService);

        var resposta = controller.cadastrar(new AuthRequest(
                "Estudante", "Estudante@Example.com", "senha-segura"));

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        verify(authService, never()).criarSessao(any());
    }

    @Test
    void cadastroInvalidoNaoChamaServicoDeContas()
    {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);

        var resposta = controller.cadastrar(
                new AuthRequest("Estudante", "email-invalido", "senha-segura"));

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        verify(authService, never()).cadastrar(any(), any(), any());
    }

    @Test
    void sessaoAutenticadaIdentificaUsuario()
    {
        AuthService authService = mock(AuthService.class);
        SessionUser usuario = new SessionUser("id-1", "Estudante");
        when(authService.usuarioDaSessao("token")).thenReturn(Optional.of(usuario));
        AuthController controller = new AuthController(authService);

        var resposta = controller.sessao("Bearer token");

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(usuario, resposta.getBody());
    }
}
