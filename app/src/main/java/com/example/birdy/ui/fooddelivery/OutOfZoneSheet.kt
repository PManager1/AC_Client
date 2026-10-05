package com.example.birdy.ui.fooddelivery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.AuthManager
import com.example.birdy.data.ServiceAreaService
import com.example.birdy.data.ZoneCheckResult
import com.example.birdy.data.isValidContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Orange = Color(0xFFCC5500)

private enum class ZoneStage { Ask, Joined, Already }

/**
 * "Not here yet" sheet, the Android version of the web address modal's panel
 * (iOS: OutOfZoneView). Shown right after an out-of-zone address is picked, or
 * when service is paused. Copy comes from the server so all apps match.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutOfZoneSheet(
    result: ZoneCheckResult,
    onTryAnother: () -> Unit,   // back to address search
    onDone: () -> Unit          // close
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var stage by remember(result) { mutableStateOf(if (result.alreadyOnList) ZoneStage.Already else ZoneStage.Ask) }
    var contact by remember(result) { mutableStateOf(result.contact.orEmpty()) }
    var editingContact by remember(result) { mutableStateOf(result.contact.isNullOrEmpty()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val token = AuthManager.getToken(context)
        if (token.isNullOrEmpty()) {
            errorText = "Please sign in to get notified."
            return
        }
        isSubmitting = true
        errorText = null
        scope.launch {
            try {
                val status = withContext(Dispatchers.IO) { ServiceAreaService.join(contact, result, token) }
                stage = if (status == "already") ZoneStage.Already else ZoneStage.Joined
            } catch (e: Exception) {
                editingContact = true
                errorText = e.message
            } finally {
                isSubmitting = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 8 }) togetherWith fadeOut()
            },
            label = "zoneStage"
        ) { current ->
            ZoneStageContent(
                stage = current,
                result = result,
                contact = contact,
                onContactChange = { contact = it; errorText = null },
                editingContact = editingContact,
                onChangeContact = { editingContact = true },
                isSubmitting = isSubmitting,
                errorText = errorText,
                onNotify = { submit() },
                onTryAnother = onTryAnother,
                onDone = onDone
            )
        }
    }
}

@Composable
private fun ZoneStageContent(
    stage: ZoneStage,
    result: ZoneCheckResult,
    contact: String,
    onContactChange: (String) -> Unit,
    editingContact: Boolean,
    onChangeContact: () -> Unit,
    isSubmitting: Boolean,
    errorText: String?,
    onNotify: () -> Unit,
    onTryAnother: () -> Unit,
    onDone: () -> Unit
) {
    val city = result.city?.takeIf { it.isNotBlank() }
    val heading = when (stage) {
        ZoneStage.Ask -> result.heading ?: "We're not in your area yet"
        ZoneStage.Joined -> "You're on the list"
        ZoneStage.Already -> if (city != null) "You're already on the list for $city" else "You're already on the list for this area"
    }
    val body = when (stage) {
        ZoneStage.Ask -> result.detail ?: "We're starting in Washington, DC, and expanding soon. Want us to let you know when we arrive?"
        ZoneStage.Joined -> "We'll reach out when we launch near you."
        ZoneStage.Already -> "We'll let you know as soon as we launch near you."
    }

    // Move TalkBack/keyboard focus to the heading whenever the stage changes
    val headingFocus = remember { FocusRequester() }
    LaunchedEffect(stage) {
        delay(300)
        runCatching { headingFocus.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Soft orange pin (green check once joined): friendly, not an error
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (stage == ZoneStage.Joined) Color(0xFF2E7D32).copy(alpha = 0.12f) else Orange.copy(alpha = 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (stage == ZoneStage.Joined) Icons.Default.Check else Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (stage == ZoneStage.Joined) Color(0xFF2E7D32) else Orange,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = heading,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .semantics { heading() }
                .focusRequester(headingFocus)
                .focusable()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = body,
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        result.address?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = it, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (stage == ZoneStage.Ask) {
            if (editingContact) {
                OutlinedTextField(
                    value = contact,
                    onValueChange = onContactChange,
                    placeholder = { Text("Email or phone number", fontSize = 15.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        cursorColor = Orange
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(text = "We'll notify you at ", fontSize = 15.sp, color = Color.Gray)
                    Text(
                        text = contact,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "  Change",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Orange,
                        modifier = Modifier.clickable { onChangeContact() }
                    )
                }
            }

            errorText?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, fontSize = 13.sp, color = Color.Red, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = if (isSubmitting) "Saving..." else "Notify me",
                enabled = isValidContact(contact) && !isSubmitting,
                onClick = onNotify
            )
        } else {
            PrimaryButton(text = "Done", enabled = true, onClick = onDone)
        }

        if (stage != ZoneStage.Joined) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onTryAnother) {
                Text(text = "Try a different address", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(if (enabled) Orange else Orange.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
