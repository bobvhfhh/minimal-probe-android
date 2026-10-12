package com.minprobe.app.ui.screen.ping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.model.PingTask
import com.minprobe.app.ui.screen.HandleEffects
import com.minprobe.app.ui.screen.server.ServerReLoginScreen
import com.minprobe.app.ui.screen.showToast

class PingMonitorScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = koinScreenModel<PingMonitorViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        var editing by remember { mutableStateOf<PingTask?>(null) }
        var adding by remember { mutableStateOf(false) }

        HandleEffects(viewModel.effect) { effect -> when (effect) {
            is PingMonitorContract.Effect.ShowToast -> showToast(context, effect.message)
            PingMonitorContract.Effect.NavigateToRelogin -> navigator.push(ServerReLoginScreen())
        } }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("TCP 延迟监控") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { adding = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加监控")
                }
            }
        ) { padding ->
            when {
                state.isLoading -> Box(Modifier.fillMaxSize().padding(padding)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                state.error != null -> Column(
                    Modifier.fillMaxSize().padding(padding).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { viewModel.onEvent(PingMonitorContract.Event.Refresh) }) { Text("重试") }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Text("每个目标由已选节点主动建立 TCP 连接；延迟和丢包会保存在你的探针服务端。", modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (state.tasks.isEmpty()) item {
                        Text("还没有监控任务", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(state.tasks, key = { it.id }) { task ->
                        PingTaskRow(task, state.nodes, onEdit = { editing = task }, onDelete = {
                            viewModel.onEvent(PingMonitorContract.Event.Delete(task.id))
                        })
                    }
                    item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }

        if (adding || editing != null) {
            PingTaskDialog(
                initial = editing,
                nodes = state.nodes,
                saving = state.isSaving,
                onDismiss = { adding = false; editing = null },
                onSave = { task -> viewModel.onEvent(PingMonitorContract.Event.Save(task)); adding = false; editing = null }
            )
        }
    }
}

@Composable
private fun PingTaskRow(task: PingTask, nodes: List<Node>, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable(onClick = onEdit),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(task.target, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                val assigned = nodes.filter { it.id in task.nodeIds }.joinToString { it.name }
                Text("${task.intervalSeconds} 秒 · ${task.nodeIds.size} 个节点${if (assigned.isBlank()) "" else "：$assigned"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "编辑") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun PingTaskDialog(initial: PingTask?, nodes: List<Node>, saving: Boolean, onDismiss: () -> Unit, onSave: (PingTask) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var target by remember(initial) { mutableStateOf(initial?.target ?: "1.1.1.1:443") }
    var interval by remember(initial) { mutableStateOf((initial?.intervalSeconds ?: 60).toString()) }
    var selected by remember(initial) { mutableStateOf(initial?.nodeIds?.toSet() ?: emptySet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加监控" else "编辑监控") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(target, { target = it }, label = { Text("目标地址") }, supportingText = { Text("host:port，例如 1.1.1.1:443") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(interval, { value -> interval = value.filter { it.isDigit() } }, label = { Text("间隔（秒）") }, supportingText = { Text("5–3600 秒") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("执行节点", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                nodes.forEach { node ->
                    Row(Modifier.fillMaxWidth().clickable { selected = selected.toggle(node.id) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = node.id in selected, onCheckedChange = { selected = selected.toggle(node.id) })
                        Spacer(Modifier.width(4.dp))
                        Text(node.name, modifier = Modifier.weight(1f))
                        Text(if (node.online) "在线" else "离线", style = MaterialTheme.typography.labelSmall, color = if (node.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                onSave(PingTask(initial?.id ?: 0, name, target, interval.toIntOrNull() ?: 0, selected.toList()))
            }) { Text(if (saving) "保存中…" else "保存") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun Set<Long>.toggle(value: Long): Set<Long> = if (value in this) this - value else this + value
