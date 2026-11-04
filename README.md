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

#### Pacote das classes de comunicação

As mensagens do protocolo ficam todas no pacote **`br.edu.time21.comunicacao`**, separado do pacote `servidor`, que guarda só a infraestrutura (`Servidor`, `AceitadoraDeConexao`, `SupervisoraDeConexao`, `Parceiro`, `RepositorioDeChamadas` e `Teclado`).

A hierarquia tem uma raiz e duas bases, uma para cada sentido da conversa:

```text
Comunicado                       raiz, implements Serializable
|-- Pedido                       backend -> servidor
|   |-- PedidoDeStatus
|   |-- PedidoParaSair
|   |-- PedidoParaSalvarRotina
|   `-- PedidoParaBuscarRotina
`-- Resposta                     servidor -> backend
    |-- RespostaDeStatus
    |-- RespostaDeErro
    |-- RespostaDeRotina
    `-- ComunicadoDeDesligamento
```

Algumas classes do pacote não são mensagens, e sim dados que viajam dentro
delas. Elas implementam `Serializable` direto, sem herdar de `Comunicado`:

```text
Rotina                           documento da coleção perfis
`-- TurnoLivre                   um item de Rotina.disponibilidade
```

Toda classe do protocolo declara o próprio `serialVersionUID = 1L`. Isso é obrigatório, e não só uma convenção: o `serialVersionUID` **não é herdado** para efeito de serialização. Sem a declaração, a JVM calcula um valor a partir da assinatura da classe, e aí qualquer alteração de campo faz o outro lado rejeitar a mensagem com `InvalidClassException`.

#### Como as duas cópias são mantidas iguais

A decisão do time foi **duplicar os arquivos**, em vez de criar um módulo Maven compartilhado. Assim cada um continua abrindo `servidor/` e `backend/backend-api/` direto na IDE, sem precisar rodar um `mvn install` de um terceiro módulo antes de compilar qualquer um dos dois.

Os mesmos arquivos existem em:

- `servidor/src/main/java/br/edu/time21/comunicacao/`
- `backend/backend-api/src/main/java/br/edu/time21/comunicacao/`

**Regra do time: quem mexer no protocolo mexe nos dois lados, no mesmo commit.** O custo dessa escolha é esse — não há nada no build que impeça as cópias de divergirem, e uma divergência só aparece em tempo de execução, como erro de desserialização. Antes de abrir o PR, confira que as pastas estão idênticas:

```bash
# Linux / Mac / Git Bash
diff -r servidor/src/main/java/br/edu/time21/comunicacao \
        backend/backend-api/src/main/java/br/edu/time21/comunicacao
```

```powershell
# Windows (PowerShell)
Compare-Object `
  (Get-ChildItem -Recurse servidor\src\main\java\br\edu\time21\comunicacao | Get-FileHash) `
  (Get-ChildItem -Recurse backend\backend-api\src\main\java\br\edu\time21\comunicacao | Get-FileHash) `
  -Property Hash
```

Sem nenhuma saída, as cópias estão iguais.

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
|   `-- backend-api/src/main/java/
|       |-- com/guiavest/backend_api/      API Spring Boot
|       `-- br/edu/time21/comunicacao/     classes do protocolo (copia)
|-- servidor/          servidor de sockets em Java + MongoDB (em desenvolvimento)
|   `-- src/main/java/
|       |-- servidor/                      infraestrutura do servidor
|       `-- br/edu/time21/comunicacao/     classes do protocolo (copia)
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

**Frontend:** abra `frontend/index.html` com o Live Server. A página consulta
`http://localhost:8080/api/health` e mostra separadamente se o servidor Java e
o MongoDB estão disponíveis. O login fica em `frontend/pages/login.html`.

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

Com o backend, o servidor e o MongoDB em execução, teste a aplicação acessando [`http://localhost:8080/api/health`](http://localhost:8080/api/health). A resposta esperada é semelhante a:

```json
{
  "status": "UP",
  "mensagem": "Servidor e banco de dados disponiveis.",
  "servidorDisponivel": true,
  "bancoDisponivel": true
}
```

Se o servidor estiver desligado, o endpoint continua respondendo com
`status: "DOWN"` e a tela inicial mostra a indisponibilidade sem interromper a
interface. Se apenas o MongoDB estiver indisponível, o status será
`"DEGRADED"` e o banco será indicado como indisponível.

### Servidor

**Servidor**

Servidor Java que recebe conexões do backend por socket, troca objetos com
`ObjectInputStream` / `ObjectOutputStream` e registra as chamadas recebidas
no MongoDB (Atlas).

- Escuta em uma porta TCP configurável (`ServerSocket`).
- Atende cada conexão em uma thread própria, sem travar as demais.
- Em cada conexão, cria o `ObjectOutputStream`, chama `flush()` e só depois
  cria o `ObjectInputStream`.
- Responde ao `PedidoDeStatus` com uma `RespostaDeStatus`.
- Salva e busca a rotina do estudante na coleção `perfis` do banco `guiavest`
  (veja `docs/dicionario-banco.md`).
- Continua no ar quando um cliente encerra ou derruba a conexão.
- Grava cada evento na coleção `chamadas` do banco `servidor`.


**Configuração**

| Parâmetro | Argumento | Variável de ambiente | Padrão |
|---|---|---|---|
| Porta | 1º | `SERVIDOR_PORTA` | `3000` |
| URI do MongoDB | 2º | `MONGO_URI` | `mongodb://localhost:27017` |

O argumento tem prioridade sobre a variável de ambiente. **Não coloque a URI
com senha no código nem no Git**: defina-a como variável de ambiente no mesmo
terminal em que o servidor será iniciado.

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
*Network Access*, e o usuário precisa ser um **Database User** com permissão
`readWrite` no banco usado pelo servidor. A senha do Database User é diferente
da senha da conta usada para entrar no site do Atlas.

#### Usando MongoDB Atlas

1. Acesse o projeto em [MongoDB Atlas](https://cloud.mongodb.com).
2. No cluster, clique em **Connect** e depois em **Drivers**.
3. Selecione **Java** e copie a URI exibida pelo Atlas.
4. Em **Security > Database Access**, crie ou confirme o Database User.
5. Em **Security > Network Access**, adicione o IP da máquina que executará o servidor.
6. Substitua na URI apenas o usuário e a senha pelos dados do Database User.

A URI deve conter o endereço real do cluster, e não um exemplo como
`cluster0.xxxxx.mongodb.net`:

```text
mongodb+srv://usuario:senha@cluster0.endereco-real.mongodb.net/servidor
```

Se a senha tiver caracteres especiais, como `@`, `#`, `:` ou `/`, eles devem
ser codificados na URI (`%40`, `%23`, `%3A` e `%2F`, respectivamente).

No Windows PowerShell, defina a variável antes de iniciar o servidor. O valor
fica disponível somente naquele terminal:

```powershell
$env:MONGO_URI='mongodb+srv://usuario:senha@cluster0.endereco-real.mongodb.net/servidor'
```

Para um MongoDB local, não defina `MONGO_URI`; o padrão usado será
`mongodb://localhost:27017`. Nesse caso, o MongoDB precisa estar rodando na
porta `27017`.

## Compilar e executar

O módulo `servidor` não possui um `mvnw.cmd` próprio. No Windows, é possível
usar o Maven Wrapper que está no módulo do backend, apontando-o para o
`pom.xml` do servidor. Abra um terminal na pasta `servidor`:

```powershell
cd C:\programacao\PI_IV-TIME21\servidor

# Se estiver usando Atlas, execute antes os comandos de configuração acima.
& "..\backend\backend-api\mvnw.cmd" -f "pom.xml" compile
& "..\backend\backend-api\mvnw.cmd" -f "pom.xml" exec:java -Dexec.args="3000"
```

O servidor deve mostrar:

```text
O servidor esta ativo na porta 3000
```

O processo fica aguardando comandos nesse terminal. Para encerrá-lo
corretamente, digite `desativar` e pressione Enter.

Se o Maven estiver instalado globalmente, os dois comandos equivalentes são
`mvn compile` e `mvn exec:java -Dexec.args="3000"`.

No Linux ou macOS, a forma equivalente, a partir da pasta `servidor`, é:

```bash
../backend/backend-api/mvnw -f pom.xml compile
../backend/backend-api/mvnw -f pom.xml exec:java -Dexec.args="3000"
```

## Protocolo

Todas as mensagens herdam de `Comunicado`, no pacote
`br.edu.time21.comunicacao`, e são serializadas com `ObjectOutputStream`. O
backend precisa ter as mesmas classes, com o mesmo pacote, o mesmo nome e o
mesmo `serialVersionUID` — veja *Comunicação entre backend e servidor*.

| Mensagem | Base | Sentido | Descrição |
|---|---|---|---|
| `PedidoDeStatus` | `Pedido` | cliente → servidor | Pergunta se o servidor está ativo |
| `PedidoParaSair` | `Pedido` | cliente → servidor | Encerra a conexão do cliente |
| `PedidoParaSalvarRotina` | `Pedido` | cliente → servidor | Cria a rotina do usuário ou substitui a existente |
| `PedidoParaBuscarRotina` | `Pedido` | cliente → servidor | Busca a rotina pelo id do usuário |
| `RespostaDeStatus` | `Resposta` | servidor → cliente | Situação, conexões ativas, hora do servidor e se o MongoDB responde |
| `RespostaDeRotina` | `Resposta` | servidor → cliente | A rotina gravada ou encontrada; `null` na busca de um usuário que ainda não tem rotina |
| `RespostaDeErro` | `Resposta` | servidor → cliente | Código e mensagem de diagnóstico, quando o pedido não pôde ser atendido |
| `ComunicadoDeDesligamento` | `Resposta` | servidor → cliente | Aviso de que o servidor foi desativado |

### Rotina

A `Rotina` corresponde a um documento da coleção `perfis`: `usuarioId` (o
`ObjectId` do usuário em hexadecimal), `vestibular`, `disponibilidade` (lista
de `TurnoLivre`, cada um com `diaSemana` de 1 a 7 e `turno` `MANHA`, `TARDE`
ou `NOITE`), `materiasPrioritarias` (da maior para a menor prioridade) e
`atualizadoEm`. O backend monta a rotina sem `atualizadoEm`; o servidor
preenche esse campo ao gravar e devolve a rotina como ficou no banco.

Salvar é sempre uma substituição completa: se o usuário já tem rotina, o
documento inteiro é trocado (o `_id` é mantido). O servidor cria, ao iniciar,
o índice único em `usuarioId`, que garante um perfil por usuário.

Antes de gravar, o servidor confere as regras do dicionário de dados e
responde `PEDIDO_INVALIDO` se alguma falhar: `usuarioId` válido, vestibular
preenchido, ao menos um turno, `diaSemana` entre 1 e 7, turno conhecido, sem
par (`diaSemana`, `turno`) repetido, ao menos uma matéria e sem matéria
repetida. A validação voltada ao usuário (vestibular existente, matérias do
vestibular, mensagens para a tela) continua sendo papel do backend.

Os códigos previstos na `RespostaDeErro` são `PEDIDO_DESCONHECIDO`,
`PEDIDO_INVALIDO`, `BANCO_INDISPONIVEL` e `ERRO_INTERNO`. O backend decide o
que devolver ao frontend a partir do código; a mensagem é só para log e
diagnóstico, e não deve ser mostrada crua ao usuário final.

O servidor só aceita desserializar as classes do protocolo: o filtro em
`SupervisoraDeConexao` é
`br.edu.time21.comunicacao.*;java.util.ArrayList;java.lang.Object;!*`. O
padrão casa o **nome totalmente qualificado**, e é por isso que o pacote
aparece nele. Campos `String` não precisam ser liberados, porque vão no stream
como `TC_STRING`, sem descritor de classe; mas qualquer novo tipo de campo
(`Date`, enum, `String[]`, outra coleção) tem de ser acrescentado ao filtro,
senão o pedido passa a ser rejeitado na recepção.

Listas devem ser declaradas como `ArrayList` e copiadas para um `ArrayList` no
construtor, como faz a `Rotina`. Só `java.util.ArrayList` está liberado:
`List.of(...)` e `Arrays.asList(...)` são outras classes e seriam rejeitadas.
`java.lang.Object` também precisa estar no filtro porque o `readObject` do
`ArrayList` valida um `Object[]` interno; isso não abre brecha, já que cada
elemento da lista continua passando pelo filtro.

## Registro no MongoDB

Cada documento da coleção `chamadas` registra um evento do servidor: conexão
aberta ou encerrada, pedido recebido, início ou desativação. Os registros são
gravados em segundo plano, por uma única thread e na ordem dos eventos, para
que a gravação do log nunca atrase a resposta ao backend. O campo `dataHora` é
o momento do evento, não o da gravação.

Se o MongoDB estiver fora do ar, o servidor continua atendendo: a falha de
gravação aparece no console, a resposta de status traz `bancoDisponivel=false`
e os pedidos que dependem do banco recebem `RespostaDeErro` com
`BANCO_INDISPONIVEL`. Cada operação espera o banco por no máximo 3 segundos
(`RepositorioDeChamadas.TIMEOUT_DO_BANCO_EM_SEGUNDOS`). Esse valor precisa
ficar abaixo do timeout do backend (`SERVIDOR_TIMEOUT_MS`, 5 segundos por
padrão); senão, o backend desiste antes da resposta e o `/api/health` mostra
`DOWN` em vez de `DEGRADED`.

## Testando

Com o servidor rodando, abra outro terminal na pasta `servidor` e execute o
cliente de teste. Ele simula o backend: envia um `PedidoDeStatus`, imprime a
`RespostaDeStatus` recebida e envia um `PedidoParaSair`.

```powershell
cd C:\programacao\PI_IV-TIME21\servidor
java -cp "target\classes" servidor.ClienteDeTeste localhost 3000
```

Saída esperada:

```
Resposta: situacao=ATIVO, conexoesAtivas=1, horaDoServidor=..., bancoDisponivel=true
```

O `ClienteDeTeste` é só uma ferramenta de verificação manual. O cliente real é
o backend. Se o servidor não conseguir acessar o MongoDB, ele ainda poderá
responder ao cliente, mas a saída indicará `bancoDisponivel=false` e a falha
de gravação aparecerá no console do servidor.

Para conferir a gravação, abra a coleção `chamadas` do banco `servidor` no
Atlas.

## Equipe

- Arthur Castro De Berto
- Edson Luan Rocha
- Felipe Augusto Dos Santos Silva
- Leonardo Santiago Tenca
- Max Thomazini Barbosa
