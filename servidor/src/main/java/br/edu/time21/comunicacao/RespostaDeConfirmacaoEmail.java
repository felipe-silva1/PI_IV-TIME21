package br.edu.time21.comunicacao;

public class RespostaDeConfirmacaoEmail extends Resposta
{
    private static final long serialVersionUID = 1L;

    private boolean confirmado;

    public RespostaDeConfirmacaoEmail(boolean confirmado)
    {
        this.confirmado = confirmado;
    }

    public boolean isConfirmado() { return this.confirmado; }
}
