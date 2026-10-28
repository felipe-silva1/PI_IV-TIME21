package br.edu.time21.comunicacao;

// resposta do PedidoDeStatus, usada no teste da M0
public class RespostaDeStatus extends Resposta
{
    private static final long serialVersionUID = 1L;

    private String  situacao;
    private int     conexoesAtivas;
    private long    horaDoServidor;   // milissegundos desde 01/01/1970 (UTC)
    private boolean bancoDisponivel;

    public RespostaDeStatus (String  situacao,
                             int     conexoesAtivas,
                             long    horaDoServidor,
                             boolean bancoDisponivel)
    {
        this.situacao        = situacao;
        this.conexoesAtivas  = conexoesAtivas;
        this.horaDoServidor  = horaDoServidor;
        this.bancoDisponivel = bancoDisponivel;
    }

    public String  getSituacao        () { return this.situacao;        }
    public int     getConexoesAtivas  () { return this.conexoesAtivas;  }
    public long    getHoraDoServidor  () { return this.horaDoServidor;  }
    public boolean isBancoDisponivel  () { return this.bancoDisponivel; }

    public String toString ()
    {
        return "situacao="          + this.situacao        +
               ", conexoesAtivas="  + this.conexoesAtivas  +
               ", horaDoServidor="  + this.horaDoServidor  +
               ", bancoDisponivel=" + this.bancoDisponivel;
    }
}
