package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Group
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    groups: List<Group>,
    onNavigateToGroup: (Long) -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val isPremium by viewModel.isPremium.collectAsState()
    val showBannerAd by viewModel.showBannerAd.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showCreateGroupDialog by remember { mutableStateOf(false) }

    // Dialog inputs
    var gName by remember { mutableStateOf("") }
    var gType by remember { mutableStateOf("ROOM") } // "ROOM" or "TRIP"
    var gDesc by remember { mutableStateOf("") }
    var gCurrency by remember { mutableStateOf("INR") }
    var gBudget by remember { mutableStateOf("") }
    var gRentDue by remember { mutableStateOf("") }
    var gMembersText by remember { mutableStateOf("") }

    // Floating sync rotate animation
    val syncRotationTransition = rememberInfiniteTransition("SyncRotation")
    val syncAngle by syncRotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "Angle"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("S", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sagar Split Pro", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                },
                actions = {
                    // Sync icon button
                    IconButton(onClick = { viewModel.triggerCloudSyncBackup() }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Cloud Backup",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(if (isSyncing) syncAngle else 0f)
                        )
                    }

                    // Settings page access
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("home_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    gName = ""
                    gDesc = ""
                    gType = "ROOM"
                    gCurrency = "INR"
                    gBudget = ""
                    gRentDue = ""
                    gMembersText = ""
                    showCreateGroupDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_group_fab")
            ) {
                Icon(Icons.Default.Add, "Plus")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Split Group")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Net Balance overview card presenting user's standing across splits (Professional Polish themed)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Total Net Balance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "₹ 12,450.00",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        // OCR quick scanner button styled as a modern button on primary background
                        Button(
                            onClick = onNavigateToOcr,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("ocr_banner_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Bill", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // You owe / You are owed divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "YOU ARE OWED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "₹ 15,200",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "YOU OWE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "₹ 2,750",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFCDD2) // Elegant soft red/pink representing debt
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Created by Sagar • Multi-currency • Indian UPI enabled",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // Sync loading banner details
            AnimatedVisibility(visible = isSyncing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Synchronizing local Room databases with secure Cloud databases...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Group split list
            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🤝", fontSize = 60.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Splitting Groups Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Hit the green Create Split Group button to split flat bills or trip costs instantly!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups) { gp ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_card_${gp.id}")
                                .clickable { onNavigateToGroup(gp.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (gp.type == "ROOM") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                    else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (gp.type == "ROOM") Icons.Default.Home else Icons.Default.FlightTakeoff,
                                                contentDescription = "Group Type",
                                                tint = if (gp.type == "ROOM") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(gp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(
                                                text = if (gp.type == "ROOM") "Room split • ${gp.currency}" else "Trip tour • ${gp.currency}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (gp.isSynced) {
                                            Icon(
                                                Icons.Default.CloudQueue,
                                                "Synced",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            "Explore",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (gp.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        gp.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Google AdMob Screen Simulator
            if (showBannerAd && !isPremium) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "GOOGLE ADMOB SPONSOR AD",
                            color = Color.LightGray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Upgrade to Sagar Split Pro Premium to clear all banner ads!",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Get unlimited room ledger reports • Advanced AI cost-forecasting analytics",
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Create Group Dialog
        if (showCreateGroupDialog) {
            AlertDialog(
                onDismissRequest = { showCreateGroupDialog = false },
                title = { Text("Create Sagar Split Group") },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = gName,
                                onValueChange = { gName = it },
                                label = { Text("Group Name (e.g., Flat 302 Room)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Text("Split Group Category Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = gType == "ROOM",
                                    onClick = { gType = "ROOM" },
                                    label = { Text("Room Flatmates") }
                                )
                                FilterChip(
                                    selected = gType == "TRIP",
                                    onClick = { gType = "TRIP" },
                                    label = { Text("Trip Vacation") }
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = gDesc,
                                onValueChange = { gDesc = it },
                                label = { Text("Description (e.g., Manali trek stays)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Text("Select Group Default Currency:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("INR", "USD", "EUR", "AED", "GBP").forEach { curr ->
                                    ElevatedFilterChip(
                                        selected = gCurrency == curr,
                                        onClick = { gCurrency = curr },
                                        label = { Text(curr) }
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = gBudget,
                                onValueChange = { gBudget = it },
                                label = { Text("Budget Warning Limit (0 = None)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        if (gType == "ROOM") {
                            item {
                                OutlinedTextField(
                                    value = gRentDue,
                                    onValueChange = { gRentDue = it },
                                    label = { Text("Monthly Rent Due Date (1-31)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = gMembersText,
                                onValueChange = { gMembersText = it },
                                label = { Text("Invite Roommates (comma-separated: Amit, Rahul)") },
                                placeholder = { Text("E.g., Rahul, Amit, Karan") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (gName.isNotBlank()) {
                                val splitMembers = gMembersText.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }

                                val budgetVal = gBudget.toDoubleOrNull() ?: 0.0
                                val rentVal = gRentDue.toLongOrNull() ?: 0L

                                viewModel.createGroup(
                                    name = gName,
                                    type = gType,
                                    description = gDesc,
                                    currency = gCurrency,
                                    budget = budgetVal,
                                    rentDue = rentVal,
                                    members = splitMembers
                                )
                                showCreateGroupDialog = false
                            }
                        },
                        enabled = gName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateGroupDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
