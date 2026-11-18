//Autor: Felipe Augusto dos Santos Silva RA: 25003353
package br.edu.time21.comunicacao;

// busca o cronograma de um usuario em uma semana. respondido com
// RespostaDeCronograma (com cronograma null se nao ha blocos nessa semana)
// ou RespostaDeErro (PEDIDO_INVALIDO, BANCO_INDISPONIVEL ou ERRO_INTERNO)
public class PedidoParaBuscarCronograma extends Pedido {
    private static final long serialVersionUID = 1L;

    private String usuarioId; // ObjectId do usuario em hexadecimal (24 caracteres)
    private String semanaInicio; // segunda-feira da semana, yyyy-MM-dd

    public PedidoParaBuscarCronograma(String usuarioId, String semanaInicio)
    {
        this.usuarioId = usuarioId;
        this.semanaInicio = semanaInicio;
    }
    public String getUsuarioId() { return this.usuarioId; }
    public String getSemanaInicio() { return this.semanaInicio; }

    @Override
    public String toString()
    {
        return "usuarioId=" + this.usuarioId +
                ", semanaInicio=" + this.semanaInicio;
    }
}
