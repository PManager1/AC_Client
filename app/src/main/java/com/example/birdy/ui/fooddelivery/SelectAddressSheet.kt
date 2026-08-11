package com.example.birdy.ui.fooddelivery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.AddressService
import com.example.birdy.data.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// MARK: - Address Model (matches iOS Address with gateCode)

data class Address(
    val id: String,
    val street: String,
    val cityStateZip: String,
    val gateCode: String? = null,
    val label: String? = null,
    val addressType: String? = null,
    val deliveryPreference: String? = null,
    val deliveryInstructions: String? = null,
    val isGifting: Boolean = false,
    val isDefault: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
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

    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(currentAddressId) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Gate code states
    var showGateCodeSheet by remember { mutableStateOf(false) }
    var gateCodeInput by remember { mutableStateOf("") }
    var editingAddressId by remember { mutableStateOf<String?>(null) }
    var isAddingNewAddress by remember { mutableStateOf(false) }
    var showAddressSearch by remember { mutableStateOf(false) }
    var pendingAddressData by remember { mutableStateOf<AddressSearchResult?>(null) }

    // New-address options state (type + details)
    var showAddressTypeSheet by remember { mutableStateOf(false) }
    var selectedAddressType by remember { mutableStateOf("house") }
    var showAddressDetailsSheet by remember { mutableStateOf(false) }
    var deliveryPreference by remember { mutableStateOf("leave_at_door") }
    var deliveryInstructions by remember { mutableStateOf("") }
    var personalLabel by remember { mutableStateOf("none") }
    var customLabel by remember { mutableStateOf("") }
    var isGifting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Addresses fetched from API
    val localAddresses = remember { mutableStateListOf<Address>() }

    LaunchedEffect(Unit) {
        val token = AuthManager.getToken(context)
        if (token.isNullOrEmpty()) {
            errorMessage = "Not authenticated"
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val fetched = withContext(Dispatchers.IO) {
                AddressService.getAddresses(token)
            }
            localAddresses.clear()
            localAddresses.addAll(fetched)
        } catch (e: Exception) {
            errorMessage = "Failed to load addresses"
        }
        isLoading = false
    }

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
                .verticalScroll(rememberScrollState())
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
                            tint = Color(0xFFD95F02), // Burnt orange
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

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading addresses...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Red
                    )
                }
            } else {
                // Saved addresses
                localAddresses.forEach { address ->
                    AddressSelectionRow(
                        title = address.street,
                        subtitle = address.cityStateZip,
                        icon = if (address.isDefault) Icons.Default.Star else Icons.Default.LocationOn,
                        label = address.label,
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
                            isAddingNewAddress = true
                            gateCodeInput = ""
                            editingAddressId = null
                            selectedAddressType = "house"
                            deliveryPreference = "leave_at_door"
                            deliveryInstructions = ""
                            personalLabel = "none"
                            customLabel = ""
                            isGifting = false
                            showAddressSearch = true
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
                        color = Color.Black
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
                    val data = pendingAddressData
                    if (data != null) {
                        scope.launch {
                            val token = AuthManager.getToken(context)
                            if (!token.isNullOrEmpty()) {
                                val created = withContext(Dispatchers.IO) {
                                    AddressService.createAddress(
                                        data.street, data.cityStateZip,
                                        data.latitude, data.longitude,
                                        code.ifEmpty { null },
                                        if (personalLabel == "custom") customLabel.ifBlank { "custom" } else (if (personalLabel == "none") null else personalLabel),
                                        selectedAddressType,
                                        deliveryPreference,
                                        deliveryInstructions,
                                        isGifting,
                                        token
                                    )
                                }
                                if (created != null) {
                                    localAddresses.add(created)
                                    onAddressSelected(created)
                                    selectedId = created.id
                                }
                            }
                            pendingAddressData = null
                        }
                    }
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

    // MARK: - Address Search Screen
    if (showAddressSearch) {
        AddressSearchScreen(
            onDismiss = {
                showAddressSearch = false
                isAddingNewAddress = false
            },
            onAddressSelected = { street, cityStateZip, lat, lng ->
                showAddressSearch = false
                pendingAddressData = AddressSearchResult(street, cityStateZip, lat, lng)
                selectedAddressType = "house"
                deliveryPreference = "leave_at_door"
                deliveryInstructions = ""
                personalLabel = "none"
                customLabel = ""
                isGifting = false
                showAddressTypeSheet = true
            }
        )
    }

    // MARK: - Address Type Sheet
    if (showAddressTypeSheet) {
        AddressTypeSheet(
            selectedType = selectedAddressType,
            onSelect = { selectedAddressType = it },
            address = pendingAddressData?.street ?: "",
            onNext = {
                showAddressTypeSheet = false
                showAddressDetailsSheet = true
            },
            onCancel = {
                showAddressTypeSheet = false
                pendingAddressData = null
            }
        )
    }

    // MARK: - Address Details Sheet
    if (showAddressDetailsSheet) {
        AddressDetailsSheet(
            address = pendingAddressData?.street ?: "",
            deliveryPreference = deliveryPreference,
            onPreferenceChange = { deliveryPreference = it },
            deliveryInstructions = deliveryInstructions,
            onInstructionsChange = { deliveryInstructions = it },
            personalLabel = personalLabel,
            onLabelChange = { personalLabel = it },
            customLabel = customLabel,
            onCustomLabelChange = { customLabel = it },
            isGifting = isGifting,
            onGiftingChange = { isGifting = it },
            onNext = {
                showAddressDetailsSheet = false
                showGateCodeSheet = true
            },
            onBack = {
                showAddressDetailsSheet = false
                showAddressTypeSheet = true
            },
            onCancel = {
                showAddressDetailsSheet = false
                pendingAddressData = null
            }
        )
    }
}

// MARK: - Address Type Sheet (matches iOS AddressTypeSheet)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressTypeSheet(
    selectedType: String,
    onSelect: (String) -> Unit,
    address: String,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    val types = listOf(
        Triple("house", "House", Icons.Default.Home),
        Triple("apartment", "Apartment", Icons.Default.Apartment),
        Triple("hotel", "Hotel", Icons.Default.Hotel),
        Triple("office", "Office", Icons.Default.Business),
        Triple("other", "Other", Icons.Default.LocationOn)
    )

    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(5.dp)
                    .background(Color(0xFFD1D1D1), RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = "Address type",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            if (address.isNotEmpty()) {
                Text(
                    text = address,
                    fontSize = 13.sp,
                    color = Color.Black,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Type grid: 2 columns (last item spans full width)
            types.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (id, label, icon) ->
                        AddressTypeOption(
                            id = id,
                            label = label,
                            icon = icon,
                            selected = selectedType == id,
                            onClick = { onSelect(id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color(0xFFCC5500), RoundedCornerShape(12.dp))
                    .clickable { onNext() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Next",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cancel
            TextButton(onClick = onCancel) {
                Text(
                    text = "Cancel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AddressTypeOption(
    id: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(84.dp)
            .background(
                if (selected) Color(0xFFCC5500).copy(alpha = 0.1f) else Color(0xFFF3F3F3),
                RoundedCornerShape(14.dp)
            )
            .border(
                width = 2.dp,
                color = if (selected) Color(0xFFCC5500) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFFCC5500) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}

// MARK: - Address Details Sheet (matches iOS AddressDetailsSheet)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressDetailsSheet(
    address: String,
    deliveryPreference: String,
    onPreferenceChange: (String) -> Unit,
    deliveryInstructions: String,
    onInstructionsChange: (String) -> Unit,
    personalLabel: String,
    onLabelChange: (String) -> Unit,
    customLabel: String,
    onCustomLabelChange: (String) -> Unit,
    isGifting: Boolean,
    onGiftingChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val labels = listOf("none", "home", "work", "custom")

    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(5.dp)
                    .background(Color(0xFFD1D1D1), RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = "Address details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            if (address.isNotEmpty()) {
                Text(
                    text = address,
                    fontSize = 13.sp,
                    color = Color.Black,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Delivery preference
            Text(
                text = "Delivery preference",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            PreferenceRow(
                title = "Leave at door",
                icon = Icons.Default.Home,
                selected = deliveryPreference == "leave_at_door",
                onClick = { onPreferenceChange("leave_at_door") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PreferenceRow(
                title = "Meet at location",
                icon = Icons.Default.LocationOn,
                selected = deliveryPreference == "meet_at_location",
                onClick = { onPreferenceChange("meet_at_location") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Delivery instructions
            Text(
                text = "Delivery instructions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = deliveryInstructions,
                onValueChange = onInstructionsChange,
                placeholder = {
                    Text(
                        text = "e.g. ring the bell after dropoff, leave next to the porch, call upon arrival",
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFCC5500),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    cursorColor = Color(0xFFCC5500)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Text(
                text = "Do not add order changes or requests here.",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Personal label
            Text(
                text = "Personal label",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                labels.forEach { label ->
                    val selected = personalLabel == label
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) Color(0xFF555555) else Color(0xFFF3F3F3),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onLabelChange(label) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.replaceFirstChar { it.uppercase() },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else Color.Black
                        )
                    }
                }
            }
            if (personalLabel == "custom") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = onCustomLabelChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. hotel, office, gym", fontSize = 14.sp) },
                    singleLine = true
                )
            }
            Text(
                text = "Only you can see this.",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Gifting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F3F3), RoundedCornerShape(12.dp))
                    .clickable { onGiftingChange(!isGifting) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isGifting,
                    onCheckedChange = { onGiftingChange(it) },
                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                        checkedColor = Color(0xFFCC5500)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "I'm sending a gift",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Add a card and note at checkout",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color(0xFFCC5500), RoundedCornerShape(12.dp))
                    .clickable { onNext() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Next",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Back
            TextButton(onClick = onBack) {
                Text(
                    text = "Back",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) Color(0xFFCC5500).copy(alpha = 0.1f) else Color(0xFFF3F3F3),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = if (selected) Color(0xFFCC5500) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFFCC5500) else Color.Gray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Color(0xFFCC5500) else Color(0xFFD1D1D1),
            modifier = Modifier.size(18.dp)
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
                color = Color.Black,
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
                        color = Color.Black,
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
                        tint = Color(0xFFD95F02),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Your Privacy",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Text(
                    text = "Your gate code is never shared publicly or stored in plain text. It is only shared with your U-DO driver at the time of arrival at your address.",
                    fontSize = 11.sp,
                    color = Color.Black,
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
                    color = Color.Black
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
    label: String? = null,
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (!label.isNullOrEmpty() && label != "none") {
                    Text(
                        text = label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF6B00),
                        modifier = Modifier
                            .background(Color(0xFFFFE9D6), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Black,
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
                    tint = Color(0xFFD95F02), // Burnt orange
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