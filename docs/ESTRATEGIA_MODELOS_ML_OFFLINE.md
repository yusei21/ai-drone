# Estrategia de Modelos de Machine Learning Offline (com sincronizacao via servidor)

## Objetivo
Permitir que o app:
- liste modelos de ML disponiveis no servidor,
- baixe modelos para uso offline no dispositivo,
- deixe o usuario escolher qual modelo esta ativo,
- rode inferencia localmente sem internet,
- e use fallback opcional de inferencia no servidor quando necessario.

## Resumo da ideia
A proposta e boa para operacao com drone porque:
- reduz dependencia de rede no campo,
- permite atualizar modelos sem publicar nova versao do app,
- habilita controle de versao e rollback de modelo,
- centraliza treinamento no servidor e inferencia no edge (celular).

## Arquitetura recomendada

## 1) Servidor (Model Registry)
Manter um catalogo de modelos com metadados:
- `model_id`
- `name`
- `version`
- `task` (ex.: person_detection, tracking, classification)
- `framework` (ex.: tflite)
- `input_shape`
- `labels_version`
- `size_bytes`
- `sha256`
- `min_app_version`
- `min_android_sdk`
- `download_url`
- `is_recommended`
- `release_notes`
- `created_at`

Endpoints sugeridos:
- `GET /ml/models` -> lista modelos compativeis
- `GET /ml/models/{id}` -> detalhes
- `GET /ml/models/{id}/download` -> arquivo binario
- `POST /ml/models/{id}/activate` (opcional, auditoria no backend)
- `POST /ml/infer` (fallback cloud opcional)

## 2) App Android
Componentes recomendados:
- `ModelRepository` (catalogo + download + ativacao)
- `ModelStorageManager` (persistencia de arquivos locais)
- `ModelIntegrityVerifier` (checksum/assinatura)
- `ModelSelectionViewModel` (estado de UI de modelos)
- `InferenceEngine` (carrega modelo ativo e executa inferencia)
- `ModelPreferences` (modelo ativo, politica de update)

## 3) Fluxo de uso
1. Usuario abre tela de "Modelos ML".
2. App busca lista no servidor.
3. Usuario escolhe um modelo e baixa.
4. App valida integridade (sha256).
5. App registra modelo como instalado.
6. Usuario define modelo ativo.
7. Tela de camera usa modelo ativo para inferencia offline.
8. Se offline falhar ou nao houver modelo local, opcionalmente usa fallback cloud.

## Tela no app (UX sugerida)
Uma tela "Modelos de IA" com:
- lista de modelos disponiveis (nome, versao, tamanho, task),
- status por item: `Nao baixado`, `Baixando`, `Instalado`, `Ativo`,
- botao `Baixar`, `Ativar`, `Remover`,
- indicador de compatibilidade do dispositivo,
- politica de atualizacao: `Manual` ou `Sugerir atualizacao`.

## Regras de seguranca e confiabilidade
- nunca ativar modelo sem validar `sha256`;
- manter 1 modelo estavel ativo enquanto novo modelo nao valida;
- permitir rollback para versao anterior instalada;
- registrar erro de carga/inferencia para diagnostico;
- bloquear download em rede movel por padrao (opcional);
- impor limite de armazenamento local.

## Estrutura de dados sugerida (local)

Tabela `ml_models`:
- `model_id` TEXT PK
- `name` TEXT
- `version` TEXT
- `task` TEXT
- `file_path` TEXT
- `sha256` TEXT
- `size_bytes` INTEGER
- `installed_at` INTEGER
- `is_active` INTEGER
- `source` TEXT (`server`/`bundled`)

Preferencias:
- `active_model_id`
- `update_policy` (`manual`/`notify`)
- `allow_mobile_download` (bool)

## Estrategia de inferencia

Modo principal:
- inferencia local com modelo ativo (offline-first).

Fallback opcional:
- se nao houver modelo local valido ou erro grave de runtime, enviar frame/recorte para endpoint de inferencia no servidor.

Observacao:
- para drone em campo, prefira inferencia local como caminho padrao por latencia e resiliencia.

## Compatibilidade de modelos
Antes de exibir/permitir download, filtrar por:
- `min_app_version`,
- `min_android_sdk`,
- suporte de aceleracao (CPU/GPU),
- tamanho maximo permitido no dispositivo.

## Plano de implementacao (fases)

## Fase 1 - Base (MVP)
1. Criar endpoints de catalogo e download no backend.
2. Criar tela de modelos no app.
3. Implementar download + checksum + ativacao.
4. Conectar inferencia local ao modelo ativo.

## Fase 2 - Robustez
1. Adicionar rollback e remocao segura.
2. Telemetria de performance (latencia, FPS, erros).
3. Politica de atualizacao e notificacoes de novo modelo.

## Fase 3 - Hibrido
1. Adicionar fallback cloud de inferencia.
2. Controle de custo/limite de requisicoes.
3. Regras de privacidade para upload de frames.

## Riscos e mitigacoes
- Risco: modelo incompatível com aparelho.
  - Mitigacao: metadata de compatibilidade + validacao antes de ativar.
- Risco: arquivo corrompido.
  - Mitigacao: checksum e nao ativar se invalido.
- Risco: regressao de qualidade.
  - Mitigacao: manter modelo anterior instalado para rollback.
- Risco: latencia alta no fallback cloud.
  - Mitigacao: usar cloud apenas como excecao.

## Conclusao
A estrategia e tecnicamente correta e escalavel para seu projeto:
- treinamento no servidor,
- distribuicao controlada de modelos,
- inferencia offline no app,
- atualizacao continua sem depender de release da Play Store.

Para operacao com drone, esse desenho aumenta seguranca operacional e previsibilidade em campo.
