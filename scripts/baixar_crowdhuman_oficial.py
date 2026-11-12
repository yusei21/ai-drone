#!/usr/bin/env python3
"""Download CrowdHuman from the Google Drive links published on crowdhuman.org.

Use solely for non-commercial research/education as required by the dataset.
Does not redistribute files. Uses gdown installed separately.
"""
from __future__ import annotations
import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

FILES = {
    "CrowdHuman_train01.zip": "134QOvaatwKdy0iIeNqA_p-xkAhkV4F8Y",
    "CrowdHuman_train02.zip": "17evzPh7gc1JBNvnW1ENXLy5Kr4Q_Nnla",
    "CrowdHuman_train03.zip": "1tdp0UCgxrqy1B6p8LkR-Iy0aIJ8l4fJW",
    "CrowdHuman_val.zip": "18jFI789CoHTppQ7vmRSFEdnGaSQZ4YzO",
    "annotation_train.odgt": "1UUTea5mYqvlUObsC1Z8CFldHJAtLtMX3",
    "annotation_val.odgt": "10WIRwu8ju8GRLuCkZ_vT6hnNxs5ptwoL",
}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--only-annotations", action="store_true")
    args = parser.parse_args()
    output = args.output.expanduser().resolve()
    output.mkdir(parents=True, exist_ok=True)
    selected = (
        {k: v for k, v in FILES.items() if k.endswith(".odgt")}
        if args.only_annotations else FILES
    )
    print("Fonte: https://www.crowdhuman.org/download")
    print("Uso: pesquisa/educacao nao comerciais; proibida redistribuicao das imagens.")
    for filename, file_id in selected.items():
        destination = output / filename
        if destination.is_file():
            print(f"Ja existe; nao substituindo: {destination}")
            continue
        url = f"https://drive.google.com/uc?id={file_id}"
        print(f"Baixando {filename} de {url}", flush=True)
        result = subprocess.run(
            [sys.executable, "-m", "gdown", "--fuzzy", "--continue", "--output", str(destination), url],
            check=False,
        )
        if result.returncode:
            raise SystemExit(
                f"Download nao concluido: {filename}. "
                "Confirme se o Google Drive permite acesso anonimo e tente novamente; "
                "nao use espelhos desconhecidos."
            )
        if destination.stat().st_size == 0:
            raise SystemExit(f"Arquivo vazio: {destination}")
    print("Downloads concluidos. Verifique os ZIPs com unzip -tq antes de extrair.")


if __name__ == "__main__":
    main()
