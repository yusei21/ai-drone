# Benchmarks — YOLO26n Pessoas 640

Atualizado em 12/11/2026. Fonte dos numeros: saidas do treinamento e exportacao fornecidas pelo operador no terminal. **Nao sao testes executados pelo CI.**

## Identificacao e reprodutibilidade

| Campo | Valor |
| --- | --- |
| Tarefa | Deteccao de pessoas (1 classe) |
| Arquitetura | Ultralytics YOLO26n |
| Treinamento | `scripts/treinar_yolo26_7h.sh` |
| Checkpoint | `runs/ai_drone/pessoas_7h_20261112/weights/best.pt` |
| Dataset YAML | `/home/yusei/Documents/workspace/datasets/pessoas_multiangulo/pessoas_multiangulo.yaml` |
| Resolucao | 640 x 640 |
| Hardware de treino | NVIDIA RTX 4060 Ti 8 GB |
| Framework no ambiente de exportacao | Ultralytics 8.4.210, Python 3.12.16 |
| Exportacao | `scripts/exportar_modelo_android.py` |

Configuracao declarada no script de treinamento: `time=7`, `epochs=100` (teto), `batch=8`, `device=0`, `optimizer=AdamW`, `lr0=0.002`, `cos_lr=True`, `amp=True`, `workers=4`, `imgsz=640`, `patience=20`, `close_mosaic=10`, `max_det=888`. A duracao de sete horas e aproximada. O numero exato de epocas completadas nao consta nos logs compartilhados.

## Composicao de datasets

Contagens compartilhadas durante a preparacao dos dados:

| Fonte | Treino | Validacao |
| --- | ---: | ---: |
| VisDrone pessoas | 6.471 | 548 |
| COCO pessoa filtrado | 58.903 | 2.466 |
| CrowdHuman-YOLO conservador | 2.875 | 875 |
| **Total** | **68.249** | **3.889** |

A validacao final informou 3.889 imagens e **32.253 instancias**. Nao ha analise publicada de recall por altura, angulo, tamanho do alvo ou origem dos dados.

## Qualidade observada

| Metrica | Primeira epoca de teste | Treino final de ~7 h | Variacao |
| --- | ---: | ---: | ---: |
| Precisao (P) | 0,663 | **0,826** | +0,163 |
| Recall (R) | 0,417 | **0,591** | +0,174 |
| mAP@50 | 0,484 | **0,697** | +0,213 |
| mAP@50–95 | N/D | **0,417** | N/D |

Os resultados iniciais vieram de uma execucao de teste de uma epoca; por isso a comparacao e exploratoria, nao uma comparacao controlada entre modelos diferentes. O recall de 59,1% evidencia falsos negativos e exige testes com pessoas pequenas, distantes, parcialmente ocluidas e vistas de cima.

## Tempos reportados pelo Ultralytics

Log final do treinamento/validacao, **por imagem, no ambiente de validacao**:

| Etapa | Tempo reportado |
| --- | ---: |
| Preprocessamento | 0,1 ms |
| Inferencia | 0,8 ms |
| Pos-processamento | 0,5 ms |
| Total da soma informada | 1,4 ms |

O log tambem apresenta `loss: 0,0 ms`, que nao e indicador de latencia da aplicacao.

**Nao extrapolar esses numeros para o Galaxy Tab S9.** Nao incorporam decodificacao de video DJI, copia de frames, renderizacao Compose, throttling termico, delegate LiteRT no Android ou FPS ponta a ponta. O dispositivo usou o aplicativo anterior em teste de conexao, mas nao ha benchmark Android reproduzivel do novo `pessoas_640.tflite`.

## Exportacao LiteRT / TFLite

Resultado fornecido pelo operador:

- Checkpoint fonte: `best.pt`
- Saida original: `runs/ai_drone/pessoas_7h_20261112/weights/best.tflite`
- Copia de distribuicao: `runs/android_models/pessoas_640.tflite`
- Arquivo incorporado ao APK: `app/src/main/assets/ml_models/pessoas_ativo.tflite`
- Tamanho incorporado: **9,43 MiB** (aproximadamente 9,4 MB conforme exportador)
- Entrada: `[1, 3, 640, 640]`, `float32`, NCHW
- Saida: `[1, 5, 8400]`, `float32`
- Verificacao de estrutura dos tensores: **aprovada pelo script**
- Equivalencia numerica e visual das caixas PT vs TFLite: **nao validada**
- Inferencia real com Mavic Pro + Tab S9 usando este modelo: **nao validada no log disponivel**

O nome `pessoas_ativo.tflite` e a identidade padrao carregada automaticamente pelo Android; trata-se do modelo exportado como `pessoas_640.tflite`, nao de um segundo treinamento.

## Build e instalacao ja relatados

O operador reportou `BUILD SUCCESSFUL in 33s` (43 tasks, 16 executadas, 27 atualizadas) e `adb install -r ...` com `Success` na versao anterior as ultimas alteracoes de UI/UX. Isso comprova compilacao e instalacao daquele commit, **nao** compila nem testa automaticamente o HEAD atual.

## Protocolo de benchmark Android (pendente)

1. Compilar o HEAD atual: `./gradlew :app:assembleDebug`.
2. Conferir a inclusao de `assets/ml_models/pessoas_ativo.tflite` no APK.
3. Instalar em Galaxy Tab S9 e confirmar em log o carregamento do asset, sem erro de delegate/tensor.
4. Com Mavic Pro no solo e o video ativo, medir 300+ inferencias com aquecimento inicial separado.
5. Registrar P50/P95 de inferencia, FPS de video e de deteccao, temperatura, consumo e taxa de quadros descartados.
6. Comparar as caixas, confiancas e falsos negativos entre `best.pt` e TFLite em um conjunto fixo de imagens aereas.
7. Repetir com CPU e GPU quando suportados e documentar versao do Android e do app.

### Resultados de dispositivo

| Metrica Android | Valor |
| --- | --- |
| Tempo P50/P95 da inferencia | Nao medido |
| FPS da deteccao sobre video DJI | Nao medido |
| Latencia camera → caixas renderizadas | Nao medida |
| Uso de memoria e aquecimento | Nao medidos |
| Precisao/recall no proprio Mavic Pro | Nao medidos |
| Estabilidade em sessao prolongada | Nao medida |

## Comandos para reproduzir a exportacao

```fish
cd /home/yusei/Documents/workspace/ai-drone
.venv/bin/python scripts/exportar_modelo_android.py \
  --pesos runs/ai_drone/pessoas_7h_20261112/weights/best.pt \
  --imgsz 640
.venv/bin/python scripts/instalar_modelo_android.py \
  --modelo runs/android_models/pessoas_640.tflite
./gradlew :app:assembleDebug
```

Os pesos e datasets ficam fora do Git; nao e possivel reproduzir os numeros de treinamento apenas clonando o repositorio.
