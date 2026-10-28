package servidor;

public class PedidoDeBusca extends Comunicado
{
    private static final long serialVersionUID = 1L;
    private String colecao;
    private String filtroJson;   // ex.: {"nome":"Ana"}

    public PedidoDeBusca (String colecao, String filtroJson)
    { this.colecao = colecao; this.filtroJson = filtroJson; }

    public String getColecao ()    { return this.colecao; }
    public String getFiltroJson () { return this.filtroJson; }
}
