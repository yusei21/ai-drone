# Produto e UX — ai-drone v1.1.0

Atualizado em 12/11/2026. Estado: implementacao no codigo, com validacao visual e instrumental em dispositivo **pendentes para os commits recentes**.

## Principio

Inspiracao em interfaces como Notion e Vercel: hierarquia clara, tipografia legivel, superficies neutras, espacamento regular, poucas acoes concorrentes e feedback de estado verdadeiro. O app nao deve se limitar a uma troca de cores.

## Jornada primaria

1. Entrar ou continuar pela sessao disponivel.
2. Dashboard / Visao geral: ver **estado real da conexao DJI**.
3. Acessar **Abrir camera DJI** (acao primaria, independente do status; quando desconectado, o video depende de reconexao).
4. Na camera, visualizar o feed, consultar status da IA e, quando necessario, abrir o painel de deteccao.
5. Ativar a deteccao de pessoas com o modelo integrado `pessoas_ativo.tflite`.
6. Usar Missoes e Relatorios por atalhos do dashboard ou pela navegacao inferior.

## Telas e o que mudou

| Area | Direcao aplicada / estado |
| --- | --- |
| Dashboard | Conteudo principal virou conexao + CTA de camera; atalhos de missoes/relatorios; bloco explicativo do YOLO26n; removeu catalogo institucional como foco da home |
| Navegacao inferior | 4 entradas: Inicio, Ao vivo, Missoes e Relatorios; sem brilho ornamental |
| Camera DJI | HUD monocromatica, status da IA, controles laterais, minimapa recolhido quando controles nao estao expandidos |
| IA | Modelo padrao por asset `ml_models/pessoas_ativo.tflite`; configuracao e ativacao na camera |
| Configuracoes | Fundo neutro e identificacao da versao do app e do modelo |
| Bem-vindo/Login | Marca ai-drone e menos efeitos de fundo |
| Missoes/Relatorios | Tokens neutros, cabecalhos simplificados; relatorios nao devem gerar dados ficticios quando vazios |
| Telas secundarias | Ainda precisam de revisao detalhada de fluxos, estados vazios, responsividade e acessibilidade |

### Identidade de versao

- Android `app/build.gradle.kts`: `versionName = "1.1.0"`, `versionCode = 2`.
- Modelo de IA independente da versao do aplicativo: `YOLO26n Pessoas 640`.
- Arquivo exportado: `runs/android_models/pessoas_640.tflite`.
- Asset Android padrao: `app/src/main/assets/ml_models/pessoas_ativo.tflite` (mesmos pesos copiados pelo script).

## Regras de usabilidade

- Primeiro mostrar a proxima acao util; depois informacoes secundarias.
- Nao dizer que IA ou DJI esta pronto sem consultar estado real.
- Nao simular missao, relatorio, deteccao ou benchmark como dado real.
- Desabilitar acoes impossiveis com explicacao visivel, nunca falhar silenciosamente.
- Em voo, evitar sobrepor controles em areas criticas do feed.
- Conservar funcionalidade DJI SDK, video e inferencia durante mudancas de layout.
- Exibir texto de tamanho suficiente e areas de toque adequadas para tablet em campo.

## Pontos de verificacao da v1.1.0

- [ ] Compilacao do HEAD apos o ultimo redesign (`./gradlew :app:assembleDebug`).
- [ ] Verificacao manual de dashboard em tablet vertical/horizontal.
- [ ] Abrir camera com e sem DJI conectado.
- [ ] Ativar IA, observar caixas com pessoas reais e verificar logs.
- [ ] Testar navegacao de ida/volta entre missoes, relatorios e dashboard.
- [ ] Garantir que relatorios vazios mostram estado vazio, sem insercao de dados falsos.
- [ ] Testar contraste, tamanho de toque, acessibilidade e rolagem.
- [ ] Medir latencia de inferencia no dispositivo (ver [benchmarks](BENCHMARKS_YOLO26_PESSOAS_640.md)).

## Observacoes operacionais

A IA e executada no tablet Android, sobre quadros do video DJI, e nao dentro do Mavic Pro. A conexao ao drone ja funcionou anteriormente segundo relato do operador. Isso nao equivale a teste end-to-end comprovado do novo modelo de 640 px.

Os modelos `.tflite` estao ignorados pelo Git; cada build local precisa manter o asset gerado pelo script antes do assemble. `git pull` sozinho nao baixa os pesos.
