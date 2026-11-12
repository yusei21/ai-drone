#!/usr/bin/env python3
"""Audit CrowdHuman ODGT annotations *without downloading any images*.

This validates the integrity of a Hugging Face community copy and its
structural consistency with the authors' published ODGT specification.
It does NOT prove that the copy is identical to the authors' original.
No external Python dependencies are required.
"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
import math
from pathlib import Path
import sys

COMMUNITY_TRAIN_SHA256 = "6bf241a79f19e30cf52681eab3392368bd5a534164be9272e7a808cb284d9f77"


def valid_box(value: object) -> bool:
    if not isinstance(value, list) or len(value) != 4:
        return False
    if not all(isinstance(v, (int, float)) and not isinstance(v, bool) and math.isfinite(v) for v in value):
        return False
    return value[2] >= 0 and value[3] >= 0


def audit(path: Path, expected_sha: str) -> int:
    if not path.is_file():
        print(f"Arquivo ausente: {path}", file=sys.stderr)
        return 2
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    actual = digest.hexdigest()
    print(f"Arquivo: {path}")
    print(f"SHA256: {actual}")
    if actual != expected_sha.lower():
        print("FALHA: hash diferente do publicado. Não use o arquivo.", file=sys.stderr)
        return 2
    print("OK: SHA-256 corresponde ao publicado pela cópia comunitária.")
    counts: Counter[str] = Counter()
    seen_ids: set[str] = set()
    errors: list[str] = []
    with path.open("r", encoding="utf-8") as file:
        for line_number, raw_line in enumerate(file, 1):
            if not raw_line.strip():
                errors.append(f"linha {line_number}: vazia")
                continue
            try:
                item = json.loads(raw_line)
            except json.JSONDecodeError as exc:
                errors.append(f"linha {line_number}: JSON inválido: {exc}")
                continue
            if not isinstance(item, dict):
                errors.append(f"linha {line_number}: registro não é objeto JSON")
                continue
            image_id = item.get("ID")
            boxes = item.get("gtboxes")
            if not isinstance(image_id, str) or not image_id or image_id in seen_ids:
                errors.append(f"linha {line_number}: ID inválido ou duplicado")
            else:
                seen_ids.add(image_id)
            if not isinstance(boxes, list):
                errors.append(f"linha {line_number}: gtboxes não é lista")
                continue
            counts["images"] += 1
            for box in boxes:
                if not isinstance(box, dict):
                    errors.append(f"linha {line_number}: gtbox inválido")
                    continue
                tag = box.get("tag")
                if tag not in ("person", "mask"):
                    errors.append(f"linha {line_number}: tag desconhecida: {tag!r}")
                    continue
                extra = box.get("extra", {})
                if not isinstance(extra, dict):
                    errors.append(f"linha {line_number}: extra inválido")
                    continue
                ignored = extra.get("ignore", 1 if tag == "mask" else 0)
                if ignored not in (0, 1):
                    errors.append(f"linha {line_number}: ignore inválido")
                if tag == "mask" and ignored != 1:
                    errors.append(f"linha {line_number}: mask sem ignore=1")
                counts[tag] += 1
                if ignored == 1:
                    counts["ignored"] += 1
                else:
                    counts["person_usable"] += 1
                for key in ("vbox", "fbox", "hbox"):
                    if not valid_box(box.get(key)):
                        errors.append(f"linha {line_number}: {key} inválido")
                if tag == "person" and ignored == 0:
                    vbox, fbox = box.get("vbox"), box.get("fbox")
                    if valid_box(vbox) and valid_box(fbox):
                        va = vbox[2] * vbox[3]
                        fa = fbox[2] * fbox[3]
                        if fa > 0 and va < fa * 0.65:
                            counts["high_occlusion_candidate"] += 1
            if len(errors) >= 20:
                print("Interrompendo após 20 problemas, exemplos:")
                break
    for key in ("images", "person", "mask", "ignored", "person_usable", "high_occlusion_candidate"):
        print(f"{key}: {counts[key]}")
    if errors:
        print("FALHA: problemas estruturais detectados:")
        for err in errors[:20]:
            print(f"  - {err}")
        return 2
    if counts["images"] == 0 or counts["person_usable"] == 0:
        print("FALHA: sem anotações aproveitáveis.")
        return 2
    print("OK: ODGT estruturalmente consistente com o esquema do CrowdHuman.")
    print("ATENÇÃO: autenticidade da distribuição original, qualidade visual, licença e")
    print("correspondência das imagens ainda exigem validação independente.")
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--file", type=Path, required=True)
    parser.add_argument("--sha256", default=COMMUNITY_TRAIN_SHA256)
    args = parser.parse_args()
    if len(args.sha256) != 64 or any(c not in "0123456789abcdefABCDEF" for c in args.sha256):
        parser.error("SHA-256 deve conter 64 caracteres hexadecimais")
    raise SystemExit(audit(args.file, args.sha256))


if __name__ == "__main__":
    main()
