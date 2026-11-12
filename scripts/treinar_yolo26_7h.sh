#!/usr/bin/env bash
# Treinamento YOLO26n por aproximadamente 7 horas na RTX 4060 Ti.
# Executar com: bash scripts/treinar_yolo26_7h.sh
# Nao inicia treinamento remotamente: deve ser executado no PC com GPU.
set -Eeuo pipefail

ROOT="/home/yusei/Documents/workspace/ai-drone"
DATA="/home/yusei/Documents/workspace/datasets/pessoas_multiangulo/pessoas_multiangulo.yaml"
PROJECT="$ROOT/runs/ai_drone"
RUN="pessoas_7h_20261112"
YOLO="$ROOT/.venv/bin/yolo"

if [[ ! -x "$YOLO" ]]; then
    echo "ERRO: CLI yolo ausente em $YOLO. Use o mesmo ambiente que executou a epoca-teste." >&2
    exit 1
fi
if [[ ! -s "$DATA" ]]; then
    echo "ERRO: YAML de datasets nao encontrado: $DATA" >&2
    exit 1
fi
if [[ -d "$PROJECT/$RUN" ]]; then
    echo "ERRO: saida $PROJECT/$RUN ja existe. Nao sobrescrevendo treinamento anterior." >&2
    exit 1
fi
mkdir -p "$PROJECT"
cd "$ROOT"
echo "Iniciando treino com limite time=7 horas; confira espaco, suspensao e refrigeracao."
echo "YAML: $DATA"
echo "Resultados: $PROJECT/$RUN"
echo "O limite de tempo e aproximado; termino de epoca e validacao podem ultrapassa-lo."

"$YOLO" detect train \
    model=yolo26n.pt \
    data="$DATA" \
    time=7 \
    epochs=100 \
    imgsz=640 \
    batch=8 \
    device=0 \
    workers=4 \
    amp=True \
    cache=False \
    optimizer=AdamW \
    lr0=0.002 \
    cos_lr=True \
    patience=20 \
    close_mosaic=10 \
    max_det=888 \
    save=True \
    save_period=5 \
    plots=True \
    project="$PROJECT" \
    name="$RUN" \
    exist_ok=False \
    2>&1 | tee "$PROJECT/${RUN}.log"

echo "Treinamento encerrado. Veja $PROJECT/$RUN/weights/best.pt"
