#!/usr/bin/env python3
"""Cria uma versão de 1 classe do VisDrone convertido pelo Ultralytics.

Não modifica os arquivos originais. Usa hardlinks por imagem (ou cópias como
fallback), para que o carregador YOLO enumere todas as imagens corretamente.
Mantém as divisões train e val.

Uso:
  python scripts/preparar_visdrone_pessoas.py
  python scripts/preparar_visdrone_pessoas.py --source /caminho/VisDrone --output /caminho/VisDrone-pessoas
"""
from __future__ import annotations

import argparse
import os
import shutil
from pathlib import Path

# Classes no VisDrone.yaml da Ultralytics: 0=pedestrian, 1=people.
PERSON_CLASSES = {0, 1}
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def prepare_split(source: Path, output: Path, split: str) -> tuple[int, int, int]:
    source_images = source / "images" / split
    source_labels = source / "labels" / split
    output_images = output / "images" / split
    output_labels = output / "labels" / split

    if not source_images.is_dir() or not source_labels.is_dir():
        raise FileNotFoundError(
            f"Divisão {split} não encontrada em {source}: "
            "precisa conter images/<split> e labels/<split>."
        )
    image_files = sorted(
        p for p in source_images.iterdir() if p.is_file() and p.suffix.lower() in IMAGE_SUFFIXES
    )
    if not image_files:
        raise ValueError(f"Nenhuma imagem encontrada em {source_images}")

    output_images.parent.mkdir(parents=True, exist_ok=True)
    output_labels.mkdir(parents=True, exist_ok=True)

    # O script antigo criou symlinks de DIRETÓRIO. O glob recursivo do
    # Ultralytics pode não percorrê-los e acabar usando só uma amostra.
    # Substituímos apenas esse tipo de link, preservando a pasta de origem.
    if output_images.is_symlink():
        if output_images.resolve() != source_images.resolve():
            raise ValueError(f"Symlink aponta para outro dataset: {output_images}")
        output_images.unlink()
    elif output_images.exists() and not output_images.is_dir():
        raise FileExistsError(f"Não é uma pasta: {output_images}")
    output_images.mkdir(parents=True, exist_ok=True)

    for image in image_files:
        target = output_images / image.name
        if target.exists():
            if target.is_file() and target.stat().st_size == image.stat().st_size:
                continue
            raise FileExistsError(f"Arquivo de saída conflitante: {target}")
        try:
            os.link(image, target)
        except OSError:
            shutil.copy2(image, target)

    if len([p for p in output_images.iterdir() if p.is_file() and p.suffix.lower() in IMAGE_SUFFIXES]) != len(image_files):
        raise RuntimeError(f"Contagem de imagens diferente para {split}")

    count_boxes, count_negative = 0, 0
    for image in image_files:
        label_file = source_labels / (image.stem + ".txt")
        translated = []
        if label_file.exists():
            for line_number, raw_line in enumerate(label_file.read_text(encoding="utf-8").splitlines(), 1):
                line = raw_line.strip()
                if not line:
                    continue
                parts = line.split()
                if len(parts) != 5:
                    raise ValueError(f"Label inválida {label_file}:{line_number}: {line!r}")
                try:
                    class_id = int(parts[0])
                    coords = [float(x) for x in parts[1:]]
                except ValueError as e:
                    raise ValueError(f"Label inválida {label_file}:{line_number}: {line!r}") from e
                if class_id in PERSON_CLASSES:
                    if not all(0 <= x <= 1 for x in coords) or coords[2] <= 0 or coords[3] <= 0:
                        raise ValueError(f"Coordenadas YOLO inválidas {label_file}:{line_number}")
                    translated.append("0 " + " ".join(parts[1:]))
        # Arquivo vazio significa imagem sem anotação de pessoa.
        (output_labels / (image.stem + ".txt")).write_text(
            ("\n".join(translated) + "\n") if translated else "",
            encoding="utf-8",
        )
        count_boxes += len(translated)
        count_negative += not translated
    return len(image_files), count_boxes, count_negative


def main() -> None:
    default_source = Path.home() / "Documents/workspace/datasets/VisDrone"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=default_source)
    parser.add_argument("--output", type=Path, default=default_source.parent / "VisDrone-pessoas")
    args = parser.parse_args()

    source, output = args.source.expanduser().resolve(), args.output.expanduser().resolve()
    if source == output or source in output.parents or output in source.parents:
        raise ValueError("Origem e saída devem ser diretórios independentes.")
    if not (source / "images").is_dir():
        raise FileNotFoundError(f"Dataset não encontrado: {source}")
    output.mkdir(parents=True, exist_ok=True)

    for split in ("train", "val"):
        images, boxes, negatives = prepare_split(source, output, split)
        print(f"{split}: {images} imagens, {boxes} pessoas, {negatives} imagens sem pessoas.")

    yaml_content = (
        f"path: {output.as_posix()}\n"
        "train: images/train\n"
        "val: images/val\n"
        "names:\n"
        "  0: pessoa\n"
        "nc: 1\n"
    )
    (output / "pessoas.yaml").write_text(yaml_content, encoding="utf-8")
    print(f"Pronto: {output / 'pessoas.yaml'}")
    # Caches antigos do carregador podem refletir o layout com symlinks.
    for cache in output.rglob("*.cache"):
        if cache.is_file():
            cache.unlink()
    print("AVISO: verifique os exemplos negativos e regiões ignoradas antes de usar operacionalmente.")


if __name__ == "__main__":
    main()
