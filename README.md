# Vantly Neural — Plataforma de Missões para Drones

Aplicativo Android em Kotlin para planejamento, execução e monitoramento de missões de drones, com foco em operação autônoma, telemetria, streaming e reconhecimento facial. O projeto evoluiu para uma arquitetura mais modular, com separação entre `core`, `features` e `presentation`.

## Status
Em desenvolvimento ativo. O fluxo principal (login → dashboard → missões → controle) já existe, e o módulo de missão foi refatorado para uso em produção (suspend functions, validações e timeouts).

## Visão Geral
O app integra:
- Conexão com drones DJI (SDK)
- Planejamento e execução de missões
- Telemetria e controle do drone
- Streaming RTMP (drone e celular)
- Reconhecimento e cadastro de rostos
- Autenticação e sessão de usuário
- Telas em Jetpack Compose

## Arquitetura e Estrutura
Estrutura geral de pacotes (simplificada):

```
app/src/main/java/com/sloth/registerapp/
├── core/
│   ├── auth/               # Sessão, token, helpers de autenticação
│   ├── dji/                # Integração com DJI SDK
│   ├── network/            # Retrofit, WebSocket, conectividade
│   ├── database/           # Room (AppDatabase, DAOs, Entities)
│   ├── settings/           # Configuração (RTMP, etc.)
│   ├── ui/theme/           # Tema base
│   └── utils/              # Helpers (permissões, arquivos, datas)
├── features/
│   ├── auth/               # Domain + data de autenticação
│   ├── facedetection/      # Domain + data (ML Kit + Room)
│   ├── mission/            # Domain + data (missões, telemetria, drone)
│   └── streaming/          # Streaming RTMP (drone/celular)
└── presentation/
    ├── app/                # Welcome, Dashboard, MainActivity
    ├── auth/               # Login/Register
    ├── facedetection/      # Telas e viewmodels de reconhecimento
    ├── mission/            # Telas de missão, câmera, controle
    ├── report/             # Relatórios
    ├── settings/           # Configurações
    └── shared/components/  # Componentes reutilizáveis
```

## Fluxo do Aplicativo
Fluxo atual da navegação (MainActivity):
1. **Welcome** → **Login** ou **Register**
2. **Dashboard** (status do drone e atalhos)
3. **Missions** (lista + criação)
4. **Mission Control** (Activity dedicada)
5. **Drone Camera / Cell Camera** (feeds)
6. **Reports** (listagem + detalhe)
7. **Settings**

## Funcionalidades
- **Autenticação** com sessão persistente
- **Dashboard** com status de conexão e ações rápidas
- **Missões**: listar, criar e executar
- **Controle de drone** (telemetria e comandos)
- **Streaming RTMP** (drone e celular)
- **Reconhecimento facial** (captura, registro e lista)
- **Relatórios** por missão
- **Tema e UI moderna** em Compose

## Alterações Recentes (Resumo)
Refatoração do `DroneMissionManager` com foco em produção:
- Callbacks → `suspend` functions
- Timeouts para upload/start/stop
- Validações robustas (waypoints, velocidades)
- Exceptions customizadas
- Cleanup e prevenção de memory leaks
- Exemplos e documentação de integração

## Documentação
- [Índice da documentação](docs/README.md)
- [Análise do projeto em campo](docs/ANALISE_DRONE_3_DIAS.md)
- [Estratégia de modelos ML offline](docs/ESTRATEGIA_MODELOS_ML_OFFLINE.md)
- [Plano auth offline first](docs/PLANO_TODO_AUTH_OFFLINE_FIRST.md)
- [Contrato API auth + missões](docs/SPEC_CONTRATO_AUTH_MISSOES.md)
- [Galeria por missão](docs/TODO_GALERIA_MISSAO_EXECUCAO.md)
- [Camada climática](docs/TODO_IMPLEMENTACAO_CLIMA.md)
- [Rework de settings e mídia](docs/TODO_SETTINGS_E_MIDIA_REWORK.md)

## To-dos
### Concluído
- Refatoração completa do `DroneMissionManager`
- Documentação técnica da refatoração
- Exemplo de `MissionViewModel`
- Exemplos de testes

### Pendente
- Implementar Waypoint Mission real no `DroneCommandManager`
- Concluir bind do feed DJI em `DroneCameraScreen` e `VideoFeedActivity`
- Implementar upload de missão e WebSocket listener no `MissionRepositoryImpl`
- Finalizar sincronização de missões (cache/conflitos)
- Implementar recuperação de senha no login
- Substituir `BASE_URL` de teste no `RetrofitClient`
- Ajustar `AuthenticationHelper` para usar context real
- Integrar telemetria real no `DroneControlScreen`

## Pré-requisitos
- Android Studio (recente)
- Kotlin/Gradle atualizados
- DJI Mobile SDK configurado
- Dispositivo físico Android (para testes reais)

## Como rodar
1. Abra o projeto no Android Studio
2. Configure a chave do DJI SDK no `AndroidManifest.xml`
3. Configure os tokens do Mapbox em `local.properties`, `gradle.properties` ou variáveis de ambiente:

```properties
MAPBOX_ACCESS_TOKEN=SEU_TOKEN_PUBLICO_DO_MAPBOX
MAPBOX_DOWNLOADS_TOKEN=SEU_TOKEN_DE_DOWNLOADS_DO_MAPBOX
WEATHER_API_KEY=SUA_CHAVE_CLIMA
```

4. Sincronize o Gradle
5. Rode em um dispositivo físico

## Licença
MIT
