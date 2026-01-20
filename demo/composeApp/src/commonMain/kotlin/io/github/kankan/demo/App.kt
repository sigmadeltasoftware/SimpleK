package io.github.kankan.demo

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.setSingletonImageLoaderFactory
import io.github.kankan.demo.appnav.AppScreen
import io.github.kankan.demo.feature.board.ui.BoardScreen
import io.github.kankan.demo.feature.board.BoardViewModel
import io.github.kankan.demo.theme.KankanTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun App() {
    // Configure Coil for network image loading (platform-specific)
    setSingletonImageLoaderFactory { context ->
        createImageLoader(context)
    }

    val navController: NavHostController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Get ViewModel early to access theme state (using koinInject for iOS compatibility)
    val viewModel: BoardViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    KankanTheme(isDarkTheme = state.isDarkTheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Let content draw edge-to-edge
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { _ ->
            // Ignore padding values - we handle insets manually in BoardScreen
            NavHost(
                navController = navController,
                startDestination = AppScreen.Board,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable<AppScreen.Board> {
                    BoardScreen(
                        viewModel = viewModel,
                        onShowSnackbar = showSnackbar,
                    )
                }
            }
        }
    }
}
