# Integração Android — Galaxy Tab S9

## Estado atual

O aplicativo Android existente foi importado do RegisterDJI e preserva módulos DJI, câmera, vídeo, telemetria e UI Jetpack Compose.

Em 2026-11-11 foi corrigida a tela `CellCameraScreen.kt`, que ainda referenciava o `PersonDetector.kt` removido. Agora ela usa o `MLDetectionViewModel` e `DetectionResult` já existentes, com seleção de um modelo TFLite local. O preview e a detecção por IA continuam componentes independentes; a IA não envia comandos ao drone.

**Ainda não foi compilado nem testado um APK em dispositivo.** Não existe detector atualizado embutido: o peso `best.pt` do YOLO26n foi treinado apenas no PC. O aplicativo não deve ser tratado como pronto para testes de inferência até exportarmos um `.tflite` compatível e verificarmos o mecanismo de saída.

## Modelo escolhido para primeira prova de conceito

- `runs/detect/runs/ai_drone/pessoas_960/weights/best.pt` é o baseline registrado com resultado de test-dev (precisão 0,572, recall 0,357, mAP50 0,378, mAP50-95 0,148).
- O operador iniciou treinamento adicional `pessoas_960_continuacao`; **não substituir o baseline automaticamente** sem validação comparável.
- Limiar de confiança de interface inicial em torno de 0,27 conforme melhor F1 da validação, não valor seguro nem calibrado para campo.
- Modelo deve ser rotulado `pessoa` e funcionar totalmente offline.

## Etapas de implementação

1. **Compilação de referência:** executar `./gradlew :app:assembleDebug` com Android SDK e JDK compatíveis, verificar conflitos do SDK DJI legado e testar navegação.
2. **Exportação no PC:** exportar uma cópia do `best.pt` para `format=tflite` (começar com `imgsz=640` para estabelecer desempenho; comparar 960 depois). O Ultralytics pode exigir dependências TensorFlow adicionais. Registrar versão, forma e tipo dos tensores de entrada/saída, modelo pós-processado/NMS ou saída crua. **Não presumir que qualquer TFLite será aceito pelo decoder atual.**
3. **Compatibilidade do motor:** `YoloInferenceEngine` atual interpreta tensores `[1,4+C,N]` (float32) com caixas `xywh` e NMS em Kotlin. Validar o formato exato produzido pelo YOLO26 (que pode ter shape/posprocessamento diferentes). Ajustar pré-processamento (letterbox vs redimensionamento simples) e conversão das coordenadas antes de confiar nas caixas. Verificar desempenho do delegado GPU e alternativa CPU.
4. **Instalação do modelo:** `ModelRegistry` lê `filesDir/ml_models/*.tflite` e arquivo de texto de mesmo nome com classes, por exemplo `pessoa`. Implementar fluxo de importação via seletor de arquivos Android para o usuário, sem necessidade de adb, e testar seleção na interface.
5. **Teste offline em tablet:** executar em imagens e vídeos de teste sem drone, registrar FPS/latência, memória, bateria, aquecimento, confidência e qualidade das caixas. Comparar com inferência PyTorch no PC na mesma imagem e resolução.
6. **Integração com câmera do Mavic Pro:** somente depois de aprovada a etapa offline, testar overlay das detecções no feed DJI com piloto no controle.
7. **Segurança:** nenhuma detecção controla automaticamente aeronave; não habilitar rastreamento, pouso, missões ou movimento automático com base em caixas sem testes independentes e controles de segurança.

## Alterações já aplicadas

- `app/src/main/java/com/sloth/registerapp/presentation/video/screens/CellCameraScreen.kt`: substituída dependência quebrada do detector antigo pelo ViewModel genérico, evitando referência a classe removida. Este código não foi compilado no ambiente Android neste momento.
- O módulo `features/mldetection` mantém a infraestrutura já existente para modelos locais.

## Verificação da versão local

```fish
cd /home/yusei/Documents/workspace/ai-drone
git pull origin main
./gradlew :app:assembleDebug
```

Se o comando Gradle falhar, registrar o **primeiro erro** relevante. Não instalar nem assumir que o APK está pronto até a compilação e os testes funcionais concluírem.

## Implementacao do teste de IA offline (11/11/2026)

Foi adicionada a atividade `OfflineAiTestActivity`, independente da conexao DJI, com:

- Camera traseira local do tablet via CameraX.
- Botao para conceder permissao de camera.
- Botao **Importar .tflite**, usando o seletor de arquivos Android.
- Decoder experimental de caixas `pessoa` e confianca; contagem e latencia por inferencia.
- Thread exclusiva para LiteRT/TFLite e delegate de GPU, com tentativa de CPU se o delegate falhar.
- Limite provisório de `conf=0.27` e captura de frame aproximadamente a cada 450 ms (nao e FPS maximo).
- Sem login, internet, controlador DJI ou comandos de voo nessa atividade.

**Como acessar:** na tela **Ao vivo** do aplicativo, tocar no novo botao de **frasco / Science (verde)**. O botao de telefone existente continua abrindo a tela anterior da camera com streaming.

**Status:** codigo publicado, **ainda nao recompilado nem testado no Tab S9**. O APK de 374 MB instalado anteriormente nao possui essa tela. A tela depende da compilacao de um novo APK.

### 1. Atualizar e compilar o novo APK no CachyOS (fish)

```fish
cd /home/yusei/Documents/workspace/ai-drone
git pull origin main
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Se o build falhar, enviar o **primeiro erro**, especialmente erros Kotlin relativos a CameraX, Compose ou interpretador.

### 2. Exportar pesos YOLO26n no PC

Apos a execucao do treinamento que estiver rodando (para evitar disputar recursos), com a venv ativa:

```fish
cd /home/yusei/Documents/workspace/ai-drone
source .venv/bin/activate.fish
python scripts/exportar_modelo_android.py --imgsz 640
```

O script usa inicialmente o benchmark conhecido `pessoas_960/weights/best.pt` e cria `runs/android_models/pessoas_640.tflite`. Para testar o ajuste fino depois de comparar com a validacao, passar o caminho de seu `best.pt` em `--pesos`.

A exportacao usa **`format="litert"`**, atualmente recomendado pela Ultralytics (o artefato continua sendo `.tflite`), com entrada e saida `float32` e `nms=None` (saida crua). Ela pode instalar dependencias e demorar.

O script **verifica explicitamente** entrada `[1,640,640,3]` e saida `[1,5,N]`. Se a versao instalada gerar formato diferente, e necessario adaptar o decoder **antes** de importar. Nao utilizar `nms=False` sem revisar o decoder, pois YOLO26 pode exportar `[1,300,6]` com caixas `xyxy`.

**Importante:** nao ha modelo dentro do repositorio nem do APK. O script deve ser executado pelo usuario no PC que possui os pesos.

### 3. Copiar modelo para o tablet e testar

```fish
adb push runs/android_models/pessoas_640.tflite /sdcard/Download/pessoas_640.tflite
```

No app, abrir **Ao vivo → frasco verde → Permitir camera → Importar .tflite**, selecionar o arquivo em Downloads. A selecao copia o modelo para o armazenamento privado do app e tenta carregá-lo. O app deve mostrar o nome, numero de detecções e latencia. O decoder atualmente redimensiona a imagem para um quadrado, sem `letterbox`; as caixas devem ser comparadas com uma inferencia do PC na **mesma** imagem para confirmar a geometria antes de considerar detecções confiaveis.

### 4. Validacao de resultado

- Compilar e abrir; verificar se a camera aparece.
- Confirmar importacao do `.tflite` e ausencia de erro de tensor.
- Observar caixas de pessoas e confiancas em condicoes variadas, incluindo objetos confundiveis e pessoas pequenas.
- Medir latencia com CPU e delegate GPU, aquecimento e estabilidade ao longo de alguns minutos.
- Verificar que a tela **nao** envia comandos DJI.
- Anotar erros em `adb logcat`.

O Tab S9 em teste reportou `PAGESIZE=4096`, ou seja, pages de 4 KB. O alerta de alinhamento 16 KB e uma verificacao separada do funcionamento atual desta tela. A tela de video DJI sem drone mostrou `Service Not Connected`, esperado quando nao ha aeronave conectada; nao comprova falha do SDK.

### Limitacoes conhecidas

Esta e uma **prova de conceito**. As predicoes sao feitas em snapshots do `PreviewView`, nao em um pipeline CameraX `ImageAnalysis` otimizado. O processamento `RGB float32` faz copia/alocacao por frame, e o decoder suporta somente a forma de saida acima. FPS, memoria e resultados visuais so podem ser considerados confirmados apos teste real no Tab S9. Nao usar detecoes para pilotagem automatica, salvamento de vidas ou monitoramento sem supervisao.

## Incidente de exportacao LiteRT e isolamento do ambiente

Em 11/11/2026, o build Android concluiu com `BUILD SUCCESSFUL` (9s no build incremental) e a instalacao no Galaxy Tab S9 concluiu com `adb install ... Success`.

A primeira execucao de `python scripts/exportar_modelo_android.py --imgsz 640` **nao produziu modelo**: o Ultralytics 8.4.210 detectou falta de `litert-torch>=0.10.0` e `ai-edge-litert>=2.3.0`, instalou automaticamente 69 pacotes e substituiu `torch==2.15.0` por `torch==2.14.1` na `.venv` de treinamento. A conversao falhou na importacao `litert_torch -> torchao -> torch._inductor` com `RuntimeError: duplicate registrations for aten.miopen_batch_norm.default`. O exportador informou ser necessario reiniciar o processo depois da instalacao. **Nenhum `.tflite` resultou deste teste.**

A chamada utilizada apontou ao **baseline antigo** `pessoas_960/weights/best.pt`; o modelo melhorado esta em `pessoas_960_continuacao/weights/best.pt`.

Recomendacao: **nao reinstalar nem remover PyTorch na `.venv` original neste momento**, pois o treino e suas dependencias seriam afetados. Testar exportacao em processo novo no ambiente original apenas se necessario; preferir criar `.venv-export` totalmente separada e instalar Ultralytics + dependencias LiteRT ali, para evitar alteracoes adicionais no ambiente de treinamento. Conferir a forma da saida antes de importar o modelo no tablet. O melhor checkpoint so deve substituir o baseline apos verificacao experimental.

