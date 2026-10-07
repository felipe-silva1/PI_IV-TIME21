package br.edu.time21;

import br.edu.time21.config.ApiServer;
import br.edu.time21.controller.HealthController;

public class Application {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        ApiServer server = new ApiServer(port, new HealthController());
        server.start();
    }
}