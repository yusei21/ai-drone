# Plano ativo de IA: Rust e precisão em múltiplos ângulos

**Status:** núcleo Rust experimental disponível em `rust/ai-drone-vision` (decodificação de caixas de pessoas e NMS, com testes). Ainda não integrado ao APK Android; preservar Kotlin/TFLite até validar JNI e desempenho.

## Prioridades

1. Receber e comparar o código-fonte da versão atualizada do aplicativo, preservando integração DJI e correções do detector.
2. Melhorar precisão e recall da classe pessoa para visão aérea, câmera na altura do rosto e visão de baixo para cima, inclusive várias pessoas lado a lado.
3. Desenvolver infraestrutura modular de IA em Rust para processamento de imagens, decodificação de caixas, NMS e medições, integrada ao Android via JNI. UI, câmera e DJI SDK continuam Kotlin.
4. Medir velocidade e qualidade antes de substituir componentes Kotlin/TFLite. Rust não acelera automaticamente os kernels da GPU nem corrige bibliotecas DJI proprietárias.

## Testes de precisão

- Montar conjuntos anotados independentes por ângulo: aéreo, frontal e baixo para cima, incluindo distâncias, oclusão, iluminação e pessoas lado a lado. Evitar vazamento entre treino/validação/teste.
- Medir precision, recall, mAP50, mAP50-95, falsos positivos e falsos negativos **por perspectiva**.
- Complementar VisDrone com dados terrestres equilibrados sem sacrificar desempenho aéreo; comparar novo treinamento com pesos atuais.
- Confirmar formato de entrada, normalização xywh, resize/letterbox, NMS, rotação e alinhamento das caixas ao vídeo DJI.

## Arquitetura Rust proposta

Vídeo DJI -> decodificação Android/Kotlin -> quadro RGB -> JNI -> pré-processamento Rust -> runtime acelerado compatível com Android -> pós-processamento/NMS Rust -> overlay Kotlin.

Não assumir que a biblioteca Rust para ONNX em desktop roda no Android com a mesma aceleração TFLite. Prototipar ABI JNI, testar equivalência de saídas e medir latência p50/p95, FPS, RAM e bateria antes de uma migração gradual.

## Pendências relacionadas

- A tela isolada de teste offline foi removida a pedido; ela era a única interface de importação manual de .tflite. Criar fluxo de importação de modelo na tela DJI quando integrarmos a nova versão do aplicativo. Modelos instalados previamente permanecem no armazenamento do app se seus dados forem preservados.
- Ajustar carregamento e inferência do delegado TFLite/GPU na mesma thread quando necessário.
- Validar vídeo DJI e detector com drone parado e sem comandos automáticos.
- Analisar bibliotecas ELF na nova versão recebida; Rust não corrige a compatibilidade do SDK DJI por si só.

## Fora do escopo atual

- Veículos e objetos gerais: documentacao/ideias futuras/deteccao-veiculos-e-objetos.md.
- Reescrever treinamento PyTorch/Ultralytics em Rust sem benefício mensurado.
- Autonomia de voo ou identificação de indivíduos.
