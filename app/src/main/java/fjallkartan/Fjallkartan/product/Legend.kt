@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fjallkartan.fjallkartan.product

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fjallkartan.fjallkartan.R

enum class LegendCountry { Sweden, Norway }

data class LegendEntry(
    @param:DrawableRes val drawable: Int,
    val title: String,
    val nativeName: String,
)

private data class LegendSection(val title: String, val entries: List<LegendEntry>)

private object LegendCatalog {
    val sweden = listOf(
        section("Trails and routes",
            e(R.drawable.legend_se_trail_summer_marked, "Marked hiking trail", "Markerad vandringsled"),
            e(R.drawable.legend_se_trail_summer_winter_marked, "Marked summer and winter trail", "Markerad sommar- och vinterled"),
            e(R.drawable.legend_se_trail_winter_marked, "Marked winter trail", "Markerad vinterled"),
            e(R.drawable.legend_se_trail_summer_only_marked, "Marked summer trail", "Markerad sommarled"),
            e(R.drawable.legend_se_trail_recommended_unmarked, "Recommended route, unmarked", "Lämplig färdväg, omarkerad"),
            e(R.drawable.legend_se_trail_poorly_marked, "Path that is hard to follow", "Svårorienterad gångstig"),
            e(R.drawable.legend_se_trail_ski, "Ski track", "Skidspår"),
            e(R.drawable.legend_se_trail_snowmobile, "Snowmobile route", "Färdväg vid skoteråkning"),
            e(R.drawable.legend_se_trail_snowmobile_mandatory, "Mandatory snowmobile route", "Påbjuden färdväg vid skoteråkning"),
            e(R.drawable.legend_se_trail_reindeer_husbandry, "Reindeer husbandry route", "Rennäringsled"),
            e(R.drawable.legend_se_trail_boat, "Boat route, rowing route", "Trafikerad båtled, roddled"),
            e(R.drawable.legend_se_trail_boat_portage, "Boat portage", "Båtdrag")),
        section("Cabins and shelter",
            e(R.drawable.legend_se_mountain_lodge, "Mountain lodge", "Fjällstation"),
            e(R.drawable.legend_se_tourist_hut, "Tourist hut, overnight hut", "Turiststuga, övernattningsstuga"),
            e(R.drawable.legend_se_solitary_cabin, "Solitary mountain cabin", "Enslig stuga i fjällen"),
            e(R.drawable.legend_se_rest_cabin, "Rest cabin", "Raststuga"),
            e(R.drawable.legend_se_wind_shelter, "Wind shelter", "Vindskydd"),
            e(R.drawable.legend_se_sami_cot, "Sami hut", "Kåta"),
            e(R.drawable.legend_se_blast_shelter, "Blast shelter", "Skyddsvärn")),
        section("Facilities and crossings",
            e(R.drawable.legend_se_parking, "Parking", "Parkering"),
            e(R.drawable.legend_se_helipad, "Helicopter pad", "Helikopterplats"),
            e(R.drawable.legend_se_emergency_phone, "Emergency telephone", "Hjälptelefon"),
            e(R.drawable.legend_se_bridge, "Bridge", "Bro"),
            e(R.drawable.legend_se_ford, "Ford", "Vad")),
        section("Fences and restrictions",
            e(R.drawable.legend_se_reindeer_fence, "Reindeer fence", "Renstängsel"),
            e(R.drawable.legend_se_reindeer_corral, "Reindeer corral", "Rengärde"),
            e(R.drawable.legend_se_area_no_tent_or_fire, "Camping and open fires prohibited", "Tält- och eldningsförbud")),
    )

    val norway = listOf(
        section("Trails and routes",
            e(R.drawable.legend_no_trail_marked, "Marked trail", "Merket sti"),
            e(R.drawable.legend_no_trail_unmarked, "Unmarked trail", "Umerket sti"),
            e(R.drawable.legend_no_tractor_road, "Tractor road, foot and cycle path", "Traktorveg, gang- og sykkelveg"),
            e(R.drawable.legend_no_offroad_route, "Off-road vehicle route, summer", "Barmarksløype"),
            e(R.drawable.legend_no_floodlit_trail, "Floodlit trail", "Lysløype"),
            e(R.drawable.legend_no_ski_lift, "Ski lift", "Skitrekk")),
        section("Cabins and shelter",
            e(R.drawable.legend_no_cabin_staffed, "Staffed tourist cabin", "Betjent turisthytte"),
            e(R.drawable.legend_no_cabin_self_service, "Self-service tourist cabin", "Selvbetjent turisthytte"),
            e(R.drawable.legend_no_cabin_unstaffed, "Unstaffed tourist cabin", "Ubetjent turisthytte"),
            e(R.drawable.legend_no_rest_cabin, "Rest cabin", "Rastebu"),
            e(R.drawable.legend_no_lean_to, "Lean-to shelter", "Gapahuk")),
        section("Facilities and crossings",
            e(R.drawable.legend_no_campsite, "Campsite", "Campingplass"),
            e(R.drawable.legend_no_parking, "Parking", "Parkering"),
            e(R.drawable.legend_no_helipad, "Large helicopter landing site", "Stor helikopterlandingsplass"),
            e(R.drawable.legend_no_pier, "Pier and jetty", "Kai og brygge")),
        section("Fences and restrictions",
            e(R.drawable.legend_no_reindeer_fence, "Reindeer fence", "Reingjerde")),
    )

    private fun e(drawable: Int, title: String, nativeName: String) =
        LegendEntry(drawable, title, nativeName)

    private fun section(title: String, vararg entries: LegendEntry) =
        LegendSection(title, entries.toList())
}

@Composable
fun LegendSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val localizer = remember { Localizer.get(context) }
    val locale = LocalConfiguration.current.locales[0]
    fun text(key: String) = localizer.text(key, locale)
    var country by remember { mutableStateOf(LegendCountry.Sweden) }
    var query by remember { mutableStateOf("") }
    val sections = when (country) {
        LegendCountry.Sweden -> LegendCatalog.sweden
        LegendCountry.Norway -> LegendCatalog.norway
    }
    val filtered = sections.mapNotNull { section ->
        val entries = section.entries.filter {
            query.isBlank() ||
                it.title.contains(query, ignoreCase = true) ||
                it.nativeName.contains(query, ignoreCase = true)
        }
        section.takeIf { entries.isNotEmpty() }?.copy(entries = entries)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(text("Legend"), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendCountry.entries.forEach {
                    FilterChip(
                        selected = country == it,
                        onClick = { country = it },
                        label = { Text(text(it.name)) },
                    )
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(text("Find a symbol")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(Modifier.heightIn(max = 620.dp)) {
                filtered.forEach { section ->
                    item { Text(text(section.title), modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) }
                    items(section.entries, key = LegendEntry::drawable) { entry ->
                        ListItem(
                            leadingContent = {
                                Image(
                                    painterResource(entry.drawable),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.width(64.dp).heightIn(min = 36.dp, max = 52.dp),
                                )
                            },
                            headlineContent = { Text(text(entry.title)) },
                            supportingContent = { Text(entry.nativeName) },
                        )
                    }
                }
                if (query.isBlank()) {
                    item { SlopeLegend(country, ::text) }
                }
            }
        }
    }
}

@Composable
private fun SlopeLegend(country: LegendCountry, text: (String) -> String) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Text(text("Steepness"), style = MaterialTheme.typography.titleMedium)
        listOf(
            0xFFFFFF00 to "30–35°",
            0xFFFFAA00 to "35–40°",
            0xFFFF5500 to "40–45°",
            0xFFFF0000 to "45–50°",
            0xFF730000 to "50° and steeper",
        ).forEach { (colour, label) ->
            Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.Canvas(Modifier.width(40.dp).heightIn(min = 20.dp)) {
                    drawRect(androidx.compose.ui.graphics.Color(colour))
                }
                Text(text(label))
            }
        }
        if (country == LegendCountry.Norway) Text(text("Blue: modelled avalanche runout"))
    }
}
