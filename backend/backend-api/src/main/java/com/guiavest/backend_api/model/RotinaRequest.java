package com.guiavest.backend_api.model;

import java.util.List;

public record RotinaRequest (
    String vestibular,
    List<TurnoItem> disponibilidade,
    List<String> materiasPrioritarias
) {
    public record TurnoItem(int diaSemana, String turno) {}
}
