package com.example.wine_app.ui.wine_list

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wine_app.data.Wine
import com.example.wine_app.data.WineRepository
import com.example.wine_app.util.AddEditWine
import com.example.wine_app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@HiltViewModel
class WineListViewModel @Inject constructor(
    private val repository: WineRepository
) : ViewModel() {

    val wines = repository.getWines()
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    var winePendingDeletion by mutableStateOf<Wine?>(null)
        private set

    fun onEvent(event: WineListEvent) {
        when (event) {
            is WineListEvent.OnWineClick -> {
                sendUiEvent(UiEvent.Navigate(AddEditWine(wineId = event.wine.id!!)))
            }

            is WineListEvent.OnDeleteWineClick -> {
                winePendingDeletion = event.wine
            }

            is WineListEvent.OnConfirmDelete -> {
                winePendingDeletion?.let { wine ->
                    viewModelScope.launch {
                        try{
                            repository.deleteWine(wine)
                            sendUiEvent(
                                UiEvent.ShowSnackbar(
                                    message = "Wine deleted"
                                )
                            )
                        }catch (e: Exception) {
                            sendUiEvent(UiEvent.ShowSnackbar(e.message ?: "Error"))
                        }

                    }
                }
                winePendingDeletion = null
            }

            is WineListEvent.OnDismissDeleteDialog -> {
                winePendingDeletion = null
            }
            is WineListEvent.OnAddWineClick -> {
                sendUiEvent(UiEvent.Navigate(AddEditWine()))
            }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}