package com.example.wine_app.ui.add_edit_wine

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wine_app.data.Wine
import com.example.wine_app.data.WineRepository
import com.example.wine_app.exception.ServerNotRespondingException
import com.example.wine_app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditWineViewModel @Inject constructor(
    private val repository: WineRepository,
    saveStateHandle: SavedStateHandle
) : ViewModel() {
    var wine by mutableStateOf<Wine?>(null)
        private set

    var name by mutableStateOf("")
        private set

    var price by mutableStateOf("")
        private set

    var productionDate by mutableStateOf("")
        private set

    var origin by mutableStateOf("")
        private set

    var alcoholDegree by mutableStateOf("")
        private set

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        val wineId = saveStateHandle.get<Int>("wineId")!!
        if (wineId != -1) {
            viewModelScope.launch {
                repository.getWineById(wineId)?.let { wine ->
                    name = wine.name
                    price = wine.price.toString()
                    productionDate = wine.productionDate
                    origin = wine.origin ?: ""
                    alcoholDegree = wine.alcoholDegree.toString()
                    this@AddEditWineViewModel.wine = wine
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onEvent(event: AddEditWineEvent) {
        when (event) {
            is AddEditWineEvent.OnNameChange -> {
                name = event.name
            }

            is AddEditWineEvent.OnPriceChange -> {
                price = event.price
            }

            is AddEditWineEvent.OnProductionDateChange -> {
                productionDate = event.date
            }

            is AddEditWineEvent.OnOriginChange -> {
                origin = event.origin
            }

            is AddEditWineEvent.OnAlcoholDegreeChange -> {
                alcoholDegree = event.alcoholDegree
            }

            is AddEditWineEvent.OnSaveWineClick -> {
                viewModelScope.launch {
                    if (name.isBlank()) {
                        sendUiEvent(
                            UiEvent.ShowSnackbar(
                                message = "The name can't be empty"
                            )
                        )
                        return@launch
                    }

                    val parsedPrice = price.toDoubleOrNull()

                    if (parsedPrice == null) {
                        sendUiEvent(UiEvent.ShowSnackbar(message = "Invalid price format"))
                        return@launch
                    }

                    if (parsedPrice < 0.0) {
                        sendUiEvent(
                            UiEvent.ShowSnackbar(
                                message = "Price must be non-negative"
                            )
                        )
                        return@launch
                    }

                    val dateString = productionDate

                    if (dateString.isBlank()) {
                        sendUiEvent(
                            UiEvent.ShowSnackbar(message = "Production date can't be empty")
                        )
                        return@launch
                    }

                    val parsedAlcoholDegree = alcoholDegree.toDoubleOrNull()

                    if (parsedAlcoholDegree == null) {
                        sendUiEvent(UiEvent.ShowSnackbar(message = "Invalid alcohol degree format"))
                        return@launch
                    }

                    if (parsedAlcoholDegree < 0.0 || parsedAlcoholDegree > 100.0) {
                        sendUiEvent(
                            UiEvent.ShowSnackbar(
                                message = "Alcohol degree must be between 0 and 100"
                            )
                        )
                        return@launch
                    }

                    try {
                        val newWine = Wine(
                            id = wine?.id,
                            name = name,
                            price = parsedPrice,
                            productionDate = productionDate,
                            origin = origin,
                            alcoholDegree = parsedAlcoholDegree
                        )

                        if (wine == null) {
                            repository.addWine(newWine)
                        } else {
                            repository.updateWine(newWine)
                        }

                        sendUiEvent(UiEvent.PopBackStack)
                    }catch (e: ServerNotRespondingException) {
                        sendUiEvent(UiEvent.ShowSnackbar(
                            message = e.message ?: "Couldn't connect. Saved locally."
                        ))
                        sendUiEvent(UiEvent.PopBackStack)

                    }
                    catch (e: Exception) {
                        sendUiEvent(
                            UiEvent.ShowSnackbar(
                                message = e.message ?: "Failed to save wine"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}