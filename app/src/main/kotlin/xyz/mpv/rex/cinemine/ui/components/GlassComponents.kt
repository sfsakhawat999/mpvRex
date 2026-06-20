package xyz.mpv.rex.cinemine.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Shared configuration constants for uniform Material 3 Glassmorphism look
val GlassShape = RoundedCornerShape(16.dp)
val GlassBorderStroke = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(GlassShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f))
            .border(GlassBorderStroke, GlassShape),
        content = content
    )
}

/**
 * 🎬 1. Movie Item Card (Handles Local and CloudRepo Movies)
 */
@Composable
fun MovieItemCard(
    title: String,
    genre: String,
    rating: Double,
    posterPath: String?,
    isCloud: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clip(GlassShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f))
            .border(GlassBorderStroke, GlassShape)
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray.copy(alpha = 0.1f))
        ) {
            AsyncImage(
                model = posterPath ?: android.R.drawable.ic_menu_gallery,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (rating > 0.0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Text(
                        text = "★ ${String.format("%.1f", rating)}",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            if (isCloud) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "CLOUD",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(genre, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
    }
}

/**
 * 📺 2. TV Show Item Card (Handles Local and Cloud Series Groups)
 * UPDATE: Aligned signature layout property mapping directly to model types context variables
 */
@Composable
fun TvShowItemCard(
    title: String,
    genre: String,
    rating: Double,
    posterPath: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clip(GlassShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f))
            .border(GlassBorderStroke, GlassShape)
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray.copy(alpha = 0.1f))
        ) {
            AsyncImage(
                model = posterPath ?: android.R.drawable.ic_menu_gallery,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (rating > 0.0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Text(
                        text = "★ ${String.format("%.1f", rating)}",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(genre, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.secondary)
    }
}

/**
 * 🌐 3. CineTube YouTube/Invidious Card Item
 */
@Composable
fun YoutubeVideoCard(
    title: String,
    author: String,
    lengthSeconds: Int,
    thumbnailUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        border = GlassBorderStroke,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
        )
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
                
                if (lengthSeconds > 0) {
                    val minutes = lengthSeconds / 60
                    val seconds = lengthSeconds % 60
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = String.format("%d:%02d", minutes, seconds),
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(author, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

/**
 * 🎭 4. Artist / Cast Info Profile Card
 */
@Composable
fun ArtistInfoCard(
    name: String,
    thumbUrl: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = if (thumbUrl.startsWith("http")) thumbUrl else "https://image.tmdb.org/t/p/w185/abstract_profile.jpg",
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 5. TV Show Episode List Item Card
 * UPDATE: Swapped out unresolved abstract standard reference identifier vector with fully qualified AutoMirrored PlayArrow
 */
@Composable
fun EpisodeItemRow(
    episodeCode: String,
    title: String,
    plot: String,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$episodeCode: $title",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (plot.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = plot,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onPlayClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlayArrow,
                    contentDescription = "Play Episode",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
