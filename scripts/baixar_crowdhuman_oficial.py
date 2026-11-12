#!/usr/bin/env python3
"""CrowdHuman downloads: verify access before using source URLs.

The Google Drive IDs published on https://www.crowdhuman.org/download
currently return "file does not exist" (verified 2026-11-12).
Do not run automatic downloads against unavailable links or unknown mirrors.
"""
from pathlib import Path
import argparse

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--only-annotations", action="store_true")
    args = parser.parse_args()
    args.output.expanduser().mkdir(parents=True, exist_ok=True)
    parser.exit(
        2,
        "CrowdHuman: downloads automáticos suspensos.\n"
        "Os links Google Drive publicados em https://www.crowdhuman.org/download\n"
        "retornaram 404 (arquivo inexistente) em 2026-11-12.\n"
        "Não foi comprovada uma origem alternativa igualmente oficial.\n"
        "Não use espelhos de terceiros sem verificar procedência, licença e integridade.\n"
        "O dataset é limitado a pesquisa/educação não comerciais.\n"
        "Os dados COCO e VisDrone já preparados não foram afetados.\n",
    )

if __name__ == "__main__":
    main()
