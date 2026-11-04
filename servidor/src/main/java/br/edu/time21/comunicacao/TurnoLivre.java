package br.edu.time21.comunicacao;

import java.io.*;
import java.util.Objects;

// um turno livre na semana do estudante: um item de perfis.disponibilidade
// (ver docs/dicionario-banco.md). nao e Comunicado: so viaja dentro de uma
// Rotina.
public class TurnoLivre implements Serializable
{
    private static final long serialVersionUID = 1L;

    public static final String MANHA = "MANHA";
    public static final String TARDE = "TARDE";
    public static final String NOITE = "NOITE";

    private int    diaSemana;   // de 1 a 7
    private String turno;       // MANHA, TARDE ou NOITE

    public TurnoLivre (int diaSemana, String turno)
    {
        this.diaSemana = diaSemana;
        this.turno     = turno;
    }

    public int    getDiaSemana () { return this.diaSemana; }
    public String getTurno     () { return this.turno;     }

    // mesmo dia e mesmo turno: usado para recusar uma disponibilidade com
    // o mesmo par repetido
    public boolean equals (Object obj)
    {
        if (this==obj)
            return true;

        if (!(obj instanceof TurnoLivre))
            return false;

        TurnoLivre outro = (TurnoLivre)obj;
        return this.diaSemana==outro.diaSemana &&
               Objects.equals (this.turno, outro.turno);
    }

    public int hashCode ()
    {
        return Objects.hash (this.diaSemana, this.turno);
    }

    public String toString ()
    {
        return this.diaSemana + "/" + this.turno;
    }
}
