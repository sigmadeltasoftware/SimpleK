package io.github.kankan.demo.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kankan.demo.theme.KankanTheme
import io.github.kankan.demo.theme.LabelPresets
import io.github.simplek.model.CardLabel
import io.github.simplek.model.CardPriority
import io.github.simplek.model.SimpleKColumn
import io.github.simplek.model.SimpleKId

data class NewCardData(
    val title: String,
    val description: String?,
    val priority: CardPriority,
    val labels: List<CardLabel>,
    val columnId: SimpleKId,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCardSheet(
    isVisible: Boolean,
    columns: List<SimpleKColumn<*>>,
    onDismiss: () -> Unit,
    onAddCard: (NewCardData) -> Unit,
) {
    val colors = KankanTheme.colors

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                AddCardContent(
                    columns = columns,
                    onDismiss = onDismiss,
                    onAddCard = onAddCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}, // Consume click to prevent dismiss
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCardContent(
    columns: List<SimpleKColumn<*>>,
    onDismiss: () -> Unit,
    onAddCard: (NewCardData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KankanTheme.colors
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(CardPriority.NONE) }
    val selectedLabels = remember { mutableStateListOf<CardLabel>() }
    var selectedColumnId by remember { mutableStateOf(columns.firstOrNull()?.id) }

    val availableLabels = remember {
        listOf(
            CardLabel(name = "Design", color = LabelPresets.Design),
            CardLabel(name = "Dev", color = LabelPresets.Development),
            CardLabel(name = "Marketing", color = LabelPresets.Marketing),
            CardLabel(name = "QA", color = LabelPresets.QA),
            CardLabel(name = "Bug", color = LabelPresets.Bug),
            CardLabel(name = "Feature", color = LabelPresets.Feature),
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(colors.surface)
            .padding(24.dp)
            .imePadding()
            .verticalScroll(scrollState),
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.border),
        )

        Spacer(Modifier.height(20.dp))

        // Header
        BasicText(
            text = "Add New Card",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            ),
        )

        Spacer(Modifier.height(24.dp))

        // Title input
        InputField(
            label = "Title",
            value = title,
            onValueChange = { title = it },
            placeholder = "Enter card title...",
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))

        // Description input
        InputField(
            label = "Description",
            value = description,
            onValueChange = { description = it },
            placeholder = "Add a description (optional)...",
            singleLine = false,
            minHeight = 80,
        )

        Spacer(Modifier.height(20.dp))

        // Priority selector
        SectionLabel("Priority")
        Spacer(Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardPriority.entries.forEach { p ->
                PriorityChip(
                    priority = p,
                    isSelected = p == priority,
                    onClick = { priority = p },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Labels selector
        SectionLabel("Labels")
        Spacer(Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            availableLabels.forEach { label ->
                LabelChip(
                    label = label,
                    isSelected = selectedLabels.contains(label),
                    onClick = {
                        if (selectedLabels.contains(label)) {
                            selectedLabels.remove(label)
                        } else {
                            selectedLabels.add(label)
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Column selector
        SectionLabel("Column")
        Spacer(Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            columns.forEach { column ->
                ColumnChip(
                    title = column.title,
                    color = column.color,
                    isSelected = column.id == selectedColumnId,
                    onClick = { selectedColumnId = column.id },
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cancel button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Cancel",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                    ),
                )
            }

            // Add button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (title.isNotBlank()) colors.accent else colors.accent.copy(alpha = 0.5f)
                    )
                    .clickable(enabled = title.isNotBlank()) {
                        selectedColumnId?.let { columnId ->
                            onAddCard(
                                NewCardData(
                                    title = title.trim(),
                                    description = description.trim().takeIf { it.isNotEmpty() },
                                    priority = priority,
                                    labels = selectedLabels.toList(),
                                    columnId = columnId,
                                )
                            )
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Add Card",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: Int = 48,
) {
    val colors = KankanTheme.colors

    Column {
        SectionLabel(label)
        Spacer(Modifier.height(8.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = colors.textPrimary,
            ),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(minHeight.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (value.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = colors.textTertiary,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = KankanTheme.colors
    BasicText(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
        ),
    )
}

@Composable
private fun PriorityChip(
    priority: CardPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = KankanTheme.colors

    val priorityColor = when (priority) {
        CardPriority.NONE -> colors.textTertiary
        CardPriority.LOW -> Color(0xFF22C55E)
        CardPriority.MEDIUM -> Color(0xFFF59E0B)
        CardPriority.HIGH -> Color(0xFFEF4444)
        CardPriority.URGENT -> Color(0xFF7C3AED)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) priorityColor.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) priorityColor else colors.border,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (priority != CardPriority.NONE) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(priorityColor, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
        }
        BasicText(
            text = priority.name.lowercase().replaceFirstChar { it.uppercase() },
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) priorityColor else colors.textSecondary,
            ),
        )
    }
}

@Composable
private fun LabelChip(
    label: CardLabel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = KankanTheme.colors

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) label.color.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) label.color else colors.border,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(label.color, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text = label.name,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) label.color else colors.textSecondary,
            ),
        )
    }
}

@Composable
private fun ColumnChip(
    title: String,
    color: Color?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = KankanTheme.colors
    val chipColor = color ?: colors.accent

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) chipColor.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) chipColor else colors.border,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (color != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
        }
        BasicText(
            text = title,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) chipColor else colors.textSecondary,
            ),
        )
    }
}
