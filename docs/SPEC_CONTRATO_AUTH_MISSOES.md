# Especificação de Contrato API (Auth + Missões)

## Objetivo
Definir um contrato estável entre backend e frontend Android para autenticação, sessão e endpoints de missões, evitando inconsistências como:
- `POST /api/auth/login` retornar `200`
- `GET /api/auth/me` retornar `200`
- `GET /api/missions` retornar `401` com o mesmo token

## Convenções Gerais
- Base URL: `https://<host>/api/`
- Formato de conteúdo: `application/json`
- Autenticação por header:
  - `Authorization: Bearer <access_token>`
- Fuso de tempo de datas no backend: UTC (ISO-8601 quando aplicável)

## Modelo de Erro (padrão)
Para respostas de erro (`4xx/5xx`), retornar JSON padronizado:

```json
{
  "error": "unauthorized",
  "message": "Access token inválido ou expirado",
  "code": "AUTH_401"
}
```

Campos mínimos:
- `error` (string curta para categorização)
- `message` (mensagem legível)
- `code` (código interno estável)

## 1) Login

### Endpoint
`POST /api/auth/login`

### Request (principal)
```json
{
  "username": "usuario",
  "password": "senha"
}
```

### Request (fallback legado aceito)
Header:
`Authorization: Basic base64(username:password)`

### Response 200 (sucesso)
```json
{
  "access_token": "jwt_access",
  "refresh_token": "jwt_refresh",
  "token_type": "Bearer",
  "user_id": "123"
}
```

Observações:
- `access_token` é obrigatório.
- `refresh_token` é recomendado.
- `user_id` é recomendado (frontend já tem fallback via `/auth/me`).
- Frontend também aceita `token` no lugar de `access_token` para compatibilidade.

### Response 401
Credenciais inválidas.

## 2) Perfil autenticado

### Endpoint
`GET /api/auth/me`

### Header obrigatório
`Authorization: Bearer <access_token>`

### Response 200
```json
{
  "id": "123",
  "username": "usuario",
  "email": "usuario@email.com"
}
```

Campos mínimos:
- `id`
- `username`

### Response 401
Token inválido/expirado/ausente.

## 3) Refresh de token

### Endpoint
`POST /api/auth/refresh`

### Forma A (preferencial)
Header:
`Authorization: Bearer <refresh_token>`

Body:
```json
{}
```

### Forma B (fallback)
```json
{
  "refresh_token": "jwt_refresh"
}
```

### Response 200
```json
{
  "access_token": "jwt_access_novo",
  "refresh_token": "jwt_refresh_novo"
}
```

Regras:
- Sempre retornar novo `access_token`.
- `refresh_token` pode ser rotacionado (recomendado).

### Response 401
Refresh inválido/expirado/revogado.

## 4) Listagem de Missões

### Endpoint
`GET /api/missions`

### Header obrigatório
`Authorization: Bearer <access_token>`

### Response 200
Lista JSON de missões:
```json
[
  {
    "id": 1,
    "name": "Missão Alpha"
  }
]
```

### Response 401
Somente quando token realmente inválido/expirado/ausente.

## Regra Crítica de Consistência
Se `GET /api/auth/me` retornar `200` para um `access_token`, então `GET /api/missions` deve aceitar o mesmo token e não retornar `401`.

Em outras palavras:
- Mesma estratégia de validação JWT.
- Mesmo segredo/algoritmo.
- Mesmas regras de expiração e verificação de claims necessárias.
- Mesmo parser de header `Authorization: Bearer <token>`.

## Critérios de Aceite (Backend)
1. Login com credenciais válidas retorna `200` + `access_token`.
2. Com esse token, `GET /api/auth/me` retorna `200`.
3. Com esse token, `GET /api/missions` retorna `200` (não `401`).
4. Access expirado em `GET /api/missions` retorna `401` + erro padronizado.
5. Refresh válido retorna novo `access_token` e permite repetir o passo 3.

## Matriz de Testes Rápida
1. `POST /api/auth/login` com usuário válido -> `200`.
2. `GET /api/auth/me` com token retornado -> `200`.
3. `GET /api/missions` com mesmo token -> `200`.
4. Forçar expiração do token -> `GET /api/missions` -> `401`.
5. `POST /api/auth/refresh` com refresh válido -> `200`.
6. Repetir `GET /api/missions` com novo access -> `200`.

## Observações para Evolução
- Ideal manter versionamento de contrato (`/api/v1/...`) quando houver breaking change.
- Evitar alterar nomes de campos já consumidos sem período de compatibilidade.
- Documentar claramente se `user_id` sai no login ou só em `/auth/me`.
