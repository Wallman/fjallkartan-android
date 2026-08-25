#!/usr/bin/env python3
"""Verify that every APK native library supports 16 KB memory pages."""

import argparse
import struct
import zipfile
from pathlib import Path

PT_LOAD = 1
MINIMUM_ALIGNMENT = 16 * 1024


def load_alignments(data: bytes) -> list[int]:
    if data[:4] != b"\x7fELF":
        raise ValueError("not an ELF file")
    bits = data[4]
    byte_order = "<" if data[5] == 1 else ">"
    if bits == 2:
        phoff = struct.unpack_from(byte_order + "Q", data, 32)[0]
        phentsize, phnum = struct.unpack_from(byte_order + "HH", data, 54)
        align_offset, align_format = 48, "Q"
    elif bits == 1:
        phoff = struct.unpack_from(byte_order + "I", data, 28)[0]
        phentsize, phnum = struct.unpack_from(byte_order + "HH", data, 42)
        align_offset, align_format = 28, "I"
    else:
        raise ValueError(f"unsupported ELF class {bits}")
    alignments = []
    for index in range(phnum):
        offset = phoff + index * phentsize
        segment_type = struct.unpack_from(byte_order + "I", data, offset)[0]
        if segment_type == PT_LOAD:
            alignments.append(struct.unpack_from(byte_order + align_format, data, offset + align_offset)[0])
    return alignments


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    failures = []
    with zipfile.ZipFile(args.apk) as apk:
        libraries = sorted(name for name in apk.namelist() if name.endswith(".so"))
        for name in libraries:
            alignments = load_alignments(apk.read(name))
            smallest = min(alignments)
            print(f"{name}: minimum LOAD alignment {smallest:#x}")
            if smallest < MINIMUM_ALIGNMENT:
                failures.append(name)
    if failures:
        raise SystemExit("Libraries below 16 KB alignment: " + ", ".join(failures))
    print(f"Verified {len(libraries)} native libraries.")


if __name__ == "__main__":
    main()
