# Ideia futura: coordenadas de pessoas detectadas

**Status:** Planejado para uma fase futura. **Não implementar agora.**

## Objetivo
Quando a IA detectar uma pessoa no vídeo do drone, estimar a posição geográfica dessa pessoa e permitir que o operador visualize ou compartilhe um ponto no mapa.

## Fluxo proposto
1. O drone envia vídeo e telemetria para o aplicativo Android no tablet.
2. O detector identifica pessoas e fornece caixas delimitadoras com pontuação de confiança.
3. O sistema associa as detecções à telemetria no instante da imagem.
4. Um módulo de geolocalização estima a interseção do raio da câmera com o terreno e calcula uma latitude/longitude aproximada.
5. A interface exibe a coordenada **estimada**, a margem de erro, o horário, uma miniatura e um botão para compartilhar ou salvar o registro.

## Informações necessárias
- Latitude e longitude do drone no momento da captura;
- Altitude, preferencialmente em relação ao terreno, e referência altimétrica;
- Orientação da aeronave e do gimbal (yaw, pitch e roll);
- Calibração e campo de visão da câmera, resolução e eventual zoom;
- Coordenadas da detecção na imagem;
- Modelo de elevação do terreno quando necessário;
- Sincronização temporal entre os frames e a telemetria.

## Limitações e cuidados
- **A coordenada do drone não é a coordenada da pessoa.**
- O cálculo é uma estimativa; a margem de erro pode aumentar com altitude, ângulo oblíquo, terreno irregular, zoom e GPS impreciso.
- Em vegetação ou diante de obstáculos, a projeção no solo pode ser incorreta.
- Confiança da detecção não equivale à precisão da coordenada GPS.
- Não apresentar localização como exata sem validação; mostrar estimativa e, se possível, raio de incerteza.
- Não enviar localização a terceiros automaticamente: exigir ação ou autorização explícita do operador e respeitar privacidade e legislação aplicável.

## Dependências e etapas futuras
- Confirmar modelo de drone DJI e suporte à telemetria/gimbal no SDK.
- Concluir primeiro a detecção de pessoas no vídeo do tablet e validar o rastreamento visual.
- Implementar módulo de projeção câmera-terreno e armazenamento de eventos.
- Testar com alvos em posições conhecidas, alturas e terrenos diferentes; medir erro em metros.
- Adicionar marcador em mapa e compartilhamento **opcional**, com consentimento do operador.

## Fora do escopo atual
Nesta fase, focar em detecção de pessoas e, se o equipamento permitir, acompanhamento visual. A estimativa e o envio de coordenadas ficam para depois.
