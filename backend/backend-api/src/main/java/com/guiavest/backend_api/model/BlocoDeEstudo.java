//Arthur Berto de Castro RA: 25009917
package com.guiavest.backend_api.model;

import java.util.List;

// Representa um bloco de estudo associado a um usuário e a uma semana.
public record BlocoDeEstudo(
        // Identifica a semana, o dia e o turno definidos pelo usuário.
        String usuarioId,
        String semanaInicio,
        int diaSemana,
        String data,
        String turno,
        // Informa a matéria, a duração e os tópicos planejados.
        String materia,
        int duracaoMin,
        List<Topico> topicos,
        // Indica se o bloco de estudo foi concluído.
        boolean concluido
) {
    // Representa um tópico individual do bloco de estudo.
    public record Topico(String nome, boolean concluido) {}
}
