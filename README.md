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
- **backend:** API consumida pelo frontend. Recebe as requisições, valida os dados, aplica as regras de negócio e pede ao servidor o que precisa ser lido ou gravado. O framework ainda será definido: Spring Boot ou Java puro.
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
| Backend | Java (framework a definir) |
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

**Backend e servidor:** as instruções serão adicionadas aqui conforme cada parte for implementada.

## Equipe

- Arthur Castro De Berto
- Edson Luan Rocha
- Felipe Augusto Dos Santos Silva
- Leonardo Santiago Tenca
- Max Thomazini Barbosa
