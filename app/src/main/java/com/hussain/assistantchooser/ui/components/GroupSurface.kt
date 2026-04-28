package com.hussain.assistantchooser.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GroupSurface(
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int, shape: RoundedCornerShape) -> Unit
) {
    Column(modifier = modifier) {
        repeat(count) { i ->
            content(i, getGroupShape(i, count))
            if (i != count - 1) Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

fun getGroupShape(index: Int, count: Int): RoundedCornerShape {
    return when {
        count == 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp,
            bottomStart = 8.dp, bottomEnd = 8.dp
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = 8.dp, topEnd = 8.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp
        )
        else -> RoundedCornerShape(8.dp)
    }
}
