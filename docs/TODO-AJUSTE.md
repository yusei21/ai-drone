Este documento descreve os refinamentos visuais, estruturais e textuais que precisam ser aplicados no aplicativo para alcançar o resultado desejado. Todos os itens abaixo devem ser considerados como ajustes obrigatórios no código existente.

## 1. Tela Inicial

- [x] Ajustes no ícone do aplicativo

* O ícone atualmente está dentro de um quadrado.
* Esse quadrado deve ser removido completamente.
* O ícone deve permanecer sozinho no local, sem moldura.
* Aumente o tamanho do ícone para ocupar aproximadamente 75% da área que hoje é ocupada pelo quadrado.

- [x] Ajustes no card principal

No card que contém:

* Título: **"Plataforma operacional de drones"**
* Descrição: **"Gerencie missões..."**

Remover apenas a lista inferior que aparece abaixo desse conteúdo, incluindo os itens:

* **"Planejamento de rotas e waypoints..."**
* **"Sistema Pronto"**
* **"Neural Command Suite"**

O título e a descrição do card devem permanecer.

- [x] Alinhamento da tela

Centralizar horizontal e verticalmente os seguintes elementos:

* Ícone do aplicativo
* Título do app
* Card com título e descrição

Os seguintes elementos devem permanecer fixados no rodapé da tela:

* Botão **"Entrar"**
* Texto **"Vently Neural - v1.0"**

---

## 2. Tela de Login

- [x] Remoções e substituições

* Remover a opção **"Continuar sem login"**
* Adicionar um botão **"X"** na cor branca no canto superior direito da tela para fechar

- [x] Remoção de textos desnecessários

Remover os textos e descrições institucionais, incluindo conteúdos como:

* **"Acesse o centro de comando com suas..."**
* **"Operações, monitoramento..."**

- [ ] Revisão ortográfica

* Revisar toda a tela de login
* Corrigir acentuação, ortografia e padronização dos textos em português

- [x] Mensagens de erro

As mensagens de erro, como por exemplo:

* **"Senha incorreta"**
* Outras mensagens similares de autenticação

Devem ser reformuladas visualmente para:

* aparecerem como aviso flutuante
* surgirem temporariamente na parte inferior da tela
* desaparecerem automaticamente após alguns segundos
* mas não atrás do botão "Voltar", "Home", "Minimizar"

---

## 3. Tela de Dashboard

- [x] Card de drones compatíveis

No card da seção de drones compatíveis:

* Remover todos os ícones dos cards
* Remover todas as descrições como:

  * **"Air"**
  * **"Portátil"**
  * **"DJI"**
* Remover também a tag **"CATÁLOGO"**

- [x] Botão dos cards de drone

No botão **"ABRIR DRONE"**:

* Alterar o texto para **"IR PARA DJI"**
* Garantir que o texto fique na cor branca
* Reduzir a margem inferior excessiva abaixo do botão
* Ajustar o espaçamento para ficar mais compacto

- [x] Card "Sobre o Projeto"

* Diminuir a margem inferior desse card
* Exemplo: se atualmente estiver em **20**, reduzir para algo próximo de **15**

---

## 4. Tela de Missões

- [x] Área visível com scroll

* A área visível da tela que está com rolagem vertical está cortando mais conteúdo no topo do que deveria
* Ajustar essa área para que fique alinhada visualmente como o **nav bottom**
* O conteúdo deve ocupar melhor a altura disponível da tela

- [ ] Revisão de textos

Adicionar e corrigir corretamente as acentuações em todo o aplicativo, especialmente nas telas:

* Dashboard
* Missões
* Criar Missões
* Vídeos
* Configurações
* Sobre

---

## 5. Tela de Criar Missão / Waypoints

- [ ] Reorganização dos cards

Na tela de criação de missão:

* O card **"Como Adicionar Waypoint"** deve ser movido para baixo do card **"Defina POI e Waypoints"**
* Reformular o texto desse card para deixar claro que o mapa está localizado logo abaixo dele

- [ ] Indicação visual de continuidade

* Aplicar alguma estratégia visual que indique que existe mais conteúdo abaixo da dobra
* Exemplo: corte parcial do conteúdo, espaçamento sugestivo ou outro recurso visual que indique rolagem

- [ ] Waypoints adicionados

No card **"Waypoints Adicionados"**:

* Remover a funcionalidade de selecionar waypoint
* Remover também o card **"Waypoint Selecionado"** por completo

---

## 6. Modais de Missão

- [ ] Modal "Configurar Waypoints"

No modal de configuração:

* Os botões **"Cancelar"** e **"Adicionar"** devem ficar na mesma linha, no rodapé do modal
* O botão **"Cancelar"** deve ficar alinhado à esquerda
* O botão **"Adicionar"** deve ficar alinhado à direita

- [ ] Modal "Adicionar ação"

Aplicar a mesma lógica:

* **"Cancelar"** à esquerda
* **"Adicionar"** à direita
* Ambos na mesma linha, no rodapé do modal

---

## 7. Relatório PDF

O relatório em PDF está quase correto, mas precisa de dois ajustes:

- [ ] Título "Resumo Executivo"

* O título **"Resumo Executivo"** está sobrepondo os campos:

  * **"DRONE"**
  * **"DURAÇÃO"**
  * **"COBERTURA"**
* Ajustar o espaçamento e posicionamento para eliminar essa sobreposição

- [ ] Cabeçalho

* O texto **"Missão demo-report-00"** no cabeçalho está muito colado na lateral
* Adicionar espaçamento lateral adequado para melhorar o alinhamento visual

---

## Instrução final para implementação

Faça todas essas alterações diretamente no código existente, preservando a estrutura atual do aplicativo sempre que possível. O foco é refinar interface, organização visual, textos em português e espaçamentos, sem alterar desnecessariamente a lógica do sistema.
