package com.minprobe.app.ui.screen.nodelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.minprobe.app.R
import com.minprobe.app.domain.model.Node
import com.minprobe.app.ui.screen.HandleEffects
import com.minprobe.app.ui.screen.nodedetail.NodeDetailScreen
import com.minprobe.app.ui.screen.ping.PingMonitorScreen
import com.minprobe.app.ui.screen.server.ServerReLoginScreen
import com.minprobe.app.ui.screen.showToast

class NodeListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val viewModel = koinScreenModel<NodeListViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val isWideScreen = LocalConfiguration.current.screenWidthDp >= 700

        // 前台时开启 WS 推送，后台时断开（省电省流量）
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.onScreenStarted()
                    Lifecycle.Event.ON_STOP -> viewModel.onScreenStopped()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                viewModel.onScreenStopped()
            }
        }

        HandleEffects(viewModel.effect) { effect ->
            when (effect) {
                is NodeListContract.Effect.ShowToast -> showToast(context, effect.message)
                is NodeListContract.Effect.NavigateToNodeDetail ->
                    navigator.push(NodeDetailScreen(effect.nodeId))
                is NodeListContract.Effect.NavigateToRelogin ->
                    navigator.push(ServerReLoginScreen())
            }
        }

        val filteredNodes by remember(
            state.nodes, state.searchQuery, state.selectedGroup, state.statusFilter, state.sort
        ) {
            derivedStateOf {
                filterAndSortNodes(
                    state.nodes, state.searchQuery, state.selectedGroup,
                    state.statusFilter, state.sort
                )
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "${state.serverName} · ${state.onlineCount}/${state.nodes.size}"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.push(PingMonitorScreen()) }) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = "TCP 延迟监控")
                        }
                        SortMenu(state.sort) { viewModel.onEvent(NodeListContract.Event.SortSelected(it)) }
                    }
                )
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.onEvent(NodeListContract.Event.Refresh) },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    state.error != null -> ErrorView(
                        message = state.error!!,
                        onRetry = { viewModel.onEvent(NodeListContract.Event.Retry) }
                    )
                    else -> Column(Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = {
                                viewModel.onEvent(NodeListContract.Event.SearchQueryChanged(it))
                            },
                            placeholder = { Text(stringResource(R.string.node_search_hint)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        FilterRow(
                            state = state,
                            onGroupSelected = {
                                viewModel.onEvent(NodeListContract.Event.GroupSelected(it))
                            },
                            onStatusSelected = {
                                viewModel.onEvent(NodeListContract.Event.StatusFilterSelected(it))
                            }
                        )
                        if (filteredNodes.isEmpty()) {
                            Box(Modifier.fillMaxSize()) {
                                Text(
                                    stringResource(R.string.node_empty),
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = if (isWideScreen) GridCells.Adaptive(minSize = 360.dp) else GridCells.Fixed(1),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredNodes, key = { it.id }) { node ->
                                    NodeCard(node = node) {
                                        viewModel.onEvent(
                                            NodeListContract.Event.NodeClicked(node.id)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    state: NodeListContract.State,
    onGroupSelected: (String?) -> Unit,
    onStatusSelected: (NodeListContract.StatusFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = state.statusFilter == NodeListContract.StatusFilter.ALL,
                onClick = { onStatusSelected(NodeListContract.StatusFilter.ALL) },
                label = { Text(stringResource(R.string.node_filter_all)) }
            )
        }
        item {
            FilterChip(
                selected = state.statusFilter == NodeListContract.StatusFilter.ONLINE,
                onClick = { onStatusSelected(NodeListContract.StatusFilter.ONLINE) },
                label = {
                    Text("${stringResource(R.string.node_filter_online)} ${state.onlineCount}")
                }
            )
        }
        item {
            FilterChip(
                selected = state.statusFilter == NodeListContract.StatusFilter.OFFLINE,
                onClick = { onStatusSelected(NodeListContract.StatusFilter.OFFLINE) },
                label = {
                    Text("${stringResource(R.string.node_filter_offline)} ${state.offlineCount}")
                }
            )
        }
        if (state.groups.isNotEmpty()) {
            item {
                FilterChip(
                    selected = state.selectedGroup == null,
                    onClick = { onGroupSelected(null) },
                    label = { Text(stringResource(R.string.node_group_all)) }
                )
            }
            items(state.groups, key = { it }) { group ->
                FilterChip(
                    selected = state.selectedGroup == group,
                    onClick = { onGroupSelected(group) },
                    label = { Text(group) }
                )
            }
        }
    }
}

@Composable
private fun SortMenu(
    current: NodeListContract.NodeSort,
    onSelected: (NodeListContract.NodeSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.sort_by))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            NodeListContract.NodeSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(sortLabel(option)) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun sortLabel(sort: NodeListContract.NodeSort): String {
    return when (sort) {
        NodeListContract.NodeSort.DEFAULT -> stringResource(R.string.sort_default)
        NodeListContract.NodeSort.NAME -> stringResource(R.string.sort_name)
        NodeListContract.NodeSort.CPU -> stringResource(R.string.sort_cpu)
        NodeListContract.NodeSort.MEM -> stringResource(R.string.sort_mem)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onRetry) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

private fun filterAndSortNodes(
    nodes: List<Node>,
    query: String,
    group: String?,
    statusFilter: NodeListContract.StatusFilter,
    sort: NodeListContract.NodeSort
): List<Node> {
    var result = nodes
    if (query.isNotBlank()) {
        val q = query.trim().lowercase()
        result = result.filter {
            it.name.lowercase().contains(q) || it.publicRemark.lowercase().contains(q)
        }
    }
    if (group != null) {
        result = result.filter { it.group == group }
    }
    result = when (statusFilter) {
        NodeListContract.StatusFilter.ALL -> result
        NodeListContract.StatusFilter.ONLINE -> result.filter { it.online }
        NodeListContract.StatusFilter.OFFLINE -> result.filter { !it.online }
    }
    return when (sort) {
        NodeListContract.NodeSort.DEFAULT -> result
        NodeListContract.NodeSort.NAME -> result.sortedBy { it.name.lowercase() }
        NodeListContract.NodeSort.CPU ->
            result.sortedByDescending { it.metrics?.cpu ?: -1.0 }
        NodeListContract.NodeSort.MEM ->
            result.sortedByDescending { it.memPercent }
    }
}
