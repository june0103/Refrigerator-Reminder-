package com.mist.refrigeratorreminder.core.privacy

import android.app.Activity
import android.content.Context
import android.util.Log
import com.mist.refrigeratorreminder.R
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PrivacyConsentManager(
    context: Context,
    private val initializeAds: () -> Unit,
) {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val _uiState = MutableStateFlow(PrivacyConsentUiState())

    val uiState: StateFlow<PrivacyConsentUiState> = _uiState.asStateFlow()

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    fun requestConsent(activity: Activity, onComplete: (() -> Unit)? = null) {
        syncUiState(isConsentFlowComplete = false)

        val paramsBuilder = ConsentRequestParameters.Builder()
        buildDebugSettingsIfNeeded(activity)?.let { debugSettings ->
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        val params = paramsBuilder.build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                syncUiState(isConsentFlowComplete = false)
                logConsentState("consentInfoUpdated")
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form dismissed with error: ${formError.message}")
                    } else {
                        Log.d(TAG, "Consent form flow completed.")
                    }
                    syncUiState(isConsentFlowComplete = true)
                    logConsentState("consentFlowCompleted")
                    initializeAdsIfAllowed()
                    onComplete?.invoke()
                }
            },
            { requestConsentError ->
                Log.w(
                    TAG,
                    "Consent info update failed. code=${requestConsentError.errorCode}, message=${requestConsentError.message}",
                )
                syncUiState(isConsentFlowComplete = true)
                logConsentState("consentInfoUpdateFailed")
                initializeAdsIfAllowed()
                onComplete?.invoke()
            },
        )
    }

    fun showPrivacyOptionsForm(activity: Activity, onComplete: ((FormError?) -> Unit)? = null) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form failed: ${formError.message}")
            }
            syncUiState(isConsentFlowComplete = true)
            logConsentState("privacyOptionsClosed")
            initializeAdsIfAllowed()
            onComplete?.invoke(formError)
        }
    }

    private fun initializeAdsIfAllowed() {
        if (consentInformation.canRequestAds()) {
            initializeAds()
        }
    }

    private fun syncUiState(isConsentFlowComplete: Boolean) {
        _uiState.value = PrivacyConsentUiState(
            canRequestAds = consentInformation.canRequestAds(),
            isPrivacyOptionsRequired =
                consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            isConsentFlowComplete = isConsentFlowComplete,
        )
    }

    private fun buildDebugSettingsIfNeeded(activity: Activity): ConsentDebugSettings? {
        if (!activity.resources.getBoolean(R.bool.ump_force_debug_eea)) return null

        val builder = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)

        val hashedDeviceId = activity.getString(R.string.ump_test_device_hashed_id).trim()
        if (hashedDeviceId.isNotEmpty()) {
            builder.addTestDeviceHashedId(hashedDeviceId)
            Log.d(TAG, "UMP debug geography enabled with explicit test device id.")
        } else {
            Log.d(
                TAG,
                "UMP debug geography enabled without explicit test device id. " +
                    "This works on emulators, and on physical devices only after adding the hashed test device id.",
            )
        }
        return builder.build()
    }

    private fun logConsentState(stage: String) {
        Log.d(
            TAG,
            "stage=$stage, canRequestAds=${consentInformation.canRequestAds()}, " +
                "privacyOptions=${consentInformation.privacyOptionsRequirementStatus}",
        )
    }

    companion object {
        private const val TAG = "PrivacyConsent"
    }
}
