# PrazosSign

API backend do PrazosSign desenvolvida com Spring Boot, Spring Security, JWT e PostgreSQL.

## Funcionalidades

- cadastro de usuário
- login com JWT
- logout com invalidação de token
- recuperação de senha
- redefinição de senha por token
- consulta de perfil autenticado
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

## Estado atual do projeto

Entregue até aqui:

- módulo de autenticação funcional
- testes de integração cobrindo os fluxos principais
- configuração local via `.env`
- banco PostgreSQL local via Docker Compose

## Próximos passos possíveis

- envio real de e-mail para recuperação de senha
- refresh token
- documentação OpenAPI/Swagger
- versionamento de banco com migrations
- pipeline de deploy para produção