#!/usr/bin/env python3
"""
Exporta YOLO26 de pessoas para LiteRT/TFLite FP32 e inspeciona os tensores.
Nao publica pesos no Git. Execute na .venv do PC apos o treinamento.
"""
from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from ultralytics import YOLO


def main() -> None:
    parser = argparse.ArgumentParser(description="Exportar modelo de pessoas para o teste offline Android")
    parser.add_argument(
        "--pesos",
        type=Path,
        default=Path("runs/detect/runs/ai_drone/pessoas_960/weights/best.pt"),
        help="best.pt validado. Alterar para outro run somente apos comparar na validacao.",
    )
    parser.add_argument("--imgsz", type=int, default=640, choices=(640, 960))
    args = parser.parse_args()
    source = args.pesos.expanduser().resolve()
    if not source.is_file():
        parser.error(f"Pesos nao encontrados: {source}")

    print(f"Exportando: {source}")
    print(f"Resolucao: {args.imgsz}; saida: LiteRT FP32 crua (NMS no Android)")
    exported = YOLO(str(source)).export(
        format="litert",
        imgsz=args.imgsz,
        nms=None,
        device="cpu",
    )
    output = Path(str(exported)).resolve()
    if not output.is_file() or output.suffix.lower() != ".tflite":
        raise RuntimeError(f"Exportacao nao produziu .tflite: {output}")

    destination = Path("runs/android_models") / f"pessoas_{args.imgsz}.tflite"
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(output, destination)
    print(f"Modelo exportado: {destination.resolve()} ({destination.stat().st_size / 1024**2:.1f} MiB)")

    # Usar o interpretador instalado pela exportacao para validar estrutura.
    try:
        from ai_edge_litert.interpreter import Interpreter
    except ImportError:
        try:
            from tensorflow.lite import Interpreter
        except ImportError as exc:
            raise RuntimeError(
                "Nao foi possivel inspecionar tensores: falta ai-edge-litert ou tensorflow. "
                "Nao importe o modelo no Android sem validar o formato."
            ) from exc
    interpreter = Interpreter(model_path=str(destination))
    input_info = interpreter.get_input_details()[0]
    output_infos = interpreter.get_output_details()
    print("Entrada:", input_info["shape"], input_info["dtype"])
    for i, info in enumerate(output_infos):
        print(f"Saida {i}: shape={info['shape']}, dtype={info['dtype']}")

    # O decoder Android deste experimento suporta SOMENTE saida crua [1,5,N].
    shape_in = list(map(int, input_info["shape"]))
    shape_out = list(map(int, output_infos[0]["shape"])) if len(output_infos) == 1 else []
    ok = (
        len(shape_in) == 4
        and shape_in == [1, args.imgsz, args.imgsz, 3]
        and str(input_info["dtype"]) in ("<class 'numpy.float32'>", "float32")
        and len(output_infos) == 1
        and len(shape_out) == 3
        and shape_out[0] == 1
        and shape_out[1] == 5
        and shape_out[2] > 0
        and str(output_infos[0]["dtype"]) in ("<class 'numpy.float32'>", "float32")
    )
    if not ok:
        raise RuntimeError(
            "Formato incompativel com o decoder Android atual. "
            "Envie o shape de entrada e de saida antes de importar."
        )
    print("OK: tensores compativeis com o decoder preliminar do Android.")
    print("A equivalencia de caixas e confiancas ainda deve ser testada em imagens reais.")


if __name__ == "__main__":
    main()
