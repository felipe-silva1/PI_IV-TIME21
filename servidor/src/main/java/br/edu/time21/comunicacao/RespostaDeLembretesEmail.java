package br.edu.time21.comunicacao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RespostaDeLembretesEmail extends Resposta
{
    private static final long serialVersionUID = 1L;

    private ArrayList<LembreteDeEmail> lembretes;

    public RespostaDeLembretesEmail(List<LembreteDeEmail> lembretes)
    {
        this.lembretes = lembretes == null ? null : new ArrayList<>(lembretes);
    }

    public List<LembreteDeEmail> getLembretes()
    {
        return this.lembretes == null ? null : Collections.unmodifiableList(this.lembretes);
    }
}
