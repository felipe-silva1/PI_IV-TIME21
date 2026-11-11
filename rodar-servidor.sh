#!/bin/bash
echo "==================================================="
echo "Iniciando o Servidor Java (Sockets) na porta 3000..."
echo "==================================================="

# Entra na pasta do servidor
cd servidor || exit

# Garante que o Maven Wrapper tem permissão de execução
chmod +x ../backend/backend-api/mvnw

# Compila e executa utilizando o Maven Wrapper do backend
../backend/backend-api/mvnw -f pom.xml compile
../backend/backend-api/mvnw -f pom.xml exec:java -Dexec.args="3000"

# Pausa o terminal caso o servidor feche ou dê erro
read -p "Pressione [Enter] para fechar..."