package com.minprobe.app.ui.screen.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.minprobe.app.R
import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.ui.screen.HandleEffects
import com.minprobe.app.ui.screen.showToast

/** 添加 / 编辑面板。editingId 为 null 时是添加模式。 */
data class AddServerScreen(val editingId: Long? = null) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = koinScreenModel<ServerViewModel>()
        val state by viewModel.addState.collectAsStateWithLifecycle()

        LaunchedEffect(editingId) {
            viewModel.initAddForm(editingId)
        }

        HandleEffects(viewModel.effect) { effect ->
            when (effect) {
                is ServerContract.Effect.ShowToast -> showToast(context, effect.message)
                is ServerContract.Effect.NavigateBack -> navigator.pop()
                is ServerContract.Effect.NavigateToNodeList -> Unit
            }
        }

        var passwordVisible by rememberSaveable { mutableStateOf(false) }
        val isHttp = remember(state.baseUrl) {
            state.baseUrl.trim().startsWith("http://", ignoreCase = true)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                if (editingId == null) R.string.server_add else R.string.server_edit
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onEvent(ServerContract.Event.UpdateName(it)) },
                    label = { Text(stringResource(R.string.server_name)) },
                    placeholder = { Text(stringResource(R.string.server_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = { viewModel.onEvent(ServerContract.Event.UpdateBaseUrl(it)) },
                    label = { Text(stringResource(R.string.server_url)) },
                    placeholder = { Text(stringResource(R.string.server_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                if (isHttp) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.server_insecure_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.auth_type),
                    style = MaterialTheme.typography.titleSmall
                )
                AuthTypeOption(
                    selected = state.authType == AuthType.PASSWORD,
                    label = stringResource(R.string.auth_type_password),
                    onClick = {
                        viewModel.onEvent(ServerContract.Event.UpdateAuthType(AuthType.PASSWORD))
                    }
                )
                AuthTypeOption(
                    selected = state.authType == AuthType.GUEST,
                    label = stringResource(R.string.auth_type_guest),
                    onClick = {
                        viewModel.onEvent(ServerContract.Event.UpdateAuthType(AuthType.GUEST))
                    }
                )

                if (state.authType == AuthType.PASSWORD) {
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.onEvent(ServerContract.Event.UpdatePassword(it)) },
                        label = { Text(stringResource(R.string.server_password)) },
                        placeholder = { Text(stringResource(R.string.server_password_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }

                state.testResult?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium)
                }
                state.testError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onEvent(ServerContract.Event.TestConnection) },
                        enabled = !state.isTesting && !state.isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Text(stringResource(R.string.action_test))
                        }
                    }
                    Button(
                        onClick = { viewModel.onEvent(ServerContract.Event.SaveServer) },
                        enabled = !state.isTesting && !state.isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AuthTypeOption(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
