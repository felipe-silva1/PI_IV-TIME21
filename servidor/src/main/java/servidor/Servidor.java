//Autor: Arthur Berto de Castro RA: 25009917
package servidor;

import java.util.*;

import br.edu.time21.comunicacao.ComunicadoDeDesligamento;

public class Servidor {
    
    public static String PORTA_PADRAO = "3000";
    public static String MONGO_URI_PADRAO = "mongodb://localhost:27017";

    // banco com os dados da aplicacao (docs/dicionario-banco.md); o log de
    // chamadas continua em RepositorioDeChamadas.BANCO_PADRAO. o MongoDB
    // diferencia maiusculas: tem de ser igual ao nome no Atlas
    public static String BANCO_GUIAVEST = "guiavest";

    // Obtém configuração dos argumentos, das variáveis de ambiente ou do padrão.
    private static String config (String[] args, int pos, String variavel, String padrao)
    {
        if (args.length>pos)
            return args[pos];

        String env = System.getenv (variavel);
        if (env!=null && !env.trim().equals(""))
            return env.trim();

        return padrao;
    }

    public static void main (String[] args) {
        // Valida os argumentos informados na inicialização.
        if (args.length > 2) {
            System.err.println("Uso esperado: java Servidor <PORTA> <MONGO_URI>\n");
            System.err.println("Ou variaveis de ambiente: SERVIDOR_PORTA, MONGO_URI\n");
            return;
        }

        // Resolve porta e URI do MongoDB usados pelo servidor.
        String porta    = Servidor.config (args, 0, "SERVIDOR_PORTA", Servidor.PORTA_PADRAO);
        String mongoUri = Servidor.config (args, 1, "MONGO_URI",      Servidor.MONGO_URI_PADRAO);

        // Inicializa o repositório e as operações da aplicação.
        RepositorioDeChamadas repositorio       = null;
        OperacoesDeRotina     operacoesDeRotina = null;
        OperacoesDeCronograma operacoesDeCronograma = null;
        try
        {
            repositorio       = new RepositorioDeChamadas(mongoUri, RepositorioDeChamadas.BANCO_PADRAO);
            operacoesDeRotina = new OperacoesDeRotina(repositorio.getBanco(Servidor.BANCO_GUIAVEST));
            operacoesDeCronograma = new OperacoesDeCronograma(repositorio.getBanco(Servidor.BANCO_GUIAVEST));
        }
        catch (Exception erro)
        {
            System.err.println(erro.getMessage() + "\n");
            return;
        }

        // Mantém a lista de clientes conectados ao servidor.
        ArrayList<Parceiro> usuarios = new ArrayList<Parceiro> ();

        // Inicia a thread responsável por aceitar novas conexões.
        AceitadoraDeConexao aceitadoraDeConexao = null;
        try
        {
            aceitadoraDeConexao = new AceitadoraDeConexao(porta, usuarios, repositorio, operacoesDeRotina,
                                                          operacoesDeCronograma);
            aceitadoraDeConexao.start();
        }
        catch (Exception erro)
        {
            System.err.println("Porta não disponível");
            repositorio.fechar();
            return;
        }

        // Registra a inicialização e prepara os índices do banco.
        System.out.println("O servidor esta ativo na porta " + porta);
        repositorio.registrar("SERVIDOR_INICIADO", "local", "porta=" + porta);
        operacoesDeRotina.garantirIndices();
        operacoesDeCronograma.garantirIndices();

        // Aguarda comandos enviados pelo operador no terminal.
        for(;;)
        {
            System.out.println("Para desativar, use o comando \"desativar\"\n");
            System.out.println("> ");

            String comando = null;
            try
            {
                comando = Teclado.getUmString();
            }
            catch (Exception erro) {}

            if (comando == null)
            {
                try {Thread.sleep (60000);} catch (InterruptedException erro) {}
                continue;
            }

            // Desativa o servidor e encerra todas as conexões ativas.
            if (comando.toLowerCase().equals("desativar"))
            {
                synchronized (usuarios)
                {
                    ComunicadoDeDesligamento comunicadoDeDesligamento = new ComunicadoDeDesligamento();

                    for (Parceiro usuario:usuarios)
                    {
                        try
                        {
                            usuario.receba(comunicadoDeDesligamento);
                            usuario.adeus();
                        }
                        catch (Exception erro) {}
                    }
                }

                repositorio.registrar("SERVIDOR_DESATIVADO", "local", null);
                repositorio.fechar();

                System.out.println("O servidor foi desativado\n");
                System.exit(0);
            }
            else
                System.err.println("Comando invalido\n");
        }
    }
}
