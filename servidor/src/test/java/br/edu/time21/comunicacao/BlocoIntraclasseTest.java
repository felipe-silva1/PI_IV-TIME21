package br.edu.time21.comunicacao;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlocoIntraclasseTest {

    // Categoria: CONSTRUTOR (novo bloco pendente) + CONSULTORES
    @Test
    void construtorDeNovoBlocoDefineEstadoInicialPendenteESemId() {
        Bloco bloco = new Bloco(
                "2026-11-03", TurnoLivre.NOITE, "Matemática", 90,
                List.of(new Topico("Funções", false))
        );

        assertNull(bloco.getId());
        assertEquals("2026-11-03", bloco.getData());
        assertEquals(TurnoLivre.NOITE, bloco.getTurno());
        assertEquals("Matemática", bloco.getMateria());
        assertEquals(90, bloco.getDuracaoMin());
        assertEquals("", bloco.getDescricao());
        assertFalse(bloco.isConcluido());
        assertEquals(0L, bloco.getConcluidoEm());
        assertEquals(1, bloco.getTopicos().size());
    }

    // Categoria: CONSTRUTOR (8 parâmetros) + CONSULTORES
    @Test
    void construtorDeBlocoPersistidoSemDescricaoRetornaDescricaoVazia() {
        Bloco bloco = new Bloco(
                "65f1c4d9e1b2c3d4e5f6071a", "2026-11-03", TurnoLivre.TARDE,
                "Física", 60, List.of(), true, 1784476800000L
        );

        assertEquals("65f1c4d9e1b2c3d4e5f6071a", bloco.getId());
        assertEquals("", bloco.getDescricao());
        assertTrue(bloco.isConcluido());
        assertEquals(1784476800000L, bloco.getConcluidoEm());
    }

    // Categoria: CONSTRUTOR COMPLETO + CONSULTORES (tratamento de nulos e imutabilidade)
    @Test
    void getDescricaoConverteNuloParaStringVaziaEProtegeListaDeTopicos() {
        Bloco blocoNulo = new Bloco(
                "id-1", "2026-11-03", TurnoLivre.MANHA, "Química",
                90, null, null, false, 0L
        );
        assertEquals("", blocoNulo.getDescricao());
        assertNull(blocoNulo.getTopicos());

        Bloco blocoComTopico = new Bloco(
                "id-2", "2026-11-03", TurnoLivre.MANHA, "Química",
                90, "Resolver lista", List.of(new Topico("Estequiometria", false)), false, 0L
        );
        assertEquals("Resolver lista", blocoComTopico.getDescricao());
        assertThrows(UnsupportedOperationException.class,
                () -> blocoComTopico.getTopicos().add(new Topico("Orgânica", false)));
    }

    // Categoria: REPRESENTAÇÃO (toString) — evidencia a Anomalia 1
    @Test
    void toStringContemCamposPrincipaisDoBloco() {
        Bloco bloco = new Bloco(
                "65f1c4d9e1b2c3d4e5f6071a", "2026-11-03", TurnoLivre.NOITE,
                "Matemática", 90, "Exercícios de funções", List.of(), true, 1784476800000L
        );

        String texto = bloco.toString();
        assertTrue(texto.contains("id=65f1c4d9e1b2c3d4e5f6071a"));
        assertTrue(texto.contains("materia=Matemática"));
        assertTrue(texto.contains("concluido=true"));
        // Observação: a anomalia documentada indica que "descricao" não está presente em toString()
        assertFalse(texto.contains("Exercícios de funções"));
    }
}