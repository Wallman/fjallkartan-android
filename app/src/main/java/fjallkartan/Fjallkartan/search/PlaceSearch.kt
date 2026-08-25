package fjallkartan.fjallkartan.search

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import java.io.File

data class PlaceResult(
    val id: Long,
    val name: String,
    val kind: Int,
    val matchedAlias: String?,
    val municipality: String?,
    val region: String?,
    val country: Int,
    val coordinate: GeoCoordinate,
) {
    val subtitle: String
        get() = listOfNotNull(municipality, region).distinct().joinToString(", ")
}

class PlaceSearch(context: Context) {
    private val database: SQLiteDatabase

    init {
        val file = File(context.noBackupFilesDir, "places.sqlite")
        val installedVersion = if (file.exists()) {
            runCatching {
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use {
                    it.version
                }
            }.getOrDefault(0)
        } else {
            0
        }
        if (installedVersion != DATABASE_VERSION) {
            val temporary = File(file.parentFile, "${file.name}.tmp")
            context.assets.open("places.sqlite").use { input ->
                temporary.outputStream().use(input::copyTo)
            }
            if (!temporary.renameTo(file)) {
                temporary.delete()
                error("Could not install places database")
            }
        }
        database = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        database.rawQuery("PRAGMA mmap_size=20971520", null).use { it.moveToFirst() }
    }

    fun search(text: String, limit: Int = 40): List<PlaceResult> {
        val expression = ftsExpression(text) ?: return emptyList()
        val needleLength = text.trim().toCharArray().size
        return database.rawQuery(
            SEARCH_SQL,
            arrayOf(expression, needleLength.toString(), SCAN_LIMIT.toString(), limit.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val isPrimary = cursor.getInt(2) == 1
                    val matched = cursor.getString(1)
                    val displayName = cursor.getString(9) ?: matched.orEmpty()
                    add(
                        PlaceResult(
                            id = cursor.getLong(0),
                            name = displayName,
                            kind = cursor.getInt(3),
                            matchedAlias = if (isPrimary) null else matched,
                            municipality = cursor.getString(7),
                            region = cursor.getString(8),
                            country = cursor.getInt(6),
                            coordinate = GeoCoordinate(
                                cursor.getLong(4) / COORDINATE_SCALE,
                                cursor.getLong(5) / COORDINATE_SCALE,
                            ),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        private const val COORDINATE_SCALE = 100_000.0
        private const val SCAN_LIMIT = 60_000
        private const val DATABASE_VERSION = 4

        fun ftsExpression(text: String): String? {
            val terms = text.lowercase()
                .split(Regex("""[^\p{L}\p{N}]+"""))
                .filter(String::isNotBlank)
                .take(6)
            return terms.takeIf(List<String>::isNotEmpty)?.joinToString(" AND ") { "$it*" }
        }

        private const val SEARCH_SQL = """
            WITH hit AS (
                SELECT p.id AS place_id,
                       COALESCE(a.name, p.name) AS matched,
                       a.id IS NULL AS is_primary,
                       p.kind AS kind,
                       (CASE WHEN length(COALESCE(a.name, p.name)) = ?2 THEN 0 ELSE 100 END)
                       + p.rank * 5
                       + CASE p.kind
                           WHEN 3 THEN 0.5 WHEN 4 THEN 1.5 WHEN 7 THEN 1.0
                           WHEN 8 THEN 2.0 WHEN 9 THEN 3.0 ELSE 0
                         END
                       + CASE WHEN a.id IS NULL THEN 0 ELSE 2 END AS score
                FROM place_fts f
                LEFT JOIN alias a ON a.id = f.rowid
                JOIN place p ON p.id = COALESCE(a.place_id, f.rowid)
                WHERE f.place_fts MATCH ?1
                LIMIT ?3
            ),
            best AS (
                SELECT place_id, matched, is_primary, kind, MIN(score) AS score
                FROM hit
                GROUP BY place_id
                ORDER BY score
                LIMIT ?4
            )
            SELECT b.place_id, b.matched, b.is_primary, b.kind,
                   p.lat, p.lon, p.country, m.name, m.region, p.name
            FROM best b
            JOIN place p ON p.id = b.place_id
            LEFT JOIN municipality m ON m.id = p.muni
            ORDER BY b.score
        """
    }
}
