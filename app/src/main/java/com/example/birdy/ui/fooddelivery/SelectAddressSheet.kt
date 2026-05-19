package com.example.birdy.ui.fooddelivery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// MARK: - Address Model (matches iOS Address with gateCode)

data class Address(
    val id: String,
    val street: String,
    val cityStateZip: String,
    val gateCode: String? = null,
    val isDefault: Boolean = false
)

// MARK: - Mock Addresses (will be replaced with AddressService API calls later)

private val mockAddresses = listOf(
    Address(id = "addr_1", street = "123 Main Street, Apt 4B", cityStateZip = "New York, NY 10001", gateCode = "#1234", isDefault = true),
    Address(id = "addr_2", street = "456 Broadway, Floor 12", cityStateZip = "New York, NY 10003", isDefault = false),
    Address(id = "addr_3", street = "789 Oak Avenue", cityStateZip = "Brooklyn, NY 11201", gateCode = "call manager", isDefault = false)
)

// MARK: - Select Address Bottom Sheet (matches iOS SelectAddress)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAddressSheet(
    currentAddressId: String?,
    onAddressSelected: (Address) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var selectedId by remember { mutableStateOf(currentAddressId) }

    // Gate code states
    var showGateCodeSheet by remember { mutableStateOf(false) }
    var gateCodeInput by remember { mutableStateOf("") }
    var editingAddressId by remember { mutableStateOf<String?>(null) }
    var isAddingNewAddress by remember { mutableStateOf(false) }

    // Local mutable addresses list (so we can update gate codes)
    val localAddresses = remember { mutableStateListOf(*mockAddresses.toTypedArray()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .background(Color.White)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Address",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )

                if (selectedId != null) {
                    // Green checkmark to confirm and close
                    IconButton(
                        onClick = {
                            // Find the selected address and notify parent
                            val selected = localAddresses.find { it.id == selectedId }
                            if (selected != null) {
                                onAddressSelected(selected)
                            }
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Confirm",
                            tint = Color(0xFF4CAF50), // Green
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    // X button when nothing selected
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

            // Scrollable address list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Location option
                AddressSelectionRow(
                    title = "Current Location",
                    subtitle = "Use your GPS location",
                    icon = Icons.Default.LocationOn,
                    gateCode = null,
                    isSelected = selectedId == "current_location",
                    onClick = {
                        selectedId = "current_location"
                        onAddressSelected(
                            Address(
                                id = "current_location",
                                street = "Current Location",
                                cityStateZip = "Using GPS"
                            )
                        )
                        onDismiss()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.Gray.copy(alpha = 0.2f)
                )

                // Saved addresses
                localAddresses.forEach { address ->
                    AddressSelectionRow(
                        title = address.street,
                        subtitle = address.cityStateZip,
                        icon = if (address.isDefault) Icons.Default.Star else Icons.Default.LocationOn,
                        gateCode = address.gateCode,
                        isSelected = selectedId == address.id,
                        onClick = {
                            selectedId = address.id
                            onAddressSelected(address)
                            onDismiss()
                        },
                        onEditGateCode = {
                            editingAddressId = address.id
                            gateCodeInput = address.gateCode ?: ""
                            isAddingNewAddress = false
                            showGateCodeSheet = true
                        }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.Gray.copy(alpha = 0.2f)
                )

                // Add New Address button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Show gate code sheet for new address
                            isAddingNewAddress = true
                            gateCodeInput = ""
                            editingAddressId = null
                            showGateCodeSheet = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Add New Address",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // MARK: - Gate Code Sheet
    if (showGateCodeSheet) {
        GateCodeEntrySheet(
            initialGateCode = gateCodeInput,
            isNewAddress = isAddingNewAddress,
            onSave = { code ->
                showGateCodeSheet = false
                if (isAddingNewAddress) {
                    // TODO: Navigate to address search, then save gate code
                    // For now just show a placeholder
                } else {
                    // Update existing address gate code
                    val addrId = editingAddressId
                    if (addrId != null) {
                        val idx = localAddresses.indexOfFirst { it.id == addrId }
                        if (idx >= 0) {
                            val old = localAddresses[idx]
                            localAddresses[idx] = old.copy(gateCode = code.ifEmpty { null })
                        }
                    }
                }
                editingAddressId = null
                isAddingNewAddress = false
            },
            onSkip = {
                showGateCodeSheet = false
                editingAddressId = null
                isAddingNewAddress = false
            }
        )
    }
}

// MARK: - Gate Code Entry Sheet (matches iOS GateCodeEntrySheet)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GateCodeEntrySheet(
    initialGateCode: String,
    isNewAddress: Boolean,
    onSave: (String) -> Unit,
    onSkip: () -> Unit
) {
    var gateCode by remember { mutableStateOf(initialGateCode) }

    ModalBottomSheet(
        onDismissRequest = onSkip,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .background(Color(0xFFD1D1D1), RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFFCC5500), // Orange
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Gate / Door Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Add a gate or door PIN so your driver can enter at arrival.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Input field
            OutlinedTextField(
                value = gateCode,
                onValueChange = { gateCode = it },
                placeholder = {
                    Text(
                        text = "e.g. #1234, call manager for code, etc.",
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFCC5500),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    cursorColor = Color(0xFFCC5500)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy disclaimer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Your Privacy",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
                Text(
                    text = "Your gate code is never shared publicly or stored in plain text. It is only shared with your U-DO driver at the time of arrival at your address.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        if (gateCode.isEmpty()) Color.Gray else Color(0xFFCC5500),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSave(gateCode) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (gateCode.isEmpty()) "Save without code" else "Save & Continue",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skip button
            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip for now",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// MARK: - Address Selection Row (matches iOS AddressSelectionRow)

@Composable
private fun AddressSelectionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gateCode: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditGateCode: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFF3F3F3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (icon == Icons.Default.Star) Color(0xFFFFC107) else Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Address text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1
                )

                // Gate code badge
                if (!gateCode.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFCC5500).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .clickable { onEditGateCode?.invoke() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFCC5500),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "Gate: $gateCode",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCC5500)
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFFCC5500),
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkmark or empty circle
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50), // Green
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color.Transparent, CircleShape)
                )
            }
        }
    }
}