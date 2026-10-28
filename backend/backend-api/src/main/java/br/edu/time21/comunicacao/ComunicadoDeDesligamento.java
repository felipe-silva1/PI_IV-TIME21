package br.edu.time21.comunicacao;

// aviso que o servidor manda a todos os conectados antes de se desativar.
// e uma Resposta por causa do sentido (servidor -> backend), mesmo nao
// sendo resposta de nenhum pedido.
public class ComunicadoDeDesligamento extends Resposta
{
    private static final long serialVersionUID = 1L;
}
