package com.minprobe.app.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.minprobe.app.mvi.UiEffect
import kotlinx.coroutines.flow.Flow

/** 收集 ViewModel 的一次性副作用，交由调用方处理 */
@Composable
fun <F : UiEffect> HandleEffects(
    effects: Flow<F>,
    onEffect: suspend (F) -> Unit
) {
    LaunchedEffect(effects) {
        effects.collect { onEffect(it) }
    }
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
