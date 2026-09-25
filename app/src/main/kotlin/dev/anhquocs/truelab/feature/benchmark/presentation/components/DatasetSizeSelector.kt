package dev.anhquocs.truelab.feature.benchmark.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.core.ui.theme.SpacingXS
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s14

/**
 * Component lựa chọn kích thước tập dữ liệu đo lường hiệu năng.
 *
 * @param sizes Danh sách các kích thước (mặc định 1K, 10K, 50K).
 * @param selectedSize Kích thước đang được chọn.
 * @param enabled Có cho phép tương tác hay không (ví dụ: vô hiệu hóa khi đang chạy đo lường).
 * @param onSelectSize Callback khi người dùng chọn một kích thước mới.
 */
@Composable
fun DatasetSizeSelector(
    sizes: List<Int>,
    selectedSize: Int,
    enabled: Boolean = true,
    onSelectSize: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.benchmark_dataset_size, selectedSize),
            style = MaterialTheme.typography.s12,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(SpacingXS))
        Row(horizontalArrangement = Arrangement.spacedBy(SpacingS)) {
            sizes.forEach { size ->
                FilterChip(
                    selected = selectedSize == size,
                    onClick = { onSelectSize(size) },
                    enabled = enabled,
                    label = {
                        Text(
                            text = "%,d items".format(size),
                            style = MaterialTheme.typography.s14
                        )
                    }
                )
            }
        }
    }
}
