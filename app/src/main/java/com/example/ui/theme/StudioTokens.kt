package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object StudioSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val section = 28.dp
}

object StudioRadii {
    val micro = 4.dp
    val control = 8.dp
    val frame = 12.dp
}

object StudioDimensions {
    val minTouchTarget = 48.dp
    val primaryActionHeight = 52.dp
    val dividerThickness = 1.dp
}

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(StudioRadii.micro),
    small = RoundedCornerShape(StudioRadii.control),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(StudioRadii.frame),
    extraLarge = RoundedCornerShape(StudioRadii.frame),
)
