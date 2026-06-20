package xyz.mpv.rex.cinemine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import xyz.mpv.rex.cinemine.data.CineMineRepo
import xyz.mpv.rex.cinemine.data.CineMineStreamResolver
import xyz.mpv.rex.features.cinehub.model.TvShowItem
import xyz.mpv.rex.features.cinehub.model.EpisodeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvShowDetailSheet(
    show: TvShowItem,
    onDismiss: () -> Unit,
    onPlayRequested: (streamUrl: String, title: String) -> Unit[span_3](start_span)[span_3](end_span)[span_4](start_span)[span_4](end_span)
) {
    val scope = rememberCoroutineScope()
    var episodesList by remember { mutableStateOf(emptyList<EpisodeItem>()) }
    var isLoadingEpisodes by remember { mutableStateOf(true) }

    // Grouping mapping context: Key = Season Number, Value = List of Episodes[span_5](start_span)[span_5](end_span)
    val groupedSeasons = remember(episodesList) {
        episodesList.groupBy { it.season }.toSortedMap()[span_6](start_span)[span_6](end_span)
    }
    
    // Tracks active season selection state[span_7](start_span)[span_7](end_span)
    var selectedSeasonTab by remember { mutableStateOf<Int?>(null) }

    // Fetch episodes dynamically using the self-contained path
    LaunchedEffect(show.folderPath) {
        isLoadingEpisodes = true
        episodesList = CineMineRepo.fetchLocalEpisodes(show.folderPath)
        isLoadingEpisodes = false
        
        // Auto-select first available season token once data sets arrive
        if (episodesList.isNotEmpty()) {
            selectedSeasonTab = episodesList.map { it.season }.minOrNull() ?: 1
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),[span_8](start_span)[span_8](end_span)
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp, top = 8.dp)[span_9](start_span)[span_9](end_span)
        ) {
            // ================= HEADER INFRASTRUCTURE =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = show.posterPath ?: android.R.drawable.ic_menu_gallery,[span_10](start_span)[span_10](end_span)
                    contentDescription = show.title,
                    modifier = Modifier
                        .width(90.dp)
                        .aspectRatio(2f / 3f)[span_11](start_span)[span_11](end_span)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray.copy(alpha = 0.2f)),[span_12](start_span)[span_12](end_span)
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))[span_13](start_span)[span_13](end_span)
                Column(modifier = Modifier.weight(1f)) {[span_14](start_span)[span_14](end_span)
                    Text(
                        text = show.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Studio: ${show.studio.ifBlank { "Unknown" }} | Genre: ${show.genre.ifBlank { "Series" }}",[span_15](start_span)[span_15](end_span)
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    if (show.userRating > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "★ ${show.getFormattedRating()}",[span_16](start_span)[span_16](end_span)
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))[span_17](start_span)[span_17](end_span)
            Text(
                text = show.plot.ifBlank { "No show summary description available." },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // ================= SEASON SELECTOR TABS =================
            if (groupedSeasons.keys.size > 1 && selectedSeasonTab != null) {[span_18](start_span)[span_18](end_span)
                Spacer(modifier = Modifier.height(16.dp))
                ScrollableTabRow(
                    selectedTabIndex = groupedSeasons.keys.indexOf(selectedSeasonTab).coerceAtLeast(0),[span_19](start_span)[span_19](end_span)
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp,[span_20](start_span)[span_20](end_span)
                    divider = {}
                ) {
                    groupedSeasons.keys.forEach { seasonNum ->
                        Tab(
                            selected = selectedSeasonTab == seasonNum,[span_21](start_span)[span_21](end_span)
                            onClick = { selectedSeasonTab = seasonNum },[span_22](start_span)[span_22](end_span)
                            text = { 
                                Text(
                                    text = "Season $seasonNum", 
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                ) 
                            }[span_23](start_span)[span_23](end_span)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))[span_24](start_span)[span_24](end_span)

            // ================= EPISODES LAZY CONTAINER =================
            if (isLoadingEpisodes) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            } else if (episodesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No local media video tracks detected inside this folder index.", fontSize = 12.sp, color = Color.Gray)[span_25](start_span)[span_25](end_span)
                }
            } else {
                val activeSeasonEpisodes = groupedSeasons[selectedSeasonTab] ?: emptyList()
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)[span_26](start_span)[span_26](end_span)
                ) {
                    items(activeSeasonEpisodes) { episode ->
                        EpisodeItemRow(
                            episodeCode = episode.getEpisodeCode(),[span_27](start_span)[span_27](end_span)
                            title = episode.title,
                            plot = episode.plot,
                            onPlayClick = {
                                scope.launch {
                                    // Resolves internal parameters hash securely before popping intent[span_28](start_span)[span_28](end_span)
                                    val streamLink = CineMineStreamResolver.resolvePlaybackUrl(episode.videoFilePath)
                                    onPlayRequested(streamLink, "${show.title} - ${episode.getEpisodeCode()}")[span_29](start_span)[span_29](end_span)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
