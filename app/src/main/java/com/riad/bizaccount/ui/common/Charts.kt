package com.riad.bizaccount.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Simple two-slice pie chart for income vs expense, drawn directly on Canvas. */
@Composable
fun IncomeExpensePieChart(incomeMinor: Long, expenseMinor: Long, incomeColor: Color, expenseColor: Color) {
    val total = (incomeMinor + expenseMinor).coerceAtLeast(1)
    val incomeSweep = 360f * incomeMinor / total

    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(90.dp)) {
            drawArc(color = incomeColor, startAngle = -90f, sweepAngle = incomeSweep, useCenter = true)
            drawArc(color = expenseColor, startAngle = -90f + incomeSweep, sweepAngle = 360f - incomeSweep, useCenter = true)
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            LegendRow(color = incomeColor, label = "আয়")
            LegendRow(color = expenseColor, label = "ব্যয়")
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(text = label, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Simple vertical bar chart, e.g. for a monthly income/expense trend, fixed pixel height. */
@Composable
fun SimpleBarChart(values: List<Float>, barColor: Color, modifier: Modifier = Modifier, chartHeight: androidx.compose.ui.unit.Dp = 120.dp) {
    val maxVal = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Row(
        modifier = modifier.fillMaxWidth().height(chartHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEach { v ->
            val fraction = (v / maxVal).coerceIn(0.02f, 1f)
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .fillMaxWidth(1f / values.size)
                    .fillMaxHeight(fraction)
                    .background(barColor)
            )
        }
    }
}
