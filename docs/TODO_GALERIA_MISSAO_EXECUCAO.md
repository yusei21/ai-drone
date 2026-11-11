# TODO - Galeria por Missão (Execução Completa)

## Objetivo
Garantir que o relatório de uma missão mostre apenas as mídias (fotos/vídeos) registradas naquela missão, com fallback offline e mensagem de conexão do drone.

## Fluxo funcional alvo
1. Executar missão `X`.
2. Tirar fotos e gravar vídeo durante a missão.
3. Finalizar missão.
4. Abrir relatório da missão `X`.
5. Ver apenas mídias de `X`.
6. Se drone desconectado:
   - mostrar mídias já baixadas/localizadas no telefone;
   - caso não haja mídia local disponível, mostrar `Conecte o drone para ver as mídias`.

## Implementação

### 1) Persistência de mídia por missão
- [x] Criar entidade `mission_media` no Room.
- [x] Criar `MissionMediaDao` com operações de:
  - [x] inserir mídia;
  - [x] listar por missão;
  - [x] marcar como baixada/local.
- [x] Criar migração `3 -> 4` no banco.

### 2) Modelo e manager de domínio
- [x] Criar modelo de domínio `MissionMedia`.
- [x] Criar `MissionMediaManager` para registrar foto/vídeo por missão e consultar galeria.

### 3) Sessão ativa da missão
- [x] Criar `ActiveMissionSessionManager` (estado global da missão ativa).
- [x] `MissionControlActivity` define missão ativa ao iniciar e limpa ao destruir.

### 4) Registro de capturas
- [x] Integrar `DroneCameraScreen` para registrar foto/vídeo no `MissionMediaManager` quando houver missão ativa.

### 5) Galeria no relatório
- [x] `ReportDetailScreen` agora carrega mídia por `missionId`.
- [x] Exibe somente itens daquela missão.
- [x] Mostra estado por item (`No drone` / `No telefone`).
- [x] Mostra mensagem de fallback quando drone não está conectado e não há mídia local.
- [x] Botão de ação por item (`Baixar` / `Abrir`).

## Observações técnicas
- Nesta versão, `Baixar` marca a mídia como disponível no telefone no estado local para permitir o fluxo de UX e filtro por missão.
- Integração de download real do SD (DJI `MediaManager`) pode ser adicionada em etapa seguinte, reaproveitando a mesma tabela/fluxo.

## Arquivos alterados
- `app/src/main/java/com/sloth/registerapp/core/database/Entities.kt`
- `app/src/main/java/com/sloth/registerapp/core/database/Daos.kt`
- `app/src/main/java/com/sloth/registerapp/core/database/AppDatabase.kt`
- `app/src/main/java/com/sloth/registerapp/features/report/domain/model/MissionMedia.kt`
- `app/src/main/java/com/sloth/registerapp/features/report/data/manager/MissionMediaManager.kt`
- `app/src/main/java/com/sloth/registerapp/core/mission/ActiveMissionSessionManager.kt`
- `app/src/main/java/com/sloth/registerapp/presentation/mission/activities/MissionControlActivity.kt`
- `app/src/main/java/com/sloth/registerapp/presentation/video/screens/DroneCameraScreen.kt`
- `app/src/main/java/com/sloth/registerapp/presentation/app/report/screens/ReportScreens.kt`

## Validação
- [x] Compilar `:app:compileDebugKotlin`
