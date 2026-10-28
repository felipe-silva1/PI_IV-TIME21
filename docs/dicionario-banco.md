# Dicionario de dados do sistema MongoDB. Banco: GuiaVest

O intuito do dicionario é ser usado durante desenvolvimento, para implementação correta do backend. Quaisquer mudanças a modelagem do banco, por favor documentar aqui...

## Histórico de versões ##
|Data|Autor|Versão|Comentário|
|---|---|---|---|
|28/10/2026|Arthur Castro|0.1|Versão inicial (sujeita a mudanças) com a especificação das entidades: usuarios, perfis, e blocos_de_estudo.

## Entidades

A seguir serão documentadas as coleções do banco:

### 1. usuarios
Contas dos estudantes (cadastro e login).

| Campo | Tipo | Obrigatório | Descrição | Exemplo |
|---|---|---|---|---|
| `_id` | ObjectId | Sim (automático) | Identificador do usuário, gerado pelo MongoDB | `65f1c2a4e1b2c3d4e5f60718` |
| `nome` | string | Sim | Nome do estudante (1 a 100 caracteres) | `Felipe Augusto` |
| `email` | string | Sim | E-mail de login. Único, salvo em minúsculas e sem espaços nas pontas | `ana@exemplo.com` |
| `senhaHash` | string | Sim | Hash BCrypt da senha. Nunca devolvido ao front | `$2a$10$...` |
| `criadoEm` | date | Sim | Instante do cadastro | `2026-10-28T12:00:00Z` |

**Índices:** `email` (único).

**Regras:**
- A senha nunca é gravada, apenas o hash.
- A duplicidade de e-mail é garantida pelo índice único (erro `11000`), não por consulta prévia.
- No login, e-mail inexistente e senha errada retornam a mesma mensagem ("credenciais inválidas").

**Exemplo:**

```json
{
  "nome": "Ana Souza",
  "email": "ana@exemplo.com",
  "senhaHash": "$2a$10$...",
  "criadoEm": "2026-10-28T12:00:00Z"
}
```
---
---

## 2. perfis

Rotina e metas do estudante: vestibular, turnos livres e matérias prioritárias. Um perfil por usuário.

| Campo | Tipo | Obrigatório | Descrição | Exemplo |
|---|---|---|---|---|
| `_id` | ObjectId | Sim (automático) | Identificador do perfil | `65f1c3b8e1b2c3d4e5f60719` |
| `usuarioId` | ObjectId | Sim | Dono do perfil (referência a `usuarios._id`). Único | `65f1c2a4e1b2c3d4e5f60718` |
| `vestibular` | string | Sim | Vestibular que o estudante vai prestar (enum `vestibular`) | `ENEM` |
| `disponibilidade` | array de objetos | Sim | Turnos livres na semana. Ao menos um item | ver abaixo |
| `disponibilidade[].diaSemana` | int | Sim | Dia da semana, de 1 a 7 | `1` |
| `disponibilidade[].turno` | string | Sim | Turno livre naquele dia (enum `turno`) | `NOITE` |
| `materiasPrioritarias` | array de strings | Sim | Matérias (enum `materia`), da maior para a menor prioridade. Sem repetição | `["Matemática", "Física"]` |
| `atualizadoEm` | date | Sim | Instante da última alteração do perfil | `2026-10-28T12:00:00Z` |

**Índices:** `usuarioId` (único).

**Regras:**
- Existe um único perfil por usuário. Ao editar a rotina, o documento é substituído.
- Não pode haver dois itens com o mesmo par (`diaSemana`, `turno`) em `disponibilidade`.
- A posição em `materiasPrioritarias` define a prioridade (a primeira é a mais importante).

**Exemplo:**

```json
{
  "usuarioId": "65f1c2a4e1b2c3d4e5f60718",
  "vestibular": "ENEM",
  "disponibilidade": [
    { "diaSemana": 1, "turno": "NOITE" },
    { "diaSemana": 6, "turno": "MANHA" }
  ],
  "materiasPrioritarias": ["Matemática", "Física"],
  "atualizadoEm": "2026-10-28T12:00:00Z"
}
```
---

## blocos_de_estudo

Cada bloco de estudo planejado em um dia e turno. O conjunto de blocos de uma semana forma o cronograma; os blocos do dia formam o checklist diário.

| Campo | Tipo | Obrigatório | Descrição | Exemplo |
|---|---|---|---|---|
| `_id` | ObjectId | Sim (automático) | Identificador do bloco | `65f1c4d9e1b2c3d4e5f6071a` |
| `usuarioId` | ObjectId | Sim | Dono do bloco (referência a `usuarios._id`) | `65f1c2a4e1b2c3d4e5f60718` |
| `semanaInicio` | string | Sim | Segunda-feira da semana do bloco (`yyyy-MM-dd`) | `2026-11-02` |
| `data` | string | Sim | Dia em que o bloco deve ser estudado (`yyyy-MM-dd`) | `2026-11-03` |
| `turno` | string | Sim | Turno do bloco (enum `turno`) | `NOITE` |
| `materia` | string | Sim | Matéria estudada (enum `materia`) | `Matemática` |
| `duracaoMin` | int | Sim | Duração planejada em minutos. Maior que zero | `90` |
| `topicos` | array de objetos | Sim | Tópicos do bloco. Pode ser vazio | ver abaixo |
| `topicos[].nome` | string | Sim | Nome do tópico | `Funções` |
| `topicos[].concluido` | boolean | Sim | Se o tópico foi concluído. Começa como `false` | `false` |
| `concluido` | boolean | Sim | Se o bloco foi concluído. Começa como `false` | `false` |
| `concluidoEm` | date | Não | Instante da conclusão. `null` enquanto pendente | `null` |

**Índices:** (`usuarioId`, `data`) para o checklist diário; (`usuarioId`, `semanaInicio`) para o cronograma e o progresso da semana.

**Regras:**
- `data` deve estar dentro da semana indicada por `semanaInicio`.
- Para regerar o cronograma de uma semana, os blocos dela são apagados e os novos inseridos.
- O progresso não é armazenado: é calculado sobre os blocos da semana.
  - % de tarefas = blocos com `concluido: true` dividido pelo total de blocos.
  - Horas cumpridas = soma de `duracaoMin` dos blocos concluídos, dividida pela soma de `duracaoMin` de todos os blocos.

**Exemplo:**

```json
{
  "usuarioId": "65f1c2a4e1b2c3d4e5f60718",
  "semanaInicio": "2026-11-02",
  "data": "2026-11-03",
  "turno": "NOITE",
  "materia": "Matemática",
  "duracaoMin": 90,
  "topicos": [
    { "nome": "Funções", "concluido": false }
  ],
  "concluido": false,
  "concluidoEm": null
}
```
---

## Relações

| Origem | Destino | Descrição |
|---|---|---|
| `perfis.usuarioId` | `usuarios._id` | Um usuário tem no máximo um perfil |
| `blocos_de_estudo.usuarioId` | `usuarios._id` | Um usuário tem vários blocos de estudo |

## Possíveis expansões

| Necessidade | Alteração prevista |
|---|---|
| Catálogo de matérias e tópicos | Nova coleção `materias`; `materiaId` nos blocos ao lado de `materia` |
| Catálogo de vestibulares | Nova coleção `vestibulares`; `vestibularId` no perfil |
| Histórico de cronogramas | Nova coleção `cronogramas`; `cronogramaId` nos blocos |
| Horário exato dos blocos | Campos `inicio` e `fim` no bloco |
| Reagendamento | Campos `dataOriginal` e `reagendado` no bloco |
| Mais estados de conclusão | Campo `status` no lugar do booleano `concluido` |
| Simulados, temporizador e lembretes | Novas coleções `simulados`, `sessoes_de_estudo` e `lembretes` |
| Conta de usuário | `atualizadoEm`, `ultimoLoginEm`, `emailVerificado`, `papel` |