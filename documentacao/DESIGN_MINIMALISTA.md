# ai-drone: identidade visual minimalista

**Direcao:** aplicativo proprio, monocromatico, alto contraste e poucas acoes visiveis ao mesmo tempo. Sem testes offline na interface.

## Tokens Compose
- Background #090909, surface #151515, elevated #202020.
- Texto primario #FFFFFF, secundario #B4B4B4, auxiliar #777777.
- Bordas #303030; botao primario branco com texto preto.
- Alvos de toque de no minimo 48 dp; espacamento 8/16/24 dp.
- Status sempre com texto + icone. Caixas de pessoas com contorno contrastante em cenas claras e escuras.

## Telas
1. **Entrada:** logo ai-drone, login e acesso local quando suportado. Nao mascarar falha no DJI SDK.
2. **Dashboard:** uma faixa Drone / Video / IA / Bateria; acao principal Ao vivo; missoes e relatorios secundarias.
3. **Ao vivo (paisagem):** video ocupa quase todo espaco. Topo: conexao e bateria reais. Overlay discreto: pessoas por quadro, IA ativa, latencia. Rodape: IA, captura, voltar. Opcoes avancadas em painel recolhivel. Importacao de modelo TFLite diretamente no painel DJI (pendente).
4. **Relatorios:** contagens por quadro, horario, modelo, confianca. Nao confundir deteccoes por quadro com individuos unicos.
5. **Configuracoes:** Drone, IA, Camera, Aparencia, Diagnostico; versoes do app, DJI SDK e modelo.

## Regras
- Nunca distorcer o video para preencher a tela: adaptar caixas ao resize/letterbox e rotacao.
- Sem controles automaticos do voo pela IA.
- Legibilidade em sol e uso no Galaxy Tab S9 horizontal.
- Preservar funcionalidades DJI atuais ate chegar o codigo atualizado do app original.

**Estado:** especificacao visual; as telas ainda nao foram redesenhadas.
