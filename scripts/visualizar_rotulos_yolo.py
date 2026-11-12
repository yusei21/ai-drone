#!/usr/bin/env python3
"""Cria uma folha de contato com caixas YOLO para revisão visual do CrowdHuman.

Não altera as imagens originais. Requer apenas Pillow.
Usa arquivos de images/<split> e labels/<split>, com classe 0 = pessoa.
"""
from __future__ import annotations

import argparse
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageOps

EXTS = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--split", choices=("train", "val"), default="train")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--limit", type=int, default=20)
    parser.add_argument("--columns", type=int, default=4)
    parser.add_argument("--tile-width", type=int, default=480)
    parser.add_argument("--tile-height", type=int, default=350)
    args = parser.parse_args()
    if args.limit < 1 or args.columns < 1 or args.tile_width < 100 or args.tile_height < 100:
        parser.error("limit, columns e dimensões precisam ser positivos")
    images_dir = args.dataset / "images" / args.split
    labels_dir = args.dataset / "labels" / args.split
    if not images_dir.is_dir() or not labels_dir.is_dir():
        parser.error("Diretórios images/<split> ou labels/<split> inexistentes")
    files = sorted(p for p in images_dir.iterdir() if p.is_file() and p.suffix.lower() in EXTS)[: args.limit]
    if not files:
        parser.error("Nenhuma imagem encontrada")
    rows = math.ceil(len(files) / args.columns)
    width, height = args.columns * args.tile_width, rows * args.tile_height
    sheet = Image.new("RGB", (width, height), "white")
    for index, filepath in enumerate(files):
        label_file = labels_dir / (filepath.stem + ".txt")
        if not label_file.is_file():
            raise SystemExit(f"Rótulo ausente: {label_file}")
        with Image.open(filepath) as image:
            photo = image.convert("RGB")
        draw = ImageDraw.Draw(photo)
        W, H = photo.size
        lines = label_file.read_text(encoding="utf-8").splitlines()
        count = 0
        for number, line in enumerate(lines, 1):
            parts = line.split()
            if len(parts) != 5 or parts[0] != "0":
                raise SystemExit(f"Rótulo inválido em {label_file}:{number}")
            cx, cy, bw, bh = map(float, parts[1:])
            if not all(math.isfinite(v) for v in (cx, cy, bw, bh)):
                raise SystemExit(f"Números inválidos em {label_file}:{number}")
            x1 = (cx - bw / 2) * W
            y1 = (cy - bh / 2) * H
            x2 = (cx + bw / 2) * W
            y2 = (cy + bh / 2) * H
            if x2 <= x1 or y2 <= y1:
                raise SystemExit(f"Caixa degenerada em {label_file}:{number}")
            draw.rectangle((x1, y1, x2, y2), outline="#00ff60", width=max(2, W // 400))
            count += 1
        target_w, target_h = args.tile_width, args.tile_height - 32
        fitted = ImageOps.contain(photo, (target_w, target_h))
        tile = Image.new("RGB", (target_w, target_h), "#202020")
        tile.paste(fitted, ((target_w - fitted.width) // 2, (target_h - fitted.height) // 2))
        caption = ImageDraw.Draw(tile)
        caption.text((8, target_h - 4), "", fill="white")
        x = (index % args.columns) * args.tile_width
        y = (index // args.columns) * args.tile_height
        sheet.paste(tile, (x, y))
        title = ImageDraw.Draw(sheet)
        title.text((x + 8, y + args.tile_height - 27), f"{filepath.stem[:36]}  |  pessoas: {count}", fill="black")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        parser.error("Arquivo de saída já existe; use outro nome para não sobrescrever")
    sheet.save(args.output, format="PNG")
    print(f"Revisão salva: {args.output}")
    print(f"Imagens analisadas: {len(files)}")
    print("Caixas verdes = corpo completo (fbox), se gerado pelo conversor padrão.")
    print("Inspecione se as caixas acompanham pessoas, especialmente sob oclusão.")

if __name__ == "__main__":
    main()
