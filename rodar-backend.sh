#!/bin/bash
echo "==================================================="
echo "Iniciando o Backend Spring Boot na porta 8080..."
echo "==================================================="

# Entra na pasta do backend
cd backend/backend-api || exit

# Garante permissao de execucao para o Maven Wrapper
chmod +x mvnw

# Executa o Spring Boot utilizando o Maven Wrapper
./mvnw spring-boot:run

# Pausa o terminal caso o backend feche ou de erro
read -p "Pressione [Enter] para fechar..."
