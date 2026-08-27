@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fjallkartan.fjallkartan.product

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fjallkartan.fjallkartan.R

/// The paper tone both agencies print their topographic maps on, used as the
/// background behind every symbol chip so line colours read the same way
/// they do on the map itself.
private val LegendPaper = Color(0xFFFAF7F0)

enum class LegendCountry { Sweden, Norway }


data class LegendEntry(
    @param:DrawableRes val drawable: Int,
    val title: String,
    val nativeName: String,
    val isLine: Boolean = false,
)

private data class LegendSection(val title: String, val entries: List<LegendEntry>)

private object LegendCatalog {
    /// Symbols wider than they are tall are lines and get a wide chip; point
    /// symbols get a square chip so they are not stretched across it.
    private val lineDrawables = setOf(
        R.drawable.legend_se_trail_summer_marked, R.drawable.legend_se_trail_summer_winter_marked,
        R.drawable.legend_se_trail_winter_marked, R.drawable.legend_se_trail_summer_only_marked,
        R.drawable.legend_se_trail_recommended_unmarked, R.drawable.legend_se_trail_poorly_marked,
        R.drawable.legend_se_trail_ski, R.drawable.legend_se_trail_snowmobile,
        R.drawable.legend_se_trail_snowmobile_mandatory, R.drawable.legend_se_trail_reindeer_husbandry,
        R.drawable.legend_se_trail_boat, R.drawable.legend_se_trail_boat_portage,
        R.drawable.legend_se_reindeer_fence,
        R.drawable.legend_no_trail_marked, R.drawable.legend_no_trail_unmarked, R.drawable.legend_no_tractor_road,
        R.drawable.legend_no_offroad_route, R.drawable.legend_no_floodlit_trail, R.drawable.legend_no_ski_lift,
        R.drawable.legend_no_reindeer_fence, R.drawable.legend_no_pier,
    )

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
        LegendEntry(drawable, title, nativeName, isLine = drawable in lineDrawables)

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
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            // Segmented country switch on the left, "Klar" dismiss button on the right --
            // mirrors the iOS navigation bar's principal picker + confirmation action.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountrySwitch(country = country, onSelect = { country = it }, text = ::text)
                TextButton(onClick = onDismiss, shape = CircleShape) {
                    Text(text("Done"), fontWeight = FontWeight.Bold)
                }
            }
            LazyColumn(Modifier.heightIn(max = 620.dp).padding(horizontal = 16.dp)) {
                filtered.forEach { section ->
                    item {
                        SectionHeader(text(section.title))
                        LegendCard {
                            section.entries.forEachIndexed { index, entry ->
                                LegendRow(entry = entry, title = text(entry.title))
                                if (index != section.entries.lastIndex) LegendDivider()
                            }
                        }
                    }
                }
                if (query.isBlank()) {
                    item { SlopeLegend(country, ::text) }
                }
            }
            Spacer(Modifier.height(4.dp))
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = text("Find a symbol"),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun CountrySwitch(country: LegendCountry, onSelect: (LegendCountry) -> Unit, text: (String) -> String) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(3.dp),
    ) {
        LegendCountry.entries.forEach { option ->
            val selected = option == country
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text(option.name),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp),
    )
}

@Composable
private fun LegendCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
        content = content,
    )
}

@Composable
private fun LegendDivider() {
    HorizontalDivider(
        Modifier.padding(start = 92.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun LegendRow(entry: LegendEntry, title: String) {
    ListItem(
        leadingContent = { LegendChip(entry.drawable, entry.isLine) },
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                entry.nativeName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/// The symbol on a light "map paper" background regardless of theme -- a
/// near-white line or a white-backed hut icon would vanish on a plain row.
@Composable
private fun LegendChip(@DrawableRes drawable: Int, isLine: Boolean) {
    val height = 44.dp
    val width = if (isLine) height * 1.7f else height
    Image(
        painterResource(drawable),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(6.dp))
            .background(LegendPaper)
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(6.dp))
            .padding(3.dp),
    )
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
    )
}

@Composable
private fun SlopeLegend(country: LegendCountry, text: (String) -> String) {
    val bands = listOf(
        Color(0xFFFFFF00) to "30–35°",
        Color(0xFFFFAA00) to "35–40°",
        Color(0xFFFF5500) to "40–45°",
        Color(0xFFFF0000) to "45–50°",
        Color(0xFF730000) to "50° and steeper",
    ) + if (country == LegendCountry.Norway) {
        listOf(Color(0xFF4C9BFF) to "Modelled avalanche runout")
    } else {
        emptyList()
    }

    SectionHeader(text("Steepness"))
    LegendCard {
        bands.forEachIndexed { index, (colour, label) ->
            ListItem(
                leadingContent = {
                    Box(
                        Modifier
                            .size(width = 74.dp, height = 44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colour)
                            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(6.dp)),
                    )
                },
                headlineContent = { Text(text(label)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            if (index != bands.lastIndex) LegendDivider()
        }
    }
}
