//Autor: Felipe Augusto dos Santos Silva RA: 25003353

package br.edu.time21.comunicacao;

import java.io.*;

// um topico d eum bloco d eestudo: um item de blocos_de_estudo.topicos
// (ver docs/dicionario-banco.md). nao e Comunicado: so viaja dentro de um Bloco.

public class Topico implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String nome;
    private boolean concluido;

    public Topico (String nome, boolean concluido)
    {
        this.nome = nome;
        this.concluido = concluido;
    }
    public String getNome() { return this.nome; }
    public boolean isConcluido() { return this.concluido; }
    
    @Override
    public String toString()
    {
        return this.nome + (this.concluido ? "(ok)" : "");
    }
}
