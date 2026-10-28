# GuiaVest

Plataforma web que ajuda vestibulandos a planejar a rotina de estudos e a acompanhar, dia a dia, o cumprimento do que foi planejado.

Projeto Integrador IV do Time 21 · Engenharia de Software · Escola Politécnica · PUC-Campinas · 2026

## Sobre o projeto

Vestibulandos recebem informações sobre provas, tarefas e prazos por canais diferentes: professores, colegas, portais acadêmicos e mensagens. Com tudo espalhado, fica difícil saber o que estudar, quando estudar e o que priorizar. O resultado costuma ser esquecimento de prazos, acúmulo de matérias e estudo de última hora.

O GuiaVest centraliza essa rotina em uma única ferramenta. O estudante informa o vestibular que vai prestar, os horários livres e as matérias prioritárias. A partir disso, o sistema gera um cronograma semanal, com checklist diário e indicador de progresso.

## Funcionalidades

### MVP

- **Cadastro e login:** o estudante cria uma conta e acessa a plataforma.
- **Cadastro de rotina e metas:** escolha do vestibular (ENEM, FUVEST, UNICAMP etc.), turnos disponíveis na semana e matérias prioritárias.
- **Geração do cronograma semanal:** distribuição dos blocos de matérias pelos dias e horários livres informados.
- **Checklist diário:** matérias e tópicos do dia, que podem ser marcados como concluídos ou pendentes.
- **Acompanhamento de progresso:** percentual de tarefas e horas cumpridas ao longo da semana.

### Pós-MVP

- Reagendamento de matérias atrasadas para o próximo dia livre.
- Histórico de simulados, com notas e taxa de acertos para recalibrar as disciplinas com mais dificuldade.
- Temporizador de estudos no estilo Pomodoro.
- Lembretes automáticos por e-mail ou pelo navegador.

## Arquitetura

O sistema é dividido em três partes, cada uma em sua própria pasta:

```text
Navegador (frontend)
   |  HTTP + JSON (fetch)
   v
backend: API em Java
   |  Socket TCP com ObjectOutputStream / ObjectInputStream
   v
servidor: servidor Java feito do zero
   |  driver do MongoDB
   v
MongoDB
```

- **frontend:** telas da aplicação em HTML, CSS e JavaScript. Conversa somente com o backend, por requisições HTTP com JSON.
- **backend:** API Spring Boot consumida pelo frontend. Recebe as requisições, valida os dados, aplica as regras de negócio e pede ao servidor o que precisa ser lido ou gravado.
- **servidor:** servidor de sockets em Java, feito do zero com `ServerSocket`, `ObjectInputStream` e `ObjectOutputStream`. É a única parte do sistema que acessa o banco de dados.
- **MongoDB:** banco de dados com usuários, rotinas, cronogramas e progresso.

### Comunicação entre backend e servidor

Backend e servidor trocam objetos Java serializados. Para que isso funcione:

- As classes trocadas devem implementar `Serializable` e existir **idênticas** nos dois projetos: mesmo pacote, mesmo nome e mesmos campos.
- Em cada lado da conexão, o `ObjectOutputStream` deve ser criado e esvaziado com `flush()` **antes** do `ObjectInputStream`. Na ordem inversa, os dois lados ficam esperando um pelo outro.

## Tecnologias

| Parte | Tecnologias |
| --- | --- |
| Frontend | HTML, CSS, JavaScript |
| Backend | Java, Spring Boot |
| Servidor | Java, sockets TCP, `ObjectInputStream`/`ObjectOutputStream` |
| Banco de dados | MongoDB |
| Prototipação | Figma |

## Estrutura de pastas

```text
PI_IV-TIME21/
|-- frontend/          interface web
|   |-- index.html
|   `-- src/
|       |-- css/styles.css
|       `-- js/app.js
|-- backend/           API em Java (em desenvolvimento)
|-- servidor/          servidor de sockets em Java + MongoDB (em desenvolvimento)
|-- .gitignore
`-- README.md
```

## Requisitos

- JDK 17 ou superior
- MongoDB
- VS Code com a extensão Live Server, para servir o frontend durante o desenvolvimento

## Como executar

Quando as três partes estiverem prontas, a ordem de inicialização será: MongoDB, servidor, backend e frontend.

| Parte | Porta |
| --- | --- |
| Frontend (Live Server) | 5500 |
| Backend | 8080 |
| Servidor | a definir |
| MongoDB | 27017 (padrão) |

**Frontend:** abra `frontend/index.html` com o Live Server. A página consulta `http://localhost:8080/api/health` e mostra se o backend está disponível.

### Backend Spring Boot

O backend funciona como intermediário entre o frontend e o servidor Java via socket. Para executá-lo pelo VS Code:

1. Certifique-se de ter as extensões **Extension Pack for Java** e **Spring Boot Extension Pack** instaladas.
2. Abra a pasta `backend/backend-api/` no VS Code.
3. Aguarde o Maven carregar as dependências. O progresso pode ser acompanhado na barra inferior.
4. Abra a classe `BackendApiApplication.java`, que contém a anotação `@SpringBootApplication`.
5. Clique em **Run** acima do método `main` ou use o **Spring Boot Dashboard** na barra lateral.
6. O backend será iniciado na porta `8080`.

Também é possível executá-lo pelo terminal, a partir da pasta `backend/backend-api/`:

```powershell
.\mvnw.cmd spring-boot:run
```

Com o backend em execução, teste a aplicação acessando [`http://localhost:8080/api/health`](http://localhost:8080/api/health). A resposta esperada é semelhante a:

```json
{
  "status": "UP",
  "mensagem": "Backend Spring Boot rodando na porta 8080!"
}
```

### Servidor

As instruções de execução do servidor de sockets serão adicionadas conforme essa parte for implementada.

**Servidor**

Servidor Java que recebe conexões do backend por socket, troca objetos com
`ObjectInputStream` / `ObjectOutputStream` e registra as chamadas recebidas
no MongoDB (Atlas).

- Escuta em uma porta TCP configurável (`ServerSocket`).
- Atende cada conexão em uma thread própria, sem travar as demais.
- Em cada conexão, cria o `ObjectOutputStream`, chama `flush()` e só depois
  cria o `ObjectInputStream`.
- Responde ao `PedidoDeStatus` com uma `RespostaDeStatus`.
- Continua no ar quando um cliente encerra ou derruba a conexão.
- Grava cada evento na coleção `chamadas` do banco `servidor`.


**Configuração**

| Parâmetro | Argumento | Variável de ambiente | Padrão |
|---|---|---|---|
| Porta | 1º | `SERVIDOR_PORTA` | `3000` |
| URI do MongoDB | 2º | `MONGO_URI` | `mongodb://localhost:27017` |

O argumento tem prioridade sobre a variável de ambiente. **Não coloque a URI
com senha no código nem no Git**: defina-a como variável de ambiente.

```bash
# Linux / Mac
export MONGO_URI='mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net'

# Windows (PowerShell)
$env:MONGO_URI="mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net"
```

Para adicionar como variável de sistema:
- Linux (adicione a ~/.bashrc):
```bash
   export MONGO_URI='mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net'
```
- Windows Powershell
```
setx MONGO_URI "mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net"
```

No Atlas, o IP da máquina que roda o servidor precisa estar liberado em
*Network Access*, e o usuário precisa de permissão `readWrite`.

## Compilar e executar

```bash
mvn compile
mvn exec:java -Dexec.args="3000"
## Executar pelo VScode também funciona
```

## Protocolo

Todas as mensagens herdam de `Comunicado` e são serializadas com
`ObjectOutputStream`. O backend precisa ter as mesmas classes, com o mesmo
nome e o mesmo `serialVersionUID`.

| Mensagem | Sentido | Descrição |
|---|---|---|
| `PedidoDeStatus` | cliente → servidor | Pergunta se o servidor está ativo |
| `RespostaDeStatus` | servidor → cliente | Situação, conexões ativas, hora do servidor e se o MongoDB responde |
| `PedidoParaSair` | cliente → servidor | Encerra a conexão do cliente |
| `ComunicadoDeDesligamento` | servidor → cliente | Aviso de que o servidor foi desativado |

O servidor só aceita desserializar as classes do protocolo (filtro em
`SupervisoraDeConexao`).

## Registro no MongoDB

Cada documento da coleção `chamadas` registra uma conexão ou desativação do servidor.

Se o MongoDB estiver fora do ar, o servidor continua atendendo: a falha de
gravação aparece no console e a resposta de status traz `bancoDisponivel=false`.

## Testando

Com o servidor rodando, em outro terminal:

```bash
mvn test-compile
java -cp target/classes:target/test-classes ClienteDeTeste localhost 3000
## Rodar ClienteDeTeste no VScode tambem funciona!
```

Saída esperada:

```
Resposta: situacao=ATIVO, conexoesAtivas=1, horaDoServidor=..., bancoDisponivel=true
```

(No Windows, troque `:` por `;` no classpath.) O `ClienteDeTeste` é só uma
ferramenta de verificação manual. O cliente real é o backend.

Para conferir a gravação, abra a coleção `chamadas` do banco `servidor` no
Atlas.

## Equipe

- Arthur Castro De Berto
- Edson Luan Rocha
- Felipe Augusto Dos Santos Silva
- Leonardo Santiago Tenca
- Max Thomazini Barbosa
