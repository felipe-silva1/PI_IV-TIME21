@echo off
title Servidor GuiaVest (Porta 3000)
echo ===================================================
echo Iniciando o Servidor Java (Sockets) na porta 3000...
echo ===================================================

:: Entra na pasta do servidor
cd servidor

:: Compila e executa utilizando o Maven Wrapper do backend
call "..\backend\backend-api\mvnw.cmd" -f "pom.xml" compile
call "..\backend\backend-api\mvnw.cmd" -f "pom.xml" exec:java -Dexec.args="3000"

:: Pausa o terminal caso o servidor feche ou dê erro, para o usuário conseguir ler a mensagem
pause