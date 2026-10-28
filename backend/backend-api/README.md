# Backend Spring Boot (Ponte REST <-> Socket)

Este projeto atua como intermediário entre o Frontend (HTML/JS) e o Servidor Java (Socket). 
Ele recebe requisições REST via HTTP e se comunica com o servidor de banco de dados via `ObjectInputStream`/`ObjectOutputStream`.

## 🚀 Requisitos Cumpridos
- [x] API rodando na porta 8080
- [x] Endpoint `GET /api/health` respondendo em JSON
- [x] CORS liberado para o VS Code Live Server (portas 5500)
- [x] Arquitetura organizada em camadas (Controller, Service, Model)
- [x] Cliente de socket com endereço, porta e timeout configuráveis

## Comunicação com o servidor

O serviço `ClienteSocket` envia objetos `Pedido` e devolve a `Resposta` recebida
usando as classes do pacote `br.edu.time21.comunicacao`. A conexão é encerrada
ao fim de cada pedido. Por padrão, o cliente usa `localhost:3000` e um timeout
de 5 segundos.

As configurações podem ser alteradas no `application.properties` ou por
variáveis de ambiente:

```properties
SERVIDOR_ENDERECO=localhost
SERVIDOR_PORTA=3000
SERVIDOR_TIMEOUT_MS=5000
```

Se o servidor estiver indisponível, `ClienteSocket.enviar` devolve uma
`RespostaDeErro` com código `ERRO_INTERNO`; a exceção de rede é registrada no
log e não chega a derrubar a API.

## 🛠️ Como executar o projeto no VS Code

1. Certifique-se de ter as extensões **Extension Pack for Java** e **Spring Boot Extension Pack** instaladas.
2. Abra a pasta deste backend no VS Code.
3. Aguarde o Maven carregar as dependências (verifique o progresso na barra inferior).
4. Navegue até a classe principal do projeto (a que contém a anotação `@SpringBootApplication`).
5. Clique no botão **Run** acima do método `main` ou use o ícone do **Spring Boot Dashboard** na barra lateral.
6. O servidor iniciará na porta `8080`.

## 🧪 Testando a aplicação

Com o servidor rodando, abra o seu navegador e acesse:
[http://localhost:8080/api/health](http://localhost:8080/api/health)

Você deverá ver uma resposta em JSON similar a:
```json
{
  "status": "UP",
  "mensagem": "Backend Spring Boot rodando na porta 8080!"
}