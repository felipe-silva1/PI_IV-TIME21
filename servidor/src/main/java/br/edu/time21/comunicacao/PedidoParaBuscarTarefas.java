//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

// busca as tarefas (blocos de estudo) de um usuario em uma data.
// respondido com RespostaDeTarefas ou RespostaDeErro.
public class PedidoParaBuscarTarefas extends Pedido
{
    private static final long serialVersionUID = 1L;

    private String usuarioId;
    private String data;

    public PedidoParaBuscarTarefas (String usuarioId, String data)
    {
        this.usuarioId = usuarioId;
        this.data = data;
    }

    public String getUsuarioId () { return this.usuarioId; }
    public String getData () { return this.data; }

    @Override
    public String toString ()
    {
        return "usuarioId=" + this.usuarioId + ", data=" + this.data;
    }
}
