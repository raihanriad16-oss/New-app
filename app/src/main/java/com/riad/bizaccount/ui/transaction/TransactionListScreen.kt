package com.riad.bizaccount.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riad.bizaccount.data.local.dao.TransactionWithCategory
import com.riad.bizaccount.ui.dashboard.DashboardViewModel
import com.riad.bizaccount.ui.dashboard.EmptyState
import com.riad.bizaccount.ui.dashboard.TransactionRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    transactionViewModel: TransactionViewModel = hiltViewModel()
) {
    val dashState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        transactionViewModel.events.collect { event ->
            if (event is TransactionEvent.Deleted) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "লেনদেন মুছে ফেলা হয়েছে",
                        actionLabel = "পূর্বাবস্থায় ফিরুন",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        transactionViewModel.undoDelete(event.id)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("সব লেনদেন") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (dashState.recent.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState("এখনো কোনো লেনদেন নেই")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = dashState.recent,
                    key = { it.id }
                ) { tx ->
                    SwipeableTransactionRow(
                        tx = tx,
                        onEdit = onEdit,
                        onDelete = { transactionViewModel.delete(tx.id) }
                    )
                }
            }
        }
    }
}

/**
 * Extracted into its own composable (rather than inlined in the items {} lambda) so the
 * SwipeToDismissBox / rememberSwipeToDismissBoxState calls sit in an unambiguous composable
 * context.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionRow(
    tx: TransactionWithCategory,
    onEdit: (Long) -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFC62828))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable { onEdit(tx.id) }
        ) {
            TransactionRow(tx)
        }
    }
}
