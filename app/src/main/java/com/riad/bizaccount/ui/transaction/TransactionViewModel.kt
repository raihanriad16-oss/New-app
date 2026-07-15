package com.riad.bizaccount.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riad.bizaccount.data.local.entity.PaymentMethod
import com.riad.bizaccount.data.local.entity.TransactionEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import com.riad.bizaccount.data.repository.CategoryRepository
import com.riad.bizaccount.data.repository.TransactionRepository
import com.riad.bizaccount.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class TransactionFormState(
    val id: Long? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val amountText: String = "",
    val description: String = "",
    val categoryId: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val referenceNumber: String = "",
    val notes: String = "",
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSaving: Boolean = false
)

sealed class TransactionEvent {
    object Saved : TransactionEvent()
    data class Deleted(val id: Long) : TransactionEvent()
}

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val _events = MutableSharedFlow<TransactionEvent>()
    val events = _events.asSharedFlow()

    val incomeCategories = categoryRepository.byType(TransactionType.INCOME)
    val expenseCategories = categoryRepository.byType(TransactionType.EXPENSE)

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            val tx = transactionRepository.getById(id) ?: return@launch
            _formState.value = TransactionFormState(
                id = tx.id,
                type = tx.type,
                date = LocalDate.ofEpochDay(tx.dateEpochDay),
                time = LocalTime.ofSecondOfDay(tx.timeOfDaySeconds.toLong()),
                amountText = (tx.amountMinor / 100.0).let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() },
                description = tx.description,
                categoryId = tx.categoryId,
                paymentMethod = tx.paymentMethod,
                referenceNumber = tx.referenceNumber ?: "",
                notes = tx.notes ?: ""
            )
        }
    }

    fun onTypeChange(type: TransactionType) {
        _formState.value = _formState.value.copy(type = type, categoryId = null)
    }

    fun onDateChange(date: LocalDate) { _formState.value = _formState.value.copy(date = date) }
    fun onTimeChange(time: LocalTime) { _formState.value = _formState.value.copy(time = time) }
    fun onAmountChange(text: String) { _formState.value = _formState.value.copy(amountText = text, amountError = null) }
    fun onDescriptionChange(text: String) { _formState.value = _formState.value.copy(description = text) }
    fun onCategoryChange(id: Long) { _formState.value = _formState.value.copy(categoryId = id, categoryError = null) }
    fun onPaymentMethodChange(pm: PaymentMethod) { _formState.value = _formState.value.copy(paymentMethod = pm) }
    fun onReferenceChange(text: String) { _formState.value = _formState.value.copy(referenceNumber = text) }
    fun onNotesChange(text: String) { _formState.value = _formState.value.copy(notes = text) }

    fun save() {
        val state = _formState.value
        val amount = state.amountText.toDoubleOrNull()

        var amountError: String? = null
        var categoryError: String? = null
        if (state.amountText.isBlank()) amountError = "AMOUNT_REQUIRED"
        else if (amount == null || amount <= 0.0) amountError = "AMOUNT_INVALID"
        if (state.categoryId == null) categoryError = "CATEGORY_REQUIRED"

        if (amountError != null || categoryError != null) {
            _formState.value = state.copy(amountError = amountError, categoryError = categoryError)
            return
        }

        viewModelScope.launch {
            _formState.value = state.copy(isSaving = true)
            val entity = TransactionEntity(
                id = state.id ?: 0,
                type = state.type,
                dateEpochDay = state.date.toEpochDay(),
                timeOfDaySeconds = DateUtils.timeOfDaySeconds(state.time),
                amountMinor = Math.round(amount!! * 100.0),
                description = state.description.trim(),
                categoryId = state.categoryId!!,
                paymentMethod = state.paymentMethod,
                referenceNumber = state.referenceNumber.trim().ifBlank { null },
                notes = state.notes.trim().ifBlank { null }
            )
            if (state.id == null) transactionRepository.add(entity) else transactionRepository.update(entity)
            _formState.value = TransactionFormState()
            _events.emit(TransactionEvent.Saved)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            transactionRepository.softDelete(id)
            _events.emit(TransactionEvent.Deleted(id))
        }
    }

    fun undoDelete(id: Long) {
        viewModelScope.launch { transactionRepository.undoDelete(id) }
    }
}
