//Max Thomazini Barbosa 25003934
package com.guiavest.backend_api.model;

// Transporta os dados completos enviados pelo formulário de edição da tarefa.
public record TarefaUpdateRequest(
        String data,
        String turno,
        String materia,
        Integer duracaoMin,
        String descricao,
        Boolean concluido
) {}
