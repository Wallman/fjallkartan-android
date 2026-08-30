#!/usr/bin/env python3
"""Import iOS translations into Android string resources.

Generates `values/strings.xml` (the English/default resource set, one entry
per UI string used by the Compose product code) plus `values-<qualifier>/
strings.xml` for every supported language, sourced from the iOS string
catalogue (`Localizable.xcstrings`). Unlike the app's previous custom
`Localizer`/`localizations.json` runtime lookup, translated files only ever
contain the languages that actually have a translation for a given key --
Android's normal resource resolution falls back to the English default for
anything missing, so partial coverage is safe.

Run after touching any user-facing string in the Kotlin source: add the new
English string to KEYS below (using the exact text passed to
`stringResource`), then re-run this script to regenerate the XML.
"""

from __future__ import annotations

import json
import re
import xml.sax.saxutils as saxutils
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / "fjallkartan" / "fjallkartan" / "Localizable.xcstrings"
APP_NAME_SOURCE = ROOT.parent / "fjallkartan" / "fjallkartan" / "InfoPlist.xcstrings"
RES = ROOT / "app" / "src" / "main" / "res"

# Maps the iOS string catalogue language code to the Android resource
# qualifier that selects it.
LANGUAGE_QUALIFIERS = {
    "da": "values-da",
    "de": "values-de",
    "es": "values-es",
    "fi": "values-fi",
    "fr": "values-fr",
    "it": "values-it",
    "nb": "values-nb",
    "nl": "values-nl",
    "sv": "values-sv",
    "zh-Hans": "values-b+zh+Hans",
}

# Every user-facing string in the Compose UI, verbatim (this is also the
# English default shown in `values/strings.xml`). Keep this list in sync with
# the `stringResource(R.string.<slug>)` call sites -- the coverage summary
# this script prints doubles as a "which strings aren't localized" report.
KEYS = [
    '30–35°',
    '35–40°',
    '40–45°',
    '45–50°',
    '50° and steeper',
    'About',
    'Ascent',
    'Available offline',
    'Blast shelter',
    'Boat portage',
    'Boat route, rowing route',
    'Bridge',
    'Cabins and shelter',
    'Camping and open fires prohibited',
    'Campsite',
    'Cancel',
    'Clear',
    'Clear map cache',
    'Clear measurement',
    'Debug tools',
    'Delete',
    'Delete route',
    'Delete this offline region?',
    'Descent',
    'Done',
    'Download',
    'Download current area',
    'Download this area',
    'Elevation',
    'Emergency telephone',
    'Facilities and crossings',
    'Fences and restrictions',
    'Find a place',
    'Find a symbol',
    'Floodlit trail',
    'Ford',
    'Frame the area inside the dashed box, then download it.',
    'Helicopter pad',
    'Highest',
    'How to use the app',
    'Large helicopter landing site',
    'Lean-to shelter',
    'Legend',
    'Mandatory snowmobile route',
    'Mark a spot',
    'Marked hiking trail',
    'Marked summer and winter trail',
    'Marked summer trail',
    'Marked trail',
    'Marked winter trail',
    'Measure a route',
    'Measure distance',
    'Modelled avalanche runout',
    'More map tools',
    'Mountain lodge',
    'Name',
    'Name offline map',
    'Next',
    'Name route',
    'No offline regions',
    'Norway',
    'Not enough free space on this device to download this area.',
    'Notes',
    'OK',
    'Off-road vehicle route, summer',
    'Offline download',
    'Offline mode',
    'Offline regions',
    'One finger draws',
    'Open this guide again from About.',
    'Parking',
    'Part of this route is outside the covered area, so the totals are a minimum.',
    'Path that is hard to follow',
    'Pause download',
    'Paused',
    'Pier and jetty',
    'Press and hold the map to drop a pin. Tap a pin to rename or delete it.',
    'Privacy Policy',
    'Recommended route, unmarked',
    'Regions',
    'Reindeer corral',
    'Reindeer fence',
    'Reindeer husbandry route',
    'Rename route',
    'Rest cabin',
    'Resume download',
    'Sami hut',
    'Save',
    'Save the result as a pin',
    'Save current route',
    'Save place',
    'Saved pin',
    'Saved routes',
    'Search Norwegian and Swedish place names, including local alternatives.',
    'Search places',
    'Self-service tourist cabin',
    'Shade the terrain by slope angle to spot steep ground.',
    'Share diagnostics',
    'Show zoom level',
    'Ski lift',
    'Ski track',
    'Skip',
    'Slope',
    'Snowmobile route',
    'Solitary mountain cabin',
    'Staffed tourist cabin',
    'Steepness',
    'Suggested routes',
    'Support',
    'Sweden',
    'Symbols',
    'Tap the distance for the terrain profile.',
    'Tap the ruler, then drag to trace your route.',
    'This area is too large to download. Zoom in and try a smaller region.',
    'This removes the downloaded area for "%@" from this device.',
    'Tourist hut, overnight hut',
    'Track my location',
    'Tractor road, foot and cycle path',
    'Trails and routes',
    'Two fingers move and zoom.',
    'Undo',
    'Undo last stroke',
    'Unmarked trail',
    'Unstaffed tourist cabin',
    'Version',
    'Wind shelter',
    "You're ready",
    'also %@',
]

# Strings with no entry in the iOS catalogue at all (Android-only debug
# tools, degree-band labels, and a few accessibility/guide strings that
# don't have a close iOS equivalent). Translated by hand rather than
# imported, using the same terminology as the surrounding iOS-sourced
# strings (e.g. "route"/"offline"/"download" vocabulary already in KEYS).
NEW_TRANSLATIONS: dict[str, dict[str, str]] = {
    '30–35°': {lang: '30–35°' for lang in LANGUAGE_QUALIFIERS},
    '35–40°': {lang: '35–40°' for lang in LANGUAGE_QUALIFIERS},
    '40–45°': {lang: '40–45°' for lang in LANGUAGE_QUALIFIERS},
    '45–50°': {lang: '45–50°' for lang in LANGUAGE_QUALIFIERS},
    'Available offline': {
        'da': 'Tilgængelig offline',
        'de': 'Offline verfügbar',
        'es': 'Disponible sin conexión',
        'fi': 'Käytettävissä offline-tilassa',
        'fr': 'Disponible hors ligne',
        'it': 'Disponibile offline',
        'nb': 'Tilgjengelig offline',
        'nl': 'Offline beschikbaar',
        'sv': 'Tillgänglig offline',
        'zh-Hans': '可离线使用',
    },
    'Clear map cache': {
        'da': 'Ryd kortcache',
        'de': 'Kartencache leeren',
        'es': 'Borrar caché del mapa',
        'fi': 'Tyhjennä karttavälimuisti',
        'fr': 'Vider le cache de la carte',
        'it': 'Cancella cache della mappa',
        'nb': 'Tøm kartbuffer',
        'nl': 'Kaartcache wissen',
        'sv': 'Rensa kartcache',
        'zh-Hans': '清除地图缓存',
    },
    'Clear measurement': {
        'da': 'Ryd måling',
        'de': 'Messung löschen',
        'es': 'Borrar medición',
        'fi': 'Tyhjennä mittaus',
        'fr': 'Effacer la mesure',
        'it': 'Cancella misurazione',
        'nb': 'Tøm måling',
        'nl': 'Meting wissen',
        'sv': 'Rensa mätning',
        'zh-Hans': '清除测量',
    },
    'Debug tools': {
        'da': 'Fejlfindingsværktøjer',
        'de': 'Debug-Werkzeuge',
        'es': 'Herramientas de depuración',
        'fi': 'Vianetsintätyökalut',
        'fr': 'Outils de débogage',
        'it': 'Strumenti di debug',
        'nb': 'Feilsøkingsverktøy',
        'nl': 'Foutopsporingshulpmiddelen',
        'sv': 'Felsökningsverktyg',
        'zh-Hans': '调试工具',
    },
    'Delete route': {
        'da': 'Slet rute',
        'de': 'Route löschen',
        'es': 'Eliminar ruta',
        'fi': 'Poista reitti',
        'fr': "Supprimer l'itinéraire",
        'it': 'Elimina percorso',
        'nb': 'Slett rute',
        'nl': 'Route verwijderen',
        'sv': 'Ta bort rutt',
        'zh-Hans': '删除路线',
    },
    'Download current area': {
        'da': 'Hent nuværende område',
        'de': 'Aktuellen Bereich herunterladen',
        'es': 'Descargar área actual',
        'fi': 'Lataa nykyinen alue',
        'fr': 'Télécharger la zone actuelle',
        'it': 'Scarica area corrente',
        'nb': 'Last ned gjeldende område',
        'nl': 'Huidig gebied downloaden',
        'sv': 'Ladda ner aktuellt område',
        'zh-Hans': '下载当前区域',
    },
    'Frame the area inside the dashed box, then download it.': {
        'da': 'Ram området inden for den stiplede boks, og hent det derefter.',
        'de': 'Rahme den Bereich innerhalb des gestrichelten Rahmens ein und lade ihn dann herunter.',
        'es': 'Encuadra el área dentro del recuadro punteado y descárgala.',
        'fi': 'Rajaa alue katkoviivalaatikon sisään ja lataa se sitten.',
        'fr': 'Cadrez la zone dans le cadre en pointillés, puis téléchargez-la.',
        'it': "Inquadra l'area all'interno del riquadro tratteggiato, poi scaricala.",
        'nb': 'Ramme inn området i den stiplede boksen, og last det ned.',
        'nl': 'Kader het gebied binnen het gestippelde vak in en download het.',
        'sv': 'Rama in området inuti den streckade rutan och ladda sedan ner det.',
        'zh-Hans': '将区域框入虚线框内,然后下载。',
    },
    'Measure distance': {
        'da': 'Mål afstand',
        'de': 'Entfernung messen',
        'es': 'Medir distancia',
        'fi': 'Mittaa etäisyys',
        'fr': 'Mesurer la distance',
        'it': 'Misura distanza',
        'nb': 'Mål avstand',
        'nl': 'Afstand meten',
        'sv': 'Mät avstånd',
        'zh-Hans': '测量距离',
    },
    'More map tools': {
        'da': 'Flere kortværktøjer',
        'de': 'Weitere Kartenwerkzeuge',
        'es': 'Más herramientas del mapa',
        'fi': 'Lisää karttatyökaluja',
        'fr': "Plus d'outils de carte",
        'it': 'Altri strumenti mappa',
        'nb': 'Flere kartverktøy',
        'nl': 'Meer kaarthulpmiddelen',
        'sv': 'Fler kartverktyg',
        'zh-Hans': '更多地图工具',
    },
    'Name offline map': {
        'da': 'Navngiv offlinekort',
        'de': 'Offlinekarte benennen',
        'es': 'Nombrar mapa sin conexión',
        'fi': 'Nimeä offline-kartta',
        'fr': 'Nommer la carte hors ligne',
        'it': 'Assegna nome alla mappa offline',
        'nb': 'Gi offlinekartet navn',
        'nl': 'Offlinekaart benoemen',
        'sv': 'Namnge offlinekarta',
        'zh-Hans': '命名离线地图',
    },
    'OK': {
        'da': 'OK',
        'de': 'OK',
        'es': 'Aceptar',
        'fi': 'OK',
        'fr': 'OK',
        'it': 'OK',
        'nb': 'OK',
        'nl': 'OK',
        'sv': 'OK',
        'zh-Hans': '确定',
    },
    'Offline download': {
        'da': 'Offlinedownload',
        'de': 'Offline-Download',
        'es': 'Descarga sin conexión',
        'fi': 'Offline-lataus',
        'fr': 'Téléchargement hors ligne',
        'it': 'Download offline',
        'nb': 'Offlinenedlasting',
        'nl': 'Offline download',
        'sv': 'Offlinenedladdning',
        'zh-Hans': '离线下载',
    },
    'Open this guide again from About.': {
        'da': 'Åbn denne guide igen fra Om.',
        'de': 'Öffne diese Anleitung erneut über „Über die App“.',
        'es': 'Vuelve a abrir esta guía desde Acerca de.',
        'fi': 'Avaa tämä opas uudelleen kohdasta Tietoja.',
        'fr': 'Rouvrez ce guide depuis « À propos ».',
        'it': 'Riapri questa guida da Informazioni.',
        'nb': 'Åpne denne guiden på nytt fra Om.',
        'nl': 'Open deze handleiding opnieuw via Over de app.',
        'sv': 'Öppna den här guiden igen från Om.',
        'zh-Hans': '可从"关于"重新打开本指南。',
    },
    'Pause download': {
        'da': 'Sæt download på pause',
        'de': 'Download pausieren',
        'es': 'Pausar descarga',
        'fi': 'Keskeytä lataus',
        'fr': 'Suspendre le téléchargement',
        'it': 'Metti in pausa il download',
        'nb': 'Sett nedlasting på pause',
        'nl': 'Download onderbreken',
        'sv': 'Pausa nedladdning',
        'zh-Hans': '暂停下载',
    },
    'Paused': {
        'da': 'Sat på pause',
        'de': 'Pausiert',
        'es': 'En pausa',
        'fi': 'Keskeytetty',
        'fr': 'En pause',
        'it': 'In pausa',
        'nb': 'På pause',
        'nl': 'Onderbroken',
        'sv': 'Pausad',
        'zh-Hans': '已暂停',
    },
    'Resume download': {
        'da': 'Genoptag download',
        'de': 'Download fortsetzen',
        'es': 'Reanudar descarga',
        'fi': 'Jatka latausta',
        'fr': 'Reprendre le téléchargement',
        'it': 'Riprendi il download',
        'nb': 'Gjenoppta nedlasting',
        'nl': 'Download hervatten',
        'sv': 'Återuppta nedladdning',
        'zh-Hans': '继续下载',
    },
    'Save current route': {
        'da': 'Gem nuværende rute',
        'de': 'Aktuelle Route speichern',
        'es': 'Guardar ruta actual',
        'fi': 'Tallenna nykyinen reitti',
        'fr': "Enregistrer l'itinéraire actuel",
        'it': 'Salva percorso corrente',
        'nb': 'Lagre gjeldende rute',
        'nl': 'Huidige route opslaan',
        'sv': 'Spara aktuell rutt',
        'zh-Hans': '保存当前路线',
    },
    'Save place': {
        'da': 'Gem sted',
        'de': 'Ort speichern',
        'es': 'Guardar lugar',
        'fi': 'Tallenna paikka',
        'fr': 'Enregistrer le lieu',
        'it': 'Salva luogo',
        'nb': 'Lagre sted',
        'nl': 'Plaats opslaan',
        'sv': 'Spara plats',
        'zh-Hans': '保存地点',
    },
    'Saved pin': {
        'da': 'Gemt pin',
        'de': 'Gespeicherte Markierung',
        'es': 'Pin guardado',
        'fi': 'Tallennettu nasta',
        'fr': 'Repère enregistré',
        'it': 'Segnaposto salvato',
        'nb': 'Lagret pin',
        'nl': 'Opgeslagen pin',
        'sv': 'Sparad nål',
        'zh-Hans': '已保存的图钉',
    },
    'Share diagnostics': {
        'da': 'Del diagnosticering',
        'de': 'Diagnosedaten teilen',
        'es': 'Compartir diagnóstico',
        'fi': 'Jaa diagnostiikka',
        'fr': 'Partager le diagnostic',
        'it': 'Condividi diagnostica',
        'nb': 'Del diagnostikk',
        'nl': 'Diagnose delen',
        'sv': 'Dela diagnostik',
        'zh-Hans': '分享诊断信息',
    },
    'Show zoom level': {
        'da': 'Vis zoomniveau',
        'de': 'Zoomstufe anzeigen',
        'es': 'Mostrar nivel de zoom',
        'fi': 'Näytä zoomaustaso',
        'fr': 'Afficher le niveau de zoom',
        'it': 'Mostra livello di zoom',
        'nb': 'Vis zoomnivå',
        'nl': 'Zoomniveau tonen',
        'sv': 'Visa zoomnivå',
        'zh-Hans': '显示缩放级别',
    },
    'Tap the distance for the terrain profile.': {
        'da': 'Tryk på afstanden for at se terrænprofilen.',
        'de': 'Tippe auf die Entfernung, um das Geländeprofil zu sehen.',
        'es': 'Toca la distancia para ver el perfil del terreno.',
        'fi': 'Napauta etäisyyttä nähdäksesi maastoprofiilin.',
        'fr': 'Touchez la distance pour afficher le profil du terrain.',
        'it': 'Tocca la distanza per vedere il profilo del terreno.',
        'nb': 'Trykk på avstanden for å se terrengprofilen.',
        'nl': 'Tik op de afstand voor het terreinprofiel.',
        'sv': 'Tryck på avståndet för att se terrängprofilen.',
        'zh-Hans': '点按距离以查看地形剖面。',
    },
    'Track my location': {
        'da': 'Spor min placering',
        'de': 'Meinen Standort verfolgen',
        'es': 'Seguir mi ubicación',
        'fi': 'Seuraa sijaintiani',
        'fr': 'Suivre ma position',
        'it': 'Segui la mia posizione',
        'nb': 'Spor posisjonen min',
        'nl': 'Mijn locatie volgen',
        'sv': 'Spåra min plats',
        'zh-Hans': '追踪我的位置',
    },
    'Two fingers move and zoom.': {
        'da': 'To fingre flytter og zoomer.',
        'de': 'Zwei Finger bewegen und zoomen.',
        'es': 'Dos dedos mueven y hacen zoom.',
        'fi': 'Kaksi sormea liikuttaa ja zoomaa.',
        'fr': 'Deux doigts déplacent et zooment.',
        'it': 'Due dita spostano e zoomano.',
        'nb': 'To fingre flytter og zoomer.',
        'nl': 'Twee vingers bewegen en zoomen.',
        'sv': 'Två fingrar flyttar och zoomar.',
        'zh-Hans': '两指移动并缩放。',
    },
    'Undo last stroke': {
        'da': 'Fortryd sidste streg',
        'de': 'Letzten Strich rückgängig machen',
        'es': 'Deshacer último trazo',
        'fi': 'Kumoa viimeisin viiva',
        'fr': 'Annuler le dernier tracé',
        'it': 'Annulla ultimo tratto',
        'nb': 'Angre siste strek',
        'nl': 'Laatste lijn ongedaan maken',
        'sv': 'Ångra senaste sträck',
        'zh-Hans': '撤销最后一笔',
    },
}

_TRANSLITERATE = {"°": "deg", "–": "-", "—": "-", "'": "", "’": "", "…": ""}


def slugify(text: str) -> str:
    """Turns an English UI string into a stable, readable Android resource name."""
    s = text
    for src, dst in _TRANSLITERATE.items():
        s = s.replace(src, dst)
    s = s.lower()
    s = re.sub(r"[^a-z0-9]+", "_", s).strip("_")
    s = re.sub(r"_+", "_", s)
    if not s:
        s = "str"
    if s[0].isdigit():
        s = "n_" + s
    if len(s) > 60:
        # Cut on a word boundary rather than mid-word so long sentences still
        # produce a readable, memorable resource name.
        truncated = s[:60].rsplit("_", 1)[0]
        s = truncated or s[:60]
    return s.rstrip("_") or "str"


def build_key_map(keys: list[str]) -> dict[str, str]:
    """Key (English text) -> resource id, deduping any slug collisions."""
    used: dict[str, str] = {}
    result: dict[str, str] = {}
    for key in sorted(set(keys)):
        base = slugify(key)
        candidate = base
        n = 2
        while candidate in used:
            candidate = f"{base}_{n}"
            n += 1
        used[candidate] = key
        result[key] = candidate
    return result


def to_android_format(value: str) -> str:
    """iOS `%@` positional placeholders become Android's `%1$s`."""
    return value.replace("%@", "%1$s")


def escape(value: str) -> str:
    escaped = saxutils.escape(to_android_format(value))
    escaped = escaped.replace("'", "\\'").replace('"', '\\"')
    return escaped


def write_strings_xml(path: Path, entries: list[tuple[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = ["<resources>"]
    for name, value in entries:
        lines.append(f'    <string name="{name}">{escape(value)}</string>')
    lines.append("</resources>\n")
    path.write_text("\n".join(lines))


def main() -> None:
    key_map = build_key_map(KEYS)
    catalogue = json.loads(SOURCE.read_text())["strings"]

    # `app_name` is the brand name, not a translatable UI string, so it isn't
    # in KEYS -- but iOS does localize it (CFBundleDisplayName) for a few
    # languages, so source it from InfoPlist.xcstrings alongside the rest.
    app_name_localizations = (
        json.loads(APP_NAME_SOURCE.read_text())["strings"].get("CFBundleDisplayName", {}).get("localizations", {})
    )
    default_entries = [("app_name", "Fjällkartan")]
    default_entries += sorted((resource_id, key) for key, resource_id in key_map.items())
    write_strings_xml(RES / "values" / "strings.xml", default_entries)

    coverage: dict[str, int] = {key: 0 for key in KEYS}
    for language, qualifier in LANGUAGE_QUALIFIERS.items():
        entries = []
        app_name_value = app_name_localizations.get(language, {}).get("stringUnit", {}).get("value")
        if app_name_value:
            entries.append(("app_name", app_name_value))
        for key, resource_id in sorted(key_map.items(), key=lambda kv: kv[1]):
            unit = catalogue.get(key, {}).get("localizations", {}).get(language, {}).get("stringUnit", {})
            value = unit.get("value") or NEW_TRANSLATIONS.get(key, {}).get(language)
            if value:
                entries.append((resource_id, value))
                coverage[key] += 1
        write_strings_xml(RES / qualifier / "strings.xml", entries)

    untranslated = [key for key, count in coverage.items() if count == 0]
    print(f"Wrote defaults for {len(key_map)} strings to values/strings.xml")
    for language, qualifier in LANGUAGE_QUALIFIERS.items():
        translated = sum(
            1
            for key in KEYS
            if catalogue.get(key, {}).get("localizations", {}).get(language)
            or NEW_TRANSLATIONS.get(key, {}).get(language)
        )
        print(f"  {language:8s} -> {qualifier}: {translated}/{len(KEYS)} translated")
    if untranslated:
        print(f"\n{len(untranslated)} strings have NO translation in any language (falls back to English):")
        for key in sorted(untranslated):
            print(f"  - {key!r}")


if __name__ == "__main__":
    main()
