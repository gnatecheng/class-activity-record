package com.classrecord.app.ui.classprofile
import androidx.compose.ui.res.stringResource
import com.classrecord.app.R

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classrecord.app.ClassRecordApp
import com.classrecord.app.data.repo.ClassRepository
import com.classrecord.app.di.AppViewModelFactory
import kotlinx.coroutines.launch

class ClassEditViewModel(private val classRepository: ClassRepository) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var loaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            name = classRepository.get()?.name.orEmpty()
            loaded = true
        }
    }

    fun onName(value: String) {
        name = value
    }

    suspend fun save() {
        classRepository.saveName(name)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassEditScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ClassRecordApp
    val vm: ClassEditViewModel = viewModel(factory = AppViewModelFactory(app.container))
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm.loaded) { /* recompose when loaded */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.class_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(20.dp)) {
            OutlinedTextField(
                value = vm.name,
                onValueChange = vm::onName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.class_edit_title)) },
                singleLine = true
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    scope.launch {
                        runCatching { vm.save(); onDone() }
                            .onFailure { snackbar.showSnackbar(context.getString(R.string.err_class_name_required)) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = vm.name.isNotBlank()
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}
