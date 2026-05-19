package com.example.birdy.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.CartItem
import kotlinx.coroutines.launch

// MARK: - Item Detail Sheet (matches iOS ItemDetailSheet)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    item: StoreMenuItem,
    restaurantName: String,
    onDismiss: () -> Unit,
    onAddToCart: (CartItem) -> Unit,
    editingCartItem: CartItem? = null,  // If set, we're editing an existing cart item (matches iOS)
    initialQuantity: Int = 1,
    initialSelectedOptionNames: List<String> = emptyList(),
    initialSpecialInstructions: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by remember { mutableStateOf(editingCartItem?.quantity ?: initialQuantity) }
    var specialInstructions by remember { mutableStateOf(editingCartItem?.specialInstructions ?: initialSpecialInstructions) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Validation state: groupId → error message
    val validationErrors = remember { mutableStateMapOf<String, String>() }
    var scrollToGroupId by remember { mutableStateOf<String?>(null) }

    // Track Y positions of each group for scrolling
    val groupPositions = remember { mutableStateMapOf<String, Float>() }

    // Track selected options per modifier group: groupId → list of selected option IDs
    val selectedOptions = remember {
        mutableStateListOf<Pair<String, String>>().also { list ->
            if (editingCartItem != null) {
                // When editing, pre-fill from the cart item's selected options by matching option names
                val editOptionNames = editingCartItem.selectedOptions.map { it.substringBefore(" +$") }
                item.modifier_groups.forEach { group ->
                    group.options.forEach { opt ->
                        if (editOptionNames.contains(opt.name)) {
                            list.add(group.id to opt.id)
                        }
                    }
                }
            } else if (initialSelectedOptionNames.isNotEmpty()) {
                // Pre-fill from initial option names
                item.modifier_groups.forEach { group ->
                    group.options.forEach { opt ->
                        if (initialSelectedOptionNames.contains(opt.name)) {
                            list.add(group.id to opt.id)
                        }
                    }
                }
            } else {
                // Pre-select default options
                item.modifier_groups.forEach { group ->
                    group.options.forEach { opt ->
                        if (opt.is_default) {
                            list.add(group.id to opt.id)
                        }
                    }
                }
            }
        }
    }

    // Calculate extra price from selected modifiers
    val modifierExtraPrice = remember(selectedOptions.size) {
        var extra = 0.0
        item.modifier_groups.forEach { group ->
            val selectedForGroup = selectedOptions.filter { it.first == group.id }
            selectedForGroup.forEach { (_, optId) ->
                group.options.find { it.id == optId }?.let { extra += it.extra_price }
            }
        }
        extra
    }
    val totalPrice = (item.price + modifierExtraPrice) * quantity

    // Validate required modifier groups
    fun validateRequiredGroups(): Boolean {
        val errors = mutableMapOf<String, String>()
        var firstErrorGroupId: String? = null

        item.modifier_groups.forEach { group ->
            val selectedCount = selectedOptions.count { it.first == group.id }
            if (group.is_required && selectedCount < group.min_selection) {
                val msg = if (group.min_selection == 1 && group.max_selection <= 1) {
                    "Please choose 1"
                } else {
                    "Please choose ${group.min_selection}"
                }
                errors[group.id] = msg
                if (firstErrorGroupId == null) {
                    firstErrorGroupId = group.id
                }
            }
        }

        validationErrors.clear()
        validationErrors.putAll(errors)
        scrollToGroupId = firstErrorGroupId

        return errors.isEmpty()
    }

    // Auto-scroll to first error group
    LaunchedEffect(scrollToGroupId) {
        scrollToGroupId?.let { targetId ->
            val position = groupPositions[targetId]
            if (position != null) {
                scrollState.animateScrollTo(position.toInt() - 100)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .verticalScroll(scrollState)
        ) {
            // Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                }
            }

            // Item image
            if (item.image_url.isNotEmpty()) {
                AsyncImage(
                    model = item.image_url,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Name & price
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = item.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${String.format("%.2f", item.price)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCC5500)
                )
            }

            // Description
            if (item.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Modifier groups
            item.modifier_groups.forEach { group ->
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .onGloballyPositioned { coordinates ->
                            groupPositions[group.id] = coordinates.positionInRoot().y
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (group.is_required) {
                            Text(
                                text = "Required",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = if (group.max_selection == 1) "Choose 1"
                        else "Choose up to ${group.max_selection}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    // Inline validation error
                    validationErrors[group.id]?.let { error ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 13.sp
                            )
                            Text(
                                text = error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Options
                    group.options.forEach { option ->
                        val isSelected = selectedOptions.any { it.first == group.id && it.second == option.id }
                        val isRadio = group.max_selection <= 1

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isRadio) {
                                        // Radio: deselect others in same group
                                        selectedOptions.removeAll { it.first == group.id }
                                        selectedOptions.add(group.id to option.id)
                                    } else {
                                        // Checkbox: toggle
                                        val existing = selectedOptions.indexOfFirst { it.first == group.id && it.second == option.id }
                                        if (existing >= 0) {
                                            selectedOptions.removeAt(existing)
                                        } else {
                                            val countForGroup = selectedOptions.count { it.first == group.id }
                                            if (countForGroup < group.max_selection) {
                                                selectedOptions.add(group.id to option.id)
                                            }
                                        }
                                    }
                                    // Clear error for this group when user selects an option
                                    validationErrors.remove(group.id)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRadio) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedOptions.removeAll { it.first == group.id }
                                        selectedOptions.add(group.id to option.id)
                                        validationErrors.remove(group.id)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFCC5500))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isSelected) Color(0xFFCC5500) else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 0.dp else 1.5.dp,
                                            color = if (isSelected) Color(0xFFCC5500) else Color.Gray,
                                            shape = RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = option.name,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )

                            if (option.extra_price > 0) {
                                Text(
                                    text = "+$${String.format("%.2f", option.extra_price)}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Special instructions
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Special Instructions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = specialInstructions,
                    onValueChange = { specialInstructions = it },
                    placeholder = {
                        Text("Add a note (e.g. no onions, extra sauce)", color = Color.Gray, fontSize = 14.sp)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.Black)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quantity stepper + Add to cart button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stepper
                Row(
                    modifier = Modifier
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(50))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                            .clickable { if (quantity > 1) quantity-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Text(
                        text = "$quantity",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                            .clickable { quantity++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                // Add to cart button
                Text(
                    text = "Add to Cart • $${String.format("%.2f", totalPrice)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            Brush.horizontalGradient(colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C))),
                            RoundedCornerShape(50.dp)
                        )
                        .clickable {
                            // Validate required modifier groups first
                            if (!validateRequiredGroups()) {
                                return@clickable
                            }

                            val selectedNames = mutableListOf<String>()
                            item.modifier_groups.forEach { group ->
                                selectedOptions.filter { it.first == group.id }.forEach { (_, optId) ->
                                    group.options.find { it.id == optId }?.let { opt ->
                                        val label = if (opt.extra_price > 0) "${opt.name} +$${String.format("%.2f", opt.extra_price)}"
                                         else opt.name
                                        selectedNames.add(label)
                                    }
                                }
                            }
                            onAddToCart(
                                CartItem(
                                    dishName = item.name,
                                    restaurantName = restaurantName,
                                    price = item.price + modifierExtraPrice,
                                    quantity = quantity,
                                    imageURL = item.image_url,
                                    specialInstructions = specialInstructions,
                                    selectedOptions = selectedNames,
                                    menuItem = item  // Store full menu item for cart re-editing (matches iOS)
                                )
                            )
                        }
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}