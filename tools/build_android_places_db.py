#!/usr/bin/env python3
"""Convert the canonical Fjällkartan place database from FTS5 to Android FTS4."""

from __future__ import annotations

import argparse
import sqlite3
from pathlib import Path


def build(source: Path, output: Path) -> None:
    if output.exists():
        output.unlink()

    db = sqlite3.connect(output)
    try:
        db.execute("PRAGMA journal_mode=OFF")
        db.execute("PRAGMA synchronous=OFF")
        db.execute("ATTACH DATABASE ? AS source", (str(source),))
        db.executescript(
            """
            CREATE TABLE place AS SELECT * FROM source.place;
            CREATE TABLE alias AS SELECT * FROM source.alias;
            CREATE TABLE municipality AS SELECT * FROM source.municipality;
            CREATE TABLE language AS SELECT * FROM source.language;

            CREATE UNIQUE INDEX place_id ON place(id);
            CREATE INDEX place_muni ON place(muni);
            CREATE UNIQUE INDEX alias_id ON alias(id);
            CREATE INDEX alias_place ON alias(place_id);
            CREATE UNIQUE INDEX municipality_id ON municipality(id);
            CREATE UNIQUE INDEX language_id ON language(id);

            CREATE VIRTUAL TABLE place_fts USING fts4(
                name,
                content='',
                tokenize=unicode61 'remove_diacritics=1'
            );
            INSERT INTO place_fts(rowid, name) SELECT id, name FROM place;
            INSERT INTO place_fts(rowid, name) SELECT id, name FROM alias;
            INSERT INTO place_fts(place_fts) VALUES ('optimize');
            """
        )
        db.execute("DETACH DATABASE source")
        db.execute("PRAGMA user_version=5")
        db.execute("VACUUM")
    finally:
        db.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    build(args.source, args.output)


if __name__ == "__main__":
    main()
