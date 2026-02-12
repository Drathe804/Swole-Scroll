package com.dravenmiller.swolescroll.features.quests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun QuestBoardDialog(
    onDismiss: () -> Unit,
    onAcceptQuest: (QuestDifficulty) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // Clean look
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "📜 Quest Board",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Choose your adventure...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                // 🟢 SCOUT
                QuestButton(
                    title = "Scout Mission",
                    subtitle = "Cardio or Quick Skirmish",
                    color = Color(0xFF4CAF50), // Green
                    onClick = { onAcceptQuest(QuestDifficulty.SCOUT) }
                )

                Spacer(Modifier.height(12.dp))

                // 🟡 RAID
                QuestButton(
                    title = "Dungeon Raid",
                    subtitle = "Standard Hypertrophy Split",
                    color = Color(0xFFFF9800), // Orange
                    onClick = { onAcceptQuest(QuestDifficulty.RAID) }
                )

                Spacer(Modifier.height(12.dp))

                // 🔴 BOSS
                QuestButton(
                    title = "Boss Battle",
                    subtitle = "Heavy Compounds + Loot Run",
                    color = Color(0xFFD32F2F), // Red
                    onClick = { onAcceptQuest(QuestDifficulty.BOSS) }
                )

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun QuestButton(title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
