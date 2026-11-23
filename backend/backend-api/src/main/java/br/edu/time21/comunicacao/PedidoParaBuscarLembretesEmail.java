package br.edu.time21.comunicacao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PedidoParaBuscarLembretesEmail extends Pedido
{
    private static final long serialVersionUID = 1L;

    private String data;
    private ArrayList<String> turnos;
    private String token;

    public PedidoParaBuscarLembretesEmail(String data, List<String> turnos, String token)
    {
        this.data = data;
        this.turnos = turnos == null ? null : new ArrayList<>(turnos);
        this.token = token;
    }

    public String getData() { return this.data; }
    public String getToken() { return this.token; }

    public List<String> getTurnos()
    {
        return this.turnos == null ? null : Collections.unmodifiableList(this.turnos);
    }
}
