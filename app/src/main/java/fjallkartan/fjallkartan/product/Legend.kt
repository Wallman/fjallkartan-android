@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fjallkartan.fjallkartan.product

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    @param:StringRes val title: Int,
    val nativeName: String,
    val isLine: Boolean = false,
)

private data class LegendSection(@param:StringRes val title: Int, val entries: List<LegendEntry>)

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
        section(R.string.trails_and_routes,
            e(R.drawable.legend_se_trail_summer_marked, R.string.marked_hiking_trail, "Markerad vandringsled"),
            e(R.drawable.legend_se_trail_summer_winter_marked, R.string.marked_summer_and_winter_trail, "Markerad sommar- och vinterled"),
            e(R.drawable.legend_se_trail_winter_marked, R.string.marked_winter_trail, "Markerad vinterled"),
            e(R.drawable.legend_se_trail_summer_only_marked, R.string.marked_summer_trail, "Markerad sommarled"),
            e(R.drawable.legend_se_trail_recommended_unmarked, R.string.recommended_route_unmarked, "Lämplig färdväg, omarkerad"),
            e(R.drawable.legend_se_trail_poorly_marked, R.string.path_that_is_hard_to_follow, "Svårorienterad gångstig"),
            e(R.drawable.legend_se_trail_ski, R.string.ski_track, "Skidspår"),
            e(R.drawable.legend_se_trail_snowmobile, R.string.snowmobile_route, "Färdväg vid skoteråkning"),
            e(R.drawable.legend_se_trail_snowmobile_mandatory, R.string.mandatory_snowmobile_route, "Påbjuden färdväg vid skoteråkning"),
            e(R.drawable.legend_se_trail_reindeer_husbandry, R.string.reindeer_husbandry_route, "Rennäringsled"),
            e(R.drawable.legend_se_trail_boat, R.string.boat_route_rowing_route, "Trafikerad båtled, roddled"),
            e(R.drawable.legend_se_trail_boat_portage, R.string.boat_portage, "Båtdrag")),
        section(R.string.cabins_and_shelter,
            e(R.drawable.legend_se_mountain_lodge, R.string.mountain_lodge, "Fjällstation"),
            e(R.drawable.legend_se_tourist_hut, R.string.tourist_hut_overnight_hut, "Turiststuga, övernattningsstuga"),
            e(R.drawable.legend_se_solitary_cabin, R.string.solitary_mountain_cabin, "Enslig stuga i fjällen"),
            e(R.drawable.legend_se_rest_cabin, R.string.rest_cabin, "Raststuga"),
            e(R.drawable.legend_se_wind_shelter, R.string.wind_shelter, "Vindskydd"),
            e(R.drawable.legend_se_sami_cot, R.string.sami_hut, "Kåta"),
            e(R.drawable.legend_se_blast_shelter, R.string.blast_shelter, "Skyddsvärn")),
        section(R.string.facilities_and_crossings,
            e(R.drawable.legend_se_parking, R.string.parking, "Parkering"),
            e(R.drawable.legend_se_helipad, R.string.helicopter_pad, "Helikopterplats"),
            e(R.drawable.legend_se_emergency_phone, R.string.emergency_telephone, "Hjälptelefon"),
            e(R.drawable.legend_se_bridge, R.string.bridge, "Bro"),
            e(R.drawable.legend_se_ford, R.string.ford, "Vad")),
        section(R.string.fences_and_restrictions,
            e(R.drawable.legend_se_reindeer_fence, R.string.reindeer_fence, "Renstängsel"),
            e(R.drawable.legend_se_reindeer_corral, R.string.reindeer_corral, "Rengärde"),
            e(R.drawable.legend_se_area_no_tent_or_fire, R.string.camping_and_open_fires_prohibited, "Tält- och eldningsförbud")),
    )

    val norway = listOf(
        section(R.string.trails_and_routes,
            e(R.drawable.legend_no_trail_marked, R.string.marked_trail, "Merket sti"),
            e(R.drawable.legend_no_trail_unmarked, R.string.unmarked_trail, "Umerket sti"),
            e(R.drawable.legend_no_tractor_road, R.string.tractor_road_foot_and_cycle_path, "Traktorveg, gang- og sykkelveg"),
            e(R.drawable.legend_no_offroad_route, R.string.off_road_vehicle_route_summer, "Barmarksløype"),
            e(R.drawable.legend_no_floodlit_trail, R.string.floodlit_trail, "Lysløype"),
            e(R.drawable.legend_no_ski_lift, R.string.ski_lift, "Skitrekk")),
        section(R.string.cabins_and_shelter,
            e(R.drawable.legend_no_cabin_staffed, R.string.staffed_tourist_cabin, "Betjent turisthytte"),
            e(R.drawable.legend_no_cabin_self_service, R.string.self_service_tourist_cabin, "Selvbetjent turisthytte"),
            e(R.drawable.legend_no_cabin_unstaffed, R.string.unstaffed_tourist_cabin, "Ubetjent turisthytte"),
            e(R.drawable.legend_no_rest_cabin, R.string.rest_cabin, "Rastebu"),
            e(R.drawable.legend_no_lean_to, R.string.lean_to_shelter, "Gapahuk")),
        section(R.string.facilities_and_crossings,
            e(R.drawable.legend_no_campsite, R.string.campsite, "Campingplass"),
            e(R.drawable.legend_no_parking, R.string.parking, "Parkering"),
            e(R.drawable.legend_no_helipad, R.string.large_helicopter_landing_site, "Stor helikopterlandingsplass"),
            e(R.drawable.legend_no_pier, R.string.pier_and_jetty, "Kai og brygge")),
        section(R.string.fences_and_restrictions,
            e(R.drawable.legend_no_reindeer_fence, R.string.reindeer_fence, "Reingjerde")),
    )

    private fun e(drawable: Int, @StringRes title: Int, nativeName: String) =
        LegendEntry(drawable, title, nativeName, isLine = drawable in lineDrawables)

    private fun section(@StringRes title: Int, vararg entries: LegendEntry) =
        LegendSection(title, entries.toList())
}

@Composable
fun LegendSheet(onDismiss: () -> Unit) {
    var country by remember { mutableStateOf(LegendCountry.Sweden) }
    var query by remember { mutableStateOf("") }
    val sections = when (country) {
        LegendCountry.Sweden -> LegendCatalog.sweden
        LegendCountry.Norway -> LegendCatalog.norway
    }
    // Resolved via stringResource (not LocalContext.current.getString) so the
    // search index stays configuration-aware if the locale changes.
    val titleLookup = sections.flatMap { it.entries }.associate { it.title to stringResource(it.title) }
    val filtered = sections.mapNotNull { section ->
        val entries = section.entries.filter {
            query.isBlank() ||
                titleLookup.getValue(it.title).contains(query, ignoreCase = true) ||
                it.nativeName.contains(query, ignoreCase = true)
        }
        section.takeIf { entries.isNotEmpty() }?.copy(entries = entries)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(bottom = 12.dp)) {
            // Segmented country switch on the left, "Klar" dismiss button on the right --
            // mirrors the iOS navigation bar's principal picker + confirmation action.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountrySwitch(country = country, onSelect = { country = it })
                TextButton(onClick = onDismiss, shape = CircleShape) {
                    Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
                }
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                filtered.forEach { section ->
                    item {
                        SectionHeader(stringResource(section.title))
                        LegendCard {
                            section.entries.forEachIndexed { index, entry ->
                                LegendRow(entry = entry, title = stringResource(entry.title))
                                if (index != section.entries.lastIndex) LegendDivider()
                            }
                        }
                    }
                }
                if (query.isBlank()) {
                    item { SlopeLegend(country) }
                }
            }
            Spacer(Modifier.height(4.dp))
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.find_a_symbol),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private val LegendCountry.labelRes: Int
    @StringRes get() = when (this) {
        LegendCountry.Sweden -> R.string.sweden
        LegendCountry.Norway -> R.string.norway
    }

@Composable
private fun CountrySwitch(country: LegendCountry, onSelect: (LegendCountry) -> Unit) {
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
                    stringResource(option.labelRes),
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
private fun SlopeLegend(country: LegendCountry) {
    val bands = listOf(
        Color(0xFFFFFF00) to R.string.n_30_35deg,
        Color(0xFFFFAA00) to R.string.n_35_40deg,
        Color(0xFFFF5500) to R.string.n_40_45deg,
        Color(0xFFFF0000) to R.string.n_45_50deg,
        Color(0xFF730000) to R.string.n_50deg_and_steeper,
    ) + if (country == LegendCountry.Norway) {
        listOf(Color(0xFF4C9BFF) to R.string.modelled_avalanche_runout)
    } else {
        emptyList()
    }

    SectionHeader(stringResource(R.string.steepness))
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
                headlineContent = { Text(stringResource(label)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            if (index != bands.lastIndex) LegendDivider()
        }
    }
}
