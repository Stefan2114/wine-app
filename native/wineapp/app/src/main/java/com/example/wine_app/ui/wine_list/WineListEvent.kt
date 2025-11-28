package com.example.wine_app.ui.wine_list

import com.example.wine_app.data.Wine

sealed class WineListEvent {
    data class OnDeleteWineClick(val wine: Wine): WineListEvent()

    object OnConfirmDelete: WineListEvent()

    object OnDismissDeleteDialog: WineListEvent()
    data class OnWineClick(val wine: Wine): WineListEvent()
    object OnAddWineClick: WineListEvent()
}