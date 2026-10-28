package com.guiavest.backend_api.controller;

import br.edu.time21.comunicacao.PedidoDeStatus;
import br.edu.time21.comunicacao.Resposta;
import br.edu.time21.comunicacao.RespostaDeErro;
import br.edu.time21.comunicacao.RespostaDeStatus;
import com.guiavest.backend_api.model.HealthResponse;
import com.guiavest.backend_api.service.ClienteSocket;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
// Libera o CORS especificamente para as URLs do Live Server
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class HealthController {

    private final ClienteSocket clienteSocket;

    public HealthController(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    @GetMapping("/health")
    public HealthResponse checkHealth() {
        Resposta resposta = clienteSocket.enviar(new PedidoDeStatus());

        if (resposta instanceof RespostaDeStatus status) {
            boolean bancoDisponivel = status.isBancoDisponivel();
            String situacao = bancoDisponivel ? "UP" : "DEGRADED";
            String mensagem = bancoDisponivel
                    ? "Servidor e banco de dados disponiveis."
                    : "Servidor disponivel, mas o banco de dados esta indisponivel.";

            return new HealthResponse(situacao, mensagem, true, bancoDisponivel);
        }

        if (resposta instanceof RespostaDeErro erro) {
            return new HealthResponse("DOWN",
                    "Servidor indisponivel para responder ao health check.",
                    false, false);
        }

        return new HealthResponse("DOWN",
                "Nao foi possivel verificar a disponibilidade do sistema.",
                false, false);
    }
}
