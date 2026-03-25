package com.dravenmiller.swolescroll.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.swolescroll.model.WorkoutExercise

@Composable
fun MiniDungeonControls(
    addedExercises: List<WorkoutExercise>,
    expandedIndex: Int,
    onExpandedIndexChange: (Int) -> Unit,
    onPrepareForSuperset: (Int) -> Unit,
    onStartDungeon: (Int) -> Unit,
    onAddToDungeon: (String) -> Unit,
    onNewDungeonFresh: () -> Unit,
    onAddNewExercise: () -> Unit
) {
    Column {
        val currentExercise = addedExercises.getOrNull(expandedIndex)
        val currentDungeonId = currentExercise?.supersetId

        val hasNeighbor = if (expandedIndex + 1 < addedExercises.size) {
            addedExercises[expandedIndex + 1].supersetId == currentDungeonId && currentDungeonId != null
        } else false

        // --- ⚔️ MINI-DUNGEON CREATION CONTROLS ---
        if (currentExercise != null && !hasNeighbor) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentDungeonId != null) {
                            onAddToDungeon(currentDungeonId)
                        } else {
                            onStartDungeon(expandedIndex)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Castle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (currentDungeonId == null) "Enter Mini-Dungeon (Create Superset)" else "New Encounter (Add to Superset)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (currentDungeonId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNewDungeonFresh,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Castle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("New Mini-Dungeon (Separate Superset)")
                    }
                }
            }
        }

        // --- 🧭 NAVIGATION MATH ---
        val prevExerciseName = if (expandedIndex > 0) {
            addedExercises[expandedIndex - 1].exercise.name
        } else null

        val nextExerciseName = if (expandedIndex < addedExercises.lastIndex) {
            addedExercises[expandedIndex + 1].exercise.name
        } else "Add New"

        val isEndOfDungeon = if (currentDungeonId != null) {
            val nextIndex = expandedIndex + 1
            nextIndex >= addedExercises.size || addedExercises[nextIndex].supersetId != currentDungeonId
        } else false

        // --- ⬆️⬇️ NAVIGATION ARROWS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ⬅️ PREV BUTTON
            OutlinedButton(
                onClick = {
                    if (expandedIndex > 0) {
                        val target = expandedIndex - 1
                        onPrepareForSuperset(target)
                        onExpandedIndexChange(target)
                    }
                },
                enabled = expandedIndex > 0,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(20.dp))
                    if (prevExerciseName != null) {
                        Text(
                            text = prevExerciseName,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ➡️ NEXT / LOOP BUTTONS
            if (isEndOfDungeon) {
                // 🚪 ESCAPE
                OutlinedButton(
                    onClick = {
                        if (expandedIndex + 1 < addedExercises.size) {
                            onExpandedIndexChange(expandedIndex + 1)
                        } else {
                            onAddNewExercise()
                        }
                    },
                    modifier = Modifier.weight(0.8f)
                ) {
                    Text("Escape", maxLines = 1, fontSize = 10.sp)
                }

                Spacer(Modifier.width(8.dp))

                // 🔄 LOOP
                Button(
                    onClick = {
                        val startOfDungeon = addedExercises.indexOfFirst { it.supersetId == currentDungeonId }
                        if (startOfDungeon != -1) {
                            onPrepareForSuperset(startOfDungeon)
                            onExpandedIndexChange(startOfDungeon)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Superset")
                }

            } else {
                // STANDARD NEXT
                Button(
                    onClick = {
                        if (expandedIndex < addedExercises.lastIndex) {
                            val target = expandedIndex + 1
                            onPrepareForSuperset(target)
                            onExpandedIndexChange(target)
                        } else {
                            onAddNewExercise()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (expandedIndex == addedExercises.lastIndex) Icons.Default.Add else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = nextExerciseName,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
