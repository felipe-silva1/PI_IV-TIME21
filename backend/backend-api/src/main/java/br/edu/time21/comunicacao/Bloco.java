//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

import java.io.*;
import java.util.*;

// um bloco de estudo planejado: um documento da colecao blocos_de_estudo
// (ver docs/dicionario-banco.md). nao e Comunicado: viaja dentro de um Cronograma.
//
// usuarioId e semanaInicio nao ficam aqui: sao os mesmos para todos os blocos da semana e vivem no Cronograma que os contem.

public class Bloco implements Serializable 
{
    private static final long serialVersionUID = 1L;
    
    private String id; // ObjectId do bloco em hexadecimal; null antes de gravado
    private String data; // yyyy-MM-dd, dentro da semana do cronograma
    private String turno; // TurnoLivre.MANHA, TARDE ou NOITE
    private String materia;
    private int duracaoMin; // maior que zero
    private ArrayList<Topico> topicos; // pode ser vazia, nunca null depois de gravado
    private boolean concluido;
    private long concluidoEm; // milissegundos desde 01/01/1970 (UTC); 0 enquanto pendente


    //usado pelo backend para montar um cronograma recem-gerado: tudo pendente e ainda sem id
    public Bloco (String data, String turno, String materia, int duracaoMin, List<Topico> topicos)
    {
        this (null, data, turno, materia, duracaoMin, topicos, false, 0L);
    }
    public Bloco (String id, String data, String turno, String materia, int duracaoMin, List<Topico> topicos, boolean concluido, long concluidoEm)
    {
        this.id = id;
        this.data = data;
        this.turno = turno;
        this.materia = materia;
        this.duracaoMin = duracaoMin;
        this.topicos = topicos==null ? null:new ArrayList<Topico> (topicos);
        this.concluido = concluido;
        this.concluidoEm = concluidoEm;
    }
    public String getId() { return this.id; }
    public String getData() { return this.data; }
    public String getTurno() { return this.turno; }
    public String getMateria() { return this.materia; }
    public int getDuracaoMin() { return this.duracaoMin; }
    public List<Topico> getTopicos() { return this.topicos==null ? null : Collections.unmodifiableList (this.topicos); }
    public boolean isConcluido() { return this.concluido; }
    public long getConcluidoEm() { return this.concluidoEm; }

    @Override
    public String toString()
    {
        return "id=" + this.id + 
        ", data=" + this.data + 
        ", turno=" + this.turno + 
        ", materia=" + this.materia +
        ", duracaoMin=" + this.duracaoMin +
        ", topicos=" + this.topicos +
        ", concluido=" + this.concluido +
        ", concluidoEm=" + this.concluidoEm;
    }
}
