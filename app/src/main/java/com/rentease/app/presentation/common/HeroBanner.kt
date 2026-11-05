package com.rentease.app.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentease.app.R

/**
 * Photo banner with a dark teal gradient and white headline, like the website's page heroes.
 * Used at the top of public pages, dashboards and the home screen so screens are not plain text.
 */
@Composable
fun HeroBanner(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes image: Int = R.drawable.hero_student_housing,
    height: Dp = 180.dp,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxWidth().height(height)) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0x33031019), Color(0xE6031019))
                )
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE4F4F6))
            }
            content()
        }
    }
}
