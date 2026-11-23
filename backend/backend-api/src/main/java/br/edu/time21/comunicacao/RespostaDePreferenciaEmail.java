package br.edu.time21.comunicacao;

public class RespostaDePreferenciaEmail extends Resposta
{
    private static final long serialVersionUID = 1L;

    private boolean emailAtivo;

    public RespostaDePreferenciaEmail(boolean emailAtivo)
    {
        this.emailAtivo = emailAtivo;
    }

    public boolean isEmailAtivo() { return this.emailAtivo; }
}
