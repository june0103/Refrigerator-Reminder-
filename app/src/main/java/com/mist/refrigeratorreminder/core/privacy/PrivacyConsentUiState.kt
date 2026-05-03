package com.mist.refrigeratorreminder.core.privacy

data class PrivacyConsentUiState(
    val canRequestAds: Boolean = false,
    val isPrivacyOptionsRequired: Boolean = false,
    val isConsentFlowComplete: Boolean = false,
)
