#!/usr/bin/env python3
"""Audit CrowdHuman ZIP archives against ODGT without extracting images.

Checks known community-distribution SHA-256 values, ZIP CRC, and exact image ID
correspondence. Accepts multiple train ZIPs or one validation ZIP.
Matches the community-hosted copies, not independently verified author hashes.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import sys
import zipfile

HASHES = {
    "CrowdHuman_train01.zip": "7ba340163cff0f2446027af95dc96bcb9c66be18506eabb57822c400a7efd3b8",
    "CrowdHuman_train02.zip": "d9ecfb43eaf8381ddd4d1ff4e9a0877b694dbfba7a7a33ed3966ee2c2a628663",
    "CrowdHuman_train03.zip": "9cac171914c4f5c7371e3d42d20b50858caa1602811ae573aa35bbfe672dc29f",
    "CrowdHuman_val.zip": "c0ab99bb80ac162cd3efdf94a1a6100c4f059a61d69596412c6b44ebc20d1363",
}

def abort(message: str) -> None:
    raise SystemExit("ERRO: " + message)

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--zip", type=Path, required=True, nargs="+", help="Um ou mais ZIPs")
    parser.add_argument("--annotations", type=Path, required=True)
    parser.add_argument("--sha256", help="SHA-256 customizado (apenas para um ZIP)")
    args = parser.parse_args()
    zips = args.zip
    if len(set(p.name for p in zips)) != len(zips):
        abort("nomes de ZIP duplicados")
    if args.sha256 and len(zips) != 1:
        abort("--sha256 customizado exige exatamente um ZIP")
    if not args.annotations.is_file():
        abort(f"arquivo de anotações inexistente: {args.annotations}")
    expected: set[str] = set()
    with args.annotations.open(encoding="utf-8") as file:
        for line_number, line in enumerate(file, 1):
            try:
                record = json.loads(line)
                item_id = record["ID"]
            except (ValueError, TypeError, KeyError) as exc:
                abort(f"registro ODGT inválido na linha {line_number}: {exc}")
            if not isinstance(item_id, str) or not item_id or item_id in expected:
                abort(f"ID inválido ou duplicado no ODGT, linha {line_number}")
            expected.add(item_id)
    if len(expected) not in (4370, 15000):
        abort(f"ODGT contém {len(expected)} IDs: esperado 4370 ou 15000")
    print(f"ODGT: {len(expected)} imagens anotadas.", flush=True)

    observed: set[str] = set()
    duplicate: list[str] = []
    unexpected_nonimage: set[str] = set()
    for path in zips:
        if not path.is_file():
            abort(f"ZIP não encontrado: {path}")
        expected_hash = args.sha256.lower() if args.sha256 else HASHES.get(path.name)
        if not expected_hash:
            abort(f"hash desconhecido para {path.name}; forneça --sha256")
        print(f"SHA256 de {path.name}...", flush=True)
        digest = hashlib.sha256()
        with path.open("rb") as file:
            for chunk in iter(lambda: file.read(4 * 1024 * 1024), b""):
                digest.update(chunk)
        actual = digest.hexdigest()
        print(f"  {actual}")
        if actual != expected_hash:
            abort(f"hash não coincide para {path.name}; não utilize este ZIP")
        print(f"  OK: hash publicado corresponde. Verificando CRC...", flush=True)
        try:
            with zipfile.ZipFile(path) as archive:
                members = archive.infolist()
                for index, info in enumerate(members, 1):
                    if info.is_dir():
                        continue
                    member_path = PurePosixPath(info.filename)
                    if member_path.is_absolute() or ".." in member_path.parts or "\\" in info.filename:
                        abort(f"nome de membro inseguro: {info.filename}")
                    suffix = member_path.suffix.lower()
                    if suffix not in (".jpg", ".jpeg", ".png"):
                        unexpected_nonimage.add(info.filename)
                        continue
                    image_id = member_path.stem
                    if image_id in observed:
                        duplicate.append(image_id)
                    observed.add(image_id)
                    with archive.open(info) as handle:
                        for _ in iter(lambda: handle.read(1024 * 1024), b""):
                            pass
                    if index % 1000 == 0:
                        print(f"  CRC: {index}/{len(members)} membros", flush=True)
        except (OSError, zipfile.BadZipFile, EOFError, RuntimeError) as exc:
            abort(f"falha de integridade ZIP/CRC em {path.name}: {exc}")
        print(f"  OK: leitura e CRC de {path.name}.", flush=True)
    missing = expected - observed
    extras = observed - expected
    print(f"IDs ODGT: {len(expected)}")
    print(f"IDs de imagens: {len(observed)}")
    print(f"IDs ausentes: {len(missing)} {sorted(missing)[:5]}")
    print(f"IDs extras: {len(extras)} {sorted(extras)[:5]}")
    print(f"IDs duplicados: {len(duplicate)} {duplicate[:5]}")
    print(f"Arquivos não-imagem: {len(unexpected_nonimage)} {sorted(unexpected_nonimage)[:5]}")
    if missing or extras or duplicate:
        abort("nomes das imagens não correspondem exatamente às anotações")
    print("OK: todos os ZIPs passaram em SHA-256, CRC e correspondência dos IDs.")
    print("PENDENTE: autenticidade em relação ao material original, termos de licença e revisão visual.")

if __name__ == "__main__":
    main()
