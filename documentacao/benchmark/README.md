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
