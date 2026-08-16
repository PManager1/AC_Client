package com.example.birdy.ui.account

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

// Color constants — mirrors the other test pages
private val OrangeSecNavyBlue = Color(0xFF1B2A4A)
private val OrangeTitle = Color(0xFFF27836)
private val OrangeSec5 = Color(0xFFF5F0EB)
private val OrangeSec2 = Color(0xFF8E8E93)
private val OrangeSec3 = Color(0xFFFF9500)

/**
 * Takepic — 3-step manual photo verification screen (1099 onboarding).
 * Guides the user through FRONT, LEFT PROFILE, RIGHT PROFILE captures using a live
 * CameraX preview. No AI/ML. On Submit runs a 1-second mock upload then shows
 * "Verification Under Review".
 */
@Composable
fun TakepicScreen(
    viewModel: VerificationViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // Request permission on first appearance if not already granted.
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrangeSec5)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 16.dp, end = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OrangeSecNavyBlue
                )
            }
        }

        if (uiState.submitted) {
            UnderReviewScreen(onDone = onBack)
        } else {
            VerificationContent(
                uiState = uiState,
                hasCameraPermission = hasCameraPermission,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onPhotoCaptured = viewModel::onPhotoCaptured,
                captureTrigger = viewModel.captureTrigger,
                onPrimary = viewModel::handlePrimaryAction,
                onPrevious = viewModel::moveToPreviousStep,
                onNextOrSubmit = viewModel::handleNextOrSubmit
            )
        }
    }
}

@Composable
private fun VerificationContent(
    uiState: VerificationUiState,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit,
    captureTrigger: kotlinx.coroutines.flow.Flow<Unit>,
    onPrimary: () -> Unit,
    onPrevious: () -> Unit,
    onNextOrSubmit: () -> Unit
) {
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraRetryKey by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Warning Banner
        WarningBanner()

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Stepper (FRONT | LEFT | RIGHT)
        StepperHeader(currentStep = uiState.currentStep)

        Spacer(modifier = Modifier.height(12.dp))

        // Camera / Photo Preview Box (strictly constrained height, clipped)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            val currentPhoto = uiState.photos[uiState.currentStep]

            when {
                currentPhoto != null -> {
                    Image(
                        bitmap = currentPhoto.asImageBitmap(),
                        contentDescription = "Captured Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                !hasCameraPermission -> {
                    PermissionGate(onRequestPermission = onRequestPermission)
                }
                cameraError != null -> {
                    CameraErrorFallback(
                        message = cameraError ?: "Camera unavailable",
                        onRetry = { cameraError = null; cameraRetryKey++ }
                    )
                }
                else -> {
                    CameraXPreview(
                        trigger = captureTrigger,
                        onImageCaptured = onPhotoCaptured,
                        onError = { cameraError = it },
                        retryKey = cameraRetryKey,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Small helper text when no photo captured yet.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Align your face within the guide",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Silhouette overlay on the live preview
            SilhouetteOverlay(step = uiState.currentStep)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Thumbnail Strip
        ThumbnailBar(photos = uiState.photos)

        Spacer(modifier = Modifier.weight(1f)) // Push buttons to bottom

        // Action Buttons
        ControlButtons(
            currentStep = uiState.currentStep,
            hasPhoto = uiState.photos.containsKey(uiState.currentStep),
            isSubmitting = uiState.isSubmitting,
            onTakeOrRetake = onPrimary,
            onPrevious = onPrevious,
            onNextOrSubmit = onNextOrSubmit
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ============================================================================
// MARK: - Warning Banner
// ============================================================================

@Composable
private fun WarningBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrangeSec3.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "IMPORTANT: Take clear, well-lit pictures. If images are blurry or unclear, " +
                "we will request you retake them. (Manual Review)",
            fontSize = 12.sp,
            color = OrangeSecNavyBlue,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================================
// MARK: - Stepper Header
// ============================================================================

@Composable
private fun StepperHeader(currentStep: VerificationStep) {
    val steps = VerificationStep.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = step == currentStep
            val isCompleted = step.number < currentStep.number
            StepDot(
                number = step.number,
                label = step.label,
                isActive = isActive,
                isCompleted = isCompleted,
                modifier = Modifier.weight(1f)
            )
            if (index < steps.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .background(
                            if (currentStep.number > step.number) OrangeTitle else OrangeSec2.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
private fun StepDot(
    number: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Color(0xFF4CAF50)
                        isActive -> OrangeTitle
                        else -> OrangeSec2.copy(alpha = 0.3f)
                    }
                )
        ) {
            Text(
                text = if (isCompleted) "✓" else "$number",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) OrangeTitle else OrangeSec2,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// MARK: - Camera Permission Gate
// ============================================================================

@Composable
private fun PermissionGate(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera access is needed to take your photos.",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Camera Access")
        }
    }
}

// ============================================================================
// MARK: - Camera Error Fallback
// ============================================================================

@Composable
private fun CameraErrorFallback(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("Retry Camera")
        }
    }
}

// ============================================================================
// MARK: - Silhouette Overlay (visual guide only, no AI)
// ============================================================================

@Composable
private fun SilhouetteOverlay(step: VerificationStep) {
    val guideColor = Color.White.copy(alpha = 0.55f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = when (step) {
            VerificationStep.FRONT -> w / 2f
            VerificationStep.LEFT -> w * 0.32f
            VerificationStep.RIGHT -> w * 0.68f
        }

        // Head (oval)
        val headRadiusX = w * 0.22f
        val headRadiusY = h * 0.20f
        val headCenterY = h * 0.40f

        // Shoulders (arc)
        val shoulderPath = Path().apply {
            moveTo(cx - headRadiusX * 1.4f, h * 0.78f)
            cubicTo(cx - headRadiusX * 1.8f, h * 0.62f, cx + headRadiusX * 1.8f, h * 0.62f, cx + headRadiusX * 1.4f, h * 0.78f)
        }

        drawOval(
            color = guideColor,
            topLeft = Offset(cx - headRadiusX, headCenterY - headRadiusY),
            size = Size(headRadiusX * 2, headRadiusY * 2),
            style = Stroke(width = 3.dp.toPx())
        )
        drawPath(
            path = shoulderPath,
            color = guideColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

// ============================================================================
// MARK: - Thumbnail Bar
// ============================================================================

@Composable
private fun ThumbnailBar(photos: Map<VerificationStep, Bitmap>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerificationStep.entries.forEach { step ->
            val photo = photos[step]
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            ) {
                if (photo != null) {
                    Image(
                        bitmap = photo.asImageBitmap(),
                        contentDescription = "${step.label} photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step.number.toString(),
                            fontSize = 16.sp,
                            color = OrangeSec2
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// MARK: - Control Buttons
// ============================================================================

@Composable
private fun ControlButtons(
    currentStep: VerificationStep,
    hasPhoto: Boolean,
    isSubmitting: Boolean,
    onTakeOrRetake: () -> Unit,
    onPrevious: () -> Unit,
    onNextOrSubmit: () -> Unit
) {
    val isLastStep = currentStep == VerificationStep.RIGHT
    val primaryLabel = when {
        isSubmitting -> "Uploading…"
        hasPhoto -> "RETAKE PHOTO"
        else -> "TAKE PHOTO"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onTakeOrRetake,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(primaryLabel, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = !isSubmitting,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("< PREVIOUS")
            }

            Button(
                onClick = onNextOrSubmit,
                enabled = (hasPhoto || isLastStep) && !isSubmitting,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isLastStep) "SUBMIT APPLICATION" else "NEXT STEP >")
            }
        }
    }
}

// ============================================================================
// MARK: - Under Review Success Screen
// ============================================================================

@Composable
private fun UnderReviewScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", fontSize = 44.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Verification Under Review",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeSecNavyBlue,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your photos have been submitted for manual review. " +
                "We'll notify you once your verification is complete.",
            fontSize = 15.sp,
            color = OrangeSec2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}
