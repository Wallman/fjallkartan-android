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
            CREATE TABLE place (
                id      INTEGER PRIMARY KEY,
                kind    INTEGER NOT NULL,
                rank    INTEGER NOT NULL,
                lat     INTEGER NOT NULL,
                lon     INTEGER NOT NULL,
                country INTEGER NOT NULL,
                muni    INTEGER,
                name    TEXT NOT NULL
            );
            CREATE TABLE alias (
                id       INTEGER PRIMARY KEY,
                place_id INTEGER NOT NULL,
                name     TEXT NOT NULL,
                lang     INTEGER
            );
            CREATE TABLE municipality (
                id      INTEGER PRIMARY KEY,
                name    TEXT,
                region  TEXT,
                country INTEGER NOT NULL
            );
            CREATE TABLE language (
                id   INTEGER PRIMARY KEY,
                code TEXT NOT NULL
            );

            INSERT INTO place SELECT * FROM source.place;
            INSERT INTO alias SELECT * FROM source.alias;
            INSERT INTO municipality SELECT * FROM source.municipality;
            INSERT INTO language SELECT * FROM source.language;

            CREATE INDEX place_muni ON place(muni);
            CREATE INDEX alias_place ON alias(place_id);

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
