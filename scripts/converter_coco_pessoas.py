#!/usr/bin/env python3
"""Create a single-class YOLO people dataset from official COCO 2017.

Requires COCO2017/images/{train2017,val2017} and
COCO2017/annotations/instances_{train2017,val2017}.json.
Does not alter COCO. Excludes images that contain person iscrowd regions,
because YOLO txt cannot encode COCO ignore masks.
Hardlinks images where possible, falling back to copying.
"""
from __future__ import annotations

import argparse
import json
import math
import os
import shutil
from collections import defaultdict
from pathlib import Path

PERSON_CATEGORY_ID = 1


def coco_to_yolo_bbox(box: list[float], width: int, height: int) -> str | None:
    if len(box) != 4 or width <= 0 or height <= 0:
        return None
    x, y, w, h = box
    if not all(math.isfinite(v) for v in (x, y, w, h)) or w <= 0 or h <= 0:
        return None
    x1, y1 = max(0., x), max(0., y)
    x2, y2 = min(float(width), x + w), min(float(height), y + h)
    if x2 <= x1 or y2 <= y1:
        return None
    cx = (x1 + x2) / (2 * width)
    cy = (y1 + y2) / (2 * height)
    bw = (x2 - x1) / width
    bh = (y2 - y1) / height
    return f"0 {cx:.8f} {cy:.8f} {bw:.8f} {bh:.8f}"


def convert_split(source: Path, output: Path, split: str, limit: int) -> None:
    coco_split = {"train": "train2017", "val": "val2017"}[split]
    annotation_path = source / "annotations" / f"instances_{coco_split}.json"
    source_images = source / "images" / coco_split
    if not annotation_path.is_file() or not source_images.is_dir():
        raise FileNotFoundError(f"Falta {annotation_path} ou {source_images}")
    with annotation_path.open(encoding="utf-8") as handle:
        data = json.load(handle)
    categories = {int(item["id"]): item["name"] for item in data["categories"]}
    if categories.get(PERSON_CATEGORY_ID) != "person":
        raise ValueError("ID 1 nao corresponde a classe person no COCO")
    images = {int(item["id"]): item for item in data["images"]}
    boxes: dict[int, list[str]] = defaultdict(list)
    ignored: set[int] = set()
    for annotation in data["annotations"]:
        if annotation.get("category_id") != PERSON_CATEGORY_ID:
            continue
        image_id = int(annotation["image_id"])
        if annotation.get("iscrowd", 0):
            ignored.add(image_id)
            continue
        image = images.get(image_id)
        if image is None:
            raise ValueError(f"Image ID ausente: {image_id}")
        if annotation.get("ignore", 0):
            ignored.add(image_id)
            continue
        bbox = coco_to_yolo_bbox(
            annotation["bbox"], int(image["width"]), int(image["height"])
        )
        if bbox is not None:
            boxes[image_id].append(bbox)
    dst_images = output / "images" / split
    dst_labels = output / "labels" / split
    dst_images.mkdir(parents=True, exist_ok=True)
    dst_labels.mkdir(parents=True, exist_ok=True)
    selected = sorted(
        (image_id for image_id in boxes if image_id not in ignored),
        key=lambda image_id: images[image_id]["file_name"],
    )
    if limit:
        selected = selected[:limit]
    written = 0
    for image_id in selected:
        item = images[image_id]
        filename = Path(item["file_name"]).name
        source_image = source_images / filename
        if not source_image.is_file():
            raise FileNotFoundError(source_image)
        target_image = dst_images / filename
        label_path = dst_labels / (Path(filename).stem + ".txt")
        labels_text = "\n".join(boxes[image_id]) + "\n"
        if target_image.exists() and target_image.stat().st_size != source_image.stat().st_size:
            raise FileExistsError(f"Imagem destino divergente: {target_image}")
        if label_path.exists() and label_path.read_text(encoding="utf-8") != labels_text:
            raise FileExistsError(f"Rotulo destino divergente: {label_path}")
        if not target_image.exists():
            try:
                os.link(source_image, target_image)
            except OSError:
                shutil.copy2(source_image, target_image)
        label_path.write_text(labels_text, encoding="utf-8")
        written += 1
    print(
        f"{split}: {written} imagens com pessoas, "
        f"{len(ignored)} imagens excluidas por iscrowd/ignore de person, "
        f"limite={limit or 'nenhum'}"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--limit-train", type=int, default=0,
                        help="0 usa todos os exemplos validos")
    parser.add_argument("--limit-val", type=int, default=0)
    args = parser.parse_args()
    if args.limit_train < 0 or args.limit_val < 0:
        parser.error("Limites nao podem ser negativos")
    source, output = args.source.expanduser().resolve(), args.output.expanduser().resolve()
    if source == output or source in output.parents or output in source.parents:
        parser.error("Destino deve ser pasta independente do COCO original")
    for split, limit in (("train", args.limit_train), ("val", args.limit_val)):
        convert_split(source, output, split, limit)
    print(f"Pronto: {output}")
    print("ATENCAO: imagens COCO nao foram filtradas por angulo de camera.")
    print("Revise rotulos e perspectivas; reserve conjunto de teste independente.")


if __name__ == "__main__":
    main()
