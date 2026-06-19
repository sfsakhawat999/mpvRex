package xyz.mpv.rex.ui.player.controls.components.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.R
import xyz.mpv.rex.repository.wyzie.WyzieSubtitle
import xyz.mpv.rex.ui.theme.spacing
import xyz.mpv.rex.utils.media.MediaInfoParser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed class OnlineSubtitleItem {
  data class OnlineTrack(val subtitle: WyzieSubtitle) : OnlineSubtitleItem()
  data class Header(val title: String) : OnlineSubtitleItem()
  object Divider : OnlineSubtitleItem()
}

@Composable
fun OnlineSubtitleSearchSheet(
  onDismissRequest: () -> Unit,
  onDownloadOnline: (WyzieSubtitle) -> Unit,
  isSearching: Boolean = false,
  isDownloading: Boolean = false,
  searchResults: ImmutableList<WyzieSubtitle> = emptyList<WyzieSubtitle>().toImmutableList(),
  isOnlineSectionExpanded: Boolean = true,
  onToggleOnlineSection: () -> Unit = {},
  modifier: Modifier = Modifier,
  mediaTitle: String = "",
  // Autocomplete & Series Selection
  mediaSearchResults: ImmutableList<xyz.mpv.rex.repository.wyzie.WyzieTmdbResult> = emptyList<xyz.mpv.rex.repository.wyzie.WyzieTmdbResult>().toImmutableList(),
  isSearchingMedia: Boolean = false,
  onSearchMedia: (String) -> Unit = {},
  onSelectMedia: (xyz.mpv.rex.repository.wyzie.WyzieTmdbResult) -> Unit = {},
  selectedTvShow: xyz.mpv.rex.repository.wyzie.WyzieTvShowDetails? = null,
  isFetchingTvDetails: Boolean = false,
  selectedSeason: xyz.mpv.rex.repository.wyzie.WyzieSeason? = null,
  onSelectSeason: (xyz.mpv.rex.repository.wyzie.WyzieSeason) -> Unit = {},
  seasonEpisodes: ImmutableList<xyz.mpv.rex.repository.wyzie.WyzieEpisode> = emptyList<xyz.mpv.rex.repository.wyzie.WyzieEpisode>().toImmutableList(),
  isFetchingEpisodes: Boolean = false,
  selectedEpisode: xyz.mpv.rex.repository.wyzie.WyzieEpisode? = null,
  onSelectEpisode: (xyz.mpv.rex.repository.wyzie.WyzieEpisode) -> Unit = {},
  onClearMediaSelection: () -> Unit = {}
) {
  val items = remember(searchResults, isSearching, isOnlineSectionExpanded) {
    val list = mutableListOf<OnlineSubtitleItem>()
    
    // Online Search Results section
    if (searchResults.isNotEmpty() || isSearching) {
        list.add(OnlineSubtitleItem.Header("Online Results (${searchResults.size})"))
        if (isOnlineSectionExpanded) {
            list.addAll(searchResults.map { OnlineSubtitleItem.OnlineTrack(it) })
        }
    }

    list.toImmutableList()
  }

  GenericTracksSheet(
    tracks = items,
    onDismissRequest = onDismissRequest,
    header = {
      val keyboardController = LocalSoftwareKeyboardController.current
      val mediaInfo = remember(mediaTitle) { MediaInfoParser.parse(mediaTitle) }
      var searchQuery by remember { mutableStateOf(mediaInfo.title) }
      var showAutocomplete by remember { mutableStateOf(false) }

      // Build the detected info string for display
      val detectedInfo = remember(mediaInfo) {
        buildString {
          append(mediaInfo.title)
          if (mediaInfo.season != null || mediaInfo.episode != null) {
            append(" • ")
            if (mediaInfo.season != null) append("S${String.format("%02d", mediaInfo.season)}")
            if (mediaInfo.episode != null) append("E${String.format("%02d", mediaInfo.episode)}")
          }
          mediaInfo.year?.let { append(" ($it)") }
        }
      }

      // Auto-trigger search on open
      LaunchedEffect(mediaInfo) {
        if (mediaInfo.title.isNotBlank()) {
          onSearchMedia(mediaInfo.title)
        }
      }

      Column(
        modifier = Modifier
            .padding(top = MaterialTheme.spacing.medium)
            .fillMaxWidth()
    ) {
        if (detectedInfo.isNotBlank() && mediaInfo.title.isNotBlank()) {
          Row(
            modifier = Modifier
              .padding(horizontal = MaterialTheme.spacing.medium, vertical = 4.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
              .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.AutoFixHigh,
              contentDescription = null,
              modifier = Modifier.size(12.dp),
              tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = detectedInfo,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              modifier = Modifier.basicMarquee()
            )
          }
        }

        TextField(
          value = searchQuery,
          onValueChange = { 
            searchQuery = it
            showAutocomplete = true
          },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
          placeholder = { Text(stringResource(R.string.pref_subtitles_search_online)) },
          leadingIcon = {
            IconButton(onClick = { 
              searchQuery = mediaInfo.title
              showAutocomplete = false 
              onSearchMedia(mediaInfo.title)
              keyboardController?.hide()
            }) {
              Icon(Icons.Default.Refresh, contentDescription = "Reset Search", tint = MaterialTheme.colorScheme.primary)
            }
          },
          trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (isSearching || isDownloading || isSearchingMedia) {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                  Spacer(Modifier.width(8.dp))
              }
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { 
                  searchQuery = ""
                  showAutocomplete = false
                  onClearMediaSelection()
                }) {
                  Icon(Icons.Default.Close, null)
                }
              }
              IconButton(onClick = { 
                val q = searchQuery.ifBlank { mediaInfo.title }
                searchQuery = q
                onSearchMedia(q)
                showAutocomplete = false
                keyboardController?.hide()
              }) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
              }
            }
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = {
            val q = searchQuery.ifBlank { mediaInfo.title }
            searchQuery = q
            onSearchMedia(q)
            showAutocomplete = false
            keyboardController?.hide()
          }),
          shape = RoundedCornerShape(24.dp),
          colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          )
        )

        // Autocomplete Results
        AnimatedVisibility(
          visible = showAutocomplete && mediaSearchResults.isNotEmpty(),
          enter = expandVertically(),
          exit = shrinkVertically()
      ) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = MaterialTheme.spacing.medium)
              .padding(bottom = 8.dp)
              .heightIn(max = 190.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {                        
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                mediaSearchResults.forEachIndexed { index, result ->
                TmdbResultRow(
                  result = result,
                  onClick = { 
                    searchQuery = result.title
                    onSelectMedia(result)
                    showAutocomplete = false
                    keyboardController?.hide()
                  }
                )
                if (index < mediaSearchResults.size - 1) {
                  HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
              }
            }
          }
        }

        // Series / Season / Episode Selection UI
        if (selectedTvShow != null) {
          SeriesDetailsSection(
            tvShow = selectedTvShow,
            isFetchingSeasons = isFetchingTvDetails,
            selectedSeason = selectedSeason,
            onSelectSeason = onSelectSeason,
            isFetchingEpisodes = isFetchingEpisodes,
            episodes = seasonEpisodes,
            selectedEpisode = selectedEpisode,
            onSelectEpisode = onSelectEpisode,
            onClose = onClearMediaSelection
          )
            
            Spacer(modifier = Modifier.height(16.dp)) 
       }
        if (isSearching) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    },
    track = { item ->
      when (item) {
        is OnlineSubtitleItem.OnlineTrack -> {
            WyzieSubtitleRow(
                subtitle = item.subtitle,
                onDownload = { onDownloadOnline(item.subtitle) }
            )
        }
        is OnlineSubtitleItem.Header -> {
            val isOnlineHeader = item.title.startsWith("Online Results")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isOnlineHeader) Modifier.clickable { onToggleOnlineSection() } else Modifier)
                    .padding(horizontal = MaterialTheme.spacing.medium, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                if (isOnlineHeader) {
                    Icon(
                        imageVector = if (isOnlineSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        OnlineSubtitleItem.Divider -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
      }
    },
    modifier = modifier,
  )
}

@Composable
fun WyzieSubtitleRow(
    subtitle: WyzieSubtitle,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
          .fillMaxWidth()
          .clickable { onDownload() }
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Language Badge
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center
      ) {
        Text(
          text = subtitle.displayLanguage.take(2).uppercase(),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = subtitle.displayName,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(text = subtitle.displayLanguage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
          subtitle.source?.let {
          Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
          Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        subtitle.format?.let {
            Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text(text = it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
      
      IconButton(
        onClick = onDownload,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
      ) {
        Icon(
          imageVector = Icons.Default.Download, 
          contentDescription = null, 
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(18.dp)
        )
      }
    }
}

@Composable
fun TmdbResultRow(
    result: xyz.mpv.rex.repository.wyzie.WyzieTmdbResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
              text = result.title,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${result.mediaType.uppercase()} ${result.releaseYear ?: ""}".trim(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SeriesDetailsSection(
    tvShow: xyz.mpv.rex.repository.wyzie.WyzieTvShowDetails,
    isFetchingSeasons: Boolean,
    selectedSeason: xyz.mpv.rex.repository.wyzie.WyzieSeason?,
    onSelectSeason: (xyz.mpv.rex.repository.wyzie.WyzieSeason) -> Unit,
    isFetchingEpisodes: Boolean,
    episodes: ImmutableList<xyz.mpv.rex.repository.wyzie.WyzieEpisode>,
    selectedEpisode: xyz.mpv.rex.repository.wyzie.WyzieEpisode?,
    onSelectEpisode: (xyz.mpv.rex.repository.wyzie.WyzieEpisode) -> Unit,
    onClose: () -> Unit
) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = MaterialTheme.spacing.medium)
        .padding(bottom = MaterialTheme.spacing.small),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = tvShow.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )

        // Season Dropdown styled as a minimal chip
        val seasonDropdownExpanded = remember { mutableStateOf(false) }
        Box {
            Surface(
              onClick = { seasonDropdownExpanded.value = true },
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.height(32.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp)
              ) {
                Text(
                  text = selectedSeason?.let { "S${it.season_number}" } ?: "Season",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
              }
            }
            DropdownMenu(
              expanded = seasonDropdownExpanded.value,
              onDismissRequest = { seasonDropdownExpanded.value = false },
              modifier = Modifier.heightIn(max = 250.dp)
            ) {
              tvShow.seasons.forEach { season ->
                DropdownMenuItem(
                  text = { Text("Season ${season.season_number}", style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                  onSelectSeason(season)
                  seasonDropdownExpanded.value = false
                }
              )
            }
          }
        }

          // Episode Dropdown styled as a minimal chip
          val episodeDropdownExpanded = remember { mutableStateOf(false) }
          Box {
              Surface(
                onClick = { episodeDropdownExpanded.value = true },
                enabled = selectedSeason != null && !isFetchingEpisodes,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.height(32.dp)
              ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                  ) {
                    if (isFetchingEpisodes) {
                      CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                      Spacer(Modifier.width(4.dp))
                    }
                    Text(
                      text = selectedEpisode?.let { "E${it.episode_number}" } ?: "Ep",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                }
              }
              DropdownMenu(
                expanded = episodeDropdownExpanded.value,
                onDismissRequest = { episodeDropdownExpanded.value = false },
                modifier = Modifier.heightIn(max = 250.dp).widthIn(min = 180.dp)
              ) {
                  episodes.forEach { episode ->
                    DropdownMenuItem(
                        text = {
                          Column {
                            Text("Ep ${episode.episode_number}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            episode.name?.let {
                              Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                              }
                            }
                          },
                          onClick = {
                            onSelectEpisode(episode)
                            episodeDropdownExpanded.value = false
                          }
                      )
                  }
              }
          }

          IconButton(
            onClick = onClose,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              Icons.Default.Close, null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
      }
  }
}
