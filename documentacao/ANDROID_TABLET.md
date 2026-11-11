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
