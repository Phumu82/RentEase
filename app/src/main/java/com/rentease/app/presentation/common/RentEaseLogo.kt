package com.rentease.app.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentease.app.R
import com.rentease.app.presentation.theme.RentEaseNavy

/** The RentEase mark (gold house + keyhole + graduation cap) on its navy tile, same artwork as the launcher icon. */
@Composable
fun RentEaseLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(
        modifier = modifier.size(size).background(RentEaseNavy, RoundedCornerShape(size / 4)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(size)
        )
    }
}
