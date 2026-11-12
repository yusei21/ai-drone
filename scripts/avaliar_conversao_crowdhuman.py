#!/usr/bin/env python3
"""Evaluate whether CrowdHuman images are safe for a single-class YOLO dataset.

Dry-run only: reads CrowdHuman ODGT and reports image-level ignored-area counts,
person-box validity, and risk from partial annotation. Does not extract,
modify, or convert files. Standard library only.
"""
from __future__ import annotations
import argparse
from collections import Counter
import json
import math
from pathlib import Path

def box_ok(box):
    return (isinstance(box, list) and len(box) == 4 and
            all(type(n) in (int, float) and math.isfinite(n) for n in box) and
            box[2] > 0 and box[3] > 0)

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--annotations", type=Path, required=True)
    p.add_argument("--sample", type=int, default=5)
    args = p.parse_args()
    total = Counter()
    examples = []
    unsafe_examples = []
    with args.annotations.open(encoding="utf-8") as f:
        for line_number, line in enumerate(f, 1):
            record = json.loads(line)
            image_id = record.get("ID")
            boxes = record.get("gtboxes")
            if not isinstance(image_id, str) or not isinstance(boxes, list):
                raise SystemExit(f"Invalid ODGT record at line {line_number}")
            total["images"] += 1
            ignored = 0
            usable = 0
            corrupted = 0
            for box in boxes:
                if not isinstance(box, dict):
                    corrupted += 1
                    continue
                tag = box.get("tag")
                extra = box.get("extra") or {}
                if not isinstance(extra, dict):
                    corrupted += 1
                    continue
                is_ignored = tag == "mask" or extra.get("ignore", 0) == 1
                if is_ignored:
                    ignored += 1
                    total["ignored_regions"] += 1
                    continue
                if tag != "person" or not box_ok(box.get("fbox")):
                    corrupted += 1
                    continue
                usable += 1
                total["usable_persons"] += 1
            if ignored:
                total["images_with_ignore"] += 1
                if len(unsafe_examples) < args.sample:
                    unsafe_examples.append((image_id, ignored, usable))
            if corrupted:
                total["images_with_invalid_nonignored"] += 1
                total["invalid_nonignored"] += corrupted
            if not ignored and not corrupted and usable:
                total["strict_safe_images"] += 1
                total["strict_safe_persons"] += usable
                if len(examples) < args.sample:
                    examples.append((image_id, usable))
            elif not ignored and not corrupted:
                total["empty_images_without_ignore"] += 1
    for key in ("images", "usable_persons", "ignored_regions", "images_with_ignore",
                "images_with_invalid_nonignored", "invalid_nonignored",
                "strict_safe_images", "strict_safe_persons",
                "empty_images_without_ignore"):
        print(f"{key}: {total[key]}")
    print("Sample eligible image IDs:", examples)
    print("Sample IDs with ignore regions (DO NOT convert naively):", unsafe_examples)
    print("This is a conservative assessment, not a YOLO conversion.")
    print("Ignoring mask/ignore boxes while training the entire image could create false negatives.")
    print("CrowdHuman usage remains subject to non-commercial research/education terms.")

if __name__ == "__main__":
    main()
