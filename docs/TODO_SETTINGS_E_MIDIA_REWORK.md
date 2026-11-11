# TODO - Rework Configurações e Mídia (Alinhado à Arquitetura por Feature)

## Objetivo
Executar as mudanças de Configurações/Mídia sem quebrar o padrão do projeto, mantendo separação por `core`, `features/*` e `presentation/*`.

## Princípios de organização (obrigatórios)
1. **UI e navegação**: sempre em `presentation/...`.
2. **Regra de negócio por domínio**: em `features/<feature>/domain/...`.
3. **Persistência específica de feature**: em `features/<feature>/data/...`.
4. **Infra compartilhada** (Room base, helpers globais, sessão, rede): em `core/...`.
5. Evitar “God classes” em `SettingsScreen`; mover lógica para managers/repositories por feature.

---

## Mapa de alocação de arquivos (revisado)

## 1) Settings (UI)
- `presentation/app/settings/screens/SettingsScreen.kt`:
- remover lápis do card de usuário;
- remover itens fora de escopo (2FA, editar email, auto-limpeza, limite cache, Wi‑Fi sync, baixa resolução, grade).
- adicionar links para novas telas (`Últimos logins`, `Permissões`, `Sobre`, `Política`).

## 2) Auth/Login History (nova feature de dados de autenticação local)
- **Entidade/DAO no banco central**:
- `core/database/Entities.kt` (nova `LoginHistoryEntity`)
- `core/database/Daos.kt` (novo `LoginHistoryDao`)
- `core/database/AppDatabase.kt` (registro + migração)
- **Manager da feature auth**:
- `features/auth/data/manager/LoginHistoryManager.kt` (novo)
- **Modelo de domínio**:
- `features/auth/domain/model/LoginHistory.kt` (novo)
- **Integração no fluxo de login**:
- `features/auth/data/repository/AuthRepositoryImpl.kt` (ou ponto central de login já usado)

## 3) Report/Mídia por missão + destino de armazenamento
- `features/report/domain/model/MissionMedia.kt` (já existe, ampliar se necessário)
- `features/report/data/manager/MissionMediaManager.kt` (aplicar regra PHONE vs DRONE_SD)
- `core/settings/MediaStorageSettingsRepository.kt` (novo; preferência compartilhada)
- `presentation/video/screens/DroneCameraScreen.kt` (captura respeitando configuração)
- `presentation/app/report/screens/ReportScreens.kt` (exibir origem correta da mídia)

## 4) Limpeza de missões locais / tamanho real
- Regras e cálculo em camada de missão:
- `features/mission/data/repository/MissionRepositoryImpl.kt` (métodos de limpeza/cálculo)
- opcional utilitário em `core/utils/FileManager.kt` (se envolver arquivos)
- Consumo na UI:
- `presentation/app/settings/screens/SettingsScreen.kt`

## 5) Tema (global)
- Persistência:
- `core/settings/AppThemeSettingsRepository.kt` (novo)
- Aplicação:
- `presentation/app/main/activities/MainActivity.kt`
- `presentation/app/theme/Theme.kt` (já existente)

## 6) Unidade de medida + telemetria
- Preferência:
- `core/settings/MeasurementSettingsRepository.kt` (novo)
- Conversão:
- `core/utils/MeasurementConverter.kt` (novo)
- Aplicação:
- `features/mission/data/drone/manager/DroneTelemetryManager.kt` e telas de telemetria em `presentation/mission/...` e `presentation/video/...`.

## 7) Permissões e Sobre (novas telas)
- `presentation/app/settings/screens/PermissionsScreen.kt` (novo)
- `presentation/app/settings/screens/AboutScreen.kt` (novo)
- `presentation/app/settings/screens/PrivacyPolicyScreen.kt` (novo)
- Rotas no `presentation/app/main/activities/MainActivity.kt`

---

## Plano passo a passo (com encaixe arquitetural)

## Etapa 1 - Limpeza de UI em Configurações
### Implementação
- Ajustar apenas `presentation/app/settings/screens/SettingsScreen.kt`.
- Sem regras de negócio nesta etapa.

### Aceite
- Lápis removido.
- Email editável e 2FA ocultos.
- Opções obsoletas removidas.

---

## Etapa 2 - Base de dados para “Últimos logins”
### Implementação
- Criar `LoginHistoryEntity`, `LoginHistoryDao`, migração no `AppDatabase`.
- Criar `features/auth/domain/model/LoginHistory.kt`.
- Criar `features/auth/data/manager/LoginHistoryManager.kt`.

### Aceite
- Tentativas de login ficam persistidas por `ownerUserId`.

---

## Etapa 3 - Integração do histórico de login no fluxo de auth
### Implementação
- Registrar eventos `SUCCESS/FAILED` no ponto de login do `features/auth`.
- Evitar lógica de persistência diretamente na tela.

### Aceite
- Históricos surgem automaticamente após tentativas reais de login.

---

## Etapa 4 - Tela “Últimos logins”
### Implementação
- Nova `RecentLoginsScreen` em `presentation/app/settings/screens`.
- Consulta via `LoginHistoryManager`.
- Navegação adicionada em `MainActivity`.

### Aceite
- Usuário visualiza lista real do SQLite sem mocks.

---

## Etapa 5 - Configuração de destino de mídia (PHONE/DRONE_SD)
### Implementação
- Criar `core/settings/MediaStorageSettingsRepository.kt`.
- Expor configuração na `SettingsScreen`.
- Aplicar regra em `DroneCameraScreen` + `MissionMediaManager`.

### Aceite
- Foto/vídeo respeitam local configurado.

---

## Etapa 6 - Limpar missões locais + tamanho real
### Implementação
- Métodos de cálculo/limpeza no `MissionRepositoryImpl`.
- UI apenas dispara ação e mostra confirmação/resultado.

### Aceite
- Exibe tamanho real.
- Limpa dados locais de missão por usuário.

---

## Etapa 7 - Tema claro/escuro funcional
### Implementação
- Repositório de preferência em `core/settings`.
- Aplicação global no `MainActivity` + `Theme`.

### Aceite
- Troca de tema afeta todo app imediatamente.

---

## Etapa 8 - Unidade de medida real na telemetria
### Implementação
- `MeasurementSettingsRepository` + `MeasurementConverter`.
- Aplicar em componentes de telemetria (missão e câmera).

### Aceite
- Valores mudam dinamicamente conforme unidade escolhida.

---

## Etapa 9 - Permissões e Sobre
### Implementação
- Criar 3 telas novas em `presentation/app/settings/screens`:
- `PermissionsScreen`
- `AboutScreen`
- `PrivacyPolicyScreen`
- Adicionar rotas e acessos.

### Aceite
- Todas as telas acessíveis por Configurações.

---

## Decisões para evitar acoplamento
1. `SettingsScreen` só orquestra UI/ações, sem lógica pesada de dados.
2. Persistência de login fica em `features/auth`, não em `presentation`.
3. Regras de mídia por missão ficam em `features/report`, não em `core`.
4. Preferências globais (tema/unidade/destino mídia) ficam em `core/settings`.
5. Limpeza e cálculo de missões ficam em `features/mission`.

---

## Checklist final
- [x] Estrutura nova respeita separação por feature/camada.
- [x] Nenhum manager de negócio criado dentro de `presentation/...`.
- [x] Histórico de login persistido em Room e exibido em tela própria.
- [x] Destino de mídia configurável aplicado na captura.
- [x] Tema e unidade funcionando globalmente.
- [x] Telas de permissões/sobre/política implementadas e roteadas.

---

## Progresso executado (rodada atual)
- [x] Etapa 1 (parcial completa): limpeza da UI de Configurações.
- [x] Etapa 2: Room + migração + manager para últimos logins.
- [x] Etapa 3: integração de registro de login no fluxo real (sucesso/falha).
- [x] Etapa 4: tela `RecentLoginsScreen` criada e roteada.
- [x] Etapa 5: destino de mídia `Celular/SD do drone` integrado ao fluxo de captura.
- [x] Etapa 6: limpeza de missões locais + cálculo de tamanho exibido.
- [x] Etapa 8: unidade de medida aplicada na telemetria (HUD de câmera + controle).
- [x] Etapa 9: `PermissionsScreen`, `AboutScreen`, `PrivacyPolicyScreen` criadas e roteadas.
- [x] Tema global com DataStore integrado no `MainActivity`.
- [ ] Etapas pendentes: refinos finais de UX/textos e possíveis integrações extras (ex.: tela de missão controle/telemetria avançada).
