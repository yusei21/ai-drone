#!/usr/bin/env python3
"""Instala TFLite exportado no caminho automatico do aplicativo Android.

Nao use best.pt diretamente no Android: execute exportar_modelo_android.py
antes. O modelo vai em assets/ml_models/pessoas_ativo.tflite, embarcado no APK.
Nao faz download nem acessa o tablet; e necessario recompilar e instalar o APK.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import shutil
import sys

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_MODEL = ROOT / "runs/android_models/pessoas_640.tflite"
DEST = ROOT / "app/src/main/assets/ml_models/pessoas_ativo.tflite"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--modelo", type=Path, default=DEFAULT_MODEL)
    parser.add_argument("--replace", action="store_true",
                        help="Substitui um modelo instalado anteriormente")
    args = parser.parse_args()
    source = args.modelo.expanduser().resolve()
    if not source.is_file() or source.suffix.lower() != ".tflite":
        parser.error("Modelo TFLite exportado nao encontrado. Exporte best.pt primeiro.")
    if source.stat().st_size < 1024:
        parser.error("Modelo muito pequeno para um TFLite valido.")
    # FlatBuffer TFLite standard file identifier, at offset 4.
    with source.open("rb") as file:
        file.seek(4)
        identifier = file.read(4)
    if identifier != b"TFL3":
        parser.error("O arquivo nao tem o identificador TFLite TFL3.")
    if DEST.exists() and not args.replace:
        parser.error("Destino ja existe. Use --replace apos comparar modelos.")
    DEST.parent.mkdir(parents=True, exist_ok=True)
    if source != DEST.resolve():
        shutil.copy2(source, DEST)
    print(f"Modelo embarcado: {DEST}")
    print(f"Tamanho: {DEST.stat().st_size / (1024*1024):.2f} MiB")
    print("Caminho no APK: assets/ml_models/pessoas_ativo.tflite")
    print("Modelo selecionado automaticamente na tela da camera DJI.")
    print("Proximo passo: ./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk")
    print("Atencao: TFL3 valida o cabecalho; os tensores devem ser verificados no exportador.")

if __name__ == "__main__":
    main()
