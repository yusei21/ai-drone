# Documentação Técnica

Esta pasta centraliza os documentos de análise, estratégia e planos de evolução do projeto.

## Índice
- [Benchmarks medidos — YOLO26n Pessoas 640](BENCHMARKS_YOLO26_PESSOAS_640.md)
- [Fluxos e UX — ai-drone v1.1.0](UX_V1_1.md)
- [Análise do projeto em campo](ANALISE_DRONE_3_DIAS.md)
- [Estratégia histórica de modelos ML offline e sincronização](ESTRATEGIA_MODELOS_ML_OFFLINE.md)
- [Plano de detecção de objetos no vídeo do drone](PLANO_DETECCAO_OBJETOS_VIDEO_DRONE.md)
- [Plano auth offline first](PLANO_TODO_AUTH_OFFLINE_FIRST.md)
- [Contrato API auth + missões](SPEC_CONTRATO_AUTH_MISSOES.md)
- [Galeria por missão](TODO_GALERIA_MISSAO_EXECUCAO.md)
- [Implementação da camada climática](TODO_IMPLEMENTACAO_CLIMA.md)
- [Rework de settings e mídia](TODO_SETTINGS_E_MIDIA_REWORK.md)

## Convenção
- `README.md` na raiz permanece como entrada principal do repositório no GitHub.
- Documentos técnicos e planos incrementais devem ser adicionados em `docs/`.

## Regra para benchmarks
Registrar separadamente métricas de validação no PC, exportação de modelo, builds confirmados e testes instrumentados no Android. Não inferir FPS no tablet a partir do tempo de validação informado pelo Ultralytics.
