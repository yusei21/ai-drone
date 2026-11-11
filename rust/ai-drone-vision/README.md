# ai-drone-vision (Rust)

Núcleo experimental e **sem dependências externas** para pós-processamento de pessoas do YOLO26. Não recebe vídeo DJI, não executa inferência e não altera a aeronave. A implementação do app Android continua usando Kotlin/LiteRT enquanto esta etapa é validada.

## Referências de arquitetura

- Hugging Face Candle: https://github.com/huggingface/candle
- Hugging Face Tokenizers (núcleo Rust reutilizável): https://huggingface.co/docs/tokenizers/
- Ultralytics Inference Rust/ONNX: https://docs.ultralytics.com/inference
- LiteRT GPU Android (mesma thread para criar e executar delegate): https://developers.google.com/edge/litert/android/gpu

## Testes

No CachyOS, com Rust instalado:

```sh
cargo test --manifest-path rust/ai-drone-vision/Cargo.toml
cargo test --release --manifest-path rust/ai-drone-vision/Cargo.toml
```

O formato esperado é um vetor de cinco linhas contíguas de `f32`, referente a `[1,5,N]`, com coordenadas em centro X, centro Y, largura, altura e confiança. `CoordinateFormat::Normalized` reproduz a escala observada no arquivo `pessoas_640.tflite` do projeto; `CoordinateFormat::Pixels` cobre outros exports. A escala é selecionada explicitamente pelo chamador, nunca inferida pelas previsões de um único quadro.

O módulo aplica limiar de confiança, filtra caixas degeneradas, normaliza, limita à imagem e usa IoU/NMS para manter pessoas distintas. Não altera nem melhora a precisão do modelo treinado: a qualidade real depende do dataset e da inferência.

## Próximos passos — ainda não implementados

1. Confirmar saída equivalente ao decodificador Kotlin em um conjunto anotado com perspectivas aérea, frontal e baixa.
2. Avaliar FFI/JNI e integração como biblioteca `.so` compilada para `aarch64-linux-android`, sem remover o fallback atual.
3. Validar build e alinhamento ELF de 16 KB para qualquer biblioteca Rust distribuída.
4. Medir tempo total de cópia JNI + decodificação/NMS, p50/p95, memória e FPS; manter Rust somente se houver benefício.
5. Separadamente, melhorar o treinamento com imagens variadas. Rust não aumenta por si só precisão ou recall.

**Importante:** não confundir o Rust de pós-processamento com uma implementação completa de YOLO em Rust. Ultralytics Inference Rust usa ONNX Runtime, enquanto o aplicativo Android atual usa TFLite; a migração de runtime exigiria validação própria.
