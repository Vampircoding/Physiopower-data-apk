package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Group
import com.example.data.remote.OcrResult
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.OcrScanState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScannerScreen(
    viewModel: MainViewModel,
    groups: List<Group>,
    onBack: () -> Unit
) {
    val ocrState by viewModel.ocrState.collectAsState()
    val scope = rememberCoroutineScope()

    var customOcrText by remember { mutableStateOf("") }
    var selectedGroupForAdd by remember { mutableStateOf<Group?>(null) }
    var selectGroupDropdown by remember { mutableStateOf(false) }

    // Preset sample scanner candidates to help user demo OCR fast
    val presets = listOf(
        Triple(
            "Goa Beach Dinner Bill",
            "₹4,480.00",
            """
            SALIM BEACH COVE RETREAT GOA
            Date: 12-06-2026 Time: 21:40
            ------------------------------------
            Seafood Platter          2,800.00
            Mocktail Coolers           700.00
            Premium Shakes             350.00
            CGST Tax 9%                315.00
            SGST Tax 9%                315.00
            ------------------------------------
            TOTAL AMOUNT DUE:      ₹4,480.00
            ------------------------------------
            Thank you for dining in Goa!
            """.trimIndent()
        ),
        Triple(
            "DMart Grocery Bill",
            "₹3,410.00",
            """
            D-MART SUPERMARKET RAJAJINAGAR
            BANGALORE SOUTH - CASH MEMO
            Date: 17/06/2026
            ------------------------------------
            Premium Gold Oil 5L        750.00
            Basmati Rice 10kg        1,100.00
            Toor Dal 5kg               350.00
            Home spices pack           480.00
            House cleaners             730.00
            ------------------------------------
            GRAND TOTAL:           ₹3,410.00
            ------------------------------------
            Saved ₹240 with DMart discounts!
            """.trimIndent()
        ),
        Triple(
            "Apartment Electricity Bill",
            "₹2,150.00",
            """
            BESCOM ELEC UTILITY BANGALORE
            Subdivision South East - Room invoice
            Invoice Period: 01-05-2026 to 01-06-2026
            Meter Reading: 4421 Kw
            Consumption units: 180
            ------------------------------------
            Fixed Charges              550.00
            Surcharge                  120.00
            Energy Tariff Consumed   1,480.00
            ------------------------------------
            TOTAL BILL AMOUNT:     ₹2,150.00
            ------------------------------------
            Due Date: 28-06-2026
            """.trimIndent()
        )
    )

    // Set default target group if any exists
    LaunchedEffect(groups) {
        if (selectedGroupForAdd == null && groups.isNotEmpty()) {
            selectedGroupForAdd = groups.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Receipt OCR Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ocr_back_button")) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DocumentScanner, "Scan logo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Fast Expense OCR Parsing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Scan paper bills or receipt printouts via camera, or use preloaded receipts. Sagar Split Pro's smart OCR instantly extracts amounts, merchants, dates and categories utilizing Gemini models!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Scanner progression handler
            item {
                AnimatedContent(
                    targetState = ocrState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "OcrStatusCard"
                ) { state ->
                    when (state) {
                        is OcrScanState.Idle -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Select Sample Receipt to Scan:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        presets.forEach { (name, total, text) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        customOcrText = text
                                                        viewModel.executeOcrScanning(text)
                                                    }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Receipt, "Receipt Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(total, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Default.ChevronRight, "Arrow", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("Or Paste Custom Bill Receipt Details manually:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = customOcrText,
                                        onValueChange = { customOcrText = it },
                                        placeholder = { Text("Paste raw supermarket receipts, restaurant bills, petrol vouchers, rent printouts...", fontSize = 12.sp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            if (customOcrText.isNotBlank()) {
                                                viewModel.executeOcrScanning(customOcrText)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = customOcrText.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("AI Scan Receipt Text")
                                    }
                                }
                            }
                        }

                        is OcrScanState.Scanning -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    // Simulated Laser Line animation moving up and down
                                    val infiniteTransition = rememberInfiniteTransition("ScannerLaser")
                                    val laserYOffset by infiniteTransition.animateFloat(
                                        initialValue = 0.1f,
                                        targetValue = 0.9f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500, easing = EaseInOutSine),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "LaserLaser"
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CenterFocusWeak,
                                            "Scanning",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Gemini AI analyzing receipt ledger...",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "Extracting amounts, dates, and categories",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    // Holographic laser line brush
                                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                        val laserHeight = constraints.maxHeight * laserYOffset
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .offset(y = (laserHeight / 2).dp)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color.Transparent, Color(0xFF10B981), Color.Transparent)
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        is OcrScanState.Success -> {
                            val res = state.result
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, "OCR Success", tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "OCR Scan Success",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(onClick = { viewModel.resetOcrScanning() }) {
                                            Icon(Icons.Default.Refresh, "Scan again", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Render Extracted Fields
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OcrFieldRow("Merchant", res.merchant)
                                        OcrFieldRow("Extracted Amount", "₹ ${String.format("%.2f", res.amount)}")
                                        OcrFieldRow("Transaction Date", res.date)
                                        OcrFieldRow("Calculated Category", res.category)
                                        OcrFieldRow("AI Confidence Score", res.confidence)
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Add to active group logic
                                    if (groups.isEmpty()) {
                                        Text(
                                            "Please create a Splitting Group first to add this extracted expense.",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text("Select active Group to split this bill:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Dropdown group selector
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectGroupDropdown = true }
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(selectedGroupForAdd?.name ?: "Select Group", fontWeight = FontWeight.Bold)
                                                Icon(Icons.Default.ArrowDropDown, "Dropdown Arrow")
                                            }

                                            DropdownMenu(
                                                expanded = selectGroupDropdown,
                                                onDismissRequest = { selectGroupDropdown = false }
                                            ) {
                                                groups.forEach { gp ->
                                                    DropdownMenuItem(
                                                        text = { Text(gp.name) },
                                                        onClick = {
                                                            selectedGroupForAdd = gp
                                                            selectGroupDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = {
                                                selectedGroupForAdd?.let { activeGp ->
                                                    scope.launch {
                                                        // Fetch active members of selected group to add evenly
                                                        val members = viewModel.getMembersForGroupSync(activeGp.id)
                                                        if (members.isNotEmpty()) {
                                                            val averageShare = res.amount / members.size
                                                            val mockShares = members.map { mb ->
                                                                com.example.data.model.ExpenseShare(
                                                                    expenseId = 0,
                                                                    memberId = mb.id,
                                                                    shareAmount = averageShare
                                                                )
                                                            }
                                                            val targetPayer = members.find { it.name == "Sagar" } ?: members.first()

                                                            viewModel.addExpense(
                                                                title = res.merchant,
                                                                amount = res.amount,
                                                                payerId = targetPayer.id,
                                                                category = res.category,
                                                                splitType = "EQUAL",
                                                                customShareAmounts = emptyMap()
                                                            )

                                                            // Auto navigate back
                                                            viewModel.selectGroup(activeGp.id)
                                                            onBack()
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("submit_ocr_expense_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Settle & Insert Expense to Group")
                                        }
                                    }
                                }
                            }
                        }

                        is OcrScanState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Error, "Error", tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Scanning Failed",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(state.message, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.resetOcrScanning() }) {
                                        Text("Retry OCR Scan")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OcrFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
