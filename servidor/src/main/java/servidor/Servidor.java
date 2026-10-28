package servidor;

import java.util.ArrayList;

public class Servidor {
    
    public static String PORTA_PADRAO = "3000";
    public static String MONGO_URI_PADRAO = "mongodb://localhost:27017";

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
        if (args.length > 2) {
            System.err.println("Uso esperado: java Servidor [PORTA] [MONGO_URI]\n");
            System.err.println("Ou variaveis de ambiente: SERVIDOR_PORTA, MONGO_URI\n");
            return;
        }

        String porta    = Servidor.config (args, 0, "SERVIDOR_PORTA", Servidor.PORTA_PADRAO);
        String mongoUri = Servidor.config (args, 1, "MONGO_URI",      Servidor.MONGO_URI_PADRAO);

        RepositorioDeChamadas repositorio = null;
        try
        {
            repositorio = new RepositorioDeChamadas(mongoUri, RepositorioDeChamadas.BANCO_PADRAO);
        }
        catch (Exception erro)
        {
            System.err.println(erro.getMessage() + "\n");
            return;
        }

        ArrayList<Parceiro> usuarios = new ArrayList<Parceiro> ();

        AceitadoraDeConexao aceitadoraDeConexao = null;
        try
        {
            aceitadoraDeConexao = new AceitadoraDeConexao(porta, usuarios, repositorio);
            aceitadoraDeConexao.start();
        }
        catch (Exception erro)
        {
            System.err.println("Porta não disponível");
            repositorio.fechar();
            return;
        }

        System.out.println("O servidor esta ativo na porta " + porta);
        repositorio.registrar("SERVIDOR_INICIADO", "local", "porta=" + porta);

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

