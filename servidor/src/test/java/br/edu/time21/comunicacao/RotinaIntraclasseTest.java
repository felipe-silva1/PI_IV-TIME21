package br.edu.time21.comunicacao;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RotinaIntraclasseTest {

    // Particionamento por ATRIBUTOS: listas preenchidas e timestamps padrão (0L)
    @Test
    void construtorBasicoInicializaAtributosETimestampsZerados() {
        List<TurnoLivre> turnos = List.of(new TurnoLivre(1, TurnoLivre.MANHA));
        List<String> materias = List.of("Matemática", "Física");

        Rotina rotina = new Rotina("65f1c2a4e1b2c3d4e5f60718", "ENEM", turnos, materias);

        assertEquals("65f1c2a4e1b2c3d4e5f60718", rotina.getUsuarioId());
        assertEquals("ENEM", rotina.getVestibular());
        assertEquals(1, rotina.getDisponibilidade().size());
        assertEquals(List.of("Matemática", "Física"), rotina.getMateriasPrioritarias());
        assertEquals(0L, rotina.getAtualizadoEm());
        assertEquals(0L, rotina.getCriadoEm());
    }

    // Particionamento por ATRIBUTOS: isolamento por cópia defensiva e imutabilidade
    @Test
    void protegeListasContraMutacaoExternaERetornaVisaoSomenteLeitura() {
        ArrayList<TurnoLivre> turnos = new ArrayList<>(List.of(new TurnoLivre(1, TurnoLivre.NOITE)));
        ArrayList<String> materias = new ArrayList<>(List.of("Matemática"));

        Rotina rotina = new Rotina("65f1c2a4e1b2c3d4e5f60718", "FUVEST", turnos, materias);

        // Altera as listas originais após instanciar
        turnos.add(new TurnoLivre(2, TurnoLivre.TARDE));
        materias.add("Química");

        assertEquals(1, rotina.getDisponibilidade().size());
        assertEquals(1, rotina.getMateriasPrioritarias().size());

        // Verifica que os getters devolvem listas não modificáveis
        assertThrows(UnsupportedOperationException.class,
                () -> rotina.getDisponibilidade().add(new TurnoLivre(3, TurnoLivre.MANHA)));
        assertThrows(UnsupportedOperationException.class,
                () -> rotina.getMateriasPrioritarias().add("História"));
    }

    // Particionamento por ATRIBUTOS: listas nulas
    @Test
    void aceitaListasNulasSemLancarNullPointerException() {
        Rotina rotina = new Rotina("65f1c2a4e1b2c3d4e5f60718", "ENEM", null, null);

        assertNull(rotina.getDisponibilidade());
        assertNull(rotina.getMateriasPrioritarias());
    }

    // Particionamento por ATRIBUTOS: timestamps positivos (atualizadoEm e criadoEm)
    @Test
    void preservaTimestampsInformadosNoConstrutorCompleto() {
        Rotina rotina = new Rotina(
                "65f1c2a4e1b2c3d4e5f60718",
                "UNICAMP",
                List.of(new TurnoLivre(6, TurnoLivre.MANHA)),
                List.of("Redação"),
                1784476800000L,
                1784000000000L
        );

        assertEquals(1784476800000L, rotina.getAtualizadoEm());
        assertEquals(1784000000000L, rotina.getCriadoEm());
        assertTrue(rotina.toString().contains("UNICAMP"));
    }
}