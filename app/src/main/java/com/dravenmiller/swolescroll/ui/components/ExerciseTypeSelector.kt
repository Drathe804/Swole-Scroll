package com.dravenmiller.swolescroll.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.model.ExerciseType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseTypeSelector(
    selectedType: ExerciseType,
    onTypeSelected: (ExerciseType) -> Unit
) {
    // 1. Determine "Category" based on the currently selected type
    // If the selected type is cardio, we are in "Cardio Mode".
    val isCardioMode by remember(selectedType) {
        derivedStateOf { selectedType.isCardio }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Exercise Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 2. TOP ROW: Category Switcher (Strength vs Cardio)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Option 1: Strength
            SegmentedButton(
                selected = !isCardioMode,
                onClick = {
                    // Default to STRENGTH when clicking this tab
                    if (isCardioMode) onTypeSelected(ExerciseType.STRENGTH)
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Weights / Lifting")
            }

            // Option 2: Cardio
            SegmentedButton(
                selected = isCardioMode,
                onClick = {
                    // Default to GENERAL CARDIO when clicking this tab
                    if (!isCardioMode) onTypeSelected(ExerciseType.CARDIO)
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Cardio")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. SUB-TYPE SELECTION (Dynamic Chips)
        Text(
            text = if (isCardioMode) "Machine / Type" else "Lifting Style",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Filter the list based on the active mode
            val visibleOptions = ExerciseType.values().filter { it.isCardio == isCardioMode }

            visibleOptions.forEach { type ->
                FilterChip(
                    selected = (type == selectedType),
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.displayName) },
                    leadingIcon = if (type == selectedType) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        // 4. HELPER TEXT (Optional Feedback)
        AnimatedVisibility(
            visible = isCardioMode && selectedType == ExerciseType.TREADMILL,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "✅ Enables Incline & Speed controls",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
