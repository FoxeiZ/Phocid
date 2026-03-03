package org.sunsetware.phocid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.sunsetware.phocid.ui.theme.Typography
import org.sunsetware.phocid.utils.roundToIntOrZero

@Composable
inline fun SliderWithInput(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    crossinline onSliderChange: (Int) -> Unit,
    crossinline onApply: (Int) -> Unit,
    modifier: Modifier = Modifier,
    crossinline numberFormatter: (Int) -> String = { it.toString() },
    steps: Int? = null,
    crossinline isValid: (Int) -> Boolean = { it > 0 },
    dialogTitle: String = label,
) {
    var showDialog by remember { mutableStateOf(false) }
    val resolvedSteps = steps ?: (max - min - 1).coerceAtLeast(0)

    Column(modifier = modifier) {
        Row {
            Text(text = label, style = Typography.labelLarge)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = numberFormatter(value),
                style = Typography.labelLarge,
                modifier = Modifier.clickable { showDialog = true },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Clamp only the thumb position so values outside [min, max] display at the boundary
        // instead of crashing or behaving unexpectedly.
        Slider(
            value = value.coerceIn(min, max).toFloat(),
            valueRange = min.toFloat()..max.toFloat(),
            steps = resolvedSteps,
            onValueChange = { onSliderChange(it.roundToIntOrZero()) },
        )
    }

    if (showDialog) {
        var inputValue by remember { mutableStateOf(value.toString()) }
        val parsed = inputValue.toIntOrNull()
        val valid = parsed != null && isValid(parsed)

        DialogBase(
            title = dialogTitle,
            onConfirm = {
                if (parsed != null) onApply(parsed)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            confirmEnabled = valid,
        ) {
            TextField(
                value = inputValue,
                onValueChange = { inputValue = it.filter { c -> c.isDigit() } },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions {
                        if (valid) {
                            onApply(parsed)
                            showDialog = false
                        }
                    },
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}
