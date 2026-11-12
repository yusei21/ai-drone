# ai-drone

Aplicativo Android para operar drones DJI, consultar telemetria, executar missoes e **detectar pessoas em video** usando um modelo YOLO26n rodando no dispositivo. Projeto em desenvolvimento ativo.

**Versao do app:** v1.1.0 (`versionCode 2`). **Modelo atual:** YOLO26n Pessoas 640. O nome do arquivo embarcado no Android e `pessoas_ativo.tflite`.

## O que o operador encontra

- **Visao geral:** estado da conexao DJI e atalho primario **Abrir camera DJI**.
- **Camera DJI:** feed, controles de camera, status da IA, telemetria e minimapa recolhivel.
- **Deteccao de pessoas:** modelo LiteRT/TFLite local com selecao automatica, ativado no painel de IA.
- **Missoes:** lista, planejamento e execucao (com limitacoes ainda em validacao).
- **Relatorios:** historico por missao; nao inserir resultados ficticios como dados reais.
- **Configuracoes:** preferências, estado da conta, versao e identificacao do modelo.

O redesign busca hierarquia simples e interfaces funcionais inspiradas em Notion/Vercel; **nem todos os fluxos secundarios passaram por auditoria de usabilidade**.

## Estado de validacao — 12/11/2026

| Etapa | Situacao |
| --- | --- |
| Treinamento YOLO26n (~7 h, 640 px) | Concluido no PC |
| Validacao do checkpoint | P=0,826; R=0,591; mAP50=0,697; mAP50–95=0,417 |
| Exportacao do TFLite | Concluida; tensores `[1,3,640,640]` → `[1,5,8400]` |
| Build Android antes do redesign final | `BUILD SUCCESSFUL` informado pelo operador |
| Instalacao do APK anterior no tablet | `adb install -r` = `Success` |
| Build/instalacao apos os commits atuais de UX | **Ainda nao confirmados** |
| Inferencia YOLO26n 640 sobre feed DJI no tablet | **Ainda nao validada com benchmark medido** |

**Importante:** a IA roda no celular/tablet, nao no hardware do Mavic Pro. O sucesso de conexao DJI em versao anterior nao confirma automaticamente estabilidade da nova interface.

Veja os [benchmarks completos](docs/BENCHMARKS_YOLO26_PESSOAS_640.md) para dados, protocolo e limitacoes.

## Arquitetura

```text
app/src/main/java/com/sloth/registerapp/
  core/             # DJI, sessao, rede, configuracoes
  features/
    mldetection/    # Registro de modelos, decodificacao e inferencia
    objectdetection/# Preferencias da deteccao
    mission/        # Missoes, telemetria e operacoes
    streaming/      # RTMP e video
  presentation/
    app/            # Dashboard, navegacao, configuracoes, relatorios
    mission/        # Telas de planejamento e execucao
    video/          # Camera DJI e celular
```

O app usa Kotlin + Jetpack Compose, DJI Mobile SDK e LiteRT/TFLite.

## Construir e instalar

Requer JDK/Android SDK configurados, dependencias do projeto e tablet Android com depuracao USB autorizada. Chaves e tokens, quando exigidos pelo ambiente:

```properties
MAPBOX_ACCESS_TOKEN=SEU_TOKEN_PUBLICO_DO_MAPBOX
MAPBOX_DOWNLOADS_TOKEN=SEU_TOKEN_DE_DOWNLOADS_DO_MAPBOX
WEATHER_API_KEY=SUA_CHAVE_CLIMA
```

O modelo nao e publicado no Git. Se estiver preparando um build novo a partir dos pesos treinados:

```fish
cd /home/yusei/Documents/workspace/ai-drone

.venv/bin/python scripts/exportar_modelo_android.py \
  --pesos runs/ai_drone/pessoas_7h_20261112/weights/best.pt \
  --imgsz 640

.venv/bin/python scripts/instalar_modelo_android.py \
  --modelo runs/android_models/pessoas_640.tflite

./gradlew :app:assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Se o asset `app/src/main/assets/ml_models/pessoas_ativo.tflite` ja existir, a instalacao do modelo podera exigir `--replace`. **Nao rode exportacao/treinamento novamente apenas para mudar a UI**; mantenha o asset local ao atualizar com `git pull`.

Ao instalar, verifique a versao nas Configuracoes, abra a camera e teste a IA com o drone **no solo**. A permissao USB deve aparecer como `device` (nao `unauthorized`).

## Documentacao

- [Indice tecnico](docs/README.md)
- [Benchmarks YOLO26n Pessoas 640](docs/BENCHMARKS_YOLO26_PESSOAS_640.md)
- [UX e verificacao da v1.1.0](docs/UX_V1_1.md)
- [Analise do projeto em campo](docs/ANALISE_DRONE_3_DIAS.md)
- [Plano de deteccao no video DJI](docs/PLANO_DETECCAO_OBJETOS_VIDEO_DRONE.md)
- [Estrategia historica de modelos e sincronizacao](docs/ESTRATEGIA_MODELOS_ML_OFFLINE.md)

## Pendencias principais

1. Compilar e testar o HEAD atual em Android real, incluindo orientacao do tablet.
2. Testar equivalencia das deteccoes `.pt` vs `.tflite`.
3. Medir latencia/FPS ponta a ponta no Tab S9 com video DJI.
4. Avaliar recall para pessoas pequenas e imagens aereas.
5. Revisar estados de erro, acessibilidade e navegacao de telas secundarias.
6. Continuar a estabilizacao dos fluxos de missoes e sincronizacao.

## Licenca

MIT
