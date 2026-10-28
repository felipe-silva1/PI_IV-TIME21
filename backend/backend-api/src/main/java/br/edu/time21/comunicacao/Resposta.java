package br.edu.time21.comunicacao;

// base de tudo que vai do servidor para o backend, inclusive dos avisos
// que o servidor manda sem ter sido perguntado (ComunicadoDeDesligamento)
public abstract class Resposta extends Comunicado
{
    private static final long serialVersionUID = 1L;
}
