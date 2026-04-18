# PrazosSign

API backend do PrazosSign desenvolvida com Spring Boot, Spring Security, JWT e PostgreSQL.

## Funcionalidades

- cadastro de usuário
- login com JWT
- logout com invalidação de token
- recuperação de senha
- redefinição de senha por token
- consulta de perfil autenticado
- criação de grupos
- listagem de grupos do usuário autenticado
- atualização de grupos do usuário autenticado
- exclusão de grupos do usuário autenticado
- criação de documentos
- edição de documentos em rascunho
- salvamento de documentos como rascunho
- movimentação de documentos entre Home e Grupos
- listagem de documentos do usuário autenticado
- filtro de documentos por status
- persistência com PostgreSQL
- ambiente local com Docker Compose

## Stack

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- PostgreSQL
- Docker Compose
- JWT

## Estrutura atual da API

### Autenticação

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/logout`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`

### Perfil

- `GET /profile`

### Grupos

- `GET /groups`
- `POST /groups`
- `PUT /groups/{id}`
- `DELETE /groups/{id}`

### Documentos

- `GET /documents`
- `POST /documents`
- `GET /documents/{id}`
- `PUT /documents/{id}`
- `DELETE /documents/{id}`
- `POST /documents/{id}/send`
- `POST /documents/{id}/move`

## Configuração local

### 1. Variáveis de ambiente

O projeto lê as variáveis locais a partir do arquivo `.env` na raiz.

Variáveis utilizadas pela aplicação:

```env
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
JWT_SECRET=
JWT_EXPIRATION=
```

Variáveis utilizadas pelo PostgreSQL no Docker Compose:

```env
POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=
POSTGRES_PORT=
```

O arquivo `.env.production` fica reservado para a configuração futura de produção.

### 2. Subir o banco local

```bash
docker compose up -d
```

Isso sobe um container PostgreSQL local com o nome `PrazosSign`.

### 3. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

### 4. Rodar os testes

```bash
./mvnw test
```

## Autenticação JWT

Os endpoints protegidos exigem o header:

```http
Authorization: Bearer SEU_TOKEN_JWT
```

Atualmente, o endpoint de perfil exige autenticação. O logout também depende de um token válido enviado no header `Authorization`.
Os endpoints de grupos também exigem autenticação e sempre operam apenas sobre os grupos do usuário autenticado.
Os endpoints de documentos também exigem autenticação e sempre operam apenas sobre os documentos do usuário autenticado.

## Modelo de usuário

Estrutura persistida para usuários:

```text
users
- id
- name
- email
- password_hash
- created_at
- updated_at
```

## Modelo de grupos

Estrutura persistida para grupos:

```text
groups
- id
- user_id
- name
- created_at
```

## Modelo de documentos

Estrutura persistida para documentos:

```text
documents
- id
- user_id
- group_id nullable
- title
- content
- status
- created_at
- updated_at
```

## Endpoints

### POST /auth/register

Cria um novo usuário e retorna um token JWT já autenticado.

#### Request

```json
{
  "name": "Vinicius",
  "email": "vinicius@example.com",
  "password": "12345678"
}
```

#### Regras

- `name` é obrigatório
- `email` é obrigatório e deve ser válido
- `password` é obrigatória e deve ter no mínimo 8 caracteres
- o e-mail deve ser único

#### Response 201

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": 1,
    "name": "Vinicius",
    "email": "vinicius@example.com",
    "createdAt": "2026-04-18T12:00:00Z",
    "updatedAt": "2026-04-18T12:00:00Z"
  }
}
```

#### Possíveis erros

- `400 Bad Request` para dados inválidos
- `409 Conflict` quando o e-mail já estiver cadastrado

### POST /auth/login

Autentica um usuário e retorna um JWT.

#### Request

```json
{
  "email": "vinicius@example.com",
  "password": "12345678"
}
```

#### Response 200

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": 1,
    "name": "Vinicius",
    "email": "vinicius@example.com",
    "createdAt": "2026-04-18T12:00:00Z",
    "updatedAt": "2026-04-18T12:00:00Z"
  }
}
```

#### Possíveis erros

- `400 Bad Request` para payload inválido
- `401 Unauthorized` para credenciais inválidas

### POST /auth/logout

Invalida o JWT atual por blacklist no servidor.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Response 204

Sem corpo de resposta.

#### Possíveis erros

- `400 Bad Request` quando o header `Authorization` estiver malformado
- `401 Unauthorized` quando o token for inválido

### POST /auth/forgot-password

Gera um token de recuperação de senha.

#### Request

```json
{
  "email": "vinicius@example.com"
}
```

#### Response 200 para e-mail existente

```json
{
  "message": "Token de recuperação gerado com sucesso.",
  "resetToken": "token-gerado"
}
```

#### Response 200 para e-mail não encontrado

```json
{
  "message": "Se o e-mail existir, um token de recuperação será gerado.",
  "resetToken": null
}
```

### POST /auth/reset-password

Redefine a senha de um usuário a partir de um token de recuperação.

#### Request

```json
{
  "token": "token-gerado",
  "newPassword": "87654321"
}
```

#### Regras

- `token` é obrigatório
- `newPassword` é obrigatória
- `newPassword` deve ter no mínimo 8 caracteres
- o token deve existir
- o token não pode estar expirado
- o token não pode já ter sido usado

#### Response 200

```json
{
  "message": "Senha redefinida com sucesso."
}
```

#### Possíveis erros

- `400 Bad Request` para token inválido
- `400 Bad Request` para token expirado
- `400 Bad Request` para token já utilizado
- `400 Bad Request` para payload inválido

### GET /profile

Retorna os dados do usuário autenticado.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Response 200

```json
{
  "id": 1,
  "name": "Vinicius",
  "email": "vinicius@example.com",
  "createdAt": "2026-04-18T12:00:00Z",
  "updatedAt": "2026-04-18T12:00:00Z"
}
```

#### Possíveis erros

- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o usuário autenticado não for encontrado

### GET /groups

Retorna os grupos do usuário autenticado ordenados por data de criação decrescente.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Response 200

```json
[
  {
    "id": 2,
    "name": "Financeiro",
    "createdAt": "2026-04-18T12:30:00Z"
  },
  {
    "id": 1,
    "name": "Contratos",
    "createdAt": "2026-04-17T09:00:00Z"
  }
]
```

#### Possíveis erros

- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o usuário autenticado não for encontrado

### POST /groups

Cria um novo grupo para o usuário autenticado.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Request

```json
{
  "name": "Contratos"
}
```

#### Regras

- `name` é obrigatório
- `name` deve ter no máximo 255 caracteres
- espaços nas extremidades são removidos antes de salvar
- após a normalização, o nome não pode ficar vazio

#### Response 201

```json
{
  "id": 1,
  "name": "Contratos",
  "createdAt": "2026-04-18T12:00:00Z"
}
```

#### Possíveis erros

- `400 Bad Request` para payload inválido
- `400 Bad Request` quando o nome informado for vazio após normalização
- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o usuário autenticado não for encontrado

### PUT /groups/{id}

Atualiza um grupo pertencente ao usuário autenticado.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Request

```json
{
  "name": "Jurídico"
}
```

#### Regras

- `name` é obrigatório
- `name` deve ter no máximo 255 caracteres
- espaços nas extremidades são removidos antes de salvar
- o grupo deve pertencer ao usuário autenticado

#### Response 200

```json
{
  "id": 1,
  "name": "Jurídico",
  "createdAt": "2026-04-18T12:00:00Z"
}
```

#### Possíveis erros

- `400 Bad Request` para payload inválido
- `400 Bad Request` quando o nome informado for vazio após normalização
- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o grupo não for encontrado para o usuário autenticado

### DELETE /groups/{id}

Remove um grupo pertencente ao usuário autenticado.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Response 204

Sem corpo de resposta.

#### Possíveis erros

- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o grupo não for encontrado para o usuário autenticado

### GET /documents

Retorna os documentos do usuário autenticado ordenados por data de atualização decrescente.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Query params opcionais

- `status`: filtra documentos por status, aceitando `RASCUNHO`, `AGUARDANDO_ASSINATURA`, `PARCIALMENTE_ASSINADO`, `ASSINADO`, `VALIDADO` ou `CANCELADO`

#### Response 200

```json
[
  {
    "id": 2,
    "groupId": 1,
    "title": "Contrato Comercial",
    "content": "Conteúdo do documento",
    "status": "AGUARDANDO_ASSINATURA",
    "createdAt": "2026-04-18T10:00:00Z",
    "updatedAt": "2026-04-18T12:30:00Z"
  },
  {
    "id": 1,
    "groupId": null,
    "title": "Proposta Inicial",
    "content": "Conteúdo em rascunho",
    "status": "RASCUNHO",
    "createdAt": "2026-04-17T09:00:00Z",
    "updatedAt": "2026-04-18T08:00:00Z"
  }
]
```

#### Possíveis erros

- `400 Bad Request` quando o filtro de status for inválido
- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o usuário autenticado não for encontrado

### GET /documents/{id}

Retorna um documento pertencente ao usuário autenticado.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Response 200

```json
{
  "id": 1,
  "groupId": null,
  "title": "Proposta Inicial",
  "content": "Conteúdo em rascunho",
  "status": "RASCUNHO",
  "createdAt": "2026-04-18T10:00:00Z",
  "updatedAt": "2026-04-18T10:00:00Z"
}
```

#### Possíveis erros

- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o documento não for encontrado para o usuário autenticado

### POST /documents

Cria um novo documento para o usuário autenticado.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Request

```json
{
  "title": "Proposta Inicial",
  "content": "Conteúdo em rascunho"
}
```

#### Regras

- `title` é obrigatório
- `title` deve ter no máximo 255 caracteres
- espaços nas extremidades do título são removidos antes de salvar
- `content` é obrigatório
- todo documento novo é criado com status `RASCUNHO`

#### Response 201

```json
{
  "id": 1,
  "groupId": null,
  "title": "Proposta Inicial",
  "content": "Conteúdo em rascunho",
  "status": "RASCUNHO",
  "createdAt": "2026-04-18T10:00:00Z",
  "updatedAt": "2026-04-18T10:00:00Z"
}
```

#### Possíveis erros

- `400 Bad Request` para payload inválido
- `400 Bad Request` quando o título informado for vazio após normalização
- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o usuário autenticado não for encontrado

### PUT /documents/{id}

Atualiza um documento em rascunho pertencente ao usuário autenticado.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Request

```json
{
  "title": "Proposta Atualizada",
  "content": "Conteúdo revisado"
}
```

#### Regras

- `title` é obrigatório
- `title` deve ter no máximo 255 caracteres
- `content` é obrigatório
- apenas documentos com status `RASCUNHO` podem ser editados
- o documento deve pertencer ao usuário autenticado

#### Response 200

```json
{
  "id": 1,
  "groupId": null,
  "title": "Proposta Atualizada",
  "content": "Conteúdo revisado",
  "status": "RASCUNHO",
  "createdAt": "2026-04-18T10:00:00Z",
  "updatedAt": "2026-04-18T11:00:00Z"
}
```

#### Possíveis erros

- `400 Bad Request` para payload inválido
- `400 Bad Request` quando o título informado for vazio após normalização
- `400 Bad Request` quando o documento já tiver sido enviado
- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o documento não for encontrado para o usuário autenticado

### DELETE /documents/{id}

Remove um documento pertencente ao usuário autenticado.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Response 204

Sem corpo de resposta.

#### Possíveis erros

- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o documento não for encontrado para o usuário autenticado

### POST /documents/{id}/send

Marca um documento como enviado.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Response 200

```json
{
  "id": 1,
  "groupId": null,
  "title": "Proposta Inicial",
  "content": "Conteúdo em rascunho",
  "status": "AGUARDANDO_ASSINATURA",
  "createdAt": "2026-04-18T10:00:00Z",
  "updatedAt": "2026-04-18T12:00:00Z"
}
```

#### Possíveis erros

- `400 Bad Request` quando o documento já tiver sido enviado
- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o documento não for encontrado para o usuário autenticado

### POST /documents/{id}/move

Move um documento para um grupo do usuário autenticado ou de volta para Home.

#### Headers

```http
Authorization: Bearer jwt-token
```

#### Request para mover para um grupo

```json
{
  "groupId": 1
}
```

#### Request para mover para Home

```json
{
  "groupId": null
}
```

#### Regras

- `groupId` pode ser `null` para remover o vínculo com grupo
- quando informado, o grupo deve pertencer ao usuário autenticado
- o documento deve pertencer ao usuário autenticado

#### Response 200

```json
{
  "id": 1,
  "groupId": 1,
  "title": "Proposta Inicial",
  "content": "Conteúdo em rascunho",
  "status": "RASCUNHO",
  "createdAt": "2026-04-18T10:00:00Z",
  "updatedAt": "2026-04-18T12:15:00Z"
}
```

#### Possíveis erros

- `401 Unauthorized` quando não houver autenticação válida
- `404 Not Found` quando o documento não for encontrado para o usuário autenticado
- `404 Not Found` quando o grupo informado não for encontrado para o usuário autenticado

## Formato de erro

Erros de validação e regras de negócio seguem um formato padronizado.

### Exemplo de erro de regra de negócio

```json
{
  "timestamp": "2026-04-18T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciais inválidas"
}
```

### Exemplo de erro de validação

```json
{
  "timestamp": "2026-04-18T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Dados inválidos",
  "errors": [
    {
      "field": "email",
      "message": "E-mail inválido"
    }
  ]
}
```

## Banco local com Docker

O `docker-compose.yml` atual sobe apenas o PostgreSQL local. Exemplo de uso:

```bash
docker compose up -d
docker compose down
```
