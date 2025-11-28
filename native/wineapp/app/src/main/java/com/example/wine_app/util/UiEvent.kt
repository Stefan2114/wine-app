package com.example.wine_app.util

sealed class UiEvent {
    object PopBackStack : UiEvent()
    data class Navigate(val destination: Any) : UiEvent()
    data class ShowSnackbar(val message: String) : UiEvent()
}