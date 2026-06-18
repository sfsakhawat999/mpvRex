package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Video.Thumbnails
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.theme.spacing
import app.marlboroadvance.mpvex.cinehub.data.NfoScanner
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PlaylistItem(
  val uri: Uri,
  val title: String,
  val index: Int,
  val isPlaying: Boolean,
  val progressPercent: Float = 0f, 
  val isWatched: Boolean = false,  
  val path: String = "", 
  val duration: String = "", 
  val resolution: String = "", 
)

class LRUBitmapCache(private val maxSize: Int) {
  private val cache = LinkedHashMap<String, Bitmap?>(maxSize + 1, 1f, true)

  operator fun get(key: String): Bitmap? = synchronized(this) { cache[key] }

  operator fun set(key: String, value: Bitmap?) = synchronized(this) {
    cache[key] = value
    if (cache.size > maxSize) {
      cache.remove(cache.keys.firstOrNull())
    }
  }

  fun containsKey(key: String): Boolean = synchronized(this) { cache.containsKey(key) }
  fun clear() = synchronized(this) { cache.clear() }
}

private suspend fun loadMediaStoreThumbnail(context: Context, uri: Uri): Bitmap? {
  return withContext(Dispatchers.IO) {
    try {
      when (uri.scheme) {
        "content" -> {
          val videoId = extractVideoId(uri, context)
          if (videoId != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
              val contentUri = android.content.ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoId
              )
              context.contentResolver.loadThumbnail(contentUri, android.util.Size(512, 512), null)
            } else {
              @Suppress("DEPRECATION")
              Thumbnails.getThumbnail(context.contentResolver, videoId, Thumbnails.MINI_KIND, null)
            }
          } else null
        }
        "file" -> {
          val filePath = uri.path ?: return@withContext null
          val projection = arrayOf(MediaStore.Video.Media._ID)
          val selection = "${MediaStore.Video.Media.DATA} = ?"
          val selectionArgs = arrayOf(filePath)

          context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null
          )?.use { cursor ->
            if (cursor.moveToFirst()) {
              val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
              val videoId = cursor.getLong(idColumn)
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentUri = android.content.ContentUris.withAppendedId(
                  MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId
                )
                context.contentResolver.loadThumbnail(contentUri, android.util.Size(512, 512), null)
              } else {
                @Suppress("DEPRECATION")
                Thumbnails.getThumbnail(context.contentResolver, videoId, Thumbnails.MINI_KIND, null)
              }
            } else null
          }
        }
        else -> null
      }
    } catch (e: Exception) {
      android.util.Log.w("PlaylistSheet", "Failed to load MediaStore thumbnail for $uri", e)
      null
    }
  }
}

private fun extractVideoId(uri: Uri, context: Context): Long? {
  return try {
    val path = uri.path ?: return null
    val idString = path.substringAfterLast('/').toLongOrNull() ?: return null

    val projection = arrayOf(MediaStore.Video.Media._ID)
    val selection = "${MediaStore.Video.Media._ID} = ?"
    val selectionArgs = arrayOf(idString.toString())

    context.contentResolver.query(
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null
    )?.use { cursor ->
      if (cursor.moveToFirst()) {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        cursor.getLong(idColumn)
      } else null
    }
  } catch (e: Exception) { null }
}

@Composable
fun PlaylistSheet(
  playlist: ImmutableList<PlaylistItem>,
  onDismissRequest: () -> Unit,
  onItemClick: (PlaylistItem) -> Unit,
  totalCount: Int = playlist.size,
  isM3UPlaylist: Boolean = false,
  playerPreferences: app.marlboroadvance.mpvex.preferences.PlayerPreferences,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val accentColor = MaterialTheme.colorScheme.primary
  val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

  val isListModePreference by playerPreferences.playlistViewMode.collectAsState()
  var isListMode by remember { mutableStateOf(if (isPortrait) true else isListModePreference) }

  LaunchedEffect(isPortrait) {
    if (isPortrait && !isListMode) {
      isListMode = true
    }
  }

  LaunchedEffect(isListMode) {
    if (!isPortrait && isListMode != isListModePreference) {
      playerPreferences.playlistViewMode.set(isListMode)
    }
  }

  val thumbnailCache by remember { mutableStateOf(LRUBitmapCache(maxSize = 50)) }
  val lazyListState = rememberLazyListState()

  val playingItemIndex by remember { derivedStateOf { playlist.indexOfFirst { it.isPlaying } } }

  LaunchedEffect(playingItemIndex) {
    if (playingItemIndex >= 0) {
      lazyListState.animateScrollToItem(playingItemIndex)
    }
  }

  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  val sheetWidth = if (isListMode) {
    if (LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 640.dp else 420.dp
  } else screenWidth * 0.85f

  PlayerSheet(
    onDismissRequest = onDismissRequest,
    modifier = Modifier.fillMaxWidth(),
    customMaxWidth = sheetWidth,
    customMaxHeight = if (isPortrait) LocalConfiguration.current.screenHeightDp.dp * 0.5f else null,
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = Color.Transparent,
      shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
      tonalElevation = 0.dp,
    ) {
      Column(
        modifier = modifier.padding(
          vertical = MaterialTheme.spacing.smaller,
          horizontal = if (!isListMode) MaterialTheme.spacing.medium else 0.dp
        )
      ) {
        val currentItem = playlist.find { it.isPlaying }

        if (currentItem != null && !isM3UPlaylist) {
          val currentFile = File(currentItem.path)
          val parentFolder = currentFile.parentFile
          val isMovieType = currentFile.absolutePath.contains("/movies/", ignoreCase = true)

          if (isMovieType && parentFolder != null) {
            val movieNfo = File(parentFolder, "movie.nfo")
            val localPoster = File(parentFolder, "poster.jpg").absolutePath
            
            var moviePlotText by remember { mutableStateOf("Premium fail-proof playback session track active.") }
            var posterImageUri by remember { mutableStateOf<String?>(localPoster) }

            LaunchedEffect(currentItem.path) {
              if (movieNfo.exists()) {
                withContext(Dispatchers.IO) {
                  val xmlDoc = NfoScanner.getXmlDocument(movieNfo)
                  if (xmlDoc != null) {
                    val root = xmlDoc.documentElement
                    moviePlotText = NfoScanner.getTagText(root, "plot").ifBlank { moviePlotText }
                    val xmlPoster = NfoScanner.getTagText(root, "thumb")
                    if (xmlPoster.startsWith("http")) {
                      posterImageUri = xmlPoster
                    }
                  }
                }
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
            ) {
              Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                  model = posterImageUri ?: android.R.drawable.ic_menu_gallery,
                  contentDescription = "Movie Poster",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.DarkGray)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = currentItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = moviePlotText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              horizontal = if (isListMode) MaterialTheme.spacing.medium else 0.dp,
              vertical = MaterialTheme.spacing.small,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
            modifier = Modifier.weight(1f)
          ) {
            if (currentItem != null) {
              Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = accentColor),
              )
              Text(text = "•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "$totalCount items", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          if (!isPortrait) {
            IconButton(onClick = { isListMode = !isListMode }) {
              Icon(
                imageVector = if (isListMode) Icons.Default.GridView else Icons.Default.ViewList,
                contentDescription = if (isListMode) "Switch to Grid View" else "Switch to List View",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        if (isListMode) {
          LazyColumn(modifier = Modifier.fillMaxWidth(), state = lazyListState) {
            items(playlist) { item ->
              var explicitTitle by remember { mutableStateOf(item.title) }
              if (!isM3UPlaylist && item.path.isNotEmpty()) {
                val currentFile = File(item.path)
                val targetNfoFile = File(currentFile.parentFile, "${currentFile.nameWithoutExtension}.nfo")
                LaunchedEffect(item.path) {
                  if (targetNfoFile.exists()) {
                    withContext(Dispatchers.IO) {
                      val xmlDoc = NfoScanner.getXmlDocument(targetNfoFile)
                      if (xmlDoc != null) {
                        val nfoTitle = NfoScanner.getTagText(xmlDoc.documentElement, "title")
                        if (nfoTitle.isNotBlank()) {
                          explicitTitle = "Episode ${item.index + 1}: $nfoTitle"
                        }
                      }
                    }
                  }
                }
              }

              PlaylistTrackListItem(
                item = item.copy(title = explicitTitle),
                context = context,
                thumbnailCache = thumbnailCache,
                onClick = { onItemClick(item) },
                skipThumbnail = isM3UPlaylist,
                accentColor = accentColor
              )
            }
          }
        } else {
          LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(
              horizontal = if (isListMode) MaterialTheme.spacing.medium else 0.dp,
              vertical = MaterialTheme.spacing.small
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
          ) {
            items(playlist) { item ->
              var explicitTitle by remember { mutableStateOf(item.title) }
              if (!isM3UPlaylist && item.path.isNotEmpty()) {
                val currentFile = File(item.path)
                val targetNfoFile = File(currentFile.parentFile, "${currentFile.nameWithoutExtension}.nfo")
                LaunchedEffect(item.path) {
                  if (targetNfoFile.exists()) {
                    withContext(Dispatchers.IO) {
                      val xmlDoc = NfoScanner.getXmlDocument(targetNfoFile)
                      if (xmlDoc != null) {
                        val nfoTitle = NfoScanner.getTagText(xmlDoc.documentElement, "title")
                        if (nfoTitle.isNotBlank()) {
                          explicitTitle = "Episode ${item.index + 1}: $nfoTitle"
                        }
                      }
                    }
                  }
                }
              }

              PlaylistTrackGridItem(
                item = item.copy(title = explicitTitle),
                context = context,
                thumbnailCache = thumbnailCache,
                onClick = { onItemClick(item) },
                skipThumbnail = isM3UPlaylist,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun PlaylistTrackListItem(
  item: PlaylistItem,
  context: Context,
  thumbnailCache: LRUBitmapCache,
  onClick: () -> Unit,
  skipThumbnail: Boolean = false,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  val accentSecondary = MaterialTheme.colorScheme.tertiary
  val videoPath = item.path.ifBlank { item.uri.toString() }
  var thumbnail by remember(videoPath) { mutableStateOf(thumbnailCache[videoPath]) }

  LaunchedEffect(videoPath) {
    if (!skipThumbnail && !thumbnailCache.containsKey(videoPath)) {
      val bmp = loadMediaStoreThumbnail(context, item.uri)
      thumbnail = bmp
      thumbnailCache[videoPath] = bmp
    }
  }

  val borderModifier = if (item.isPlaying) {
    Modifier.border(
      width = 2.dp,
      brush = Brush.linearGradient(listOf(accentColor, accentSecondary)),
      shape = RoundedCornerShape(12.dp),
    )
  } else Modifier

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall)
      .clip(RoundedCornerShape(12.dp))
      .then(borderModifier)
      .clickable(onClick = onClick),
    color = if (item.isPlaying) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else Color.Transparent,
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.smaller),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
      Box(
        modifier = Modifier
          .width(100.dp)
          .height(56.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        thumbnail?.let { bmp ->
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Thumbnail",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
          )
        } ?: run {
          Icon(
            imageVector = Icons.Outlined.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp),
          )
        }

        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(6.dp)
            .background(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
          Text(
            text = "${item.index + 1}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = Color.White,
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (item.isPlaying) FontWeight.Bold else FontWeight.Normal,
            color = if (item.isPlaying) accentColor else if (item.isWatched) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (item.duration.isNotEmpty()) {
            Surface(
              color = if (item.isPlaying) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest,
              shape = RoundedCornerShape(4.dp),
            ) {
              Text(
                text = item.duration,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (item.isPlaying) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LoadingChip(width = 40.dp)
          }
          
          if (item.resolution.isNotEmpty()) {
            Surface(
              color = if (item.isPlaying) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest,
              shape = RoundedCornerShape(4.dp),
            ) {
              Text(
                text = item.resolution,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (item.isPlaying) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LoadingChip(width = 60.dp)
          }
        }
      }

      if (item.isPlaying) {
        Surface(color = accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp)) {
          Text(
            text = "Playing",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = accentColor),
          )
        }
      }
    }
  }
}

@Composable
fun PlaylistTrackGridItem(
  item: PlaylistItem,
  context: Context,
  thumbnailCache: LRUBitmapCache,
  onClick: () -> Unit,
  skipThumbnail: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val accentColor = MaterialTheme.colorScheme.primary
  val accentSecondary = MaterialTheme.colorScheme.tertiary
  val videoPath = item.path.ifBlank { item.uri.toString() }
  var thumbnail by remember(videoPath) { mutableStateOf(thumbnailCache[videoPath]) }

  LaunchedEffect(videoPath) {
    if (!skipThumbnail && !thumbnailCache.containsKey(videoPath)) {
      val bmp = loadMediaStoreThumbnail(context, item.uri)
      thumbnail = bmp
      thumbnailCache[videoPath] = bmp
    }
  }

  val borderModifier = if (item.isPlaying) {
    Modifier.border(
      width = 2.dp,
      brush = Brush.linearGradient(listOf(accentColor, accentSecondary)),
      shape = RoundedCornerShape(12.dp),
    )
  } else Modifier

  Surface(
    modifier = modifier
      .width(200.dp)
      .clip(RoundedCornerShape(12.dp))
      .then(borderModifier)
      .clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(modifier = Modifier.padding(MaterialTheme.spacing.smaller), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(112.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        thumbnail?.let { bmp ->
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Thumbnail",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
          )
        } ?: run {
          Icon(
            imageVector = Icons.Outlined.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp),
          )
        }

        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(6.dp)
            .background(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
          Text(
            text = "${item.index + 1}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = Color.White,
          )
        }

        if (item.duration.isNotEmpty()) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(6.dp)
              .background(color = Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp),
          ) {
            Text(
              text = item.duration,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
              color = Color.White,
            )
          }
        } else {
          Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
            LoadingChip(width = 40.dp, height = 18.dp, isDark = true)
          }
        }

        if (item.isPlaying) {
          Box(
            modifier = Modifier
              .matchParentSize()
              .background(brush = Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.3f), accentColor.copy(alpha = 0.1f))))
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = item.title,
          modifier = Modifier.height(44.dp),
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (item.isPlaying) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            color = if (item.isPlaying) accentColor else if (item.isWatched) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          if (item.resolution.isNotEmpty()) {
            Surface(
              color = if (item.isPlaying) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest,
              shape = RoundedCornerShape(4.dp),
            ) {
              Text(
                text = item.resolution,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (item.isPlaying) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LoadingChip(width = 60.dp)
          }

          if (item.isPlaying) {
            Surface(color = accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
              Text(
                text = "Playing",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = accentColor),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun LoadingChip(
  width: androidx.compose.ui.unit.Dp,
  height: androidx.compose.ui.unit.Dp = 18.dp,
  isDark: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
  val shimmerTranslate = infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1000f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer"
  )

  val baseColor = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHighest
  val shimmerColor = if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh

  Box(
    modifier = modifier
      .width(width)
      .height(height)
      .clip(RoundedCornerShape(4.dp))
      .background(
        brush = Brush.linearGradient(
          colors = listOf(baseColor, shimmerColor, baseColor),
          start = Offset(shimmerTranslate.value - 200f, 0f),
          end = Offset(shimmerTranslate.value, 0f)
        )
      )
  )
}