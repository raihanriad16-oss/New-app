package com.riad.bizaccount.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riad.bizaccount.data.local.dao.TransactionWithCategory
import com.riad.bizaccount.data.local.entity.CategoryEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import com.riad.bizaccount.data.repository.SortOption
import com.riad.bizaccount.ui.dashboard.EmptyState
import com.riad.bizaccount.ui.dashboard.TransactionRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onEdit: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle(initialValue = emptyList())
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("খুঁজুন") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::onQueryChange,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("লেনদেন খুঁজুন…") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filters.type == TransactionType.INCOME,
                        onClick = { viewModel.onTypeFilter(if (filters.type == TransactionType.INCOME) null else TransactionType.INCOME) },
                        label = { Text("আয়") }
                    )
                }
                item {
                    FilterChip(
                        selected = filters.type == TransactionType.EXPENSE,
                        onClick = { viewModel.onTypeFilter(if (filters.type == TransactionType.EXPENSE) null else TransactionType.EXPENSE) },
                        label = { Text("ব্যয়") }
                    )
                }
                items(categories, key = { it.id }) { cat: CategoryEntity ->
                    FilterChip(
                        selected = filters.categoryId == cat.id,
                        onClick = { viewModel.onCategoryFilter(if (filters.categoryId == cat.id) null else cat.id) },
                        label = { Text(cat.name) }
                    )
                }
                item {
                    Box {
                        TextButton(onClick = { sortMenuExpanded = true }) { Text("সাজান ▾") }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("নতুন থেকে পুরাতন") }, onClick = { viewModel.onSort(SortOption.DATE_DESC); sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("পুরাতন থেকে নতুন") }, onClick = { viewModel.onSort(SortOption.DATE_ASC); sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("বেশি পরিমাণ আগে") }, onClick = { viewModel.onSort(SortOption.AMOUNT_DESC); sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("কম পরিমাণ আগে") }, onClick = { viewModel.onSort(SortOption.AMOUNT_ASC); sortMenuExpanded = false })
                        }
                    }
                }
            }

            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState("কোনো ফলাফল পাওয়া যায়নি")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { tx: TransactionWithCategory ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onEdit(tx.id) }
                        ) {
                            TransactionRow(tx)
                        }
                    }
                }
            }
        }
    }
}
