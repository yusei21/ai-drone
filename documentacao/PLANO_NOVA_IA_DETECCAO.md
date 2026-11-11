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
O log recebido demonstra que CUDA foi reconhecido e o download do VisDrone começou (captura mostrava 60%). **Não há confirmação de que o download nem o treinamento terminaram.**

No log, o Ultralytics resolveu `save_dir` para:
`runs/detect/runs/ai_drone/teste_cuda`
Verifique o caminho no log final; não assuma que os pesos foram criados.

## Etapas propostas
1. Concluir o teste de 3 épocas e guardar métricas, erros e tempos de treino; verificar uso de VRAM.
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
Remover o detector legado **não** remove o reconhecimento facial, pois é um módulo distinto. Não houve execução de compilação Gradle ou teste físico pelo assistente. O sucesso do primeiro treinamento continua pendente do log final do PC.
