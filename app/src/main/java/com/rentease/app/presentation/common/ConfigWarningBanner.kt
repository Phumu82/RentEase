package com.rentease.app.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rentease.app.BuildConfig
import com.rentease.app.R
import com.rentease.app.domain.model.SupabaseConfig

/** Shown only when the build has no real Supabase project, so an empty app is explained instead of silent. */
@Composable
fun ConfigWarningBanner(modifier: Modifier = Modifier) {
    if (SupabaseConfig.isConfigured(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.config_banner_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                stringResource(R.string.error_not_configured),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
