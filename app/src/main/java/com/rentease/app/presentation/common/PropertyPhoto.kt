package com.rentease.app.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rentease.app.R

/**
 * Sample photos shipped with the app (the same four images the web uses for its demo listings).
 * They are shown, clearly labelled "Sample photo", only when a listing has no photo of its own or
 * its photo fails to load, so a card or gallery is never blank.
 */
object SamplePhotos {
    private val drawables = intArrayOf(
        R.drawable.sample_property_1,
        R.drawable.sample_property_2,
        R.drawable.sample_property_3,
        R.drawable.sample_property_4
    )

    /** Stable pick for a listing so the same property always shows the same sample. */
    fun indexFor(key: String, size: Int): Int {
        require(size > 0) { "size must be positive" }
        return (key.hashCode() and Int.MAX_VALUE) % size
    }

    @DrawableRes
    fun drawableFor(key: String): Int = drawables[indexFor(key, drawables.size)]
}

/** One property photo: the real image when available, otherwise a labelled sample photo. */
@Composable
fun PropertyPhoto(
    url: String?,
    contentDescription: String?,
    fallbackKey: String,
    modifier: Modifier = Modifier,
    showSampleLabel: Boolean = true
) {
    var failed by remember(url) { mutableStateOf(false) }
    val useSample = url.isNullOrBlank() || failed
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (useSample) {
            Image(
                painter = painterResource(SamplePhotos.drawableFor(fallbackKey)),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (showSampleLabel) SampleBadge(Modifier.align(Alignment.BottomStart))
        } else {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SampleBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Text(
            stringResource(R.string.photo_sample_label),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Swipeable 4:3 gallery with a "1 / 3" counter, as on the web listing page. */
@Composable
fun PropertyGallery(
    images: List<String>,
    title: String,
    fallbackKey: String,
    modifier: Modifier = Modifier
) {
    val pages = images.filter { it.isNotBlank() }.ifEmpty { listOf("") }
    val pager = rememberPagerState(pageCount = { pages.size })
    Box(modifier = modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            PropertyPhoto(
                url = pages[page].ifBlank { null },
                contentDescription = stringResource(R.string.photo_of_property, title, page + 1, pages.size),
                // Different sample per page so an image-less listing still shows a gallery feel.
                fallbackKey = fallbackKey + page,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (pages.size > 1) {
            val counter = stringResource(R.string.photo_counter, pager.currentPage + 1, pages.size)
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                Text(counter, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}
