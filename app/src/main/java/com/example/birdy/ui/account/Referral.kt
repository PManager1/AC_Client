package com.example.birdy.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val OrangeSecNavyBlue = Color(0xFF1B2A4A)
private val BurntOrange = Color(0xFFCC5500)
private val OrangeSec2 = Color(0xFF8E8E93)
private val OrangeSec3 = Color(0xFFFF9500)
private val OrangeSec5 = Color(0xFFF5F0EB)
private val OrangeSec6 = Color(0xFFE5E5EA)
private val OrangeSec7 = Color(0xFF1C1C1E)

// MARK: - Invite data model
data class ReferralInvite(
    val name: String,
    val date: String,
    val status: String,
    val earned: Double
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun ReferralScreen(
    onBack: () -> Unit = {},
    onNavigateToReferralCode: () -> Unit = {}
) {
    var invitees by remember { mutableIntStateOf(0) }
    var totalEarned by remember { mutableDoubleStateOf(0.0) }
    var showingInviteOptions by remember { mutableStateOf(false) }
    var showingInvites by remember { mutableStateOf(false) }
    var showSMSError by remember { mutableStateOf(false) }
    // Referral code state
    var referralCode by remember { mutableStateOf("") }
    var isLoadingCode by remember { mutableStateOf(false) }
    var isEditingCode by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var showCodeError by remember { mutableStateOf(false) }
    var codeErrorMessage by remember { mutableStateOf("") }
    var showCopiedToast by remember { mutableStateOf(false) }

    // Invites data
    var invites by remember { mutableStateOf(listOf<ReferralInvite>()) }
    var isLoadingInvites by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("active") }

    val rewardDetail = "You also earn money as your referrals make money."
    val inviteMessage = "Hey! Try U-DO for food. No inflated menu prices or shady fees like Uber/DoorDash. It's good for us and pays drivers more. Get it on https://udonow.com "

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Fetch referral stats on load
    LaunchedEffect(Unit) {
        scope.launch {
            fetchReferralStats(context) { code, inv, earned ->
                referralCode = code
                invitees = inv
                totalEarned = earned
            }
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let {
            val phoneNumbers = mutableListOf<String>()
            context.contentResolver.query(
                it, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val number = cursor.getString(0)
                    val cleaned = number.replace(Regex("[^+0-9]"), "")
                    phoneNumbers.add(cleaned)
                }
            }
            if (phoneNumbers.isNotEmpty()) {
                val smsIntent = Intent(Intent.ACTION_SENDTO, "sms:${phoneNumbers.joinToString(",")}".toUri()).apply {
                    putExtra("sms_body", inviteMessage)
                }
                try {
                    context.startActivity(smsIntent)
                } catch (_: Exception) {
                    showSMSError = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OrangeSec5)
                .verticalScroll(rememberScrollState())
        ) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = OrangeSecNavyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // Title + icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Refer friends",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSecNavyBlue,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Refer",
                    tint = OrangeSec3,
                    modifier = Modifier.size(50.dp)
                )
            }

            HorizontalDivider(
                color = OrangeSec6,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rules section
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Invite friends who are new to U-DO",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = OrangeSec7
                )

                Text(
                    text = "Refer Friends, Earn Big\u2014up to $500! per referral",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSec7
                )

                // Reward detail box
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = "Dollar",
                            tint = BurntOrange,
                            modifier = Modifier.size(30.dp, 20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Get up to $400 for referrals",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeSec7
                            )
                            Text(
                                text = rewardDetail,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = OrangeSec2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MARK: - Referral Code Card
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = BurntOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Your Referral Code",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeSecNavyBlue
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Dashed border card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = OrangeSec5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, OrangeSec3.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp)
                    ) {
                        if (isLoadingCode) {
                            CircularProgressIndicator(
                                color = BurntOrange,
                                modifier = Modifier.size(32.dp)
                            )
                        } else if (isEditingCode) {
                            // Edit mode
                            Text(
                                text = "Enter your custom code",
                                fontSize = 13.sp,
                                color = OrangeSec2
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editText,
                                onValueChange = {
                                    if (it.length <= 20) editText = it.uppercase()
                                },
                                placeholder = { Text("e.g. JAY2026") },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    autoCorrectEnabled = false
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = OrangeSec5,
                                    unfocusedContainerColor = OrangeSec5,
                                    cursorColor = BurntOrange,
                                    focusedIndicatorColor = OrangeSec3.copy(alpha = 0.4f),
                                    unfocusedIndicatorColor = OrangeSec3.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.width(220.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeSec2,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(OrangeSec5)
                                        .clickable {
                                            isEditingCode = false
                                            editText = ""
                                        }
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                )

                                Text(
                                    text = "✓ Save",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (editText.length >= 4) BurntOrange else Color.Gray)
                                        .clickable {
                                            if (editText.length >= 4) {
                                                scope.launch {
                                                    isLoadingCode = true
                                                    isEditingCode = false
                                                    saveReferralCode(context, editText.trim().uppercase()) { savedCode, error ->
                                                        isLoadingCode = false
                                                        if (error != null) {
                                                            codeErrorMessage = error
                                                            showCodeError = true
                                                        } else if (savedCode != null) {
                                                            referralCode = savedCode
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            // Display mode
                            Text(
                                text = referralCode.ifEmpty { "------" },
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = if (referralCode.isEmpty()) OrangeSec2.copy(alpha = 0.4f) else OrangeSecNavyBlue,
                                letterSpacing = 3.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                // Copy button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (referralCode.isNotEmpty()) {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Referral Code", referralCode))
                                                showCopiedToast = true
                                                scope.launch {
                                                    kotlinx.coroutines.delay(2000)
                                                    showCopiedToast = false
                                                }
                                            }
                                        },
                                        enabled = referralCode.isNotEmpty()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = BurntOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text("Copy", fontSize = 11.sp, color = OrangeSec2)
                                }

                                // Edit button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(onClick = {
                                        editText = referralCode
                                        isEditingCode = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Create,
                                            contentDescription = "Edit",
                                            tint = BurntOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text("Edit", fontSize = 11.sp, color = OrangeSec2)
                                }

                                // Share button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = {
                                            if (referralCode.isNotEmpty()) {
                                                val shareText = "Hey! Use my referral code $referralCode on U-DO for food. No inflated menu prices or shady fees like Uber/DoorDash. Get it on https://udonow.com"
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                                            }
                                        },
                                        enabled = referralCode.isNotEmpty()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = BurntOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text("Share", fontSize = 11.sp, color = OrangeSec2)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = OrangeSec6,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
            )

            // MARK: - Status Section
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Status",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSec7
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    Column {
                        Text(
                            text = "Invitees",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = OrangeSec2
                        )
                        Text(
                            text = "$invitees",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeSecNavyBlue
                        )
                    }

                    Column {
                        Text(
                            text = "You earned",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = OrangeSec2
                        )
                        val earnedText = "$" + "%.0f".format(totalEarned)
                        Text(
                            text = earnedText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeSecNavyBlue
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White
                ) {
                    Text(
                        text = "Show invites",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeSecNavyBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showingInvites = true
                                scope.launch {
                                    isLoadingInvites = true
                                    fetchReferralInvites(context) { loadedInvites ->
                                        invites = loadedInvites
                                        isLoadingInvites = false
                                    }
                                }
                            }
                            .padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Invite button
            Button(
                onClick = { showingInviteOptions = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeSecNavyBlue
                )
            ) {
                Text(
                    text = "Invite",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // MARK: - Copied Toast
        AnimatedVisibility(
            visible = showCopiedToast,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✅", fontSize = 16.sp)
                    Text(
                        "Code copied!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeSecNavyBlue
                    )
                }
            }
        }

        // MARK: - Invite Options Bottom Sheet
        AnimatedVisibility(
            visible = showingInviteOptions,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = "Send invite",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSec7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 15.dp, horizontal = 16.dp)
                )

                HorizontalDivider()

                Text(
                    text = "Invite from contacts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showingInviteOptions = false
                            try {
                                contactPickerLauncher.launch(null)
                            } catch (_: Exception) {
                                showSMSError = true
                            }
                        }
                        .padding(vertical = 20.dp, horizontal = 16.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Text(
                    text = "Share your link",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showingInviteOptions = false
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                        }
                        .padding(vertical = 20.dp, horizontal = 16.dp)
                )

                HorizontalDivider()

                Text(
                    text = "Cancel",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showingInviteOptions = false }
                        .padding(vertical = 20.dp, horizontal = 16.dp)
                )
            }
        }

        // MARK: - Invites Overlay (full screen)
        AnimatedVisibility(
            visible = showingInvites,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Toolbar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    IconButton(onClick = { showingInvites = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Back",
                            tint = OrangeSecNavyBlue
                        )
                    }
                    Text(
                        text = "Status",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeSec7
                    )
                    IconButton(onClick = { /* Help */ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Help",
                            tint = OrangeSec7
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Status",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeSec7
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Stats row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = OrangeSec2
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invitees", fontSize = 15.sp, color = OrangeSec2)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$invitees", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OrangeSec7)
                        }
                        Spacer(modifier = Modifier.width(20.dp))

                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .width(1.dp)
                                .background(OrangeSec6)
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = OrangeSec2
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("You earned", fontSize = 15.sp, color = OrangeSec2)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val earnedText = "$" + "%.0f".format(totalEarned)
                            Text(earnedText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OrangeSec7)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tabs
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = "active" },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Active Invites",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == "active") OrangeSec7 else OrangeSec2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (selectedTab == "active") OrangeSec7 else Color.Transparent)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = "past" },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Past Invites",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == "past") OrangeSec7 else OrangeSec2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (selectedTab == "past") OrangeSec7 else Color.Transparent)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab content
                    if (isLoadingInvites) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BurntOrange)
                        }
                    } else {
                        val filtered = invites.filter {
                            if (selectedTab == "active") it.status == "active" else it.status == "completed"
                        }

                        if (filtered.isEmpty()) {
                            // Empty state
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF007AFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "No invites",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = if (selectedTab == "active") "No active invitations" else "No past invites yet.",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeSec7
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Invite friends to U-DO and earn rewards. When they join, you'll see them listed here.",
                                    fontSize = 15.sp,
                                    color = OrangeSec2,
                                    modifier = Modifier.padding(horizontal = 40.dp)
                                )
                            }
                        } else {
                            // Invite rows
                            filtered.forEach { invite ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        // Avatar circle
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(OrangeSec3.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = invite.name.take(1).uppercase(),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BurntOrange
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Name & date
                                        Column {
                                            Text(
                                                invite.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OrangeSec7
                                            )
                                            Text(
                                                invite.date,
                                                fontSize = 13.sp,
                                                color = OrangeSec2
                                            )
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        // Status badge
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (invite.status == "active") Color.Green.copy(alpha = 0.12f) else OrangeSec2.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = if (invite.status == "active") "Active" else "Completed",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (invite.status == "active") Color.Green else OrangeSec2,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
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

    // Error dialogs
    if (showSMSError) {
        AlertDialog(
            onDismissRequest = { showSMSError = false },
            title = { Text("Cannot Send Messages") },
            text = { Text("This device cannot send text messages. Please use 'Share your link' instead.") },
            confirmButton = { TextButton(onClick = { showSMSError = false }) { Text("OK") } }
        )
    }

    if (showCodeError) {
        AlertDialog(
            onDismissRequest = { showCodeError = false },
            title = { Text("Referral Code") },
            text = { Text(codeErrorMessage) },
            confirmButton = { TextButton(onClick = { showCodeError = false }) { Text("OK") } }
        )
    }
}

// MARK: - API Functions

private suspend fun fetchReferralStats(
    context: Context,
    onResult: (String, Int, Double) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val token = AuthManager.getToken(context)
            if (token.isNullOrEmpty()) {
                println("❌ No auth token for referral stats")
                withContext(Dispatchers.Main) { onResult("", 0, 0.0) }
                return@withContext
            }

            val url = URL("${Config.API_BASE_URL}/referral/stats")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
            }

            val code = conn.responseCode
            if (code != 200) {
                println("❌ Referral stats returned $code")
                withContext(Dispatchers.Main) { onResult("", 0, 0.0) }
                return@withContext
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)
            if (!success) {
                println("❌ Referral stats success=false")
                withContext(Dispatchers.Main) { onResult("", 0, 0.0) }
                return@withContext
            }

            val referralCode = json.optString("referralCode", "")
            val invitees = json.optInt("invitees", 0)
            val totalEarned = json.optDouble("totalEarned", 0.0)

            println("✅ Referral stats: code=$referralCode invitees=$invitees earned=$totalEarned")
            withContext(Dispatchers.Main) { onResult(referralCode, invitees, totalEarned) }
        } catch (e: Exception) {
            println("❌ Fetch referral stats error: ${e.message}")
            withContext(Dispatchers.Main) { onResult("", 0, 0.0) }
        }
    }
}

private suspend fun saveReferralCode(
    context: Context,
    code: String,
    onResult: (String?, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val token = AuthManager.getToken(context)
            if (token.isNullOrEmpty()) {
                withContext(Dispatchers.Main) { onResult(null, "No auth token") }
                return@withContext
            }

            val url = URL("${Config.API_BASE_URL}/referral/code")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                doOutput = true
            }

            val body = JSONObject().put("referralCode", code)
            conn.outputStream.use { os ->
                val input = body.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = conn.responseCode

            if (responseCode == 409) {
                withContext(Dispatchers.Main) { onResult(null, "That referral code is already taken. Try another one.") }
                return@withContext
            }
            if (responseCode == 429) {
                withContext(Dispatchers.Main) { onResult(null, "You've changed your code too many times. Try again later.") }
                return@withContext
            }
            if (responseCode == 400) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                val msg = try {
                    JSONObject(errorBody).optString("error", "Invalid referral code.")
                } catch (_: Exception) { "Invalid referral code." }
                withContext(Dispatchers.Main) { onResult(null, msg) }
                return@withContext
            }

            if (responseCode != 200) {
                withContext(Dispatchers.Main) { onResult(null, "Server error ($responseCode)") }
                return@withContext
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)
            if (!success) {
                withContext(Dispatchers.Main) { onResult(null, "Failed to save code") }
                return@withContext
            }

            val savedCode = json.optString("referralCode", code)
            println("✅ Saved referral code: $savedCode")
            withContext(Dispatchers.Main) { onResult(savedCode, null) }
        } catch (e: Exception) {
            println("❌ Save referral code error: ${e.message}")
            withContext(Dispatchers.Main) { onResult(null, e.message) }
        }
    }
}

private suspend fun fetchReferralInvites(
    context: Context,
    onResult: (List<ReferralInvite>) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val token = AuthManager.getToken(context)
            if (token.isNullOrEmpty()) {
                println("❌ No auth token for referral invites")
                withContext(Dispatchers.Main) { onResult(emptyList()) }
                return@withContext
            }

            val url = URL("${Config.API_BASE_URL}/referral/invites")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
            }

            val code = conn.responseCode
            if (code != 200) {
                println("❌ Referral invites returned $code")
                withContext(Dispatchers.Main) { onResult(emptyList()) }
                return@withContext
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val success = json.optBoolean("success", false)
            if (!success) {
                println("❌ Referral invites success=false")
                withContext(Dispatchers.Main) { onResult(emptyList()) }
                return@withContext
            }

            val invitesArray = json.optJSONArray("invites") ?: run {
                withContext(Dispatchers.Main) { onResult(emptyList()) }
                return@withContext
            }

            val loaded = mutableListOf<ReferralInvite>()
            for (i in 0 until invitesArray.length()) {
                val item = invitesArray.getJSONObject(i)
                loaded.add(ReferralInvite(
                    name = item.optString("name", "Unknown"),
                    date = item.optString("date", ""),
                    status = item.optString("status", "active"),
                    earned = item.optDouble("earned", 0.0)
                ))
            }

            println("✅ Loaded ${loaded.size} invites")
            withContext(Dispatchers.Main) { onResult(loaded) }
        } catch (e: Exception) {
            println("❌ Fetch invites error: ${e.message}")
            withContext(Dispatchers.Main) { onResult(emptyList()) }
        }
    }
}