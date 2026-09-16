package dev.anhquocs.truelab.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.theme.SpacingXXS
import dev.anhquocs.truelab.core.ui.utils.s10

@Composable
fun MainNavBar(
    currentRoute: String?,
    onNavigateToDestination: (MainNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {

    val pillShape = remember { RoundedCornerShape(RadiusPill) }
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Dimen.PaddingSM, vertical = Dimen.PaddingS)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = pillShape)
            .clip(pillShape)
            .background(color = MaterialTheme.colorScheme.background)
            .padding(all = Dimen.PaddingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainNavDestination.bottomNavItems.forEach { destination ->
            val isSelected = currentRoute == destination.route
            val color = if (isSelected) activeColor else inactiveColor

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onNavigateToDestination(destination)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(pillShape)
                        .then(
                            if (isSelected) {
                                Modifier.paint(
                                    painter = painterResource(R.drawable.bg_tab_selected),
                                )
                            } else {
                                Modifier
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Image(
                        painter = painterResource(destination.iconRes),
                        contentDescription = stringResource(destination.titleRes),
                        modifier = Modifier.size(Dimen.SizeS2),
                        colorFilter = ColorFilter.tint(color)
                    )

                    Spacer(modifier = Modifier.height(SpacingXXS))

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(destination.titleRes),
                        overflow = TextOverflow.Clip,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.s10.copy(fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal),
                        textAlign = TextAlign.Center,
                        color = if (isSelected) activeColor else inactiveColor
                    )
                }
            }
        }
    }
}