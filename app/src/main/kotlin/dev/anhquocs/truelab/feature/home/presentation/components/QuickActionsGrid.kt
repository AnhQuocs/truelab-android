package dev.anhquocs.truelab.feature.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingXL

@Composable
fun QuickActionsGrid(
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingM)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingM)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.SportsSoccer,
                title = stringResource(R.string.nav_matches),
                onClick = { onNavigate("matches") }
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Group,
                title = stringResource(R.string.nav_teams),
                onClick = { onNavigate("teams") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingM)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                title = stringResource(R.string.nav_analytics),
                onClick = { onNavigate("analytics") }
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Calculate,
                title = stringResource(R.string.nav_prediction),
                onClick = { onNavigate("prediction") }
            )
        }
        ActionTile(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Speed,
            title = stringResource(R.string.nav_benchmark),
            onClick = { onNavigate("benchmark") }
        )
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingL),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SpacingXL)
            )
            Spacer(modifier = Modifier.width(SpacingM))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
