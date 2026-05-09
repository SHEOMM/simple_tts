import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cli.CliEntry
import infra.AppModule
import kotlinx.coroutines.MainScope
import ui.ConvertScreen
import ui.ConvertViewModel

fun main(args: Array<String>) {
    val module = AppModule()
    if (args.isNotEmpty()) {
        CliEntry(module).run(args)
        return
    }
    startGui(module)
}

private fun startGui(module: AppModule) = application {
    val viewModel = remember { ConvertViewModel(module, MainScope()) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Gemini TTS",
        state = rememberWindowState(width = 760.dp, height = 720.dp),
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                ConvertScreen(viewModel)
            }
        }
    }
}
