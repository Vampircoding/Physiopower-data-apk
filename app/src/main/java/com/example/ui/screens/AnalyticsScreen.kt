package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.Group
import com.example.data.model.GroupSummary
import com.example.data.model.MemberBalance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    group: Group,
    summary: GroupSummary?,
    expenses: List<Expense>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${group.name} Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("analytics_back_button")) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (expenses.isEmpty() || summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("📊", fontSize = 60.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Expenses Available",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Log roommate rents, flight bookings, or utility bills first to compile beautiful dynamic analytics charts!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Compile category splits
            val categoryTotals = expenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val totalAmount = categoryTotals.values.sum()

            // Map color palette for standard expense categories
            val categoryColors = mapOf(
                "Rent" to Color(0xFFF43F5E), // Coral
                "Electricity" to Color(0xFFEAB308), // Yellow
                "Water" to Color(0xFF3B82F6), // Blue
                "Internet" to Color(0xFF06B6D4), // Cyan
                "Gas" to Color(0xFFF97316), // Orange
                "Maid" to Color(0xFF8B5CF6), // Purple
                "Groceries" to Color(0xFF10B981), // Emerald Green
                "Hotel" to Color(0xFFEC4899), // Pink
                "Flight" to Color(0xFF14B8A6), // Teal
                "Train" to Color(0xFF6366F1), // Indigo
                "Taxi" to Color(0xFF0EA5E9), // Sky Blue
                "Fuel" to Color(0xFFF59E0B), // Amber Yellow
                "Food" to Color(0xFFEF4444), // Crimson
                "Shopping" to Color(0xFFD946EF), // Fuchsia
                "Activities" to Color(0xFF84CC16), // Lime Green
                "Emergency" to Color(0xFFDC2626), // Deep Red
                "Other Expenses" to Color(0xFF64748B) // Slate Gray
            )

            val defaultColor = Color(0xFF94A3B8)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Header Stats
                item {
                    Text(
                        "Overview Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Expenses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                    Text(
                                        "${group.currency} ${String.format("%.2f", totalAmount)}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Cost Per Person", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                    Text(
                                        "${group.currency} ${String.format("%.2f", summary.perPersonExpense)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Budget alerts if targetBudget is set
                            if (group.targetBudget > 0.0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val pct = (totalAmount / group.targetBudget).coerceIn(0.0, 2.0)
                                Text(
                                    "Monthly Budget Limit: ${group.currency} ${group.targetBudget}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (pct > 0.9) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${String.format("%.1f", pct * 100)}% consumed",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (pct > 0.9) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    if (pct > 1.0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Warning,
                                                "Over budget limit",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Budget Overrun!", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Donut Share Breakdown
                item {
                    Text(
                        "Category Dispersal Chart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Draw a beautiful custom donut chart on a Canvas
                            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var startAngle = -90f
                                    categoryTotals.forEach { (cat, amt) ->
                                        val sweep = ((amt / totalAmount) * 360f).toFloat()
                                        val color = categoryColors[cat] ?: defaultColor
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round),
                                            size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()),
                                            topLeft = Offset(12.dp.toPx(), 12.dp.toPx())
                                        )
                                        startAngle += sweep
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(
                                        "${expenses.size} Bills",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Custom Legend
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryTotals.toList().sortedByDescending { it.second }.forEach { (cat, amt) ->
                                    val color = categoryColors[cat] ?: defaultColor
                                    val percent = (amt / totalAmount) * 100
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Text(
                                            "${group.currency} ${String.format("%.2f", amt)} (${String.format("%.1f", percent)}%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Individual Member Contributions (Progress Splitting)
                item {
                    Text(
                        "Member Financial Weight",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Total funding paid by each split member:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val maxPaid = summary.memberBalances.maxOfOrNull { it.totalPaid } ?: 1.0

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                summary.memberBalances.sortedByDescending { it.totalPaid }.forEach { mb ->
                                    val fraction = if (maxPaid > 0) (mb.totalPaid / maxPaid).toFloat().coerceIn(0f, 1f) else 0f
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(mb.member.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${group.currency} ${String.format("%.2f", mb.totalPaid)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
