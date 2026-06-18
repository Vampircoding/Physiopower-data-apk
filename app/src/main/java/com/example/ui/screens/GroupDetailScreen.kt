package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.viewmodel.AiInsightsState
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: MainViewModel,
    group: Group,
    members: List<GroupMember>,
    expenses: List<Expense>,
    summary: GroupSummary?,
    onNavigateToAnalytics: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val aiInsightsState by viewModel.aiInsights.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val targetDisplayCurrency by viewModel.targetDisplayCurrency.collectAsState()

    var activeTab by remember { mutableStateOf("EXPENSES") } // EXPENSES, BALANCES, AI_ADVISOR
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showUpiTransferSheet by remember { mutableStateOf<SettlementSuggestion?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    // Add Expense Fields
    var expTitle by remember { mutableStateOf("") }
    var expAmount by remember { mutableStateOf("") }
    var expPayerId by remember { mutableStateOf<Long?>(null) }
    var expCategory by remember { mutableStateOf("") }
    var expSplitType by remember { mutableStateOf("EQUAL") } // EQUAL, CUSTOM, PERCENTAGE, SHARES

    // Maps for CUSTOM, PERCENTAGE, SHARES inputs (memberId -> value)
    val splitInputs = remember { mutableStateMapOf<Long, String>() }

    // Dynamic pre-fill category categories based on Group Type
    val categories = if (group.type == "ROOM") {
        listOf("Rent", "Electricity", "Water", "Internet", "Gas", "Maid", "Groceries", "Maintenance", "Other Expenses")
    } else {
        listOf("Hotel", "Flight", "Train", "Taxi", "Fuel", "Food", "Shopping", "Activities", "Emergency")
    }

    // Set default payer on load
    LaunchedEffect(members) {
        if (expPayerId == null && members.isNotEmpty()) {
            expPayerId = members.first().id
        }
        members.forEach {
            if (!splitInputs.containsKey(it.id)) {
                splitInputs[it.id] = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(group.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (group.type == "ROOM") "Monthly flat shares • Created by Sagar" else "Tour expenses • Created by Sagar",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("group_back_button")) {
                        Icon(Icons.Default.ChevronLeft, "Back")
                    }
                },
                actions = {
                    // Quick Analytics link
                    IconButton(onClick = onNavigateToAnalytics, modifier = Modifier.testTag("group_analytics_button")) {
                        Icon(Icons.Default.BarChart, "Analytics", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Share Text report
                    IconButton(onClick = {
                        ReportsExporter.shareReportViaText(context, group, summary, members, expenses)
                    }, modifier = Modifier.testTag("group_share_button")) {
                        Icon(Icons.Default.Share, "Share text", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Export Excel Sheet
                    IconButton(onClick = {
                        ReportsExporter.exportExcelCsvReport(context, group, summary, members, expenses)
                    }, modifier = Modifier.testTag("group_excel_button")) {
                        Icon(Icons.Default.TableChart, "Excel Export", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeTab == "EXPENSES") {
                FloatingActionButton(
                    onClick = {
                        expTitle = ""
                        expAmount = ""
                        expCategory = categories.first()
                        expSplitType = "EQUAL"
                        members.forEach { splitInputs[it.id] = "" }
                        showAddExpenseDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_expense_fab")
                ) {
                    Icon(Icons.Default.AddCard, "Add Card")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Segmented Tab bar
            TabRow(
                selectedTabIndex = when (activeTab) {
                    "EXPENSES" -> 0
                    "BALANCES" -> 1
                    else -> 2
                }
            ) {
                Tab(
                    selected = activeTab == "EXPENSES",
                    onClick = { activeTab = "EXPENSES" },
                    text = { Text("Bills Ledger", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == "BALANCES",
                    onClick = { activeTab = "BALANCES" },
                    text = { Text("Settle Balances", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == "AI_ADVISOR",
                    onClick = { activeTab = "AI_ADVISOR" },
                    text = { Text("Smart AI", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Quick display target selector for foreign multi currencies
            if (group.type == "TRIP") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Display Currency:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("INR", "USD", "EUR", "AED", "GBP").forEach { curr ->
                            val active = targetDisplayCurrency == curr
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.targetDisplayCurrency.value = curr }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = curr,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "DetailPages",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { tab ->
                when (tab) {
                    "EXPENSES" -> {
                        if (expenses.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                    Text("📝", fontSize = 54.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No bills registered", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Tap the floating card button to split your flat rent, electricity meter, or Goa paragliding charges evenly or customs!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    // Room warning alert / rent alarms
                                    if (group.type == "ROOM" && group.rentDueDate > 0L) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Alarm, "Alarm", tint = Color(0xFFB45309))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Rent Due Date Reminder: Flat shares rent should be settled on or before the ${group.rentDueDate}th of this month!",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFB45309),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                items(expenses) { exp ->
                                    val payer = members.find { it.id == exp.paidByMemberId }?.name ?: "Unknown"

                                    // Local Currency Conversion calculations
                                    val renderedAmount = viewModel.convertAmount(exp.amount, group.currency)
                                    val showCurr = if (group.type == "TRIP") targetDisplayCurrency else group.currency

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(exp.category.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column {
                                                    Text(exp.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        text = "Paid by $payer • Category: ${exp.category}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        "$showCurr ${String.format("%.2f", renderedAmount)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "Split: ${exp.splitType}",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                IconButton(
                                                    onClick = { viewModel.deleteExpense(exp.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        "Delete",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "BALANCES" -> {
                        if (summary == null || members.size < 2) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Invite members onto the group first to calculate auto-reductions.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Members list chip
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Invited Roommates", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        IconButton(onClick = { showAddMemberDialog = true }) {
                                            Icon(Icons.Default.PersonAdd, "Add roommate", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        members.forEach { mb ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(mb.name.take(1).uppercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(mb.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    if (mb.upiId.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("(${mb.upiId})", fontSize = 10.sp, color = Color(0xFF2563EB))
                                                    }
                                                }

                                                // Do not delete owner 'Sagar'
                                                if (mb.name != "Sagar") {
                                                    IconButton(
                                                        onClick = { viewModel.deleteMemberFromGroup(mb.id) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Settlements
                                item {
                                    Text("Minimum Transactions Settlement Suggestions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }

                                if (summary.settlements.isEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.1f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🎉", fontSize = 38.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("All balances are completely settled!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                } else {
                                    items(summary.settlements) { st ->
                                        val cAmt = viewModel.convertAmount(st.amount, group.currency)
                                        val showCurr = if (group.type == "TRIP") targetDisplayCurrency else group.currency

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showUpiTransferSheet = st },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(st.debtor.name, fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                                                        Text(" owes ", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 13.sp)
                                                        Text(st.creditor.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    if (st.creditor.upiId.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Click to Pay via UPI: ${st.creditor.upiId}", fontSize = 10.sp, color = Color(0xFF2563EB))
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        "$showCurr ${String.format("%.2f", cAmt)}",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Icon(Icons.Default.ChevronRight, "Settle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "AI_ADVISOR" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🔮", fontSize = 28.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Sagar Split Pro AI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            "Our smart advisor audits flatmate electricity/water bills, grocery layouts, or holiday packages. Let AI suggest cost savings and forecast future trends!",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = { viewModel.triggerAiMetricsAnalysis() },
                                            modifier = Modifier.fillMaxWidth().testTag("add_ai_analysis_button")
                                        ) {
                                            Text("Fetch AI Split Analysis")
                                        }
                                    }
                                }
                            }

                            item {
                                AnimatedContent(
                                    targetState = aiInsightsState,
                                    label = "AiProgress"
                                ) { state ->
                                    when (state) {
                                        is AiInsightsState.Idle -> {
                                            Text("Tap the button above to request Gemini's audits.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                        }
                                        is AiInsightsState.Loading -> {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text("Gemini is crunching spending indices...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        is AiInsightsState.Success -> {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(
                                                        text = state.insights,
                                                        fontSize = 13.sp,
                                                        lineHeight = 18.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                        is AiInsightsState.Error -> {
                                            Text("Failed to retrieve insights: ${state.message}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Expense Modal
        if (showAddExpenseDialog) {
            AlertDialog(
                onDismissRequest = { showAddExpenseDialog = false },
                title = { Text("Log Split Expense") },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = expTitle,
                                onValueChange = { expTitle = it },
                                label = { Text("Bill Title (e.g., Internet bill)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = expAmount,
                                onValueChange = { expAmount = it },
                                label = { Text("Amount (${group.currency})") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        // Payer list
                        item {
                            Text("Who Paid this?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                members.forEach { mb ->
                                    FilterChip(
                                        selected = expPayerId == mb.id,
                                        onClick = { expPayerId = mb.id },
                                        label = { Text(mb.name) }
                                    )
                                }
                            }
                        }

                        // Category Choose list
                        item {
                            Text("Select Bill Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.take(4).forEach { cat ->
                                    ElevatedFilterChip(
                                        selected = expCategory == cat,
                                        onClick = { expCategory = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }
                        }

                        // Split rules config
                        item {
                            Text("Split parameters:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                listOf("EQUAL", "CUSTOM", "PERCENTAGE", "SHARES").forEach { rule ->
                                    FilterChip(
                                        selected = expSplitType == rule,
                                        onClick = { expSplitType = rule },
                                        label = { Text(rule) }
                                    )
                                }
                            }
                        }

                        // Custom splitting details slider inputs
                        if (expSplitType != "EQUAL") {
                            item {
                                Text("Assign Custom factors for each roommate:", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }

                            items(members) { mb ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(mb.name, fontSize = 13.sp)
                                    OutlinedTextField(
                                        value = splitInputs[mb.id] ?: "",
                                        onValueChange = { splitInputs[mb.id] = it },
                                        placeholder = { Text(if (expSplitType == "PERCENTAGE") "%" else "Amt") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(100.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = expAmount.toDoubleOrNull() ?: 0.0
                            val payerId = expPayerId
                            if (expTitle.isNotBlank() && amt > 0.0 && payerId != null) {
                                // Compile share details
                                val customShares = mutableMapOf<Long, Double>()
                                if (expSplitType != "EQUAL") {
                                    members.forEach { mb ->
                                        val sVal = splitInputs[mb.id]?.toDoubleOrNull() ?: 0.0
                                        customShares[mb.id] = sVal
                                    }
                                }

                                viewModel.addExpense(
                                    title = expTitle,
                                    amount = amt,
                                    payerId = payerId,
                                    category = expCategory,
                                    splitType = expSplitType,
                                    customShareAmounts = customShares
                                )
                                showAddExpenseDialog = false
                            } else {
                                Toast.makeText(context, "Fill titles and non-zero amounts correctly", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = expTitle.isNotBlank() && expAmount.isNotBlank()
                    ) {
                        Text("Add Bill")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddExpenseDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Member Modal
        if (showAddMemberDialog) {
            var mName by remember { mutableStateOf("") }
            var mUpi by remember { mutableStateOf("") }
            var mPhone by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddMemberDialog = false },
                title = { Text("Invite Roommate / Member") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = mName,
                            onValueChange = { mName = it },
                            label = { Text("Full Name (e.g., Karan Kumar)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = mUpi,
                            onValueChange = { mUpi = it },
                            label = { Text("UPI ID (e.g., karan@paytm)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = mPhone,
                            onValueChange = { mPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (mName.isNotBlank()) {
                                viewModel.addMemberToSelectedGroup(mName, mUpi, mPhone)
                                showAddMemberDialog = false
                            }
                        },
                        enabled = mName.isNotBlank()
                    ) {
                        Text("Invite")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMemberDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // UPI Pay Screen BottomSheet modal
        if (showUpiTransferSheet != null) {
            val st = showUpiTransferSheet!!
            AlertDialog(
                onDismissRequest = { showUpiTransferSheet = null },
                confirmButton = {},
                text = {
                    UpiPaymentSheet(
                        debtor = st.debtor,
                        creditor = st.creditor,
                        amount = st.amount,
                        currency = group.currency,
                        onPaymentSuccess = {
                            Toast.makeText(context, "UPI Settle recorded!", Toast.LENGTH_SHORT).show()
                            // Mark settlement mathematically by registering a counter refund expense!
                            viewModel.addExpense(
                                title = "Settle Reversal: ${st.debtor.name} to ${st.creditor.name}",
                                amount = st.amount,
                                payerId = st.debtor.id,
                                category = "Emergency",
                                splitType = "CUSTOM",
                                customShareAmounts = mapOf(
                                    st.debtor.id to 0.0,
                                    st.creditor.id to st.amount
                                )
                            )
                        },
                        onDismiss = { showUpiTransferSheet = null }
                    )
                }
            )
        }
    }
}
