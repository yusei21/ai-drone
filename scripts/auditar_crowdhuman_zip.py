#!/usr/bin/env python3
"""Audit CrowdHuman validation ZIP against ODGT without extracting images.

Compares the SHA-256 published at Hugging Face, CRC integrity of each ZIP member,
and image basenames against all annotated IDs. This does not authenticate the
community mirror against an original author-provided checksum or license.
No third-party modules required.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import zipfile

EXPECTED_SHA256 = "c0ab99bb80ac162cd3efdf94a1a6100c4f059a61d69596412c6b44ebc20d1363"


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--zip", type=Path, required=True)
    ap.add_argument("--annotations", type=Path, required=True)
    ap.add_argument("--sha256", default=EXPECTED_SHA256)
    args = ap.parse_args()
    if not args.zip.is_file() or not args.annotations.is_file():
        ap.error("ZIP ou anotações ausentes")
    if len(args.sha256) != 64 or any(c not in "0123456789abcdefABCDEF" for c in args.sha256):
        ap.error("SHA-256 inválido")
    print("1/3 Conferindo SHA-256 do ZIP...", flush=True)
    sha = hashlib.sha256()
    with args.zip.open("rb") as file:
        for chunk in iter(lambda: file.read(4 * 1024 * 1024), b""):
            sha.update(chunk)
    actual = sha.hexdigest()
    print("SHA256:", actual)
    if actual != args.sha256.lower():
        ap.exit(2, "ERRO: SHA-256 diferente do publicado, não use o arquivo.\n")
    print("OK: hash igual ao da cópia comunitária.", flush=True)

    expected: set[str] = set()
    with args.annotations.open(encoding="utf-8") as file:
        for line, raw in enumerate(file, 1):
            record = json.loads(raw)
            image_id = record.get("ID")
            if not isinstance(image_id, str) or not image_id or image_id in expected:
                ap.exit(2, f"ERRO: ID inválido/duplicado na linha {line}.\n")
            expected.add(image_id)

    print("2/3 Verificando membros, nomes e CRC do ZIP (pode demorar)...", flush=True)
    observed: set[str] = set()
    duplicate: list[str] = []
    with zipfile.ZipFile(args.zip, "r") as archive:
        names = archive.infolist()
        for index, info in enumerate(names, 1):
            if info.is_dir():
                continue
            path = PurePosixPath(info.filename)
            if path.is_absolute() or ".." in path.parts:
                ap.exit(2, f"ERRO: caminho inseguro no ZIP: {info.filename}\n")
            if path.suffix.lower() not in (".jpg", ".jpeg", ".png"):
                continue
            stem = path.stem
            if stem in observed:
                duplicate.append(stem)
            observed.add(stem)
            if index % 1000 == 0:
                print(f"  Verificados {index}/{len(names)} arquivos...", flush=True)
            with archive.open(info) as handle:
                for _ in iter(lambda: handle.read(1024 * 1024), b""):
                    pass  # ZipExtFile verifies CRC on EOF.
    print("3/3 Comparando IDs...", flush=True)
    missing = expected - observed
    unexpected = observed - expected
    print("IDs ODGT:", len(expected))
    print("IDs de imagens no ZIP:", len(observed))
    print("IDs ausentes:", len(missing), sorted(missing)[:5])
    print("IDs extras:", len(unexpected), sorted(unexpected)[:5])
    print("IDs duplicados:", len(duplicate), duplicate[:5])
    if missing or unexpected or duplicate or len(expected) != 4370:
        ap.exit(2, "AUDITORIA INCOMPLETA: revisar diferenças antes de usar.\n")
    print("OK: ZIP passou no SHA-256, leitura/CRC e correspondência de todos os 4370 IDs.")
    print("PENDENTE: comparação com distribuição original dos autores, licença e revisão visual.")


if __name__ == "__main__":
    main()
