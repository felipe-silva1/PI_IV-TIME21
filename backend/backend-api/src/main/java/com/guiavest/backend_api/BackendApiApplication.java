//Max Thomazini Barbosa 25003934
package com.guiavest.backend_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApiApplication {

	// Inicia o contêiner do Spring e registra os controladores, serviços e configurações da API.
	public static void main(String[] args) {
		SpringApplication.run(BackendApiApplication.class, args);
	}

}
