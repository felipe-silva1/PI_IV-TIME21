package br.edu.time21.comunicacao;

// resposta de PedidoParaSalvarRotina e de PedidoParaBuscarRotina.
// rotina e null quando a busca nao encontrou nada: usuario sem rotina
// cadastrada e um caso normal (primeiro acesso), nao um erro.
public class RespostaDeRotina extends Resposta
{
    private static final long serialVersionUID = 1L;

    private Rotina rotina;

    public RespostaDeRotina (Rotina rotina)
    {
        this.rotina = rotina;
    }

    public Rotina getRotina () { return this.rotina; }

    public String toString ()
    {
        return "rotina=" + this.rotina;
    }
}
