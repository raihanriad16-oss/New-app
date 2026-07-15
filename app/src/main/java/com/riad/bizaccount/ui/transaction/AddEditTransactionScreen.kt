package com.riad.bizaccount.ui.transaction

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riad.bizaccount.data.local.entity.CategoryEntity
import com.riad.bizaccount.data.local.entity.PaymentMethod
import com.riad.bizaccount.data.local.entity.TransactionType
import com.riad.bizaccount.util.DateUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    editId: Long?,
    onDone: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle(initialValue = emptyList())
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current

    LaunchedEffect(editId) { if (editId != null) viewModel.loadForEdit(editId) }
    LaunchedEffect(Unit) { viewModel.events.collect { onDone() } }

    val categories = if (state.type == TransactionType.INCOME) incomeCategories else expenseCategories

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (editId != null) "লেনদেন সম্পাদনা করুন" else "লেনদেন যোগ করুন") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChange(TransactionType.INCOME) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("আয়") }
                SegmentedButton(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChange(TransactionType.EXPENSE) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("ব্যয়") }
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text("পরিমাণ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.amountError != null,
                supportingText = {
                    when (state.amountError) {
                        "AMOUNT_REQUIRED" -> Text("পরিমাণ লিখুন")
                        "AMOUNT_INVALID" -> Text("সঠিক পরিমাণ লিখুন")
                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("বিবরণ") },
                modifier = Modifier.fillMaxWidth()
            )

            CategoryDropdown(
                categories = categories,
                selectedId = state.categoryId,
                isError = state.categoryError != null,
                onSelect = viewModel::onCategoryChange
            )

            Text("পেমেন্ট পদ্ধতি", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.paymentMethod == PaymentMethod.CASH, onClick = { viewModel.onPaymentMethodChange(PaymentMethod.CASH) }, label = { Text("নগদ") })
                FilterChip(selected = state.paymentMethod == PaymentMethod.BANK, onClick = { viewModel.onPaymentMethodChange(PaymentMethod.BANK) }, label = { Text("ব্যাংক") })
                FilterChip(selected = state.paymentMethod == PaymentMethod.MOBILE_BANKING, onClick = { viewModel.onPaymentMethodChange(PaymentMethod.MOBILE_BANKING) }, label = { Text("মোবাইল ব্যাংকিং") })
            }

            OutlinedTextField(
                value = DateUtils.formatBangla(state.date),
                onValueChange = {},
                readOnly = true,
                label = { Text("তারিখ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> viewModel.onDateChange(LocalDate.of(y, m + 1, d)) },
                            state.date.year, state.date.monthValue - 1, state.date.dayOfMonth
                        ).show()
                    }
            )

            OutlinedTextField(
                value = state.referenceNumber,
                onValueChange = viewModel::onReferenceChange,
                label = { Text("রেফারেন্স নম্বর") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("মন্তব্য (ঐচ্ছিক)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving) {
                Text("সংরক্ষণ করুন")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    isError: Boolean,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.find { it.id == selectedId }?.name ?: ""

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("ক্যাটাগরি") },
            isError = isError,
            supportingText = { if (isError) Text("একটি ক্যাটাগরি নির্বাচন করুন") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(text = { Text(cat.name) }, onClick = { onSelect(cat.id); expanded = false })
            }
        }
    }
}
