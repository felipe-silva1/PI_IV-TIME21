package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.PedidoDeLogin;
import br.edu.time21.comunicacao.RespostaDeConta;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest
{
    @Test
    void loginValidoCriaSessaoVerificavel()
    {
        ClienteSocket clienteSocket = mock(ClienteSocket.class);
        String hash = new BCryptPasswordEncoder().encode("senha-segura-123");
        when(clienteSocket.enviar(any(PedidoDeLogin.class))).thenReturn(
                new RespostaDeConta(RespostaDeConta.OK, "id-1", "Estudante", hash));
        AuthService servico = new AuthService(clienteSocket);

        var resposta = servico.entrar("estudante@example.com", "senha-segura-123");

        assertEquals("id-1", resposta.usuarioId());
        assertEquals("Estudante", resposta.nome());
        assertTrue(servico.usuarioDaSessao(resposta.token()).isPresent());
    }

    @Test
    void loginInvalidoNaoCriaSessao()
    {
        ClienteSocket clienteSocket = mock(ClienteSocket.class);
        String hash = new BCryptPasswordEncoder().encode("senha-correta-123");
        when(clienteSocket.enviar(any(PedidoDeLogin.class))).thenReturn(
                new RespostaDeConta(RespostaDeConta.OK, "id-1", "Estudante", hash));
        AuthService servico = new AuthService(clienteSocket);

        AuthService.AuthException erro = assertThrows(AuthService.AuthException.class,
                () -> servico.entrar("estudante@example.com", "senha-incorreta"));

        assertEquals(401, erro.getStatus());
    }

    @Test
    void sessaoEncerradaDeixaDeSerValida()
    {
        ClienteSocket clienteSocket = mock(ClienteSocket.class);
        AuthService servico = new AuthService(clienteSocket);
        var resposta = servico.criarSessao(new RespostaDeConta(
                RespostaDeConta.OK, "id-1", "Estudante", null));

        servico.encerrarSessao(resposta.token());

        assertFalse(servico.usuarioDaSessao(resposta.token()).isPresent());
    }
}
