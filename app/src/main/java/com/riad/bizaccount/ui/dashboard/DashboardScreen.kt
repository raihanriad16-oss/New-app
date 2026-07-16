package com.riad.bizaccount.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riad.bizaccount.R
import com.riad.bizaccount.data.local.dao.TransactionWithCategory
import com.riad.bizaccount.data.local.entity.TransactionType
import com.riad.bizaccount.ui.common.IncomeExpensePieChart
import com.riad.bizaccount.ui.theme.ExpenseRed
import com.riad.bizaccount.ui.theme.IncomeGreen
import com.riad.bizaccount.ui.theme.ProfitBlue
import com.riad.bizaccount.util.CurrencyFormatter
import com.riad.bizaccount.util.DateUtils

@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    onSeeAll: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddTransaction, icon = { Icon(Icons.Default.Add, null) }, text = { Text(stringRes(R.string.add_transaction)) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle(stringRes(R.string.nav_dashboard)) }

            item {
                PeriodSummaryCard(title = "আজ", stats = state.today)
            }
            item {
                PeriodSummaryCard(title = "এই মাস", stats = state.month)
            }
            item {
                PeriodSummaryCard(title = "এই বছর", stats = state.year)
            }

            item {
                Card(shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("আয় বনাম ব্যয় (এই মাস)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        IncomeExpensePieChart(state.month.incomeMinor, state.month.expenseMinor, IncomeGreen, ExpenseRed)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringRes(R.string.recent_transactions), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringRes(R.string.see_all),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                    )
                }
            }

            if (state.recent.isEmpty()) {
                item { EmptyState(stringRes(R.string.no_transactions_yet)) }
            } else {
                items(state.recent, key = { it.id }) { tx: TransactionWithCategory ->
                    TransactionRow(tx)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun PeriodSummaryCard(title: String, stats: PeriodStats) {
    Card(shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("আয়", stats.incomeMinor, IncomeGreen)
                StatColumn("ব্যয়", stats.expenseMinor, ExpenseRed)
                StatColumn(
                    if (stats.isProfit) "লাভ" else "ক্ষতি",
                    kotlin.math.abs(stats.profitMinor),
                    if (stats.isProfit) ProfitBlue else ExpenseRed
                )
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, minor: Long, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(CurrencyFormatter.format(minor), style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransactionRow(tx: TransactionWithCategory) {
    Card(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(tx.description.ifBlank { tx.categoryName }, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${tx.categoryName} • ${DateUtils.formatBangla(java.time.LocalDate.ofEpochDay(tx.dateEpochDay))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                (if (tx.type == TransactionType.INCOME) "+ " else "- ") + CurrencyFormatter.format(tx.amountMinor),
                color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)
