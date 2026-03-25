package com.dravenmiller.swolescroll.ui.dialogs // Make sure this matches your folder!

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.ui.components.SwoleButton

@Composable
fun ResumeWorkoutDialog(
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Force user to choose */ },
        title = { Text("Unfinished Workout Found") },
        text = { Text("Do you want to resume your unsaved workout?") },
        confirmButton = {
            TextButton(onClick = onResume) { Text("Resume") }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun ExitWorkoutDialog(
    onSaveAndExit: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Save or Discard?") },
        text = { Text("You have an active workout. What would you like to do before leaving?") },
        confirmButton = {
            TextButton(onClick = onSaveAndExit) {
                Text("Save & Exit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun FinishWorkoutDialog(
    workoutNote: String,
    onNoteChange: (String) -> Unit,
    onSaveAndFinish: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Workout Summary") },
        text = {
            Column {
                Text("Great job! Any notes for next time?")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = workoutNote,
                    onValueChange = onNoteChange,
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            SwoleButton(
                text = "Save & Finish",
                onClick = onSaveAndFinish
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteWorkoutDialog(
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete Workout?") },
        text = { Text("This action cannot be undone. Are you sure?") },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

@Composable
fun DistanceEntryDialog(
    tempDistance: String,
    onDistanceChange: (String) -> Unit,
    onCalculateAndSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Workout Paused") },
        text = {
            Column {
                Text("Enter total distance shown on machine:")
                OutlinedTextField(
                    value = tempDistance,
                    onValueChange = {
                        if (it.count { char -> char == '.' } <= 1 && it.all { char -> char.isDigit() || char == '.' }) {
                            onDistanceChange(it)
                        }
                    },
                    placeholder = { Text("e.g. 3.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onCalculateAndSave) { Text("Calculate & Save") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
