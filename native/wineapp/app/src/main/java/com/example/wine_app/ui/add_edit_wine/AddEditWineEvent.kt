package com.example.wine_app.ui.add_edit_wine

sealed class AddEditWineEvent {
    data class OnNameChange(val name: String) : AddEditWineEvent()
    data class OnPriceChange(val price: String) : AddEditWineEvent()
    data class OnProductionDateChange(val date: String) : AddEditWineEvent()
    data class OnOriginChange(val origin: String) : AddEditWineEvent()
    data class OnAlcoholDegreeChange(val alcoholDegree: String) : AddEditWineEvent()
    object OnSaveWineClick : AddEditWineEvent()
}