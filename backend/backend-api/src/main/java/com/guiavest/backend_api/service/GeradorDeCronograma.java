package com.guiavest.backend_api.service;

import br.edu.time21.comunicacao.Rotina;
import br.edu.time21.comunicacao.TurnoLivre;
import com.guiavest.backend_api.model.BlocoDeEstudo;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Algoritmo de geração do cronograma semanal de estudos do GuiaVest.
 *
 * REGRA DE DISTRIBUIÇÃO:
 * 1. Ordenação Cronológica: Os turnos livres informados pelo estudante são ordenados por dia da
 *    semana (1 = Segunda a 7 = Domingo) e, dentro do mesmo dia, na ordem MANHA -> TARDE -> NOITE.
 * 2. Cobertura Mínima: Se o número de turnos livres (T) for maior ou igual ao número de matérias (M),
 *    cada matéria recebe inicialmente 1 bloco garantido para que todas apareçam ao menos uma vez na semana.
 *    Se houver menos turnos que matérias (T < M), apenas as T matérias mais prioritárias recebem 1 bloco.
 * 3. Priorização por Peso: Os turnos restantes (T - M) são distribuídos proporcionalmente ao peso
 *    de prioridade de cada matéria (a 1ª matéria da lista tem peso M, a 2ª tem peso M-1, ..., até a
 *    última com peso 1). Eventuais sobras de arredondamento vão para as matérias do topo da lista.
 * 4. Alocação Anti-Fadiga (sem repetição seguida no mesmo dia): Para preencher cada turno livre,
 *    o algoritmo escolhe a matéria que possui mais blocos restantes a alocar e que NÃO seja igual
 *    à matéria alocada no turno imediatamente anterior do mesmo dia. Só repete a mesma matéria
 *    seguida no mesmo dia quando não houver nenhuma outra matéria com blocos restantes.
 */
@Service
public class GeradorDeCronograma
{
    public static final int DURACAO_PADRAO_MIN = 90;

    /**
     * Atalho que recebe o objeto Rotina completo e a data da segunda-feira da semana.
     */
    public List<BlocoDeEstudo> gerar(Rotina rotina, LocalDate semanaInicio)
    {
        if (rotina == null)
            throw new IllegalArgumentException("Rotina ausente");

        return gerar(
                rotina.getUsuarioId(),
                semanaInicio,
                rotina.getDisponibilidade(),
                rotina.getMateriasPrioritarias()
        );
    }

    /**
     * Método principal e puro de geração do cronograma (fácil de chamar direto nos testes unitários).
     */
    public List<BlocoDeEstudo> gerar(
            String usuarioId,
            LocalDate semanaInicio,
            List<TurnoLivre> disponibilidade,
            List<String> materiasComPrioridade)
    {
        if (disponibilidade == null || disponibilidade.isEmpty())
            return List.of();
        if (materiasComPrioridade == null || materiasComPrioridade.isEmpty())
            return List.of();

        // Garante uma segunda-feira válida como base da semana
        LocalDate segundaFeira = (semanaInicio != null ? semanaInicio : LocalDate.now())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 1. Limpa duplicatas mantendo a ordem e ordena os turnos cronologicamente
        List<TurnoLivre> turnosOrdenados = ordenarTurnos(disponibilidade);
        List<String> materias = limparMaterias(materiasComPrioridade);

        if (turnosOrdenados.isEmpty() || materias.isEmpty())
            return List.of();

        // 2. Calcula quantos blocos cada matéria terá na semana
        int[] cotasPorMateria = calcularCotas(turnosOrdenados.size(), materias.size());

        // 3. Aloca as matérias nos turnos evitando repetir em turnos seguidos no mesmo dia
        List<BlocoDeEstudo> cronograma = new ArrayList<>(turnosOrdenados.size());
        int diaAnterior = -1;
        int indiceMateriaAnterior = -1;

        for (TurnoLivre slot : turnosOrdenados)
        {
            int escolhida = escolherMateriaParaSlot(
                    cotasPorMateria,
                    slot.getDiaSemana() == diaAnterior ? indiceMateriaAnterior : -1
            );

            cotasPorMateria[escolhida]--;
            diaAnterior = slot.getDiaSemana();
            indiceMateriaAnterior = escolhida;

            LocalDate dataDoBloco = segundaFeira.plusDays(slot.getDiaSemana() - 1L);

            cronograma.add(new BlocoDeEstudo(
                    usuarioId,
                    segundaFeira.toString(),
                    slot.getDiaSemana(),
                    dataDoBloco.toString(),
                    slot.getTurno(),
                    materias.get(escolhida),
                    DURACAO_PADRAO_MIN,
                    List.of(),
                    false
            ));
        }

        return List.copyOf(cronograma);
    }

    private static List<TurnoLivre> ordenarTurnos(List<TurnoLivre> disponibilidade)
    {
        // LinkedHashSet remove eventuais pares (diaSemana, turno) repetidos
        List<TurnoLivre> unicos = new ArrayList<>(new LinkedHashSet<>(disponibilidade));
        unicos.removeIf(t -> t == null || t.getDiaSemana() < 1 || t.getDiaSemana() > 7 || ordemDoTurno(t.getTurno()) == 0);

        unicos.sort(Comparator
                .comparingInt(TurnoLivre::getDiaSemana)
                .thenComparingInt(t -> ordemDoTurno(t.getTurno())));
        return unicos;
    }

    private static int ordemDoTurno(String turno)
    {
        if (TurnoLivre.MANHA.equals(turno)) return 1;
        if (TurnoLivre.TARDE.equals(turno)) return 2;
        if (TurnoLivre.NOITE.equals(turno)) return 3;
        return 0;
    }

    private static List<String> limparMaterias(List<String> materias)
    {
        List<String> limpas = new ArrayList<>();
        for (String m : materias)
        {
            if (m != null && !m.isBlank() && !limpas.contains(m.trim()))
                limpas.add(m.trim());
        }
        return limpas;
    }

    /**
     * Calcula quantos blocos cada matéria (índice 0 = mais prioritária) vai receber.
     */
    private static int[] calcularCotas(int totalTurnos, int totalMaterias)
    {
        int[] cotas = new int[totalMaterias];

        // Se há menos turnos que matérias, apenas as mais prioritárias ganham 1 bloco
        if (totalTurnos <= totalMaterias)
        {
            for (int i = 0; i < totalTurnos; i++)
                cotas[i] = 1;
            return cotas;
        }

        // Passo A: Garante pelo menos 1 bloco para cada matéria
        for (int i = 0; i < totalMaterias; i++)
            cotas[i] = 1;

        int turnosExtras = totalTurnos - totalMaterias;

        // Passo B: Pesos decrescentes por prioridade (ex.: 3 matérias -> pesos 3, 2, 1)
        int somaDosPesos = totalMaterias * (totalMaterias + 1) / 2;
        int distribuidos = 0;

        for (int i = 0; i < totalMaterias; i++)
        {
            int peso = totalMaterias - i;
            int cotaExtraInteira = (turnosExtras * peso) / somaDosPesos;
            cotas[i] += cotaExtraInteira;
            distribuidos += cotaExtraInteira;
        }

        // Passo C: Sobras da divisão inteira vão para as matérias mais prioritárias, do topo para baixo
        int sobra = turnosExtras - distribuidos;
        for (int i = 0; i < sobra; i++)
        {
            cotas[i % totalMaterias]++;
        }

        return cotas;
    }

    /**
     * Escolhe a matéria com mais blocos restantes que seja diferente da matéria do turno anterior no mesmo dia.
     */
    private static int escolherMateriaParaSlot(int[] cotasRestantes, int indiceEvitado)
    {
        int melhorIndice = -1;
        int maiorCota = 0;

        // 1ª tentativa: procura a matéria com maior cota restante ignorando a matéria evitada
        for (int i = 0; i < cotasRestantes.length; i++)
        {
            if (i == indiceEvitado)
                continue;

            // Em caso de empate na cota restante, o '>' mantém o menor índice (maior prioridade)
            if (cotasRestantes[i] > maiorCota)
            {
                maiorCota = cotasRestantes[i];
                melhorIndice = i;
            }
        }

        if (melhorIndice != -1)
            return melhorIndice;

        // Fallback ("Quando possível"): se só sobrou a matéria evitada, usa ela mesma
        return indiceEvitado;
    }
}