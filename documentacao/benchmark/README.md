# Benchmark da detecção de pessoas — ai-drone

Esta pasta contém o **protocolo, as tabelas de resultados e as conclusões** de avaliação dos modelos de visão computacional do ai-drone. É documentação de trabalho atual, não `ideias futuras/`.

## O que é um teste independente?

Durante o treinamento o modelo ajusta seus pesos com imagens de **treino**. O conjunto de **validação** ajuda a acompanhar os resultados e escolher resolução, modelo e outros parâmetros. Como já usamos a validação do VisDrone para escolher 960 px e o melhor experimento, ela não equivale mais a uma avaliação inteiramente independente dessas decisões.

Um **conjunto de teste independente (test set)** é formado por imagens e cenas que não foram usadas para treinar, validar ou ajustar o modelo. Idealmente, são vídeos/cenas e locais diferentes, capturados por outra ocasião/câmera, inclusive com características próximas ao DJI Mavic Pro. **Não dividir quadros muito próximos do mesmo vídeo entre treino e teste**, pois isso pode inflar os resultados.

Exemplos de cenas relevantes: pessoas pequenas e distantes; parcialmente cobertas por árvores; campo/vegetação; sombras; grupos; pessoas isoladas; cenas sem pessoas (falsos alarmes); diferentes alturas e iluminações. Respeitar licença e privacidade no uso dos dados.

## O que medir?

1. **Precisão (precision)**: proporção das detecções consideradas corretas.
2. **Recall**: proporção das pessoas anotadas que foram encontradas. Fundamental para buscas, mas não garante segurança operacional.
3. **mAP50 e mAP50-95**: qualidade de detecção em diferentes limiares de sobreposição.
4. **Falsos positivos e falsos negativos por cenário**, com revisão visual das caixas.
5. **Latência e FPS no Samsung Galaxy Tab S9**, consumo de memória, aquecimento e estabilidade. Velocidades da RTX 4060 Ti não representam o desempenho no tablet.

Para calcular métricas quantitativas, precisamos de **anotações de referência (ground truth)** independentes no formato esperado pelo avaliador. Imagens sem anotação podem servir para uma revisão qualitativa, mas não sustentam conclusões sobre recall/mAP. Evitar imagens que têm pessoas visíveis mas rótulos ausentes, pois distorcem métricas.

## Regras para uma comparação justa

- Guardar a versão/peso do modelo e seus hiperparâmetros.
- Avaliar modelos **no mesmo conjunto de teste**, mesma resolução de inferência e mesmos critérios de avaliação.
- Separar resultados de *validação* e de *teste independente*; não chamar os primeiros de benchmark final.
- Não ajustar o modelo repetidamente com base no teste independente: isso contaminaria o conjunto. Se ajustes forem necessários, criar novo conjunto independente para a avaliação final.
- Guardar os dados grandes e pesos fora da pasta `documentacao/` e, em geral, fora do Git; registrar apenas manifestos/caminhos, licenças, scripts, tabelas e pequenas evidências autorizadas.

## Resultados atuais — validação VisDrone, não teste independente

Dataset: `VisDrone-pessoas`; 548 imagens, 13.969 instâncias anotadas de `pessoa`.

| Modelo | Treino (imgsz) | Avaliação (imgsz) | Precisão | Recall | mAP50 | mAP50-95 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| YOLO26n especializado | 640 | 640 | 0,610 | 0,427 | 0,476 | 0,185 |
| YOLO26n especializado | 640 | 960 | 0,666 | 0,501 | 0,566 | 0,237 |
| YOLO26n especializado | 960 | 960 | **0,706** | **0,532** | **0,608** | **0,262** |

Esses números vieram dos logs informados pelo operador e são relativos ao mesmo conjunto de validação que orientou decisões do projeto. **Ainda não há métricas em um teste realmente independente nem medição no Tab S9.**

## Próximo procedimento

1. Separar um conjunto independente com imagens e rótulos de pessoas, de preferência cenas distintas do VisDrone train/val (ou outro dataset compatível/licenciado e material próprio autorizado).
2. Registrar origem, licença, cenas e quantidade de imagens; conferir anotações.
3. Criar arquivo YAML de teste com a classe `pessoa` e executar `yolo detect val` usando o modelo congelado `pessoas_960/weights/best.pt`.
4. Registrar as métricas e revisar falsos negativos e falsos alarmes.
5. Só depois comparar alternativas como YOLO26s, inferência a 640 e 960, TFLite e execução no tablet.

O detector atual ainda **não é adequado para decisões autônomas de voo ou busca e resgate operacional** sem validação adicional e supervisão.

## Benchmark automatizado com o VisDrone test-dev

Há uma terceira divisão oficial do VisDrone, `test-dev`, contendo **1.610 imagens com anotações**, separadas de `train` e `val`. O script `scripts/preparar_visdrone_pessoas.py` agora prepara também `test` quando existem `images/test` e `labels/test` no VisDrone original. Esta avaliação será independente do conjunto que orientou a escolha de 960 px, embora continue no mesmo domínio de imagens VisDrone. <https://docs.ultralytics.com/datasets/detect/visdrone/>

### Procedimento no PC (Fish)

```fish
cd /home/yusei/Documents/workspace/ai-drone
git pull origin main
python scripts/preparar_visdrone_pessoas.py
```

Confirmar no terminal `test: 1610 imagens` e conferir que o arquivo `/home/yusei/Documents/workspace/datasets/VisDrone-pessoas/pessoas.yaml` contém `test: images/test`.

Se `test` não estiver disponível, não executar a avaliação até obter a divisão oficial do VisDrone. O comando `yolo detect val model=yolo26n.pt data=VisDrone.yaml split=test` pode inicializar/baixar a divisão de teste por meio da configuração oficial Ultralytics, mas **usa outro modelo** apenas para preparar os dados e não deve ser confundido com a avaliação do detector do ai-drone. Em seguida reexecutar o script de preparação.

**Benchmark do modelo de 960 px (depois de confirmar `test`):**

```fish
yolo detect val \
    model=runs/detect/runs/ai_drone/pessoas_960/weights/best.pt \
    data=/home/yusei/Documents/workspace/datasets/VisDrone-pessoas/pessoas.yaml \
    split=test \
    imgsz=960 \
    batch=4 \
    device=0 \
    workers=4 \
    project=runs/ai_drone \
    name=benchmark_testdev_960
```

Verificar no log que o carregador analisou **1.610 imagens**. Anotar precisão, recall, mAP50 e mAP50-95; os valores só serão conhecidos após execução no PC. Não fazer ajustes usando esses resultados e depois tratá-los como teste final intocado.

**Limitação importante:** `test-dev` verifica generalização para outras imagens do VisDrone, mas não substitui testes em vídeos reais do DJI Mavic Pro, ambientes de vegetação locais e medições no Galaxy Tab S9.

## Preparação local confirmada — divisão test-dev

O operador confirmou no PC a presença de **1.610 JPGs** em `VisDrone/images/test` e **1.610 arquivos TXT** em `VisDrone/labels/test`. Após executar `scripts/preparar_visdrone_pessoas.py`, o terminal registrou:

| Split | Imagens | Anotações de pessoas | Imagens sem pessoas anotadas |
| --- | ---: | ---: | ---: |
| train | 6.471 | 106.396 | 787 |
| val | 548 | 13.969 | 17 |
| test | **1.610** | **27.382** | **343** |

O arquivo YAML foi gerado em `/home/yusei/Documents/workspace/datasets/VisDrone-pessoas/pessoas.yaml`. **Benchmark ainda não executado** no momento deste registro. Próximo passo: rodar `yolo detect val` com `split=test`, `imgsz=960` e `pessoas_960/weights/best.pt` para obter as métricas. Conferir se o carregador efetivamente lê 1.610 imagens e registrar os resultados reais. As 343 imagens sem rótulo de pessoa podem incluir regiões ignoradas ou alvos não anotados; revisar antes de interpretar falsos positivos.

## Resultado do benchmark independente — execução concluída

Modelo: `runs/detect/runs/ai_drone/pessoas_960/weights/best.pt`. Configuração: `split=test`, `imgsz=960`, `batch=4`, `device=0`, `workers=4`. Ultralytics 8.4.210, RTX 4060 Ti.

Conjunto: **1.610 imagens** e **27.382 anotações de pessoas** do VisDrone test-dev; a divisão test-dev não participou do treino nem da escolha de hiperparâmetros registrados até aqui.

| Métrica | Validação (548 imagens) | Benchmark test-dev (1.610 imagens) | Diferença |
| --- | ---: | ---: | ---: |
| Precisão | 0,706 | **0,572** | −13,4 p.p. |
| Recall | 0,532 | **0,357** | −17,5 p.p. |
| mAP50 | 0,608 | **0,378** | −23,0 p.p. |
| mAP50-95 | 0,262 | **0,148** | −11,4 p.p. |

- Velocidade de validação no PC: pré-processamento 0,6 ms, **inferência 2,4 ms**, pós-processamento 0,6 ms por imagem.
- O carregador elevou `max_det` automaticamente de 300 para 376 devido a uma imagem com até 376 objetos.
- Resultados locais: `runs/detect/runs/ai_drone/benchmark_testdev_960`.
- **Interpretação:** há queda substancial de desempenho no conjunto independente. Isso sugere dificuldade de generalização e/ou diferenças na composição e nas anotações entre splits, mas o log não permite apontar uma causa única. Recall de 35,7% não é adequado para considerar busca operacional confiável.
- O benchmark test-dev já foi consultado; evitar otimizá-lo repetidamente e depois alegar que continua independente da seleção de hiperparâmetros.

### Próximas ações

1. Investigar qualitativamente falsos negativos, caixas sobre áreas ignoradas e erros de anotação; considerar distribuição de tamanhos de pessoas por split.
2. Criar um conjunto **novo e externo ao VisDrone** com imagens de vegetação e terrenos semelhantes ao uso pretendido, com rótulos revisados. Reservar uma parte intocada para a avaliação final.
3. Comparar propostas de melhoria em validação/desenvolvimento, com versões e protocolos registrados; não usar test-dev como único guia iterativo.
4. Executar inferência offline no Galaxy Tab S9 e medir tempo/memória antes da integração DJI. Não habilitar controle de voo automático a partir de um detector ainda experimental.

Resultados informados pelo operador a partir da execução local; o assistente não executou o benchmark no dispositivo do usuário.
