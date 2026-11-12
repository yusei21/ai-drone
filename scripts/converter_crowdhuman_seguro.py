#!/usr/bin/env python3
"""Converte o subconjunto conservador do CrowdHuman para YOLO (uma classe: pessoa).

Somente imagens sem regioes tag=mask/ignore sao elegiveis. Regioes ignoradas
nao podem ser silenciosamente consideradas fundo num detector YOLO comum.

Lê os ZIPs originais sem descompactá-los todos; usa Pillow para dimensoes.
Caixas fbox (corpo completo) sao usadas por padrao, com clipping e exclusao
de caixas degeneradas. Use --box vbox para treinar por corpo visivel.
Use somente para pesquisa/educacao nao comerciais conforme os termos do dataset.
"""
from __future__ import annotations
import argparse
from collections import Counter
from io import BytesIO
import json
import math
from pathlib import Path, PurePosixPath
import shutil
import sys
import zipfile

try:
    from PIL import Image
except ImportError:
    raise SystemExit("Pillow ausente. Use o ambiente Python do YOLO, com Pillow instalado.")

def valid_box(box):
    return (isinstance(box, list) and len(box) == 4 and
            all(type(x) in (int, float) and math.isfinite(x) for x in box) and
            box[2] > 0 and box[3] > 0)

def labels_for_record(record, width, height, box_key, stats):
    labels = []
    for obj in record["gtboxes"]:
        if obj["tag"] != "person" or (obj.get("extra") or {}).get("ignore", 0) == 1:
            continue
        box = obj.get(box_key)
        if not valid_box(box):
            stats["invalid_box"] += 1
            continue
        x, y, w, h = box
        x1, y1 = max(0., x), max(0., y)
        x2, y2 = min(float(width), x+w), min(float(height), y+h)
        if x2 <= x1 or y2 <= y1:
            stats["outside_image"] += 1
            continue
        cx, cy = (x1+x2)/2/width, (y1+y2)/2/height
        bw, bh = (x2-x1)/width, (y2-y1)/height
        if not (0 <= cx <= 1 and 0 <= cy <= 1 and 0 < bw <= 1 and 0 < bh <= 1):
            stats["invalid_normalized"] += 1
            continue
        labels.append(f"0 {cx:.8f} {cy:.8f} {bw:.8f} {bh:.8f}")
    return labels

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--annotations", type=Path, required=True)
    ap.add_argument("--zip", type=Path, required=True, nargs="+")
    ap.add_argument("--output", type=Path, required=True)
    ap.add_argument("--split", choices=["train", "val"], required=True)
    ap.add_argument("--box", choices=["fbox", "vbox"], default="fbox")
    ap.add_argument("--max-images", type=int, default=0,
                    help="Para inspecao; 0 = converter todas as elegiveis")
    args = ap.parse_args()
    if not args.annotations.is_file():
        ap.error("ODGT nao encontrado")
    if any(not p.is_file() for p in args.zip):
        ap.error("ZIP nao encontrado")
    if args.max_images < 0:
        ap.error("--max-images deve ser >= 0")

    # Filtra integralmente antes de extrair, descartando cenas com qualquer
    # regiao ignore (nao ha semantica de ignore no YOLO bbox convencional).
    eligible = {}
    stats = Counter()
    with args.annotations.open(encoding="utf-8") as f:
        for lineno, raw in enumerate(f, 1):
            rec = json.loads(raw)
            image_id, boxes = rec.get("ID"), rec.get("gtboxes")
            if not isinstance(image_id, str) or not isinstance(boxes, list):
                raise SystemExit(f"ODGT invalido na linha {lineno}")
            if image_id in eligible:
                raise SystemExit(f"ID duplicado no ODGT: {image_id}")
            stats["all_images"] += 1
            if any((not isinstance(b, dict) or
                    b.get("tag") == "mask" or
                    (b.get("extra") or {}).get("ignore", 0) == 1 or
                    b.get("tag") != "person" or
                    not valid_box(b.get(args.box)))
                   for b in boxes):
                stats["rejected_image"] += 1
                continue
            if not boxes:
                stats["empty_image"] += 1
                continue
            eligible[image_id] = rec
    if args.max_images:
        eligible = dict(list(eligible.items())[:args.max_images])
    images_dir = args.output / "images" / args.split
    labels_dir = args.output / "labels" / args.split
    # Nunca sobrescrever datasets preexistentes.
    if images_dir.exists() or labels_dir.exists():
        raise SystemExit("Saida ja existe: escolha uma pasta nova ou remova somente a saida criada por este conversor.")
    images_dir.mkdir(parents=True)
    labels_dir.mkdir(parents=True)

    seen = set()
    with_contained_labels = 0
    try:
        for zippath in args.zip:
            print(f"Lendo {zippath.name}...", flush=True)
            with zipfile.ZipFile(zippath) as zf:
                for entry in zf.infolist():
                    if entry.is_dir():
                        continue
                    path = PurePosixPath(entry.filename)
                    if path.is_absolute() or ".." in path.parts or "\\" in entry.filename:
                        raise SystemExit(f"Caminho inseguro no ZIP: {entry.filename}")
                    if path.suffix.lower() not in (".jpg", ".jpeg", ".png"):
                        continue
                    image_id = path.stem
                    if image_id not in eligible:
                        continue
                    if image_id in seen:
                        raise SystemExit(f"Imagem repetida: {image_id}")
                    with zf.open(entry) as inf:
                        content = inf.read()
                    with Image.open(BytesIO(content)) as im:
                        width, height = im.size
                        im.verify()
                    if width <= 0 or height <= 0:
                        raise SystemExit(f"Dimensoes invalidas: {image_id}")
                    labels = labels_for_record(eligible[image_id], width, height, args.box, stats)
                    if not labels:
                        stats["images_without_valid_labels"] += 1
                        continue
                    imgfile = images_dir / (image_id + path.suffix.lower())
                    with imgfile.open("wb") as outfile:
                        outfile.write(content)
                    (labels_dir / (image_id + ".txt")).write_text(
                        "\n".join(labels) + "\n", encoding="utf-8"
                    )
                    seen.add(image_id)
                    with_contained_labels += len(labels)
                    if len(seen) % 500 == 0:
                        print(f"  Convertidas {len(seen)}/{len(eligible)} imagens...", flush=True)
        missing = set(eligible) - seen
        if missing:
            # Some images may have no remaining in-frame labels. Distinguish those.
            unresolved = len(missing) - stats["images_without_valid_labels"]
            if unresolved:
                raise SystemExit(f"ERRO: {unresolved} IDs elegiveis ausentes dos ZIPs.")
        print(f"Concluido: imagens={len(seen)} rotulos={with_contained_labels}")
        print(f"Descartadas por regioes ignore / anotacoes ruins: {stats['rejected_image']}")
        print(f"Sem rotulos apos clipping: {stats['images_without_valid_labels']}")
        print(f"Caixas fora da imagem: {stats['outside_image']}")
        print(f"Saida: {args.output}")
        print("ATENCAO: verificar visualmente labels e licenca antes de treinar.")
    except Exception:
        print("Falha: arquivos de saida parciais podem ter sido gerados.", file=sys.stderr)
        raise

if __name__ == "__main__":
    main()
