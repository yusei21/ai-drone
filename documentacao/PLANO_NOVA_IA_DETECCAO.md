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

## Preparação do dataset especializado (script adicionado ao GitHub)

Arquivo: `scripts/preparar_visdrone_pessoas.py`.

O script converte os rótulos YOLO do VisDrone que já foi baixado pelo Ultralytics, fazendo `pedestrian` (classe 0) e `people` (classe 1) virarem uma só classe `pessoa` (0). Os rótulos das outras classes não são mantidos. O script cria uma pasta nova de anotações, usa links simbólicos para imagens originais e preserva os splits `train`/`val`. Não mexe nos arquivos do dataset original.

Comandos sugeridos no Fish:

```fish
cd /home/yusei/Documents/workspace/ai-drone
git pull origin main
python scripts/preparar_visdrone_pessoas.py
```

Saída YAML esperada: `/home/yusei/Documents/workspace/datasets/VisDrone-pessoas/pessoas.yaml`.

Antes de treinar é necessário inspecionar exemplos com caixas: classes excluídas (ex.: motos) e regiões ignoradas podem conter pessoas não rotuladas, o que pode gerar falsos negativos e prejudicar a IA. O novo dataset **não é adequado para uma aplicação operacional sem revisão e testes adicionais**.

Após a revisão, experimento inicial sugerido (não executado pelo assistente):

```fish
yolo detect train model=yolo26n.pt data=/home/yusei/Documents/workspace/datasets/VisDrone-pessoas/pessoas.yaml epochs=30 imgsz=640 batch=4 device=0 workers=4 project=runs/ai_drone name=pessoas_baseline
```

Usar `yolo26n` como primeiro baseline especializado antes de investir em modelos maiores. Validar mAP, recall, falsos alarmes e resultados em cenas independentes. Não usar o novo modelo para guiar voos automaticamente sem validação física e controles de segurança.

## Conversão do VisDrone para detecção de pessoas — concluída localmente

O operador executou `python scripts/preparar_visdrone_pessoas.py` com sucesso após atualizar o repositório até `af75f5f`.

Resultado reportado:
- Treino: **6.471 imagens**, **106.396 anotações de pessoas**, **787 imagens sem pessoas anotadas**.
- Validação: **548 imagens**, **13.969 anotações de pessoas**, **17 imagens sem pessoas anotadas**.
- Total: **7.019 imagens**, **120.365 anotações de pessoas**, **804 imagens sem pessoas anotadas**.
- Saída: `/home/yusei/Documents/workspace/datasets/VisDrone-pessoas/pessoas.yaml`.
- Classes `pedestrian` e `people` foram remapeadas para `pessoa`, preservando a base original.

### Verificação antes do próximo treinamento
1. Examinar amostras com muitas pessoas, pessoas pequenas e arquivos de rótulos vazios; não presumir que todas as imagens com rótulos vazios estejam de fato sem pessoas.
2. Considerar as regiões ignoradas do VisDrone e os objetos excluídos do remapeamento; evitar penalizar previsões sobre alvos não anotados.
3. Manter treino e validação independentes e auditar a distribuição de classes.
4. Executar primeiro um treino especializado de referência com YOLO26n, 640 px, batch inicial 4 e 30 épocas, após revisão dos rótulos.
5. Medir recall e mAP para a classe `pessoa`, além de avaliar imagens de campo e situações negativas.

Estes resultados foram informados pelo terminal do operador; o assistente não executou os scripts localmente.

## Incidente: treinamento especializado de 30 épocas com conjunto incompleto

O operador relatou que o treino de 30 épocas concluiu em 0,015 h (~54 s), com 26 batches por época usando batch=4 e validação sobre apenas **1 imagem / 1 instância**, em vez das 548 imagens esperadas. O `best.pt` gerado **não está validado**: mAP50=0,995 e recall=1 sobre uma única anotação não são métricas representativas. Não usar no aplicativo.

**Hipótese técnica:** o script inicial criou symlinks de diretórios em `images/train` e `images/val`. Alguns mecanismos de varredura recursiva de arquivos não seguem symlinks de diretório. Como correção, `scripts/preparar_visdrone_pessoas.py` agora cria hardlinks de imagens individuais (com fallback por cópia), recria o layout local preservando origem e rótulos, e remove caches antigos sob a saída.

Reexecutar depois de `git pull origin main`:

```fish
python scripts/preparar_visdrone_pessoas.py
find /home/yusei/Documents/workspace/datasets/VisDrone-pessoas/images/train -maxdepth 1 -type f | wc -l
find /home/yusei/Documents/workspace/datasets/VisDrone-pessoas/images/val -maxdepth 1 -type f | wc -l
```

**Valores esperados: 6471 train e 548 val.** Conferir também nas linhas de inicialização do YOLO `train: Scanning ... 6471` e `val: Scanning ... 548` antes de treinar 30 épocas novamente. Usar outro nome de experimento, por exemplo `pessoas_baseline_corrigido`, e não sobrescrever o experimento anterior. Se a contagem inicial ainda estiver errada, interromper e investigar antes do treino. Mesmo com essa correção, é necessária revisão das imagens negativas e anotações ignoradas.
