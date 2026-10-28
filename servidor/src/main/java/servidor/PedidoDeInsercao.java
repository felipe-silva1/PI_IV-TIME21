package servidor;

public class PedidoDeInsercao extends Comunicado
{
    private static final long serialVersionUID = 1L;
    private String colecao;
    private String documentoJson;

    public PedidoDeInsercao (String colecao, String documentoJson)
    { this.colecao = colecao; this.documentoJson = documentoJson; }

    public String getColecao ()       { return this.colecao; }
    public String getDocumentoJson () { return this.documentoJson; }
}