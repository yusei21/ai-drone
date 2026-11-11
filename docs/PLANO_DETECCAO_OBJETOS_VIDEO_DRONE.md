# Plano de Implementacao: Deteccao de Objetos no Video do Drone

## Objetivo
Adicionar suporte a ate 4 modelos de deteccao de objetos executados sobre a imagem de video do drone, com:
- escolha do modelo ativo em tempo de uso,
- execucao sobre frames do feed ao vivo,
- overlay visual com bounding boxes e labels,
- base preparada para persistir o modelo selecionado,
- e reaproveitamento do legado de deteccao facial como referencia estrutural.

Este documento descreve como a implementacao deve ser feita no codigo atual. Nao e uma especificacao abstrata; ele considera as classes e o fluxo que ja existem hoje no app.

## Escopo do MVP
O MVP deve cobrir:
- ate 4 modelos locais de deteccao,
- apenas 1 modelo ativo por vez,
- botao na tela do video do drone para abrir um modal de selecao,
- inferencia local no dispositivo,
- overlay em cima do preview do video,
- controle simples de FPS e backpressure para nao travar a UI,
- telemetria basica de runtime (modelo ativo, ultimo tempo de inferencia, erros).

Nao entra no MVP:
- rodar mais de um modelo simultaneamente,
- fallback cloud,
- tracking multi-objeto entre frames,
- gravacao de video com overlay embutido no arquivo,
- download remoto de modelos.

## Analise do codigo atual

## 1) Onde o video do drone entra hoje
O ponto central e [DroneCameraScreen.kt](/home/peterson/Documentos/Faculdade/Projeto%20-%20Drone/Drone-App/Drone%20App/app/src/main/java/com/sloth/registerapp/presentation/video/screens/DroneCameraScreen.kt).

Hoje o fluxo e:
- `VideoFeedView(...)` cria um `TextureView` Compose/Android em [VideoFeedView.kt](/home/peterson/Documentos/Faculdade/Projeto%20-%20Drone/Drone-App/Drone%20App/app/src/main/java/com/sloth/registerapp/presentation/video/components/VideoFeedView.kt).
- `DroneCameraScreen` recebe o `SurfaceTexture`.
- `DJICodecManager` decodifica o feed do drone nesse surface.
- `VideoFeeder.getInstance().primaryVideoFeed?.addVideoDataListener(...)` injeta os frames compactados no decoder.

Conclusao:
- o app ja exibe o feed do drone corretamente,
- mas hoje nao existe pipeline de inferencia em cima dos frames do feed,
- nem uma camada de overlay especifica para deteccao de objetos.

## 2) Onde a UI do novo recurso deve entrar
Ainda em [DroneCameraScreen.kt](/home/peterson/Documentos/Faculdade/Projeto%20-%20Drone/Drone-App/Drone%20App/app/src/main/java/com/sloth/registerapp/presentation/video/screens/DroneCameraScreen.kt), a tela ja tem:
- `CompactHeader(...)`,
- `CameraControls(...)`,
- overlays de missao,
- mini mapa,
- badges climaticas.

O melhor ponto para a acao de IA e dentro de `CameraControls(...)`, que ja agrupa:
- tirar foto,
- iniciar/parar streaming,
- gravar video,
- trocar para camera do celular.

Recomendacao de UX:
- adicionar um botao de IA em `CameraControls(...)`,
- ao tocar, abrir um `ModalBottomSheet` ou dialog fullscreen leve,
- exibir os 4 modelos disponiveis,
- permitir ativar/desativar a deteccao,
- mostrar qual modelo esta ativo no momento.

## 3) O que existe de legado e pode ser reaproveitado
O modulo legado de rosto e util como referencia arquitetural:

- [FaceAnalyzer.kt](/home/peterson/Documentos/Faculdade/Projeto%20-%20Drone/Drone-App/Drone%20App/app/src/main/java/com/sloth/registerapp/features/facedetection/data/ml/FaceAnalyzer.kt)
  - mostra o padrao `Analyzer + Listener + Config`.
- [FaceOverlayView.kt](/home/peterson/Documentos/Faculdade/Projeto%20-%20Drone/Drone-App/Drone%20App/app/src/main/java/com/sloth/registerapp/presentation/facedetection/components/FaceOverlayView.kt)
  - mostra como desenhar caixas sobre preview, com traducao de coordenadas.
- [FaceRecognitionScreen.kt](/home/peterson/Documentos/Faculdade/Projeto%20-%20Drone/Drone-App/Drone%20App/app/src/main/java/com/sloth/registerapp/presentation/facedetection/screens/FaceRecognitionScreen.kt)
  - mostra um fluxo onde camera + analise + estado de UI coexistem.
- [RecognizePersonUseCase.kt](/home/peterson/Documentos/Faculdade/Projeto%20-%20Drone/Drone-App/Drone%20App/app/src/main/java/com/sloth/registerapp/features/facedetection/domain/usecase/RecognizePersonUseCase.kt)
  - mostra o padrao de orquestracao por caso de uso.

Conclusao:
- o legado nao resolve o feed DJI diretamente,
- mas serve muito bem como molde de organizacao por camadas,
- principalmente para `Analyzer`, `Overlay`, `UseCase` e `UiState`.

## Restricoes tecnicas reais

## 1) O feed do drone nao esta em CameraX
Ao contrario da tela de rosto, o video do drone nao chega via `ImageAnalysis`/`ImageProxy`. Ele chega assim:
- buffer comprimido da DJI,
- enviado ao `DJICodecManager`,
- desenhado em `TextureView`.

Impacto:
- nao da para plugar `FaceAnalyzer` diretamente no feed do drone.
- sera necessario criar uma captura de frame a partir do `TextureView` ou um pipeline paralelo de frame extraction.

## 2) Inference em cima do preview exige controle de carga
Rodar deteccao em todos os frames do feed tende a derrubar FPS e aquecer o aparelho.

Recomendacao:
- inferencia amostrada, por exemplo entre 2 e 5 FPS no MVP,
- nunca processar frame novo enquanto o anterior ainda estiver em inferencia,
- descartar frames intermediarios quando estiver ocupado.

Observacao importante:
- `TextureView.getBitmap()` deve ser tratado como fallback tecnico de primeira iteracao, nao como solucao inocente.
- ele pode alocar bitmap novo por chamada, competir com renderizacao e piorar muito quando combinado com RTMP.
- antes de congelar a implementacao, o app deve medir latencia, memoria e estabilidade em aparelho real.
- tambem vale investigar se o caminho de bytes do feed DJI pode ser interceptado antes do render final, para evitar a copia do frame ja renderizado.

## 3) Ate 4 modelos pede contrato padronizado
Se cada modelo tiver assinatura diferente, a tela e o pipeline vao ficar acoplados.

Recomendacao:
- todos os modelos devem respeitar uma interface unica,
- todos retornam a mesma estrutura de deteccao,
- labels, threshold e input size devem vir do descriptor do modelo.

## Proposta de arquitetura

## Visao geral
Criar um modulo novo focado em object detection no feed do drone, separado do legado de rosto:

- `features/objectdetection/domain`
- `features/objectdetection/data`
- `presentation/video/...` consumindo esse modulo

Fluxo proposto:
1. Usuario abre a tela do drone.
2. `DroneCameraScreen` carrega configuracao da deteccao.
3. Usuario toca no botao de IA.
4. Modal lista no maximo 4 modelos disponiveis.
5. Usuario escolhe um modelo e ativa.
6. O app inicia captura amostrada de bitmap do `TextureView`.
7. O frame segue para um engine de inferencia.
8. O resultado volta como lista padronizada de deteccoes.
9. Um overlay Compose/AndroidView desenha caixas e labels sobre o feed.

## Componentes recomendados

## 1) Dominio

### `ObjectDetectionModel`
Representa um modelo disponivel no app.

Campos sugeridos:
- `id`
- `displayName`
- `assetPath`
- `task`
- `inputWidth`
- `inputHeight`
- `labels`
- `confidenceThreshold`
- `nmsThreshold`
- `requiresPostProcessingNms`
- `deliveryMode`
- `isDefault`

Campos operacionais recomendados:
- `deliveryMode` pode ser `bundled`, `dynamic_feature` ou `external_file`.
- se o modelo estiver registrado mas indisponivel fisicamente no dispositivo, ele nao pode aparecer como selecionavel.

### `DetectedObject`
Contrato padrao de saida da inferencia.

Campos sugeridos:
- `label`
- `score`
- `boundingBoxNormalized`
- `trackingId` opcional
- `modelId`
- `timestampMs`

### `ObjectDetectionResult`
Estados de retorno:
- `Idle`
- `NoObjects`
- `Success(detections, inferenceMs, frameWidth, frameHeight)`
- `Error(message)`

### `DetectionModelRegistry`
Fonte central dos ate 4 modelos suportados.

Responsabilidades:
- listar modelos bundled,
- devolver descritor do modelo ativo,
- validar se o modelo existe.

## 2) Data / ML

### `ObjectDetectionEngine`
Interface unica para inferencia.

Responsabilidades:
- carregar modelo,
- preprocessar bitmap,
- executar inferencia,
- mapear tensores para `DetectedObject`,
- aplicar NMS quando `requiresPostProcessingNms == true`,
- liberar recursos.

### `TfliteObjectDetectionEngine`
Implementacao concreta para TFLite.

Observacao:
- este desenho suporta trocar depois para outro runtime sem quebrar a UI.

### `DroneFrameSampler`
Responsavel por extrair frames do preview do drone.

Responsabilidades:
- ler bitmap do `TextureView` em intervalo controlado,
- redimensionar se necessario,
- despachar frame para analise,
- impedir concorrencia excessiva.

Opcao inicial mais simples:
- usar `TextureView.getBitmap(...)`.

Risco:
- custo de copia de frame.

Mitigacao:
- reduzir resolucao de inferencia,
- limitar FPS de analise,
- reusar buffers quando possivel.
- ter caminho de evolucao para captura mais robusta fora do render path.

Otimizacoes que devem entrar no desenho:
- implementar pool de bitmaps para evitar `Bitmap.createBitmap(...)` por frame;
- fazer downscale agressivo antes da inferencia, por exemplo para `320x320` ou `416x416`, conforme o modelo;
- investigar como opcao evolutiva o acesso aos bytes do feed antes do `TextureView`, caso a SDK/pipe atual permita isso.

Decisao explicita de execucao:
- o sampler nao deve rodar na main thread.
- a implementacao inicial deve usar `HandlerThread` dedicado ou `ScheduledExecutorService` proprio para captura amostrada.
- a inferencia deve rodar em dispatcher/executor separado do sampler.
- a main thread deve apenas refletir estado de UI e desenho do overlay.

Evolucao recomendada:
- se `TextureView.getBitmap()` se mostrar instavel, o plano B deve ser um surface paralelo de menor resolucao para analise ou outro mecanismo de captura desacoplado do render principal.
- se houver acesso viavel ao frame em formato bruto antes do render, priorizar esse caminho sobre captura do bitmap exibido.

### `ObjectDetectionAnalyzer`
Equivalente conceitual do `FaceAnalyzer`, mas para o feed do drone.

Responsabilidades:
- receber bitmap amostrado,
- chamar `ObjectDetectionEngine`,
- publicar resultado via callback/flow,
- ignorar frame novo quando ocupado.

### `ObjectDetectionPreferencesRepository`
Persistencia leve para:
- `activeModelId`
- `detectionEnabled`
- `lastConfidenceThresholdOverride` opcional

Pode usar DataStore ou SharedPreferences, conforme o padrao ja adotado no app.

## 3) Apresentacao

### `ObjectDetectionViewModel`
Responsabilidades:
- expor o modelo ativo,
- expor lista de modelos disponiveis,
- expor `detectionEnabled`,
- expor ultimo `ObjectDetectionResult`,
- controlar inicio/parada da analise,
- receber mudanca de modelo pelo modal.

Observacao de desenho:
- para evitar um ViewModel pesado demais, separar estado persistente/configuracao de estado de runtime da inferencia.
- recomendacao pratica:
  - `ObjectDetectionViewModel` cuida de inferencia, sampler, engine e resultado;
  - o estado efemero do modal pode ficar no composable ou em um state holder mais leve.

### `DetectionOverlay`
Novo componente de overlay sobre o feed do drone.

Pode ser:
- Compose `Canvas`, ou
- `AndroidView` no estilo do `FaceOverlayView`.

Responsabilidades:
- desenhar bounding boxes,
- desenhar label + score,
- traduzir coordenadas normalizadas do frame para a area renderizada,
- lidar com aspect ratio do preview.

### `DetectionModelPickerSheet`
Modal aberto pelo novo botao de IA.

Conteudo sugerido:
- toggle principal `Deteccao ativa`,
- lista dos 4 modelos disponiveis,
- indicacao de modelo ativo,
- breve descricao de cada modelo,
- aviso de impacto em bateria/performance.

## Estrutura de pastas sugerida

```text
app/src/main/java/com/sloth/registerapp/features/objectdetection/
  data/
    engine/
      ObjectDetectionEngine.kt
      TfliteObjectDetectionEngine.kt
    frame/
      DroneFrameSampler.kt
    repository/
      ObjectDetectionPreferencesRepository.kt
    registry/
      DetectionModelRegistryImpl.kt
  domain/
    model/
      ObjectDetectionModel.kt
      DetectedObject.kt
      ObjectDetectionResult.kt
    registry/
      DetectionModelRegistry.kt
    usecase/
      GetAvailableDetectionModelsUseCase.kt
      ObserveActiveDetectionModelUseCase.kt
      SetActiveDetectionModelUseCase.kt
      RunObjectDetectionUseCase.kt
presentation/
  video/
    components/
      DetectionOverlay.kt
      DetectionModelPickerSheet.kt
    viewmodels/
      ObjectDetectionViewModel.kt
```

## Integracao com a tela atual

## 1) Mudancas em `DroneCameraScreen`
As seguintes mudancas sao previstas:

### Estado de UI novo
- `showDetectionModelSheet`
- `detectionEnabled`
- `activeDetectionModel`
- `detectionResult`

### Novo botao em `CameraControls`
Adicionar um botao com icone de IA/visao:
- abre o modal de modelos,
- exibe estado ativo/inativo visualmente,
- pode usar a mesma linguagem visual dos outros botoes compactos.

### Overlay sobre o feed
Inserir `DetectionOverlay(...)` no mesmo `Box` que contem o `VideoFeedView(...)`.

Ordem recomendada:
1. `VideoFeedView`
2. `DetectionOverlay`
3. gradientes/HUD
4. controles

### Inicio da analise
Quando:
- feed estiver disponivel,
- deteccao estiver ativada,
- preview nao estiver desativado,
- e existir `TextureView`/bitmap acessivel.

### Pausa automatica da analise
Pausar quando:
- streaming RTMP estiver consumindo recurso demais,
- preview estiver desativado,
- tela sair de foco,
- usuario desativar deteccao,
- drone desconectar.

## 2) Ajuste necessario em `VideoFeedView`
Hoje [VideoFeedView.kt](/home/peterson/Documentos/Faculdade/Projeto%20-%20Drone/Drone-App/Drone%20App/app/src/main/java/com/sloth/registerapp/presentation/video/components/VideoFeedView.kt) so expone callbacks do `SurfaceTexture`.

Para o pipeline de deteccao, ele deve passar tambem acesso ao `TextureView` ou equivalente.

Exemplo de necessidade:
- callback `onTextureViewReady(textureView: TextureView)`,
- ou retornar referencia para sampler capturar bitmaps.

Sem isso, a camada de analise nao consegue extrair frame do preview renderizado.

Tambem sera necessario definir quem e dono do ciclo de vida dessa referencia:
- `DroneCameraScreen` entrega o `TextureView` ao sampler,
- o sampler deve ser encerrado em `DisposableEffect`/`onDispose`,
- toda referencia ao `TextureView` deve ser anulada ao destruir a tela,
- nenhum worker pode continuar com referencia forte a view depois da saida da tela.

Complemento importante:
- expor tambem dimensoes relevantes por callback:
  - tamanho do preview renderizado,
  - dimensoes originais do frame quando disponiveis,
  - matrix de transformacao/crop aplicada no `TextureView`, se acessivel.

## Modal de escolha de modelo

## Requisitos de UX
- abrir por um botao dedicado na tela do drone;
- mostrar no maximo 4 opcoes;
- permitir apenas 1 modelo ativo;
- mostrar claramente `Ativo`, `Disponivel`, `Desativado`;
- manter a escolha apos fechar a tela;
- permitir desligar a deteccao sem trocar o modelo.

## Proposta de layout do modal
- titulo: `Deteccao de objetos`
- subtitulo: `Escolha o modelo ativo para a analise do video ao vivo`
- switch: `Deteccao em tempo real`
- lista de cards:
  - nome do modelo,
  - tipo de deteccao,
  - classes principais,
  - custo estimado (`leve`, `medio`, `alto`),
  - badge `Ativo`
- rodape opcional:
  - ultimo tempo de inferencia,
  - observacao sobre consumo de bateria

## Exemplos de 4 modelos suportados
Os nomes finais podem mudar, mas o contrato do sistema deve suportar algo assim:
- `general-lite`
- `person-vehicle`
- `infra-structures`
- `thermal-targets` ou outro especializado

O importante nao e o nome; e o sistema aceitar ate 4 entradas padronizadas.

## Pipeline de inferencia proposto

## Passo 1: capturar frame
Origem:
- `TextureView` do `VideoFeedView`.

Estratégia:
- capturar bitmap a cada intervalo fixo,
- por exemplo 250 ms no MVP.

Decisao complementar:
- a captura deve ocorrer em worker dedicado, nunca em loop Compose/main thread.
- a implementacao deve prever troca de estrategia de captura sem alterar a UI.
- `TextureView.getBitmap()` e aceitavel apenas como baseline instrumentado.
- o benchmark dessa captura deve acontecer antes de consolidar a arquitetura definitiva do sampler.

## Passo 2: preprocessar
- redimensionar para o `inputWidth`/`inputHeight` do modelo ativo,
- normalizar pixels,
- aplicar rotacao se necessario,
- preservar referencia do tamanho original para remapeamento das caixas.

## Passo 3: inferencia
- executar em thread dedicada,
- idealmente com `Dispatchers.Default` ou executor proprio,
- nunca na main thread.

Separacao recomendada:
- captura de frame em worker do sampler,
- inferencia em worker do engine,
- entrega do resultado para UI por `StateFlow`.

## Passo 4: pos-processamento
- aplicar confidence threshold,
- aplicar NMS quando `requiresPostProcessingNms == true`,
- converter saida para `DetectedObject`.

Responsabilidade:
- a decisao sobre NMS pertence ao descriptor do modelo;
- a execucao do NMS pertence a `ObjectDetectionEngine`;
- a UI nao deve saber se o modelo usa NMS interno ou externo.

## Passo 5: desenho
- overlay recebe:
  - lista de deteccoes,
  - dimensoes do frame de origem,
  - dimensoes do preview,
  - info de rotacao/espelhamento, se houver.

## Coordenadas e overlay
Este e um dos pontos mais sensiveis.

O `FaceOverlayView` ja mostra a ideia de traducao entre:
- tamanho da imagem analisada,
- tamanho da view,
- rotacao,
- aspect ratio.

Para objeto no drone, precisamos adotar a mesma disciplina, mas preferindo:
- caixas normalizadas de `0f..1f`,
- mapper unico para desenhar na view final.

Recomendacao:
- a engine sempre retorna coordenadas normalizadas;
- o overlay faz a traducao final.

Assim a engine fica desacoplada do tamanho real do preview.

Ponto que precisa estar fechado antes da implementacao do overlay:
- definir a origem da rotacao/orientacao do frame DJI.

Decisao recomendada para o plano:
- o sampler deve publicar junto do frame metadados de orientacao efetiva:
  - `frameWidth`
  - `frameHeight`
  - `rotationDegrees`
  - `isMirrored`
- o overlay deve consumir esses metadados e nunca assumir portrait fixo.
- o overlay nao deve confiar apenas no tamanho do bitmap capturado; ele deve considerar tambem crop, aspect ratio visivel e matrix de transformacao do `TextureView`, quando existir.
- se o feed DJI nao expuser rotacao confiavel, a implementacao MVP deve fixar uma orientacao suportada e documentar a limitacao explicitamente.

## Persistencia do modelo ativo
Como o usuario vai escolher no modal qual modelo usar no momento, o sistema deve persistir a ultima escolha.

Chaves sugeridas:
- `object_detection_active_model_id`
- `object_detection_enabled`

Comportamento esperado:
- ao abrir a tela novamente, o ultimo modelo volta como ativo;
- se o modelo nao existir mais no app, cair para o default;
- se o modelo existir no registry mas o arquivo nao estiver disponivel localmente, marcar como indisponivel e nao permitir ativacao;
- se a deteccao estiver desligada, manter o modelo salvo mas nao iniciar analise.

## Performance e estabilidade

## Regras recomendadas para o MVP
- inferencia maxima de 2 a 5 FPS;
- apenas 1 frame em processamento por vez;
- descartar frames atrasados;
- parar analise quando app estiver em segundo plano;
- parar analise quando `previewEnabled == false`;
- expor `inferenceMs` na UI de debug, pelo menos em log.
- medir memoria/alocacao do caminho de captura de frame.
- medir latencia end-to-end, desde captura do frame ate desenho do overlay.

## Sinais de risco
- `TextureView.getBitmap()` muito pesado em aparelhos mais fracos;
- gargalo se o modelo for grande;
- drift de coordenadas se aspect ratio nao for tratado corretamente;
- conflito de performance com streaming RTMP simultaneo.

## Mitigacoes
- usar modelos leves no MVP;
- permitir desligar deteccao rapidamente;
- reduzir resolucao do frame de inferencia;
- limitar analise quando streaming estiver ativo;
- manter medicao de latencia por modelo.
- preparar fallback de delegates por dispositivo.
- reduzir automaticamente FPS de inferencia quando latencia total ou aquecimento subirem demais.

## Lifecycle e liberacao de recursos
Este ponto deve ser tratado como requisito, nao detalhe.

Regras:
- `DroneFrameSampler` deve ter `start()`, `stop()` e `release()`.
- `ObjectDetectionEngine` deve ter `load()`, `unload()` e `close()`.
- `DroneCameraScreen` deve parar sampler e engine em `onDispose`.
- ao trocar o modelo ativo, descarregar o anterior antes de carregar o novo.
- workers precisam usar referencia fraca ou nulificavel da view para evitar leak.

## Delegates e aceleracao
A decisao sobre delegate nao deve ficar para depois.

Diretriz para a Fase 1:
- definir politica de tentativa nesta ordem: GPU quando compativel, depois NNAPI quando fizer sentido, e por fim CPU.
- registrar em log qual delegate foi realmente usado.
- se o modelo nao atingir latencia minima aceitavel em CPU no aparelho alvo, ele nao deve entrar no conjunto inicial dos 4 modelos.

Observacao de produto:
- 4 modelos bundled podem ser pesados demais para o APK.
- para MVP, considerar:
  - 1 modelo multiclasse forte para validar a pipeline primeiro, ou
  - entrega modular/sob demanda quando o custo final de tamanho for alto.

## Testes
O plano precisa prever validacao automatizada minima.

Cobertura recomendada:
- testes unitarios para parser/pos-processamento da engine;
- testes unitarios do NMS quando aplicavel;
- testes unitarios do mapper de coordenadas do overlay;
- testes do registry verificando fallback quando modelo nao existe ou arquivo esta ausente;
- testes do sampler e do ciclo de vida usando fake `TextureView` ou abstraindo a fonte de frame.
- benchmark simples de captura/inferencia em aparelho real antes de fixar a estrategia do sampler.

## Robustez operacional
Em tela de drone, a pipeline de ML nao pode competir com o controle operacional.

Diretrizes:
- toda chamada ao engine deve ter protecao por `try/catch` na camada Kotlin;
- a pipeline de deteccao deve falhar em modo degradado, nunca bloquear os controles da tela;
- se a deteccao falhar repetidamente, desativar automaticamente e avisar o usuario;
- a thread de inferencia deve ser separada da thread de captura e isolada da UI principal;
- mesmo sabendo que crash nativo pode derrubar o processo inteiro, o desenho deve minimizar estado inconsistente e evitar excecoes nao tratadas.

## Monitoramento termico
Video DJI + inferencia + RTMP + UI pode acionar throttling termico.

Diretriz:
- monitorar sinais de degradacao pratica, como aumento sustentado de latencia e queda de throughput;
- opcionalmente monitorar temperatura do dispositivo quando a API/estrategia adotada permitir;
- ao detectar degradacao, reduzir automaticamente o FPS de inferencia ou desligar a deteccao temporariamente com aviso na UI.

## Estrategia de implementacao por fases

## Fase 1 - Base tecnica
1. Criar dominio `objectdetection`.
2. Criar `DetectionModelRegistry` com ate 4 modelos estaticos.
3. Criar `ObjectDetectionPreferencesRepository`.
4. Criar `ObjectDetectionEngine` com interface unica.
5. Definir politica de delegate e contrato de NMS no descriptor do modelo.
6. Validar estrategia de empacotamento: modelos bundled versus distribuicao modular.

Resultado:
- sistema conhece modelos, mas ainda sem UI final.

## Fase 2 - Integracao com o feed do drone
1. Criar prototipo de benchmark para medir `getBitmap()`, memoria, latencia e impacto visual no feed por pelo menos 1 minuto.
2. Ajustar `VideoFeedView` para expor `TextureView`.
3. Expor tambem metadados do preview/frame necessarios para overlay.
4. Criar `DroneFrameSampler`.
5. Criar `ObjectDetectionAnalyzer`.
6. Validar captura amostrada de frames.
7. Medir custo real de `getBitmap()` antes de fixar a estrategia.

Resultado:
- feed do drone passa a gerar frames utilizaveis para inferencia.

## Fase 3 - UI e controle
1. Criar `ObjectDetectionViewModel`.
2. Adicionar botao de IA em `CameraControls`.
3. Criar `DetectionModelPickerSheet`.
4. Persistir modelo ativo e toggle.

Resultado:
- usuario consegue escolher o modelo no modal.

## Fase 4 - Overlay
1. Criar `DetectionOverlay`.
2. Fechar contrato de orientacao/rotacao do frame.
3. Mapear coordenadas corretamente.
4. Desenhar labels e scores.
5. Tratar estados `sem objetos`, `erro`, `desligado`.

Resultado:
- deteccoes aparecem sobre o video.

## Fase 5 - Robustez
1. Limitar FPS de inferencia.
2. Pausar durante RTMP se necessario.
3. Adicionar logs/metricas.
4. Ajustar thresholds por modelo.

Resultado:
- recurso pronto para uso em campo com previsibilidade maior.

## Contrato minimo entre UI e dominio

## Entrada
- `selectedModelId`
- `detectionEnabled`
- `bitmap/frame`

## Saida
- `activeModel`
- `list<DetectedObject>`
- `inferenceMs`
- `status`

Esse contrato deve ficar independente do tipo exato de modelo. Assim, trocar um dos 4 modelos no futuro nao exige refazer a tela.

## Relacao com a estrategia de modelos offline
Este plano conversa diretamente com [ESTRATEGIA_MODELOS_ML_OFFLINE.md](/home/peterson/Documentos/Faculdade/Projeto%20-%20Drone/Drone-App/Drone%20App/docs/ESTRATEGIA_MODELOS_ML_OFFLINE.md).

Diferenca:
- aquele documento cobre catalogo/download/ativacao em nivel mais amplo;
- este documento cobre a integracao concreta com o video do drone no app atual.

Para o MVP imediato, pode-se usar modelos bundled no APK e adiar download remoto.

## Decisoes recomendadas
- manter no maximo 4 modelos registrados localmente no app;
- permitir apenas 1 modelo ativo por vez;
- usar `TextureView.getBitmap()` apenas como baseline instrumentado, com plano de substituicao se o custo for alto;
- criar modulo novo `objectdetection`, sem misturar com `facedetection`;
- reaproveitar o padrao de `Analyzer + Overlay + ViewModel + UseCase` do legado de rosto;
- encaixar a escolha do modelo via modal dentro de `DroneCameraScreen`.

## Riscos abertos
- a extracao de bitmap do preview DJI pode ter custo alto dependendo do aparelho;
- a SDK/feed atual pode nao oferecer caminho pratico para bytes brutos, forçando a captura do render;
- alguns modelos podem exigir delegados GPU/NNAPI para ficar viaveis;
- 4 modelos bundled podem inflar demais o APK;
- overlay pode precisar ajuste fino com rotacao e crop real do preview;
- latencia total pode ficar aceitavel na inferencia, mas ruim no ciclo completo captura -> inferencia -> overlay;
- streaming e deteccao simultaneos podem precisar uma politica de prioridade.
- o ciclo de vida do sampler precisa ser rigidamente controlado para evitar leaks de view/context.

## Proxima etapa sugerida
Quando for partir para implementacao, a ordem ideal e:
1. expor o `TextureView` em `VideoFeedView`,
2. criar `DroneFrameSampler`,
3. definir `ObjectDetectionModel` + `ObjectDetectionEngine`,
4. adicionar o modal de selecao na `DroneCameraScreen`,
5. depois ligar o overlay.

Essa ordem reduz risco porque resolve primeiro o gargalo tecnico principal: conseguir frames confiaveis do feed do drone.
