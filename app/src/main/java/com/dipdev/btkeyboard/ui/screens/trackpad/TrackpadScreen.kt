package com.dipdev.btkeyboard.ui.screens.trackpad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dipdev.btkeyboard.ui.viewmodel.KeyboardViewModel

private val KeyBg = Color(0xFF1E2235)

@Composable
fun TrackpadScreen(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Trackpad Area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(KeyBg)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { viewModel.sendMouseClick(leftClick = true) },
                        onDoubleTap = {
                            // Double tap could trigger drag or specialized behavior
                            viewModel.sendMouseClick(leftClick = true)
                            viewModel.sendMouseClick(leftClick = true)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Sensitivity multiplier (adjust if needed)
                        val multiplier = 1.5f
                        val dx = (dragAmount.x * multiplier).toInt()
                        val dy = (dragAmount.y * multiplier).toInt()

                        // Clamping to Byte size limits -127 to 127
                        val byteDx = dx.coerceIn(-127, 127).toByte()
                        val byteDy = dy.coerceIn(-127, 127).toByte()

                        if (byteDx != 0.toByte() || byteDy != 0.toByte()) {
                            viewModel.sendMouseMovement(dx = byteDx, dy = byteDy)
                        }
                    }
                },
            color = KeyBg,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Trackpad",
                    color = Color.DarkGray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Mouse Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Click
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { viewModel.sendMouseClick(leftClick = true) },
                shape = RoundedCornerShape(12.dp),
                color = KeyBg,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Left Click", color = Color.White)
                }
            }

            // Right Click
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { viewModel.sendMouseClick(rightClick = true) },
                shape = RoundedCornerShape(12.dp),
                color = KeyBg,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Right Click", color = Color.White)
                }
            }
        }
    }
}
