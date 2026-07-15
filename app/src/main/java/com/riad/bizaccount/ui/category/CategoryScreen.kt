package com.riad.bizaccount.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riad.bizaccount.data.local.entity.CategoryEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    val income by viewModel.incomeCategories.collectAsStateWithLifecycle(initialValue = emptyList())
    val expense by viewModel.expenseCategories.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories = if (selectedType == TransactionType.INCOME) income else expense

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteBlockedMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is CategoryUiEvent.DeleteBlocked) {
                deleteBlockedMessage = "এই ক্যাটাগরিতে ${event.count}টি লেনদেন থাকায় মুছে ফেলা যাবে না"
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ক্যাটাগরি") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddDialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("ক্যাটাগরি যোগ করুন") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                SegmentedButton(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("আয়ের ক্যাটাগরি") }
                SegmentedButton(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("ব্যয়ের ক্যাটাগরি") }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { cat ->
                    Card(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(1.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat.name, style = MaterialTheme.typography.titleMedium)
                            Row {
                                IconButton(onClick = { editTarget = cat }) { Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা") }
                                IconButton(onClick = { viewModel.deleteCategory(cat.id) }) { Icon(Icons.Default.Delete, contentDescription = "মুছে ফেলুন") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryNameDialog(
            title = "ক্যাটাগরি যোগ করুন",
            initialValue = "",
            onConfirm = { name -> viewModel.addCategory(name, selectedType); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }

    editTarget?.let { cat ->
        CategoryNameDialog(
            title = "ক্যাটাগরি সম্পাদনা",
            initialValue = cat.name,
            onConfirm = { name -> viewModel.renameCategory(cat, name); editTarget = null },
            onDismiss = { editTarget = null }
        )
    }

    deleteBlockedMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { deleteBlockedMessage = null },
            confirmButton = { TextButton(onClick = { deleteBlockedMessage = null }) { Text("ঠিক আছে") } },
            title = { Text("মুছে ফেলা যাবে না") },
            text = { Text(msg) }
        )
    }
}

@Composable
private fun CategoryNameDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("ক্যাটাগরির নাম") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("সংরক্ষণ করুন") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
