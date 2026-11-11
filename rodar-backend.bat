@echo off
title Backend API GuiaVest (Porta 8080)
echo ===================================================
echo Iniciando o Backend Spring Boot na porta 8080...
echo ===================================================

:: Entra na pasta do backend
cd backend\backend-api

:: Executa o Spring Boot utilizando o Maven Wrapper
call mvnw.cmd spring-boot:run

:: Pausa o terminal caso o backend feche ou de erro
pause
