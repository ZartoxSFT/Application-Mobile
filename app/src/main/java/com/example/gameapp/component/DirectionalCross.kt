package com.example.gameapp.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DirectionalCross(
    modifier: Modifier = Modifier,
    onDirectionClick: (String) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = CircleShape
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // UP button
            Button(
                onClick = { onDirectionClick("up") },
                modifier = Modifier.size(width = 72.dp, height = 64.dp),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("▲", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("UP", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Middle Row: LEFT - Center Hub - RIGHT
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // LEFT button
                Button(
                    onClick = { onDirectionClick("left") },
                    modifier = Modifier.size(width = 64.dp, height = 72.dp),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        bottomStart = 16.dp,
                        topEnd = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("◄", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("LEFT", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Center Hub
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎮", fontSize = 20.sp)
                }

                // RIGHT button
                Button(
                    onClick = { onDirectionClick("right") },
                    modifier = Modifier.size(width = 64.dp, height = 72.dp),
                    shape = RoundedCornerShape(
                        topEnd = 16.dp,
                        bottomEnd = 16.dp,
                        topStart = 4.dp,
                        bottomStart = 4.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("►", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("RIGHT", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // DOWN button
            Button(
                onClick = { onDirectionClick("down") },
                modifier = Modifier.size(width = 72.dp, height = 64.dp),
                shape = RoundedCornerShape(
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp,
                    topStart = 4.dp,
                    topEnd = 4.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DOWN", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("▼", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
