package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Common Metadata Chip for media cards
 */
@Composable
fun MediaMetadataChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * Base Media Card that provides a unified layout for List and Grid modes.
 * Used by VideoCard, NetworkVideoCard, etc.
 */
@Composable
fun BaseMediaCard(
    title: String,
    modifier: Modifier = Modifier,
    thumbnail: ImageBitmap? = null,
    thumbnailIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onThumbClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isRecentlyPlayed: Boolean = false,
    isNeverPlayed: Boolean = false,
    isGridMode: Boolean = false,
    gridColumns: Int = 1,
    progressPercentage: Float? = null,
    maxTitleLines: Int = 2,
    thumbnailSize: Dp = 64.dp,
    thumbnailAspectRatio: Float = 16f / 9f,
    infoContent: @Composable (RowScope.() -> Unit)? = null,
    chipsContent: @Composable (FlowRowScope.() -> Unit)? = null,
    overlayContent: @Composable (BoxScope.() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        if (isGridMode) {
            // GRID LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f) 
                        else Color.Transparent
                    )
                    .then(if (gridColumns == 1) Modifier else Modifier.padding(12.dp)),
                horizontalAlignment = if (gridColumns == 1) Alignment.Start else Alignment.CenterHorizontally,
            ) {
                // Thumbnail Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(thumbnailAspectRatio)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .then(
                            if (onThumbClick != null) {
                                Modifier.combinedClickable(
                                    onClick = { onThumbClick() },
                                    onLongClick = onLongClick
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (thumbnailIcon != null) {
                        thumbnailIcon()
                    } else {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    // Progress Bar
                    if (progressPercentage != null) {
                        LinearProgressIndicator(
                            progress = { progressPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Black.copy(alpha = 0.3f),
                        )
                    }
                    
                    overlayContent?.invoke(this)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        isRecentlyPlayed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        isNeverPlayed -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                    maxLines = maxTitleLines,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (gridColumns == 1) TextAlign.Start else TextAlign.Center,
                    fontWeight = if (isRecentlyPlayed) FontWeight.Black else FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Extra info row (if any)
                if (infoContent != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().then(
                            if (gridColumns == 1) Modifier.padding(vertical = 4.dp) else Modifier
                        ),
                        horizontalArrangement = if (gridColumns == 1) Arrangement.Start else Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        infoContent()
                    }
                }
            }
        } else {
            // LIST LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
        // Thumbnail Box
        Box(
          modifier = Modifier
            .width(thumbnailSize)
            .aspectRatio(thumbnailAspectRatio)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (onThumbClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onThumbClick() },
                        onLongClick = onLongClick
                    )
                } else Modifier
            ),
          contentAlignment = Alignment.Center,
        ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (thumbnailIcon != null) {
                        thumbnailIcon()
                    } else {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(thumbnailSize / 1.5f),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    // Progress Bar
                    if (progressPercentage != null) {
                        LinearProgressIndicator(
                            progress = { progressPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Black.copy(alpha = 0.3f),
                        )
                    }
                    
                    overlayContent?.invoke(this)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = when {
                            isRecentlyPlayed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            isNeverPlayed -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        },
                        maxLines = maxTitleLines,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isRecentlyPlayed) FontWeight.Black else FontWeight.Normal,
                    )

                    if (infoContent != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            infoContent()
                        }
                    }

                    if (chipsContent != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            chipsContent()
                        }
                    }
                }
            }
        }
    }
}
