package com.riad.bizaccount.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riad.bizaccount.ui.theme.ExpenseRed
import com.riad.bizaccount.ui.theme.IncomeGreen
import com.riad.bizaccount.ui.theme.ProfitBlue
import com.riad.bizaccount.util.CurrencyFormatter
import com.riad.bizaccount.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.exportEvents.collect { event ->
            scope.launch {
                val message = when (event) {
                    is ExportEvent.Success -> "এক্সপোর্ট সফল হয়েছে: ${event.file.name}"
                    is ExportEvent.Failure -> "এক্সপোর্ট ব্যর্থ হয়েছে"
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("রিপোর্ট") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.period == ReportPeriod.DAILY, onClick = { viewModel.selectPeriod(ReportPeriod.DAILY) }, label = { Text("দৈনিক") }) }
                    item { FilterChip(selected = state.period == ReportPeriod.WEEKLY, onClick = { viewModel.selectPeriod(ReportPeriod.WEEKLY) }, label = { Text("সাপ্তাহিক") }) }
                    item { FilterChip(selected = state.period == ReportPeriod.MONTHLY, onClick = { viewModel.selectPeriod(ReportPeriod.MONTHLY) }, label = { Text("মাসিক") }) }
                    item { FilterChip(selected = state.period == ReportPeriod.YEARLY, onClick = { viewModel.selectPeriod(ReportPeriod.YEARLY) }, label = { Text("বার্ষিক") }) }
                }
            }

            item {
                Text(
                    "${DateUtils.formatBangla(state.startDate)} \u2014 ${DateUtils.formatBangla(state.endDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Card(shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReportLine("মোট আয়", state.incomeMinor, IncomeGreen)
                        ReportLine("মোট ব্যয়", state.expenseMinor, ExpenseRed)
                        ReportLine(
                            if (state.isProfit) "নিট লাভ" else "নিট ক্ষতি",
                            kotlin.math.abs(state.netMinor),
                            if (state.isProfit) ProfitBlue else ExpenseRed
                        )
                        ReportLine("গড় লেনদেন", state.averageMinor, MaterialTheme.colorScheme.onSurface)
                        Text("মোট লেনদেন: ${state.transactionCount}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { viewModel.exportPdf() }, modifier = Modifier.weight(1f)) { Text("পিডিএফ এক্সপোর্ট") }
                    OutlinedButton(onClick = { viewModel.exportExcel() }, modifier = Modifier.weight(1f)) { Text("এক্সেল এক্সপোর্ট") }
                }
            }
        }
    }
}

@Composable
private fun ReportLine(label: String, minor: Long, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(CurrencyFormatter.format(minor), style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}
