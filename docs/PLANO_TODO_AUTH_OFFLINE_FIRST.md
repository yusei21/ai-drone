# Plano de Implementação - Auth Opcional + Offline First

## Objetivo
Implementar o novo fluxo do app com login não obrigatório, separando claramente operações locais e operações de servidor, sem quebrar funcionalidades atuais.

## Princípios
- Operações locais funcionam sem token e sem internet.
- Operações de servidor exigem internet + autenticação válida.
- Expiração de token não bloqueia uso local.
- Todas as missões locais têm dono (`owner_user_id`).

---

## Etapa 0 - Diagnóstico e baseline
- [x] Mapear pontos que hoje dependem de token para navegar.
- [x] Mapear telas/ações que hoje chamam servidor automaticamente.
- [x] Congelar comportamento atual com build verde e smoke test.

### Critério de aceite
- [x] `:app:compileDebugKotlin` verde.
- [x] Lista de pontos críticos documentada (MainActivity, Mission screens, repos).

### Resultado da etapa
- `MainActivity`: gate de navegação inicial atrelado a sessão/token.
- `MissionListViewModel` e telas de missão: chamadas automáticas ao servidor em `fetchMissions`.
- `MissionRepositoryImpl`: sincronização de fila ocorre quando `getMissions()` é acionado e há internet.
- Build de baseline e pós-ajuste confirmada com `BUILD SUCCESSFUL`.

---

## Etapa 1 - Modelo de estados do app
- [x] Criar modelo de estado de sessão local:
  - `LOCAL_LOGGED_OUT`
  - `LOCAL_LOGGED_IN(userId)`
- [x] Definir estado de rede:
  - `ONLINE`
  - `OFFLINE`
- [x] Definir estado de auth servidor (interno):
  - `SERVER_AUTH_OK`
  - `SERVER_ACCESS_EXPIRED`
  - `SERVER_AUTH_REQUIRED`

### Critério de aceite
- [x] Estados disponíveis para UI e camada de dados.
- [x] Sem impacto visual ainda (somente infraestrutura).

---

## Etapa 2 - Fluxo de entrada com botão "Pular"
- [x] Ajustar fluxo inicial para não exigir login obrigatório.
- [x] Adicionar/validar botão `Pular` na Welcome/Login flow.
- [x] Remover gate global de navegação baseado apenas em token.

### Critério de aceite
- [x] Usuário entra no app sem login.
- [x] Dashboard/vídeo/telemetria acessíveis sem login.

### Resultado da etapa
- `WelcomeScreen` recebeu ação explícita `Pular por agora`.
- Novo `LocalSessionManager` implementado para estado local + modo convidado.
- `MainActivity` usa estado local (usuário ou guest) para decidir entrada em `dashboard`.
- `LoginScreen` agora define sessão local ao autenticar com servidor.

---

## Etapa 3 - Sessão local (dono ativo)
- [x] Criar/ajustar `LocalSessionManager` para armazenar `current_user_id`.
- [x] Login online define `LOCAL_LOGGED_IN(user_id)`.
- [x] Logout define `LOCAL_LOGGED_OUT`.
- [x] Troca de conta muda apenas usuário ativo local.

### Critério de aceite
- [x] Estado local muda corretamente em login/logout/troca de conta.
- [x] Sem limpar cache global automaticamente.

### Resultado da etapa
- `LocalSessionState` evoluiu para modelo com payload de usuário: `LOCAL_LOGGED_IN(userId)`.
- `LocalSessionManager` mantém `current_user_id` e adiciona `switchActiveUser(...)`.
- Login/logout do app atualizam estado local sem gatilho de limpeza de cache global.

---

## Etapa 4 - Ownership no SQLite
- [x] Adicionar `owner_user_id` nas tabelas locais de missão/relatório.
- [x] Criar migração de Room segura para schema novo.
- [x] Garantir inserts com owner correto.
- [x] Garantir queries filtradas por `owner_user_id == current_user_id`.

### Critério de aceite
- [x] Dados locais isolados por usuário ativo.
- [x] Troca de conta só troca filtro; dados antigos permanecem no banco.

### Resultado da etapa
- Schema atualizado com `ownerUserId` em `mission_cache` e `flight_reports` (e também em `sync_queue` para preservar isolamento da fila).
- Migração `2 -> 3` adicionada em `AppDatabase` com default `__guest__`.
- `MissionRepositoryImpl` agora lê/escreve cache/fila por owner atual (`currentUserId`, fallback `__guest__`).
- `FlightReportManager` passa a carregar e persistir relatórios filtrados por owner.
- Build validada: `:app:compileDebugKotlin` verde.

---

## Etapa 5 - Regras de permissão por estado
- [x] `LOCAL_LOGGED_OUT`:
  - [x] Permitir: dashboard, vídeo, telemetria, execução local.
  - [x] Bloquear: criar missão, sync, pull/push servidor.
- [x] `LOCAL_LOGGED_IN`:
  - [x] Permitir: criar/editar local, executar local.
  - [x] Servidor apenas quando online + auth válida.

### Critério de aceite
- [x] Botões/ações respeitam regra em tempo real.
- [x] Mensagens de bloqueio claras para o usuário.

### Resultado da etapa
- Em modo `LOCAL_LOGGED_OUT` (`owner = __guest__`), `MissionRepositoryImpl` não chama servidor para listar/detalhar missões e não dispara sync.
- Criação de missão em modo convidado foi bloqueada no repositório com mensagem de domínio.
- Tela de missões agora indica bloqueio com mensagem clara e direciona para login ao tentar criar.

---

## Etapa 6 - Auth servidor sob demanda
- [x] Manter `access + refresh` com renovação automática no `401`.
- [x] Não forçar login até que o usuário execute ação de servidor.
- [x] Se refresh falhar: marcar `SERVER_AUTH_REQUIRED` e pedir login apenas para ações remotas.

### Critério de aceite
- [x] Offline continua operacional localmente mesmo com access expirado.
- [x] Ao tentar sync/pull: fluxo 401 -> refresh -> retry funcionando.

### Resultado da etapa
- `LocalSessionManager` passou a manter `serverAuthState` (`SERVER_AUTH_OK` / `SERVER_AUTH_REQUIRED`).
- Login bem-sucedido marca `SERVER_AUTH_OK`.
- `TokenRefreshAuthenticator` marca `SERVER_AUTH_OK` no refresh com sucesso e `SERVER_AUTH_REQUIRED` quando refresh falha.
- Entrada do app não depende só de token: sessão local ativa continua no dashboard.
- UI de missões exibe mensagem de sessão remota expirada sem forçar logout global.

---

## Etapa 7 - Sincronização offline-first por usuário
- [x] Pré-condições de sync:
  - [x] `ONLINE`
  - [x] `LOCAL_LOGGED_IN`
  - [x] auth servidor válida/renovável
- [x] Push local -> servidor para `pending_upload`/`dirty` do usuário atual.
- [x] Pull servidor -> local apenas do usuário atual.
- [x] Atualizar `last_sync_at`.
- [ ] Tratar conflitos (`conflict`) com estratégia inicial (marcar e adiar resolução UI).

### Critério de aceite
- [x] Sync não roda em `LOCAL_LOGGED_OUT`.
- [x] Sync não mistura dados entre usuários.

### Resultado da etapa
- `MissionRepositoryImpl` passou a aplicar pré-condições de sync (`ONLINE`, owner válido, `serverAuthState` diferente de `SERVER_AUTH_REQUIRED`).
- Em ausência de pré-condições, operações de listagem/detalhe usam apenas cache local do owner.
- Push da fila e pull remoto seguem isolados por owner (`ownerUserId`).
- `last_sync_at` é atualizado após pull/upload remoto com sucesso.

---

## Etapa 8 - Relatórios e missão integrados ao owner
- [x] Integrar `FlightReportManager` com owner atual.
- [x] Garantir que relatório local também siga filtro por usuário.
- [x] Preparar espaço para novos campos futuros sem quebrar migrações.

### Critério de aceite
- [x] Tela de relatório só mostra dados do usuário local ativo.

### Resultado da etapa
- `FlightReportManager` resolve owner local ativo e persiste/consulta por `ownerUserId`.
- DAO de relatórios e entidade passaram a operar com isolamento por usuário.
- Estrutura de `extraData` permanece flexível para expansão futura sem quebra.

---

## Etapa 9 - UX e mensagens
- [x] Mensagens consistentes:
  - [x] "Operação local disponível"
  - [x] "Faça login para sincronizar com servidor"
  - [x] "Sem internet, trabalhando localmente"
- [x] Ajustar mensagens de erro técnico para linguagem de produto.

### Critério de aceite
- [x] Usuário entende claramente quando está em modo local vs servidor.

### Resultado da etapa
- Tela de missões exibe aviso contextual por estado (convidado, sessão remota expirada, offline).
- Fluxo de criação direciona para login apenas quando necessário (sem bloquear uso local geral).

---

## Etapa 10 - Testes e validação final
- [ ] Testes manuais guiados (matriz):
  - [ ] sem login + offline
  - [ ] sem login + online
  - [ ] logado + offline
  - [ ] logado + online
  - [ ] access expirado + refresh válido
  - [ ] access expirado + refresh expirado
  - [ ] troca de conta sem limpar cache
- [ ] Build final.

### Critério de aceite
- [ ] `:app:compileDebugKotlin` verde.
- [ ] Sem regressão em drone/mission/video/report.

---

## Ordem de execução (para não se perder)
1. Etapa 0
2. Etapa 1
3. Etapa 2
4. Etapa 3
5. Etapa 4
6. Etapa 5
7. Etapa 6
8. Etapa 7
9. Etapa 8
10. Etapa 9
11. Etapa 10

---

## Riscos e mitigação
- Risco: migração Room quebrar base existente.
  - Mitigação: migração incremental + validação de schema.
- Risco: gate incorreto bloquear funcionalidades locais.
  - Mitigação: feature flags internas por etapa + smoke tests por tela.
- Risco: concorrência de sync gerar estado inconsistente.
  - Mitigação: fila serial para operações de sync.

---

## Definição de pronto (DoD)
- [ ] Login não obrigatório funcional.
- [ ] Modo local robusto sem servidor.
- [ ] Sync seguro por usuário ativo.
- [ ] Tokens renovados sob demanda, sem fricção desnecessária.
- [ ] Build verde e fluxo principal validado manualmente.
