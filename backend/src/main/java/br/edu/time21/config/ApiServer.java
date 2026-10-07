package br.edu.time21.config;

import br.edu.time21.controller.HealthController;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ApiServer {
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:5500",
            "http://127.0.0.1:5500"
    );

    private final HttpServer server;
    private final HealthController healthController;

    public ApiServer(int port, HealthController healthController) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.healthController = healthController;
        server.createContext("/api/health", exchange -> {
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (ALLOWED_ORIGINS.contains(origin)) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
                exchange.getResponseHeaders().set("Vary", "Origin");
            }

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET, OPTIONS");
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            byte[] response = healthController.getStatus().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
    }

    public void start() {
        server.start();
        System.out.println("API disponível em http://localhost:" + server.getAddress().getPort());
    }
}