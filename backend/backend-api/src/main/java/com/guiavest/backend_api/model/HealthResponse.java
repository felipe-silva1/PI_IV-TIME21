package com.guiavest.backend_api.model;

public class HealthResponse {
    private String status;
    private String mensagem;
    private boolean servidorDisponivel;
    private boolean bancoDisponivel;

    public HealthResponse(String status, String mensagem) {
        this(status, mensagem, false, false);
    }

    public HealthResponse(String status, String mensagem,
                          boolean servidorDisponivel, boolean bancoDisponivel) {
        this.status = status;
        this.mensagem = mensagem;
        this.servidorDisponivel = servidorDisponivel;
        this.bancoDisponivel = bancoDisponivel;
    }

    // Os Getters são OBRIGATÓRIOS para o Spring Boot converter para JSON
    public String getStatus() {
        return status;
    }

    public String getMensagem() {
        return mensagem;
    }

    public boolean isServidorDisponivel() {
        return servidorDisponivel;
    }

    public boolean isBancoDisponivel() {
        return bancoDisponivel;
    }
}
