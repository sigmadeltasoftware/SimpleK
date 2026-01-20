package io.github.kankan.demo.feature.board

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.simplek.config.BoardTheme
import io.github.simplek.config.SimpleKConfig
import io.github.simplek.config.OverlayTheme
import io.github.kankan.demo.components.BoardStats
import io.github.kankan.demo.components.CardStyle
import io.github.kankan.demo.components.NewCardData
import io.github.kankan.demo.theme.LabelPresets
import io.github.simplek.dsl.ColumnColors
import io.github.simplek.dsl.simpleKBoard
import io.github.simplek.model.CardLabel
import io.github.simplek.model.CardPriority
import io.github.simplek.model.DefaultCard
import io.github.simplek.model.SimpleKBoard
import io.github.simplek.model.SimpleKId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BoardViewModel : ViewModel() {

    private val _state = MutableStateFlow(BoardContract.State())
    val state: StateFlow<BoardContract.State> = _state.asStateFlow()

    private val _sideEffects = MutableSharedFlow<BoardContract.SideEffect>()
    val sideEffects = _sideEffects.asSharedFlow()

    init {
        loadBoard()
    }

    fun loadBoard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val board = createDemoBoard()

            _state.update {
                it.copy(
                    board = board,
                    isLoading = false,
                )
            }
        }
    }

    fun onBoardUpdated(board: SimpleKBoard<DefaultCard>) {
        _state.update { it.copy(board = board) }
    }

    fun toggleTheme() {
        _state.update { current ->
            val newIsDark = !current.isDarkTheme
            current.copy(
                isDarkTheme = newIsDark,
                config = current.config.copy(
                    overlayTheme = if (newIsDark) OverlayTheme.Dark else OverlayTheme.Light,
                    boardTheme = if (newIsDark) BoardTheme.Dark else BoardTheme.Light,
                ),
            )
        }
    }

    fun setCardStyle(style: CardStyle) {
        _state.update { it.copy(cardStyle = style) }
    }

    fun setConfig(config: SimpleKConfig) {
        _state.update { current ->
            current.copy(
                config = config.copy(
                    overlayTheme = if (current.isDarkTheme) OverlayTheme.Dark else OverlayTheme.Light,
                    boardTheme = if (current.isDarkTheme) BoardTheme.Dark else BoardTheme.Light,
                ),
            )
        }
    }

    fun showAddCardSheet() {
        _state.update { it.copy(showAddCardSheet = true) }
    }

    fun hideAddCardSheet() {
        _state.update { it.copy(showAddCardSheet = false) }
    }

    fun toggleSidebar() {
        _state.update { it.copy(isSidebarVisible = !it.isSidebarVisible) }
    }

    fun addCard(data: NewCardData) {
        viewModelScope.launch {
            val currentBoard = state.value.board ?: return@launch

            val newCard = DefaultCard(
                title = data.title,
                description = data.description,
                priority = data.priority,
                labels = data.labels,
            )

            val updatedColumns = currentBoard.columns.map { column ->
                if (column.id == data.columnId) {
                    column.copy(items = column.items + newCard)
                } else {
                    column
                }
            }

            val updatedBoard = currentBoard.copy(columns = updatedColumns)
            _state.update {
                it.copy(
                    board = updatedBoard,
                    showAddCardSheet = false,
                )
            }

            _sideEffects.emit(BoardContract.SideEffect.ShowMessage("Card added: ${data.title}"))
        }
    }

    fun addCard(columnId: SimpleKId, title: String) {
        viewModelScope.launch {
            val currentBoard = state.value.board ?: return@launch

            val newCard = DefaultCard(
                title = title,
                description = "New card created",
            )

            val updatedColumns = currentBoard.columns.map { column ->
                if (column.id == columnId) {
                    column.copy(items = column.items + newCard)
                } else {
                    column
                }
            }

            val updatedBoard = currentBoard.copy(columns = updatedColumns)
            _state.update { it.copy(board = updatedBoard) }

            _sideEffects.emit(BoardContract.SideEffect.ShowMessage("Card added: $title"))
        }
    }

    fun getBoardStats(): BoardStats {
        val board = state.value.board ?: return BoardStats(0, 0, 0, 0)
        val allCards = board.columns.flatMap { it.items }

        return BoardStats(
            totalCards = allCards.size,
            columnsCount = board.columns.size,
            inProgressCount = board.columns
                .find { it.title.contains("Progress", ignoreCase = true) }
                ?.items?.size ?: 0,
            completedCount = board.columns
                .find { it.title.equals("Done", ignoreCase = true) }
                ?.items?.size ?: 0,
        )
    }

    private fun createDemoBoard(): SimpleKBoard<DefaultCard> = simpleKBoard {
        column("Backlog", color = ColumnColors.Todo) {
            card(
                title = "User authentication flow",
                description = "Implement OAuth2 with Google and Apple sign-in",
                priority = CardPriority.MEDIUM,
                imageUrl = "https://picsum.photos/400/200",
            ) {
                feature()
                label("Backend", LabelPresets.Development)
            }

            card(
                title = "Design system documentation",
                description = "Document color tokens, typography, and spacing",
                priority = CardPriority.LOW,
            ) {
                documentation()
                label("Design", LabelPresets.Design)
            }

            card(
                title = "Performance audit",
                description = "Run Lighthouse and optimize bundle size",
                imageUrl = "https://picsum.photos/seed/perf/400/200",
            ) {
                enhancement()
            }

            card(
                title = "Accessibility review",
                description = "WCAG 2.1 AA compliance check",
            )
        }

        column("To Do", color = ColumnColors.Todo, maxItems = 5) {
            card(
                title = "Dashboard widgets API",
                description = "Create REST endpoints for widget data",
                priority = CardPriority.HIGH,
                imageUrl = "https://picsum.photos/seed/dash/400/200",
            ) {
                feature()
                label("API", LabelPresets.Development)
            }

            card(
                title = "Email notification system",
                description = "Set up SendGrid integration",
                priority = CardPriority.MEDIUM,
                imageUrl = "https://picsum.photos/seed/email/400/200",
            ) {
                feature()
            }

            card(
                title = "Dark mode polish",
                description = "Fix contrast issues in navigation",
            ) {
                label("Design", LabelPresets.Design)
                bug()
            }
        }

        column("In Progress", color = ColumnColors.InProgress, maxItems = 3) {
            card(
                title = "Search functionality",
                description = "Implement fuzzy search with Algolia",
                priority = CardPriority.HIGH,
                imageUrl = "https://picsum.photos/seed/search/400/200",
            ) {
                feature()
                urgent()
            }

            card(
                title = "Mobile responsive layout",
                description = "Adapt dashboard for tablet and phone",
                priority = CardPriority.MEDIUM,
                imageUrl = "https://picsum.photos/seed/mobile/400/200",
            ) {
                label("Design", LabelPresets.Design)
                label("Frontend", LabelPresets.Development)
            }
        }

        column("Review", color = ColumnColors.Review, maxItems = 2) {
            card(
                title = "Export to PDF",
                description = "Generate reports as downloadable PDFs",
                priority = CardPriority.LOW,
                imageUrl = "https://picsum.photos/seed/pdf/400/200",
            ) {
                enhancement()
                label("QA", LabelPresets.QA)
            }

            card(
                title = "User settings page",
                description = "Profile settings and preferences UI",
                priority = CardPriority.MEDIUM,
            ) {
                feature()
                label("Frontend", LabelPresets.Development)
            }
        }

        column("Done", color = ColumnColors.Done) {
            card(
                title = "Project scaffolding",
                description = "Set up Compose Multiplatform project",
            ) {
                label("Dev", LabelPresets.Development)
            }

            card(
                title = "CI/CD pipeline",
                description = "GitHub Actions for build and deploy",
                imageUrl = "https://picsum.photos/seed/cicd/400/200",
            ) {
                label("DevOps", LabelPresets.Development)
            }

            card(
                title = "Analytics integration",
                description = "Added Mixpanel tracking events",
            ) {
                label("Marketing", LabelPresets.Marketing)
            }

            card(
                title = "Onboarding flow",
                description = "3-step welcome wizard for new users",
                imageUrl = "https://picsum.photos/seed/onboard/400/200",
            ) {
                feature()
                label("Design", LabelPresets.Design)
            }
        }
    }
}
