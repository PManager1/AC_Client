package com.example.birdy.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitorDetailsView(
    subtotal: Double,
    competitorEstimate: Double,
    onDismiss: () -> Unit
) {
    val markedUpSubtotal = subtotal * 1.15
    val serviceFee = markedUpSubtotal * 0.10
    val deliveryFee = 4.99
    val tax = (markedUpSubtotal + serviceFee + deliveryFee) * 0.08

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(Color.White)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Price Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray
                )
            }
        }

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "On Competitor Platforms*",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                CheckoutPriceRow(title = "Item subtotal", amount = subtotal)
                CheckoutPriceRow(title = "Hidden markup (15%)", amount = markedUpSubtotal - subtotal)
                CheckoutPriceRow(title = "Service fee (10%)", amount = serviceFee)
                CheckoutPriceRow(title = "Delivery fee", amount = deliveryFee)
                CheckoutPriceRow(title = "Tax (8%)", amount = tax)

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                CheckoutPriceRow(title = "Total", amount = competitorEstimate, isBold = true)
            }
        }
    }
}
