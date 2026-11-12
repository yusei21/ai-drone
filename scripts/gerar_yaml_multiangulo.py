#!/usr/bin/env python3
"""Gera YAML para treinar pessoas em múltiplos ângulos, sem inventar datasets.

Exemplo:
 python scripts/gerar_yaml_multiangulo.py \
   --source aereo=/caminho/VisDrone-pessoas \
   --source olhos=/caminho/Pessoas-Olhos-YOLO \
   --source baixo=/caminho/Pessoas-Angulo-Baixo-YOLO \
   --output /caminho/pessoas_multiangulo/pessoas_multiangulo.yaml

Cada fonte deve conter images/train, labels/train, images/val e labels/val.
Todas as classes precisam ser 0 (pessoa); não use diretamente COCO nem
CrowdHuman sem converter suas anotações e selecionar a classe correta.
"""
import argparse
from pathlib import Path

IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}


def check_source(name: str, root: Path) -> tuple[list[str], dict[str, int]]:
    paths: list[str] = []
    counts: dict[str, int] = {}
    for split in ("train", "val"):
        images_dir = root / "images" / split
        labels_dir = root / "labels" / split
        if not images_dir.is_dir() or not labels_dir.is_dir():
            raise ValueError(f"{name}: faltam {images_dir} ou {labels_dir}")
        images = sorted(p for p in images_dir.rglob("*") if p.is_file() and p.suffix.lower() in IMAGE_EXTENSIONS)
        if not images:
            raise ValueError(f"{name}: nenhuma imagem em {images_dir}")
        missing = 0
        for image in images:
            label = labels_dir / image.relative_to(images_dir).with_suffix(".txt")
            if not label.is_file():
                missing += 1
                continue
            for number, line in enumerate(label.read_text(encoding="utf-8").splitlines(), 1):
                fields = line.split()
                if not fields:
                    continue
                if len(fields) != 5 or fields[0] != "0":
                    raise ValueError(f"{label}:{number}: esperado '0 cx cy largura altura'")
                try:
                    coords = [float(v) for v in fields[1:]]
                except ValueError as exc:
                    raise ValueError(f"{label}:{number}: coordenadas invalidas") from exc
                if not all(0 <= v <= 1 for v in coords) or coords[2] <= 0 or coords[3] <= 0:
                    raise ValueError(f"{label}:{number}: coordenadas fora de 0..1")
        if missing:
            raise ValueError(
                f"{name}/{split}: {missing} imagens sem arquivo .txt. "
                "Crie arquivos vazios para imagens realmente sem pessoas, "
                "ou corrija as anotacoes."
            )
        counts[split] = len(images)
        paths.append(str(images_dir))
    return paths, counts


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", action="append", required=True, metavar="ANGULO=PASTA")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    source_dirs: dict[str, Path] = {}
    for entry in args.source:
        if "=" not in entry:
            parser.error(f"Fonte invalida: {entry}. Use ANGULO=PASTA")
        name, raw_path = entry.split("=", 1)
        name = name.strip().lower()
        if name not in ("aereo", "olhos", "baixo"):
            parser.error(f"Angulo {name!r} nao reconhecido. Use aereo, olhos ou baixo")
        if name in source_dirs:
            parser.error(f"Angulo duplicado: {name}")
        source_dirs[name] = Path(raw_path).expanduser().resolve()
    required = {"aereo", "olhos", "baixo"}
    if set(source_dirs) != required:
        parser.error("Para o treino multiangulo, forneca as fontes aereo, olhos e baixo")
    if len(set(source_dirs.values())) != 3:
        parser.error("As tres fontes precisam apontar para pastas distintas")

    groups: dict[str, list[str]] = {"train": [], "val": []}
    for name in ("aereo", "olhos", "baixo"):
        paths, counts = check_source(name, source_dirs[name])
        groups["train"].append(paths[0])
        groups["val"].append(paths[1])
        print(f"{name}: train={counts['train']} imagens; val={counts['val']} imagens")

    destination = args.output.expanduser().resolve()
    if destination.exists():
        parser.error(f"Arquivo ja existe: {destination}. Escolha outro caminho ou remova manualmente apos revisar.")
    destination.parent.mkdir(parents=True, exist_ok=True)
    lines = ["# Dataset multiangulo de UMA classe: pessoa", "train:"]
    lines.extend(f"  - '{p}'" for p in groups["train"])
    lines.append("val:")
    lines.extend(f"  - '{p}'" for p in groups["val"])
    lines.extend(["names:", "  0: pessoa", ""])
    destination.write_text("\n".join(lines), encoding="utf-8")
    print(f"YAML validado e criado: {destination}")
    print("ATENCAO: separar train/val por cena, pessoa e video; este script nao detecta vazamento nem duplicatas.")


if __name__ == "__main__":
    main()
