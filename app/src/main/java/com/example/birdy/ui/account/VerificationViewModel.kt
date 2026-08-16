package com.example.birdy.ui.account

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The three sequential photo steps for manual 1099 verification.
 */
enum class VerificationStep(val label: String, val number: Int) {
    FRONT("FRONT", 1),
    LEFT("LEFT PROFILE", 2),
    RIGHT("RIGHT PROFILE", 3)
}

/**
 * UI state for the 3-step photo verification flow.
 *
 * @param currentStep the active step.
 * @param photos captured bitmap per step (empty unless captured).
 * @param isSubmitting true during the mock upload delay.
 * @param submitted true once the mock upload finishes (shows Under Review).
 */
data class VerificationUiState(
    val currentStep: VerificationStep = VerificationStep.FRONT,
    val photos: Map<VerificationStep, Bitmap> = emptyMap(),
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false
)

/**
 * ViewModel for the 3-step manual photo verification flow (1099 onboarding).
 * No AI/ML — purely manual capture. Retake removes the step's photo so the live
 * camera re-activates. Submit runs a 1-second mock upload, then marks submitted.
 */
class VerificationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Signals the CameraX preview that a capture should be taken for the current step.
     * The preview composable collects this and calls [onPhotoCaptured] with the result.
     */
    val captureTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Whether the current step already has a captured photo. */
    fun hasCurrentPhoto(): Boolean =
        _uiState.value.photos.containsKey(_uiState.value.currentStep)

    /** Store the captured bitmap for the current step. */
    fun onPhotoCaptured(bitmap: Bitmap) {
        _uiState.update { state ->
            state.copy(photos = state.photos + (state.currentStep to bitmap))
        }
    }

    /**
     * Primary action:
     * - If the current step already has a photo → RETAKE: remove it so the live camera re-appears.
     * - Otherwise → TAKE PHOTO: emit a capture trigger event.
     */
    fun handlePrimaryAction() {
        val state = _uiState.value
        if (state.photos.containsKey(state.currentStep)) {
            val updatedPhotos = state.photos.toMutableMap().apply { remove(state.currentStep) }
            _uiState.update { it.copy(photos = updatedPhotos) }
        } else {
            captureTrigger.tryEmit(Unit)
        }
    }

    /** Go back to the previous step; nothing to do on FRONT (UI handles go-back-out). */
    fun moveToPreviousStep() {
        _uiState.update { state ->
            when (state.currentStep) {
                VerificationStep.RIGHT -> state.copy(currentStep = VerificationStep.LEFT)
                VerificationStep.LEFT -> state.copy(currentStep = VerificationStep.FRONT)
                VerificationStep.FRONT -> state
            }
        }
    }

    /**
     * Advance to the next step, or on the last step trigger the mock upload + Under Review.
     */
    fun handleNextOrSubmit() {
        val state = _uiState.value
        if (state.currentStep == VerificationStep.RIGHT) {
            if (state.photos.containsKey(VerificationStep.RIGHT) && !state.isSubmitting) {
                submitApplication()
            }
        } else {
            val next = when (state.currentStep) {
                VerificationStep.FRONT -> VerificationStep.LEFT
                VerificationStep.LEFT -> VerificationStep.RIGHT
                else -> state.currentStep
            }
            _uiState.update { it.copy(currentStep = next) }
        }
    }

    private fun submitApplication() {
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            delay(1000)
            _uiState.update { it.copy(isSubmitting = false, submitted = true) }
        }
    }
}
