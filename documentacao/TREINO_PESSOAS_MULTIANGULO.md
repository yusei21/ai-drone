# ai-drone: treinamento YOLO26 de pessoas em multiplos angulos

## Entendendo o erro

O arquivo de exemplo pessoas_multiangulo.yaml ainda nao existia; colar YAML em fish faz o terminal interpretar palavras como comandos. O treinamento falhou antes de comecar, nao e erro de GPU.

## Fontes necessarias

- aereo: VisDrone-pessoas existente e validado (vista superior e obliqua).
- olhos: imagens na altura do rosto, por exemplo COCO filtrado para person, convertido para YOLO classe 0.
- baixo: imagens coletadas com permissao e cameras apontadas de baixo para cima. COCO e CrowdHuman nao garantem boa cobertura desse angulo.
- CrowdHuman pode complementar pessoas lado a lado e parcialmente ocluidas, apos conversao e revisao.

Cada fonte deve conter images/train, labels/train, images/val e labels/val. Anotacoes YOLO: 0 centro_x centro_y largura altura, coordenadas entre 0 e 1. Imagens sem pessoas devem ter .txt vazio. Nao misturar quadros do mesmo video entre treino e validacao. Reservar teste por cena e sujeito, separado do treino.

## Conferir dados no CachyOS

    cd /home/yusei/Documents/workspace/ai-drone
    source .venv/bin/activate.fish
    ls -la /home/yusei/Documents/workspace/datasets

O script novo nao faz download nem converte imagens, apenas verifica as tres fontes, os rotulos e gera YAML. Substitua os caminhos pelos locais reais antes de executar:

    python scripts/gerar_yaml_multiangulo.py \
      --source aereo=/home/yusei/Documents/workspace/datasets/VisDrone-pessoas \
      --source olhos=/home/yusei/Documents/workspace/datasets/Pessoas-Olhos-YOLO \
      --source baixo=/home/yusei/Documents/workspace/datasets/Pessoas-Angulo-Baixo-YOLO \
      --output /home/yusei/Documents/workspace/datasets/pessoas_multiangulo/pessoas_multiangulo.yaml

Se uma fonte ainda nao existir, o script explica o problema e nao cria YAML falso.

## Treinar (apenas depois do YAML validado)

    yolo detect train \
      model=yolo26n.pt \
      data=/home/yusei/Documents/workspace/datasets/pessoas_multiangulo/pessoas_multiangulo.yaml \
      epochs=100 imgsz=640 batch=8 device=0 workers=4 \
      project=runs/ai_drone name=pessoas_multiangulos

Comparar com fine-tuning partindo do best.pt atual, sem sobrescrever pesos anteriores. Reduzir batch para 4 se RTX 4060 Ti 8 GB ficar sem memoria.

## Medir qualidade

Avaliar precision, recall, mAP50 e mAP50-95 **separadamente** para aereo, olhos e baixo. Contar falsos negativos, pessoas pequenas, grupos e oclusoes; guardar teste independente por video/cena. Comparar 640 com 960 e medir no Tab S9 latencia, FPS e estabilidade temporal.

## Exportar para Android

O script do repositorio valida os tensores LiteRT/TFLite:

    python scripts/exportar_modelo_android.py \
      --pesos runs/detect/runs/ai_drone/pessoas_multiangulos/weights/best.pt \
      --imgsz 640

A importacao na tela DJI ainda precisa ser implementada: o antigo fluxo na tela de teste offline foi removido. Nao presumir que exportar no PC instala o modelo no tablet.

## Checklist
- [ ] VisDrone-pessoas validado.
- [ ] Dados ao nivel dos olhos com anotacoes YOLO classe 0.
- [ ] Dados de baixo para cima com anotacoes YOLO classe 0.
- [ ] Separacao de cenas entre train/val/test.
- [ ] YAML gerado e inspecionado.
- [ ] Validacao por angulo antes de substituir modelo em uso.
