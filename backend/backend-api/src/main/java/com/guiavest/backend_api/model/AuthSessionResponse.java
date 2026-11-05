package com.guiavest.backend_api.model;

public record AuthSessionResponse(String token, String usuarioId, String nome) {}
