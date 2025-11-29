package com.example.arena.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import kotlin.math.roundToInt

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SlideToConfirmButton(
    text: String = "SLIDE TO CONFIRM",
    onConfirm: () -> Unit
) {
    val width = 300.dp
    val dragSize = 50.dp

    val swipeableState = rememberSwipeableState(0)
    val sizePx = with(LocalDensity.current) { (width - dragSize).toPx() }
    val anchors = mapOf(0f to 0, sizePx to 1)


    if (swipeableState.currentValue == 1) {
        LaunchedEffect(Unit) {
            onConfirm()


        }
    }


    val progress = (swipeableState.offset.value / sizePx).coerceIn(0f, 1f)
    val alpha by animateFloatAsState(targetValue = 1f - progress)

    Box(
        modifier = Modifier
            .width(width)
            .height(60.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF1A1A1A))
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.8f) },
                orientation = Orientation.Horizontal
            )
    ) {

        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(alpha)
        )


        Box(
            modifier = Modifier
                .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
                .size(60.dp)
                .padding(4.dp)
                .clip(CircleShape)
                .background(ArenaGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ArenaBlack,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}