# Plano de substituição da IA de detecção — ai-drone

Data: 2026-11-10
Status: preparação da nova IA; **não validada no dispositivo nem no drone**.

## Equipamento e ambiente
- Drone: DJI Mavic Pro original (Mobile SDK V4).
- Dispositivo Android pretendido: Samsung Galaxy Tab S9.
- PC: CachyOS, Fish, NVIDIA GeForce RTX 4060 Ti com 8 GB de VRAM (8188 MiB), 32 GB RAM.
- Python de treinamento em ambiente virtual: 3.12.16, gerenciado por uv.
- Ultralytics: 8.4.210; PyTorch: 2.15.0+cu132.
- Verificação realizada pelo operador: CUDA disponível = True, GPU = RTX 4060 Ti.

## Alterações realizadas no código
- Removido `app/src/main/assets/person_detector.tflite` (modelo de detecção antigo).
- Removida a classe legada `features/detection/data/ml/PersonDetector.kt`.
- Removido o registro fixo de `person-detector` no `ModelRegistry`; o catálogo genérico de modelos locais continua disponível.
- Removido o ID padrão `person-detector` das preferências; enquanto não houver novo modelo, não existe detector ativo.
- Preservados os componentes genéricos de inferência e seleção de modelos, integração DJI, streaming, missões, telemetria e o módulo independente de reconhecimento facial, incluindo `mobile_face_net.tflite`.
- Protegidos por `.gitignore` os ambientes virtuais, datasets e resultados de treino locais.

**Atenção:** esta remoção não adiciona uma nova IA nem comprova compilação Android. A interface de detecção pode ficar sem modelo até a integração. Preferências de instalações antigas podem apontar ao ID legado; o app não deve considerá-lo um modelo disponível.

## Treinamento: andamento reportado
Foi iniciado localmente pelo operador o teste de 3 épocas com:
```fish
yolo detect train model=yolo26n.pt data=VisDrone.yaml epochs=3 imgsz=640 batch=4 device=0 workers=4 project=runs/ai_drone name=teste_cuda
```
O usuário confirmou a conclusão do treinamento e da validação das três épocas, com pesos best.pt e last.pt gravados.

O Ultralytics salvou os resultados e pesos em `runs/detect/runs/ai_drone/teste_cuda/`. A validação final foi executada. Os pesos não foram enviados ao GitHub.

## Etapas propostas
1. **Concluído:** teste de 3 épocas, captura das métricas e do consumo de VRAM.
2. Preparar uma variante do VisDrone para detectar pessoas (classes `pedestrian` e `people` remapeadas conforme o alvo), mantendo separação por cenas e preservando conjuntos de validação/teste.
3. Treinar YOLO26n como baseline e avaliar YOLO26s / YOLO11s nas mesmas condições, adaptando `batch` para 8 GB VRAM.
4. Avaliar mAP50, mAP50-95, recall de pessoas pequenas e falsos positivos; testar com vídeos aéreos não usados no treino.
5. Exportar modelo aprovado para formato Android (TFLite/LiteRT), validar a assinatura dos tensores, pré/pós-processamento, classes e NMS no runtime real.
6. Integrar ao `DroneCameraScreen` com frames amostrados e fila limitada; fazer overlay de caixas e medir latência, FPS, memória e aquecimento no Tab S9.
7. Implementar rastreamento visual; voo automático dependente de validação das APIs do Mavic Pro e testes seguros com controle manual de emergência.
8. Só depois instalar e validar com Mavic Pro real. A geolocalização permanece em `documentacao/ideias futuras/`.

## Versionamento
- Não enviar `.venv/`, `runs/`, datasets ou pesos temporários para GitHub.
- Versionar scripts, configurações, documentação e resultados resumidos.
- Distribuir modelo aprovado de forma versionada, verificando tamanho, licença e integridade (SHA-256).
- Após uma alteração remota, atualizar a cópia local com `git pull` quando não houver alterações locais conflitantes.

## Limites desta etapa
Remover o detector legado **não** remove o reconhecimento facial, pois é um módulo distinto. Não houve execução de compilação Gradle ou teste físico pelo assistente. O primeiro treinamento de teste foi concluído, mas os modelos continuam experimentais e não validados no Android.

## Resultado do primeiro treinamento local (log informado pelo operador)

- Modelo: YOLO26n, VisDrone com 10 classes, 3 épocas, `imgsz=640`, `batch=4`, CUDA na RTX 4060 Ti 8 GB.
- Dataset: 6.471 imagens de treino; 548 de validação, 38.759 instâncias na validação.
- Duração: 0,079 h (~4 min 44 s); épocas de aproximadamente 1min30s.
- Uso de memória GPU mostrado durante as épocas: ~1,3 GB (não equivale ao pico total de VRAM).
- Geral: precisão 0,296; recall 0,235; mAP50 0,194; mAP50-95 0,104.
- `pedestrian`: precisão 0,234; recall 0,357; mAP50 0,221; mAP50-95 0,0832.
- `people`: precisão 0,410; recall 0,170; mAP50 0,165; mAP50-95 0,0461.
- Inferência reportada: 1,2 ms/imagem na GPU do PC (não extrapolar para Tab S9).
- Aviso de dataset: até 902 objetos por imagem; o Ultralytics elevou automaticamente `max_det` para 902. Aviso de `_POSIX_C_SOURCE` sem falha fatal.
- Arquivos produzidos localmente: `runs/detect/runs/ai_drone/teste_cuda/weights/best.pt` e `last.pt`.

**Interpretação:** é um teste de funcionamento bem-sucedido do pipeline CUDA/dataset. A detecção de pessoas ainda apresenta recall insuficiente para uso operacional. Não remover outras funcionalidades do aplicativo durante o desenvolvimento.

**Próximas ações:** criar dataset de uma classe `pessoa` com remapeamento de `pedestrian` e `people`, avaliar divisões e anotações; treinar baseline mais longo, comparar modelos com métricas e analisar falsos negativos. Não usar a IA para comandar voo autônomo sem validação rigorosa.

## Teste de predição visual realizado no PC

O operador confirmou que o comando `yolo detect predict` processou completamente as 548 imagens de validação do VisDrone com o modelo experimental `runs/detect/runs/ai_drone/teste_cuda/weights/best.pt`.

- Inferência média reportada no log: 6,1 ms/imagem, pré-processamento 0,9 ms e pós-processamento 0,8 ms, no PC RTX 4060 Ti.
- As imagens anotadas foram salvas em `runs/detect/predict/`, na máquina local.
- O log inclui detecções das classes `pedestrian`, `people`, `car` etc. Os números indicam predições, **não** confirmam sua correção.
- O desempenho medido no PC não representa a velocidade no tablet Galaxy Tab S9.
- Próximo passo: revisar visualmente uma amostra diversificada, quantificar falsos positivos e negativos e montar um dataset específico para pessoas. Manter o conjunto de validação separado do treino.
- A documentação foi atualizada no GitHub; resultados de predição locais não foram enviados para o repositório.

## Revisão visual preliminar (captura enviada pelo operador)

- O usuário abriu a pasta local `runs/detect/predict/` no VS Code e mostrou uma predição da imagem `0000001_03999_d_0000007.jpg`.
- A imagem urbana aérea mostra caixas `pedestrian` e outros rótulos (`car`, `motor`). Algumas detecções de pessoas exibem confiança em torno de 0,30–0,44.
- O resultado demonstra que o pipeline visual desenha caixas, mas **não comprova acurácia nem o número de pessoas realmente presentes**; comparar com as anotações originais e inspecionar falsos negativos.
- Próxima fase: mapear categorias VisDrone `pedestrian` (0) e `people` (1) para classe única `pessoa` (0), excluindo as caixas de outras categorias. Tratar regiões ignoradas do dataset e possíveis pessoas ambíguas com cuidado, para não gerar rótulos incorretos.
- Separar treino e validação originais; preservar as imagens sem pessoas como negativas quando apropriado, sem vazamento entre splits.
