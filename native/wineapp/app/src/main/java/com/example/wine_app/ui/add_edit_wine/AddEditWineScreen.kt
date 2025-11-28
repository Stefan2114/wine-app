package com.example.wine_app.ui.add_edit_wine

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.wine_app.util.UiEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWineScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditWineViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        // Try to parse the existing date, default to now
        initialSelectedDateMillis = try {
            LocalDate.parse(viewModel.productionDate).toMillis()
        } catch (e: Exception) {
            Instant.now().toEpochMilli()
        },
        // Don't allow selecting future dates
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= Instant.now().toEpochMilli()
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        // Get the selected date and send it to the ViewModel
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = millis.toLocalDate()
                            viewModel.onEvent(
                                AddEditWineEvent.OnProductionDateChange(selectedDate.toString())
                            )
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.PopBackStack -> onPopBackStack()
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(AddEditWineEvent.OnSaveWineClick) }) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save wine")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                value = viewModel.name, onValueChange = {
                    viewModel.onEvent(AddEditWineEvent.OnNameChange(it))
                },
                label = {
                    Text(text = "Name")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = viewModel.price,
                onValueChange = {
                    viewModel.onEvent(AddEditWineEvent.OnPriceChange(it))
                },
                label = {
                    Text(text = "Price")
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = viewModel.productionDate,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = {
                    Text(text = "Production Date (YYYY-MM-DD)")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Button
                        onClick(label = "Open date picker", action = null)
                    }
                    .clickable { showDatePicker = true }
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = viewModel.origin, onValueChange = {
                    viewModel.onEvent(AddEditWineEvent.OnOriginChange(it))
                },
                label = {
                    Text(text = "Origin location (Optional)")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = viewModel.alcoholDegree,
                onValueChange = {
                    viewModel.onEvent(AddEditWineEvent.OnAlcoholDegreeChange(it))
                },
                label = {
                    Text(text = "Alcohol degrees")
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.toMillis(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()