package com.example.wine_app.ui.wine_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.wine_app.util.UiEvent

@Composable
fun WineListScreen(
    onNavigate: (UiEvent.Navigate) -> Unit,
    viewModel: WineListViewModel = hiltViewModel()
) {
    val wines = viewModel.wines.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val wineToDelete = viewModel.winePendingDeletion

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigate -> onNavigate(event)
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }

                else -> Unit
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.onEvent(WineListEvent.OnAddWineClick)
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add new wine")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(wines.value) { wine ->
                WineItem(
                    wine = wine,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            onClick(label = "Edit wine details", action = null)
                        }
                        .clickable {
                            viewModel.onEvent(WineListEvent.OnWineClick(wine))
                        }
                        .padding(16.dp)
                )
            }
        }
    }

    if (wineToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onEvent(WineListEvent.OnDismissDeleteDialog)
            },
            title = { Text(text = "Confirm Deletion") },
            text = { Text(text = "Are you sure you want to delete '${wineToDelete.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(WineListEvent.OnConfirmDelete)
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(WineListEvent.OnDismissDeleteDialog)
                    }
                ) {
                    Text("No")
                }
            }
        )
    }
}