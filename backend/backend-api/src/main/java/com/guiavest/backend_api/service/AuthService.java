package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.PedidoDeCadastro;
import br.edu.time21.comunicacao.PedidoDeLogin;
import br.edu.time21.comunicacao.Resposta;
import br.edu.time21.comunicacao.RespostaDeConta;
import br.edu.time21.comunicacao.RespostaDeErro;
import com.guiavest.backend_api.model.AuthSessionResponse;
import com.guiavest.backend_api.model.SessionUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService
{
    private static final long DURACAO_SESSAO_MS = 24 * 60 * 60 * 1000L;

    private final ClienteSocket clienteSocket;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, Sessao> sessoes = new ConcurrentHashMap<>();

    public AuthService(ClienteSocket clienteSocket)
    {
        this.clienteSocket = clienteSocket;
    }

    public RespostaDeConta cadastrar(String nome, String email, String senha)
    {
        String senhaHash = this.passwordEncoder.encode(senha);
        Resposta resposta = this.clienteSocket.enviar(
                new PedidoDeCadastro(nome, email, senhaHash));
        if (resposta instanceof RespostaDeConta conta)
            return conta;
        if (resposta instanceof RespostaDeErro erro
                && RespostaDeErro.PEDIDO_INVALIDO.equals(erro.getCodigo()))
            throw new AuthException(400, "Verifique os dados informados.");
        throw new AuthException(503, "Não foi possível acessar o serviço de contas.");
    }

    public AuthSessionResponse entrar(String email, String senha)
    {
        Resposta resposta = this.clienteSocket.enviar(new PedidoDeLogin(email));
        if (resposta instanceof RespostaDeErro erro
                && RespostaDeErro.PEDIDO_INVALIDO.equals(erro.getCodigo()))
            throw new AuthException(400, "Informe um e-mail válido.");
        if (!(resposta instanceof RespostaDeConta conta))
            throw new AuthException(503, "Não foi possível acessar o serviço de contas.");
        if (RespostaDeConta.BANCO_INDISPONIVEL.equals(conta.getCodigo()))
            throw new AuthException(503, "Não foi possível acessar o banco de dados.");
        if (!RespostaDeConta.OK.equals(conta.getCodigo())
                || conta.getSenhaHash() == null
                || !this.passwordEncoder.matches(senha, conta.getSenhaHash()))
            throw new AuthException(401, "E-mail ou senha incorretos.");

        return criarSessao(conta);
    }

    public AuthSessionResponse criarSessao(RespostaDeConta conta)
    {
        byte[] bytes = new byte[32];
        this.secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long expiraEm = System.currentTimeMillis() + DURACAO_SESSAO_MS;
        this.sessoes.put(token, new Sessao(conta.getUsuarioId(), conta.getNome(), expiraEm));
        return new AuthSessionResponse(token, conta.getUsuarioId(), conta.getNome());
    }

    public Optional<SessionUser> usuarioDaSessao(String token)
    {
        if (token == null || token.isBlank())
            return Optional.empty();

        Sessao sessao = this.sessoes.get(token);
        if (sessao == null)
            return Optional.empty();
        if (sessao.expiraEm() <= System.currentTimeMillis())
        {
            this.sessoes.remove(token, sessao);
            return Optional.empty();
        }
        return Optional.of(new SessionUser(sessao.usuarioId(), sessao.nome()));
    }

    public void encerrarSessao(String token)
    {
        if (token != null)
            this.sessoes.remove(token);
    }

    public static boolean senhaValidaParaHash(String senha)
    {
        return senha != null
                && senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private record Sessao(String usuarioId, String nome, long expiraEm) {}

    public static class AuthException extends RuntimeException
    {
        private final int status;

        public AuthException(int status, String mensagem)
        {
            super(mensagem);
            this.status = status;
        }

        public int getStatus() { return this.status; }
    }
}
