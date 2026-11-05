package com.rentease.app.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rentease.app.R
import com.rentease.app.domain.model.PropertySummary
import com.rentease.app.presentation.theme.RentEaseGold

/**
 * Card in the style of the website's "Handpicked accommodation near campus" grid: photo with
 * Verified / Furnished badges, title, rating and distance, beds/baths/rooms open, price and a
 * "View details" pill. Every value comes from the listing row; nothing is invented.
 *
 * The favourite heart and the compare tick are optional so the same card serves the home screen,
 * the full listings screen and the "similar properties" row.
 */
@Composable
fun FeaturedPropertyCard(
    property: PropertySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavouriteToggle: (() -> Unit)? = null,
    compareSelected: Boolean = false,
    onCompareToggle: (() -> Unit)? = null
) {
    RentEaseCard(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            PropertyPhoto(
                url = property.coverImageUrl,
                contentDescription = property.title,
                fallbackKey = property.slug,
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (property.verified) {
                    Badge(stringResource(R.string.verification_status_verified), MaterialTheme.colorScheme.primary)
                }
                if (property.furnished) {
                    Badge(stringResource(R.string.furnished_label), MaterialTheme.colorScheme.secondary)
                }
            }
            if (onFavouriteToggle != null) {
                IconButton(
                    onClick = onFavouriteToggle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                ) {
                    Icon(
                        imageVector = if (property.isFavourited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.save_listing_action),
                        tint = if (property.isFavourited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(property.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (property.reviewCount > 0) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = RentEaseGold, modifier = Modifier.size(16.dp))
                    Text(
                        " " + stringResource(R.string.property_rating_value, property.rating.toString(), property.reviewCount) + " · ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    stringResource(R.string.km_from_campus, String.format("%.1f", property.distanceKm)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.property_beds_baths_rooms, property.bedrooms, property.bathrooms, property.roomsAvailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.price_per_month, property.pricePerMonth.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text(
                        stringResource(R.string.view_details_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            if (onCompareToggle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = compareSelected, onCheckedChange = { onCompareToggle() })
                    Text(stringResource(R.string.compare_action), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun Badge(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (color == MaterialTheme.colorScheme.secondary) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
