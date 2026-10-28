package br.edu.time21.comunicacao;

import java.io.*;

// raiz do protocolo: tudo que trafega pelo socket entre backend e servidor
// herda daqui, direta ou indiretamente, passando por Pedido ou Resposta.
//
// serialVersionUID NAO e herdado: cada classe serializavel precisa declarar
// o seu, senao a JVM calcula um valor a partir da assinatura da classe e
// qualquer alteracao de campo quebra a compatibilidade entre os dois lados.
public abstract class Comunicado implements Serializable
{
    private static final long serialVersionUID = 1L;
}
