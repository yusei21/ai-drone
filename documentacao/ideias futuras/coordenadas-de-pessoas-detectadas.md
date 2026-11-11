# Ideias futuras — ai-drone

Este diretório documenta funcionalidades propostas, **não implementadas**.

## Coordenadas de pessoas detectadas

Status: **adiado**.

Objetivo: estimar a latitude e longitude de uma pessoa detectada pela câmera do drone DJI Mavic Pro, registrar horário, evidência visual e margem de erro, e permitir que o operador veja e compartilhe a estimativa.

### Informações necessárias
- GPS e altitude do drone sincronizados com o frame;
- atitude (yaw/pitch/roll) do drone e orientação do gimbal;
- parâmetros da câmera, campo de visão e zoom;
- bounding box da pessoa e elevação do terreno;
- algoritmo de projeção da câmera no terreno.

### Restrições
- GPS do drone não é GPS da pessoa.
- Confiança da IA não representa precisão geográfica.
- A posição é **estimada**, podendo ser muito imprecisa quando o terreno é irregular, a imagem é oblíqua ou há vegetação.
- Não enviar localização automaticamente; exigir autorização do operador e observar privacidade.

### Etapas futuras
1. Concluir detecção de pessoas e rastreamento visual no tablet Samsung.
2. Validar telemetria e ângulos do gimbal do Mavic Pro com o DJI Mobile SDK V4.
3. Implementar geolocalização aproximada e estimativa de erro.
4. Testar com alvos em locais conhecidos e mostrar margem de incerteza.
5. Adicionar marcadores e compartilhamento opcional.

## Prioridades atuais
1. Compatibilidade Android + DJI Mavic Pro (Mobile SDK V4).
2. Streaming de vídeo para o tablet.
3. Detecção de pessoas com modelo local TFLite.
4. Rastreamento visual de pessoas com confirmação pelo operador.
5. Estudar acompanhamento do drone somente com verificações de compatibilidade, segurança e comando manual de interrupção.
