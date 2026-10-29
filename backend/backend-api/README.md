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

O endpoint `GET /api/health` envia um `PedidoDeStatus` ao servidor. A resposta
inclui `servidorDisponivel` e `bancoDisponivel`, permitindo que o frontend
mostre o estado de toda a cadeia. Os estados possíveis são `UP` (servidor e
MongoDB disponíveis), `DEGRADED` (servidor disponível, MongoDB indisponível) e
`DOWN` (servidor indisponível).

## Cadastro e login

A API implementa `POST /api/auth/register` (JSON com `nome`, `email` e `senha`)
e `POST /api/auth/login` (JSON com `email` e `senha`). A confirmação de senha
fica na validação do formulário. As contas são persistidas pelo servidor na
coleção `usuarios`; a API armazena somente o hash BCrypt da senha. Sucesso
retorna um token opaco, o identificador do estudante e seu nome.

O frontend guarda o token em `sessionStorage`. Requisições autenticadas podem
usar `Authorization: Bearer <token>`; `GET /api/auth/session` verifica o token
e `DELETE /api/auth/session` encerra a sessão. Os tokens expiram em 24 horas e
são invalidados quando a API reinicia.

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
  "mensagem": "Servidor e banco de dados disponiveis.",
  "servidorDisponivel": true,
  "bancoDisponivel": true
}
```