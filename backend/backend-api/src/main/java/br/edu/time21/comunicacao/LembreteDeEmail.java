package br.edu.time21.comunicacao;

import java.io.Serializable;

public class LembreteDeEmail implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String blocoId;
    private String usuarioId;
    private String email;
    private String data;
    private String turno;
    private String materia;

    public LembreteDeEmail(
            String blocoId, String usuarioId, String email,
            String data, String turno, String materia)
    {
        this.blocoId = blocoId;
        this.usuarioId = usuarioId;
        this.email = email;
        this.data = data;
        this.turno = turno;
        this.materia = materia;
    }

    public String getBlocoId() { return this.blocoId; }
    public String getUsuarioId() { return this.usuarioId; }
    public String getEmail() { return this.email; }
    public String getData() { return this.data; }
    public String getTurno() { return this.turno; }
    public String getMateria() { return this.materia; }
}
