//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

import java.io.*;
import java.util.*;

// cronograma de uma semana: o conjunto dos documentos do blocos_de_estudo com o mesmo usuarioId e semanaInicio 
// (ver docs/dicionario-banco.md). nao existe uma colecao "cronogramas"; esta classe so agrupa os blocos 
// para viajarem juntos dentro de PedidoParasalvarCronograma e RespostaDeCronograma
public class Cronograma implements Serializable{
    private static final long serialVersionUID = 1L;

    private String usuarioId;
    private String semanaInicio;
    private ArrayList<Bloco> blocos;

    public Cronograma(String usuarioId, String semanaInicio, List<Bloco> blocos)
    {
        this.usuarioId = usuarioId;
        this.semanaInicio = semanaInicio;
        this.blocos = blocos==null ? null: new ArrayList<Bloco> (blocos);
    }
    public String getUsuarioId() { return this.usuarioId; }
    public String getSemanaInicio() { return this.semanaInicio; }
    public List<Bloco> getBlocos() { return this.blocos==null ? null : Collections.unmodifiableList (this.blocos); }

    @Override
    public String toString()
    {
        return "usuarioId=" + this.usuarioId +
                ", semanaInicio=" + this.semanaInicio +
                ", blocos=" + this.blocos;
    }
}
