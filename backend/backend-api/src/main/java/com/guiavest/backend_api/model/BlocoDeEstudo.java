package com.guiavest.backend_api.model;

import java.util.List;

public record BlocoDeEstudo(
        String usuarioId,
        String semanaInicio,
        int diaSemana,
        String data,
        String turno,
        String materia,
        int duracaoMin,
        List<Topico> topicos,
        boolean concluido
) {
    public record Topico(String nome, boolean concluido) {}
}
