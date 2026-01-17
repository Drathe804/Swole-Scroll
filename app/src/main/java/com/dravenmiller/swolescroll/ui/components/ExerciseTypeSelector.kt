package com.dravenmiller.swolescroll.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.model.ExerciseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTypeSelector(
    selectedType: ExerciseType,
    onTypeSelected: (ExerciseType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // 1. Helper to make names look nice (e.g. LoadedCarry -> "Loaded Carry")
    fun getDisplayName(type: ExerciseType): String {
        return when (type) {
            ExerciseType.LoadedCarry -> "Loaded Carry"
            ExerciseType.ISOMETRIC -> "Isometric"
            ExerciseType.CARDIO -> "Cardio"
            ExerciseType.STRENGTH -> "Strength"
            // Fallback for any future types
            else -> type.name.replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // SELECTED EXERCISE FIELD (Default: STRENGTH)
        OutlinedTextField(
            readOnly = true,
            value = getDisplayName(selectedType),
            onValueChange = { },
            label = { Text("Exercise Type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ExerciseType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(text = getDisplayName(type)) }, // 👈 And here
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
