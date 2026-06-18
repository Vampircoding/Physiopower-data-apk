package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GroupMember
import kotlinx.coroutines.delay

@Composable
fun UpiPaymentSheet(
    debtor: GroupMember,
    creditor: GroupMember,
    amount: Double,
    currency: String,
    onPaymentSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var paymentStage by remember { mutableStateOf("SUMMARY") } // SUMMARY, SCANNER, SUCCESS

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Page")
            }

            AnimatedContent(
                targetState = paymentStage,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "PaymentStages"
            ) { stage ->
                when (stage) {
                    "SUMMARY" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Settlement QR Link",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Transfer representation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("From", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                    Text(debtor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Text("⮕", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("To", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                    Text(creditor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "$currency ${String.format("%.2f", amount)}",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "UPI ID: ${creditor.upiId.ifBlank { "${creditor.name.lowercase()}@ybl" }}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Buttons
                            Button(
                                onClick = { paymentStage = "SCANNER" },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = "Scan")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Indian UPI QR")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    paymentStage = "SUCCESS"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Simulate Direct UPI Settlement")
                            }
                        }
                    }

                    "SCANNER" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Scan UPI QR to Pay",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "Transfer ring to ${creditor.name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Custom drawing of a realistic fintech QR code
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Draw concentric squares mimicking QR finder patterns
                                    val sizeFactor = size.width
                                    val innerPad = 12f

                                    // Top-left block
                                    drawRect(Color.Black, Offset(0f, 0f), Size(50f, 50f))
                                    drawRect(Color.White, Offset(10f, 10f), Size(30f, 30f))
                                    drawRect(Color.Black, Offset(15f, 15f), Size(20f, 20f))

                                    // Top-right block
                                    drawRect(Color.Black, Offset(sizeFactor - 50f, 0f), Size(50f, 50f))
                                    drawRect(Color.White, Offset(sizeFactor - 40f, 10f), Size(30f, 30f))
                                    drawRect(Color.Black, Offset(sizeFactor - 35f, 15f), Size(20f, 20f))

                                    // Bottom-left block
                                    drawRect(Color.Black, Offset(0f, sizeFactor - 50f), Size(50f, 50f))
                                    drawRect(Color.White, Offset(10f, sizeFactor - 40f), Size(30f, 30f))
                                    drawRect(Color.Black, Offset(15f, sizeFactor - 35f), Size(20f, 20f))

                                    // Draw random barcode hashes mimicking QR segments
                                    val hashColor = Color(0xFF1E293B)
                                    drawRect(hashColor, Offset(70f, 10f), Size(40f, 15f))
                                    drawRect(hashColor, Offset(120f, 30f), Size(20f, 20f))
                                    drawRect(hashColor, Offset(10f, 70f), Size(15f, 50f))
                                    drawRect(hashColor, Offset(70f, 70f), Size(60f, 60f))
                                    drawRect(hashColor, Offset(150f, 70f), Size(40f, 15f))

                                    drawRect(hashColor, Offset(10f, 140f), Size(50f, 15f))
                                    drawRect(hashColor, Offset(80f, 150f), Size(30f, 40f))
                                    drawRect(hashColor, Offset(140f, 120f), Size(50f, 50f))

                                    // Center UPI Logo representation
                                    drawCircle(Color(0xFF2563EB), 22f, center = Offset(sizeFactor / 2, sizeFactor / 2))
                                }
                                Text("UPI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "$currency ${String.format("%.2f", amount)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFFFFF3CD),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Disclaimer", tint = Color(0xFF856404))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Scan with BHIM, Google Pay, PhonePe or Paytm to settle using standard UPI protocols.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF856404)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { paymentStage = "SUCCESS" },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mark Settle Complete")
                            }
                        }
                    }

                    "SUCCESS" -> {
                        LaunchedEffect(key1 = true) {
                            delay(1800)
                            onPaymentSuccess()
                            onDismiss()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "Tick")
                            val tickScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "ScaleTick"
                            )

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Checked Sync",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(80.dp)
                                    .animateContentSize()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Settlement Logged!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${debtor.name} paid $currency ${String.format("%.2f", amount)} to ${creditor.name} successfully inside Sagar Split Pro.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
