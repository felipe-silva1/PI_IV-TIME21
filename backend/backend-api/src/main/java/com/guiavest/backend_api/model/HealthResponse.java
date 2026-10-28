package com.guiavest.backend_api.model;

public class HealthResponse {
    private String status;
    private String mensagem;

    public HealthResponse(String status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
    }

    // Os Getters são OBRIGATÓRIOS para o Spring Boot converter para JSON
    public String getStatus() {
        return status;
    }

    public String getMensagem() {
        return mensagem;
    }
}
