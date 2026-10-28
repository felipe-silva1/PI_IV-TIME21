package servidor;

import java.util.ArrayList;

public class RespostaDeOperacao extends Comunicado
{
    private static final long serialVersionUID = 1L;
    private boolean           sucesso;
    private String            mensagem;
    private ArrayList<String> documentosJson;   // preenchido nas buscas

    public RespostaDeOperacao (boolean sucesso, String mensagem, ArrayList<String> documentosJson)
    { this.sucesso = sucesso; this.mensagem = mensagem; this.documentosJson = documentosJson; }

    public boolean           isSucesso ()       { return this.sucesso;        }
    public String            getMensagem ()     { return this.mensagem;       }
    public ArrayList<String> getDocumentosJson () { return this.documentosJson; }
}
