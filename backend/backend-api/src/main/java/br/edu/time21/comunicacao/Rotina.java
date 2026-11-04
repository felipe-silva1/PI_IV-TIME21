package br.edu.time21.comunicacao;

import java.io.*;
import java.util.*;

// rotina e metas do estudante: um documento da colecao perfis (ver
// docs/dicionario-banco.md). nao e Comunicado: viaja dentro de
// PedidoParaSalvarRotina e de RespostaDeRotina.
//
// as listas recebidas sao sempre copiadas para um ArrayList. o filtro de
// desserializacao do servidor libera java.util.ArrayList, mas recusaria
// outras implementacoes de List (List.of, Arrays.asList, subList...).
public class Rotina implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String                usuarioId;             // ObjectId do usuario em hexadecimal (24 caracteres)
    private String                vestibular;
    private ArrayList<TurnoLivre> disponibilidade;
    private ArrayList<String>     materiasPrioritarias;  // da maior para a menor prioridade
    private long                  atualizadoEm;          // milissegundos desde 01/01/1970 (UTC); 0 se ainda nao gravada

    // usado pelo backend para montar o PedidoParaSalvarRotina; quem preenche
    // atualizadoEm e o servidor, no momento em que grava
    public Rotina (String           usuarioId,
                   String           vestibular,
                   List<TurnoLivre> disponibilidade,
                   List<String>     materiasPrioritarias)
    {
        this (usuarioId, vestibular, disponibilidade, materiasPrioritarias, 0L);
    }

    public Rotina (String           usuarioId,
                   String           vestibular,
                   List<TurnoLivre> disponibilidade,
                   List<String>     materiasPrioritarias,
                   long             atualizadoEm)
    {
        this.usuarioId            = usuarioId;
        this.vestibular           = vestibular;
        this.disponibilidade      = Rotina.copia (disponibilidade);
        this.materiasPrioritarias = Rotina.copia (materiasPrioritarias);
        this.atualizadoEm         = atualizadoEm;
    }

    private static <T> ArrayList<T> copia (List<T> lista)
    {
        return lista==null ? null : new ArrayList<T> (lista);
    }

    private static <T> List<T> somenteLeitura (ArrayList<T> lista)
    {
        return lista==null ? null : Collections.unmodifiableList (lista);
    }

    public String           getUsuarioId            () { return this.usuarioId;  }
    public String           getVestibular           () { return this.vestibular; }
    public List<TurnoLivre> getDisponibilidade      () { return Rotina.somenteLeitura (this.disponibilidade);      }
    public List<String>     getMateriasPrioritarias () { return Rotina.somenteLeitura (this.materiasPrioritarias); }
    public long             getAtualizadoEm         () { return this.atualizadoEm; }

    public String toString ()
    {
        return "usuarioId="              + this.usuarioId            +
               ", vestibular="           + this.vestibular           +
               ", disponibilidade="      + this.disponibilidade      +
               ", materiasPrioritarias=" + this.materiasPrioritarias +
               ", atualizadoEm="         + this.atualizadoEm;
    }
}
