# Ideia futura: avaliar migração de Python para Rust

**Status:** pesquisa futura; nenhuma migração aprovada ou iniciada.

## Escopo correto

Avaliar substituir **partes dos scripts e ferramentas Python** por Rust, **não** migrar a infraestrutura Android/Kotlin ou o DJI SDK.

Scripts Python atuais no repositório:

- `scripts/preparar_visdrone_pessoas.py`: preparação de dataset, leitura e filtragem de anotações, hardlinks/cópias e geração de YAML. É candidato a benchmark se o tempo de preparação for gargalo.
- `scripts/exportar_modelo_android.py`: utiliza `ultralytics.YOLO(...).export(...)` para exportar modelos LiteRT/TFLite. Manter em Python enquanto depender da cadeia Ultralytics de exportação.

Treinamento e avaliação YOLO com PyTorch/Ultralytics também devem permanecer em Python neste estágio: os cálculos pesados já usam código nativo e CUDA; reescrever o orquestrador em Rust não garante aceleração do treino.

## Resultado preliminar da pesquisa

- A Ultralytics publica `ultralytics-inference`, biblioteca Rust que executa modelos YOLO exportados para ONNX via ONNX Runtime, sem Python na etapa de **inferência**. Documentação: https://docs.ultralytics.com/inference
- A Ultralytics recomenda formatos de execução como ONNX e TensorRT conforme hardware; escolher runtime e precisão pode trazer mais ganho do que trocar a linguagem do código que chama o modelo. Documentação: https://docs.ultralytics.com/modes/export
- Rust pode reduzir overhead, memória e melhorar previsibilidade em utilitários, pipelines ou serviços de inferência, mas a velocidade do núcleo GPU depende do runtime e kernels. Existem relatos de Rust inicialmente mais lento que Python na mesma GPU, logo ganhos não são garantidos: https://github.com/ultralytics/inference/issues/87

## Experimento proposto (sem substituir o atual)

1. Medir duração, pico de RAM e resultado do `preparar_visdrone_pessoas.py` no mesmo dataset.
2. Criar protótipo Rust isolado somente se o tempo de preparação for significativo; confirmar igualdade de imagens, labels e YAML, incluindo erros e casos de borda.
3. Se surgir uma ferramenta **desktop** de inferência contínua, comparar Python/Ultralytics versus Rust/`ultralytics-inference` com o **mesmo modelo exportado**, mesmos parâmetros e o mesmo hardware, contabilizando pré-processamento, inferência, NMS, memória e FPS de ponta a ponta. ONNX e TFLite não são diretamente o mesmo formato; validar equivalência de saída primeiro.
4. Decidir por dados medidos; manter Python quando o benefício não justificar complexidade e manutenção.

## Limites

- O aplicativo do Galaxy Tab S9 executa Kotlin + TFLite nativo, não Python; trocar scripts Python por Rust **não melhora automaticamente** a detecção no tablet.
- Esta proposta não substitui nem reimplementa o SDK DJI e não corrige a incompatibilidade de suas bibliotecas ELF com páginas de 16 KB.
- Prioridade do projeto permanece estabilizar o vídeo do Mavic Pro e validar a detecção de pessoas no Android.
