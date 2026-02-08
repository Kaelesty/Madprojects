
package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.ProfileCard
import ru.kaelesty.madprojects.ui.fields.AppTextField
import ru.kaelesty.madprojects.ui.menus.AppDropdownMenu
import ru.kaelesty.madprojects.ui.menus.AppDropdownMenuItem
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto
import shared_domain.entities.KanbanState
import kotlin.math.abs

@Composable
fun ProjectKanbanScreen(
    projectId: String,
    onOpenChat: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = koinViewModel<ProjectKanbanViewModel>(parameters = { parametersOf(projectId) })
    val state by vm.state.collectAsState()

    var dragState by remember { mutableStateOf<DragState>(DragState.None) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val columnLayouts = remember { mutableStateMapOf<Int, ColumnLayoutInfo>() }
    val cardLayouts = remember { mutableStateMapOf<Int, CardLayoutInfo>() }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }

    var showCreateColumn by remember { mutableStateOf(false) }
    var editColumn by remember { mutableStateOf<KanbanState.Column?>(null) }
    var recolorColumn by remember { mutableStateOf<KanbanState.Column?>(null) }
    var deleteColumn by remember { mutableStateOf<KanbanState.Column?>(null) }
    var createCardIn by remember { mutableStateOf<KanbanState.Column?>(null) }
    var editCard by remember { mutableStateOf<KanbanState.Kard?>(null) }
    var deleteCard by remember { mutableStateOf<KanbanState.Kard?>(null) }

    LaunchedEffect(vm) {
        vm.events.collectLatest { event ->
            when (event) {
                is ProjectKanbanViewModel.Event.OpenChat -> onOpenChat(event.chatId)
            }
        }
    }

    val handleDrop = {
        val active = dragState
        if (active != DragState.None) {
            val dropPosition = when (active) {
                is DragState.Card -> active.bounds.center + dragOffset
                is DragState.Column -> active.bounds.center + dragOffset
                DragState.None -> Offset.Zero
            }
            when (active) {
                is DragState.Card -> {
                    val targetColumn = findTargetColumn(
                        dropPosition,
                        columnLayouts.values,
                        active.columnId
                    )
                    val targetColumnId = targetColumn?.columnId ?: active.columnId
                    val targetCards = cardLayouts.values
                        .filter { it.columnId == targetColumnId && it.cardId != active.cardId }
                        .sortedBy { it.bounds.top }
                    val newIndex = findTargetCardIndex(dropPosition, targetCards)
                    val shouldMove = targetColumnId != active.columnId || newIndex != active.index
                    if (shouldMove) {
                        vm.moveKard(
                            id = active.cardId,
                            columnId = active.columnId,
                            newColumnId = targetColumnId,
                            newPosition = newIndex
                        )
                    }
                }
                is DragState.Column -> {
                    val targetColumns = columnLayouts.values.sortedBy { it.bounds.top }
                    val newIndex = findTargetColumnIndex(dropPosition, targetColumns)
                    if (newIndex != active.index) {
                        vm.moveColumn(active.columnId, newIndex)
                    }
                }
                DragState.None -> Unit
            }
        }
        dragState = DragState.None
        dragOffset = Offset.Zero
    }

    val handleDragCancel = {
        dragState = DragState.None
        dragOffset = Offset.Zero
    }

    val activeCard = dragState as? DragState.Card
    val hoveredColumnId = activeCard?.let { card ->
        findTargetColumn(card.bounds.center + dragOffset, columnLayouts.values, card.columnId)?.columnId
    }
    val draggedCard = activeCard?.let { card -> findCard(state.kanban, card.cardId) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .onGloballyPositioned { coordinates -> rootBounds = coordinates.boundsInWindow() },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionTitle(
                text = StringResources.ProjectKanbanTitle,
                action = {
                    IconButton(
                        onClick = { showCreateColumn = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = StringResources.ProjectKanbanAddColumn,
                            tint = Palette.OnCard
                        )
                    }
                }
            )

            if (state.errorMessage != null && state.kanban != null) {
                Text(
                    text = state.errorMessage ?: StringResources.ProjectKanbanError,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = Roboto,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when {
                state.isLoading && state.kanban == null -> {
                    ProfileCard {
                        RowWithLoader(text = StringResources.ProjectKanbanLoading)
                    }
                }
                state.errorMessage != null && state.kanban == null -> {
                    ProfileCard {
                        Text(
                            text = state.errorMessage ?: StringResources.ProjectKanbanError,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = Roboto,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {
                    val columns = state.kanban?.columns.orEmpty()
                    if (columns.isEmpty()) {
                        ProfileCard {
                            Text(
                                text = StringResources.ProjectKanbanEmpty,
                                color = Palette.FieldLabel,
                                fontFamily = Roboto,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            itemsIndexed(columns, key = { _, column -> column.id }) { index, column ->
                                KanbanColumnCard(
                                    column = column,
                                    index = index,
                                    isHighlighted = hoveredColumnId == column.id,
                                    dragState = dragState,
                                    dragOffset = dragOffset,
                                    isCreatingChat = state.isCreatingChat,
                                    creatingChatForId = state.creatingChatForId,
                                    onStartDragColumn = { layout ->
                                        if (dragState == DragState.None) {
                                            dragState = DragState.Column(layout.columnId, layout.index, layout.bounds)
                                            dragOffset = Offset.Zero
                                        }
                                    },
                                    onStartDragCard = { layout ->
                                        if (dragState == DragState.None) {
                                            dragState = DragState.Card(
                                                layout.cardId,
                                                layout.columnId,
                                                layout.index,
                                                layout.bounds
                                            )
                                            dragOffset = Offset.Zero
                                        }
                                    },
                                    onDrag = { amount ->
                                        dragOffset += amount
                                    },
                                    onDrop = handleDrop,
                                    onDragCancel = handleDragCancel,
                                    registerColumnLayout = { layout ->
                                        if (dragState !is DragState.Column || (dragState as DragState.Column).columnId != layout.columnId) {
                                            columnLayouts[layout.columnId] = layout
                                        }
                                    },
                                    registerCardLayout = { layout ->
                                        if (dragState !is DragState.Card || (dragState as DragState.Card).cardId != layout.cardId) {
                                            cardLayouts[layout.cardId] = layout
                                        }
                                    },
                                    onAddCard = { createCardIn = it },
                                    onEditColumn = { editColumn = it },
                                    onDeleteColumn = { deleteColumn = it },
                                    onRecolorColumn = { recolorColumn = it },
                                    onEditCard = { editCard = it },
                                    onDeleteCard = { deleteCard = it },
                                    onCreateChat = vm::createChatForKard,
                                    onOpenChat = onOpenChat,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (activeCard != null && draggedCard != null && rootBounds != null) {
            DraggedCardOverlay(
                card = draggedCard,
                bounds = activeCard.bounds,
                offset = dragOffset,
                rootBounds = rootBounds
            )
        }
    }

    if (showCreateColumn) {
        CreateColumnDialog(
            onDismiss = { showCreateColumn = false },
            onConfirm = { name, color ->
                showCreateColumn = false
                vm.createColumn(name, color)
            }
        )
    }
    editColumn?.let { column ->
        EditNameDialog(
            title = StringResources.ProjectKanbanEditColumn,
            label = StringResources.ProjectKanbanColumnNameLabel,
            placeholder = StringResources.ProjectKanbanColumnNamePlaceholder,
            initialValue = column.name,
            onDismiss = { editColumn = null },
            onConfirm = { name ->
                editColumn = null
                vm.updateColumn(column.id, name, null)
            }
        )
    }
    recolorColumn?.let { column ->
        RecolorDialog(
            onDismiss = { recolorColumn = null },
            onConfirm = { color ->
                recolorColumn = null
                vm.updateColumn(column.id, null, color)
            }
        )
    }
    deleteColumn?.let { column ->
        ConfirmDialog(
            title = StringResources.ProjectKanbanDeleteColumn,
            message = "${StringResources.ProjectKanbanDeleteColumnConfirm} \"${column.name}\"?",
            onDismiss = { deleteColumn = null },
            onConfirm = {
                deleteColumn = null
                vm.deleteColumn(column.id)
            }
        )
    }
    createCardIn?.let { column ->
        EditNameDialog(
            title = StringResources.ProjectKanbanAddCard,
            label = StringResources.ProjectKanbanCardNameLabel,
            placeholder = StringResources.ProjectKanbanCardNamePlaceholder,
            initialValue = "",
            onDismiss = { createCardIn = null },
            onConfirm = { name ->
                createCardIn = null
                vm.createKard(name, column.id)
            }
        )
    }
    editCard?.let { card ->
        EditNameDialog(
            title = StringResources.ProjectKanbanEditCard,
            label = StringResources.ProjectKanbanCardNameLabel,
            placeholder = StringResources.ProjectKanbanCardNamePlaceholder,
            initialValue = card.title,
            onDismiss = { editCard = null },
            onConfirm = { name ->
                editCard = null
                vm.updateKard(card.id, name)
            }
        )
    }
    deleteCard?.let { card ->
        ConfirmDialog(
            title = StringResources.ProjectKanbanDeleteCard,
            message = "${StringResources.ProjectKanbanDeleteCardConfirm} \"${card.title}\"?",
            onDismiss = { deleteCard = null },
            onConfirm = {
                deleteCard = null
                vm.deleteKard(card.id)
            }
        )
    }
}

@Composable
private fun KanbanColumnCard(
    column: KanbanState.Column,
    index: Int,
    isHighlighted: Boolean,
    dragState: DragState,
    dragOffset: Offset,
    isCreatingChat: Boolean,
    creatingChatForId: Int?,
    onStartDragColumn: (ColumnLayoutInfo) -> Unit,
    onStartDragCard: (CardLayoutInfo) -> Unit,
    onDrag: (Offset) -> Unit,
    onDrop: () -> Unit,
    onDragCancel: () -> Unit,
    registerColumnLayout: (ColumnLayoutInfo) -> Unit,
    registerCardLayout: (CardLayoutInfo) -> Unit,
    onAddCard: (KanbanState.Column) -> Unit,
    onEditColumn: (KanbanState.Column) -> Unit,
    onDeleteColumn: (KanbanState.Column) -> Unit,
    onRecolorColumn: (KanbanState.Column) -> Unit,
    onEditCard: (KanbanState.Kard) -> Unit,
    onDeleteCard: (KanbanState.Kard) -> Unit,
    onCreateChat: (Int) -> Unit,
    onOpenChat: (Int) -> Unit,
) {
    val columnColor = parseHexColor(column.color, Palette.AccentBlue)
    val isDragged = dragState is DragState.Column && dragState.columnId == column.id
    val dragTranslation = if (isDragged) dragOffset else Offset.Zero
    val elevation = if (isDragged) 10.dp else 4.dp
    val shape = RoundedCornerShape(16.dp)
    var columnBounds by remember(column.id) { mutableStateOf<Rect?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                translationX = dragTranslation.x
                translationY = dragTranslation.y
            }
            .border(
                width = if (isHighlighted) 2.dp else 0.dp,
                color = if (isHighlighted) Palette.AccentBlue else Color.Transparent,
                shape = shape
            )
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                columnBounds = bounds
                registerColumnLayout(
                    ColumnLayoutInfo(
                        columnId = column.id,
                        index = index,
                        bounds = bounds
                    )
                )
            },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Palette.CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(columnColor)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ColumnHeader(
                    column = column,
                    color = columnColor,
                    columnBounds = columnBounds,
                    onEdit = { onEditColumn(column) },
                    onDelete = { onDeleteColumn(column) },
                    onRecolor = { onRecolorColumn(column) },
                    onStartDrag = { bounds ->
                        onStartDragColumn(
                            ColumnLayoutInfo(
                                columnId = column.id,
                                index = index,
                                bounds = bounds
                            )
                        )
                    },
                    onDrag = onDrag,
                    onDrop = onDrop,
                    onDragCancel = onDragCancel
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    column.kards.forEachIndexed { cardIndex, card ->
                        val isCardDragged = dragState is DragState.Card && dragState.cardId == card.id
                        val cardTranslation = Offset.Zero
                        KanbanCardItem(
                            card = card,
                            isDragged = isCardDragged,
                            translation = cardTranslation,
                            isCreatingChat = isCreatingChat && creatingChatForId == card.id,
                            onStartDrag = { bounds ->
                                onStartDragCard(
                                    CardLayoutInfo(
                                        cardId = card.id,
                                        columnId = column.id,
                                        index = cardIndex,
                                        bounds = bounds
                                    )
                                )
                            },
                            onDrag = onDrag,
                            onDrop = onDrop,
                            onDragCancel = onDragCancel,
                            registerLayout = { bounds ->
                                registerCardLayout(
                                    CardLayoutInfo(
                                        cardId = card.id,
                                        columnId = column.id,
                                        index = cardIndex,
                                        bounds = bounds
                                    )
                                )
                            },
                            onEdit = { onEditCard(card) },
                            onDelete = { onDeleteCard(card) },
                            onCreateChat = { onCreateChat(card.id) },
                            onOpenChat = onOpenChat
                        )
                    }
                    AddCardButton(onClick = { onAddCard(column) })
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader(
    column: KanbanState.Column,
    color: Color,
    columnBounds: Rect?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRecolor: () -> Unit,
    onStartDrag: (Rect) -> Unit,
    onDrag: (Offset) -> Unit,
    onDrop: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var menuExpanded by remember(column.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = column.name,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .pointerInput(column.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            columnBounds?.let { onStartDrag(it) }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount)
                        },
                        onDragEnd = onDrop,
                        onDragCancel = onDragCancel
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DragIndicator,
                contentDescription = null,
                tint = Palette.FieldLabel,
                modifier = Modifier.size(18.dp)
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = Palette.OnCard
                )
            }
            AppDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                items = listOf(
                    AppDropdownMenuItem(StringResources.ProjectKanbanEditColumn) {
                        menuExpanded = false
                        onEdit()
                    },
                    AppDropdownMenuItem(StringResources.ProjectKanbanRecolorColumn) {
                        menuExpanded = false
                        onRecolor()
                    },
                    AppDropdownMenuItem(StringResources.ProjectKanbanDeleteColumn) {
                        menuExpanded = false
                        onDelete()
                    }
                )
            )
        }
    }
}

@Composable
private fun KanbanCardItem(
    card: KanbanState.Kard,
    isDragged: Boolean,
    translation: Offset,
    isCreatingChat: Boolean,
    onStartDrag: (Rect) -> Unit,
    onDrag: (Offset) -> Unit,
    onDrop: () -> Unit,
    onDragCancel: () -> Unit,
    registerLayout: (Rect) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCreateChat: () -> Unit,
    onOpenChat: (Int) -> Unit,
) {
    var menuExpanded by remember(card.id) { mutableStateOf(false) }
    var cardBounds by remember(card.id) { mutableStateOf<Rect?>(null) }
    val shape = RoundedCornerShape(14.dp)
    val chatId = card.chatId?.toIntOrNull()
    val unreadCount = card.unreadMessage ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                translationX = translation.x
                translationY = translation.y
                alpha = if (isDragged) 0f else 1f
            }
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                cardBounds = bounds
                registerLayout(bounds)
            }
            .pointerInput(card.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        cardBounds?.let { onStartDrag(it) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = onDrop,
                    onDragCancel = onDragCancel
                )
            }
            .background(Palette.Background, shape)
            .border(1.dp, Palette.FieldBorder, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = card.title,
                color = Palette.OnCard,
                fontFamily = Roboto,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = Palette.FieldLabel
                    )
                }
                AppDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    items = listOf(
                        AppDropdownMenuItem(StringResources.ProjectKanbanEditCard) {
                            menuExpanded = false
                            onEdit()
                        },
                        AppDropdownMenuItem(StringResources.ProjectKanbanDeleteCard) {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                isCreatingChat -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Palette.AccentBlue,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = StringResources.ProjectKanbanChatCreating,
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 12.sp
                    )
                }
                chatId != null -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenChat(chatId) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Chat,
                            contentDescription = null,
                            tint = Palette.AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = StringResources.ProjectKanbanChatOpen,
                            color = Palette.AccentBlue,
                            fontFamily = Roboto,
                            fontSize = 12.sp
                        )
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            UnreadBadge(count = unreadCount)
                        }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCreateChat() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Palette.FieldLabel,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = StringResources.ProjectKanbanChatCreate,
                            color = Palette.FieldLabel,
                            fontFamily = Roboto,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCardButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(Palette.FieldBg)
            .border(1.dp, Palette.FieldBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = Palette.FieldLabel,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = StringResources.ProjectKanbanAddCard,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Palette.AccentBlue)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            action()
        }
    }
}

@Composable
private fun RowWithLoader(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = Palette.AccentBlue,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun CreateColumnDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(STANDARD_COLORS.first()) }
    var customColor by remember { mutableStateOf("#FFAAFF") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 420.dp),
            shape = RoundedCornerShape(16.dp),
            color = Palette.CardSurface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = StringResources.ProjectKanbanAddColumn,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                AppTextField(
                    label = StringResources.ProjectKanbanColumnNameLabel,
                    value = name,
                    onValueChange = { name = it },
                    placeholder = StringResources.ProjectKanbanColumnNamePlaceholder
                )
                ColorPicker(
                    selectedColor = selectedColor,
                    customColor = customColor,
                    onColorSelected = { selectedColor = it },
                    onCustomColorChange = {
                        customColor = it
                        selectedColor = it
                    }
                )
                PrimaryActionButton(
                    text = StringResources.ProjectKanbanConfirm,
                    onClick = { onConfirm(name.trim(), normalizeColor(selectedColor)) },
                    enabled = name.isNotBlank()
                )
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(text = StringResources.ProjectKanbanCancel, fontFamily = Roboto)
                }
            }
        }
    }
}

@Composable
private fun EditNameDialog(
    title: String,
    label: String,
    placeholder: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 420.dp),
            shape = RoundedCornerShape(16.dp),
            color = Palette.CardSurface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                AppTextField(
                    label = label,
                    value = value,
                    onValueChange = { value = it },
                    placeholder = placeholder
                )
                PrimaryActionButton(
                    text = StringResources.ProjectKanbanConfirm,
                    onClick = { onConfirm(value.trim()) },
                    enabled = value.isNotBlank()
                )
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(text = StringResources.ProjectKanbanCancel, fontFamily = Roboto)
                }
            }
        }
    }
}

@Composable
private fun RecolorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selectedColor by remember { mutableStateOf(STANDARD_COLORS.first()) }
    var customColor by remember { mutableStateOf("#FFAAFF") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 420.dp),
            shape = RoundedCornerShape(16.dp),
            color = Palette.CardSurface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = StringResources.ProjectKanbanRecolorColumn,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                ColorPicker(
                    selectedColor = selectedColor,
                    customColor = customColor,
                    onColorSelected = { selectedColor = it },
                    onCustomColorChange = {
                        customColor = it
                        selectedColor = it
                    }
                )
                PrimaryActionButton(
                    text = StringResources.ProjectKanbanConfirm,
                    onClick = { onConfirm(normalizeColor(selectedColor)) },
                    enabled = selectedColor.isNotBlank()
                )
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(text = StringResources.ProjectKanbanCancel, fontFamily = Roboto)
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 420.dp),
            shape = RoundedCornerShape(16.dp),
            color = Palette.CardSurface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = message,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 14.sp
                )
                PrimaryActionButton(
                    text = StringResources.ProjectKanbanConfirm,
                    onClick = onConfirm
                )
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(text = StringResources.ProjectKanbanCancel, fontFamily = Roboto)
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(
    selectedColor: String,
    customColor: String,
    onColorSelected: (String) -> Unit,
    onCustomColorChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = StringResources.ProjectKanbanColorLabel,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            STANDARD_COLORS.forEach { color ->
                val isSelected = normalizeColor(color) == normalizeColor(selectedColor)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(normalizeColor(color), Palette.AccentBlue))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Palette.AccentBlue else Palette.FieldBorder,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
        AppTextField(
            label = StringResources.ProjectKanbanColorCustomLabel,
            value = customColor,
            onValueChange = onCustomColorChange,
            placeholder = StringResources.ProjectKanbanColorPlaceholder
        )
    }
}

@Composable
private fun DraggedCardOverlay(
    card: KanbanState.Kard,
    bounds: Rect,
    offset: Offset,
    rootBounds: Rect?,
) {
    val density = LocalDensity.current
    val width = with(density) { bounds.width.toDp() }
    val height = with(density) { bounds.height.toDp() }
    val topLeft = Offset(bounds.left, bounds.top) + offset
    val rootTopLeft = rootBounds?.let { Offset(it.left, it.top) } ?: Offset.Zero
    val localOffset = topLeft - rootTopLeft

    Box(
        modifier = Modifier
            .size(width, height)
            .graphicsLayer {
                translationX = localOffset.x
                translationY = localOffset.y
                shadowElevation = 16f
                scaleX = 1.02f
                scaleY = 1.02f
            }
            .zIndex(10f)
    ) {
        DraggedCardContent(card = card)
    }
}

@Composable
private fun DraggedCardContent(card: KanbanState.Kard) {
    val shape = RoundedCornerShape(14.dp)
    val chatId = card.chatId?.toIntOrNull()
    val unreadCount = card.unreadMessage ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.Background, shape)
            .border(1.dp, Palette.FieldBorder, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = card.title,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        when {
            chatId != null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Chat,
                        contentDescription = null,
                        tint = Palette.AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = StringResources.ProjectKanbanChatOpen,
                        color = Palette.AccentBlue,
                        fontFamily = Roboto,
                        fontSize = 12.sp
                    )
                    if (unreadCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        UnreadBadge(count = unreadCount)
                    }
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Palette.FieldLabel,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = StringResources.ProjectKanbanChatCreate,
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    val label = if (count > 9) "9+" else count.toString()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.AccentBlue)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Palette.ButtonTextOnPrimary,
            fontFamily = Roboto,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun parseHexColor(raw: String, fallback: Color): Color {
    val cleaned = raw.trim().removePrefix("#")
    return runCatching {
        val value = cleaned.toLong(16)
        val hasAlpha = cleaned.length == 8
        val alpha = if (hasAlpha) ((value shr 24) and 0xFF) else 0xFF
        val red = (value shr 16) and 0xFF
        val green = (value shr 8) and 0xFF
        val blue = value and 0xFF
        Color(
            red = (red / 255f),
            green = (green / 255f),
            blue = (blue / 255f),
            alpha = (alpha / 255f)
        )
    }.getOrDefault(fallback)
}

private fun normalizeColor(value: String): String = value.trim().removePrefix("#")

private fun findCard(kanban: KanbanState?, cardId: Int): KanbanState.Kard? {
    return kanban?.columns
        ?.asSequence()
        ?.flatMap { it.kards.asSequence() }
        ?.firstOrNull { it.id == cardId }
}

private fun findTargetColumn(
    position: Offset,
    columns: Collection<ColumnLayoutInfo>,
    fallbackColumnId: Int,
): ColumnLayoutInfo? {
    var inside: ColumnLayoutInfo? = null
    for (column in columns) {
        val bounds = column.bounds
        if (position.y in bounds.top..bounds.bottom) {
            inside = column
            break
        }
    }
    if (inside != null) return inside
    var nearest: ColumnLayoutInfo? = null
    var minDistance = Float.MAX_VALUE
    for (column in columns) {
        val distance = abs(position.y - column.bounds.center.y)
        if (distance < minDistance) {
            minDistance = distance
            nearest = column
        }
    }
    return nearest ?: columns.firstOrNull { it.columnId == fallbackColumnId }
}

private fun findTargetColumnIndex(
    position: Offset,
    columns: List<ColumnLayoutInfo>,
): Int {
    for (index in columns.indices) {
        val bounds = columns[index].bounds
        if (position.y < bounds.center.y) {
            return index
        }
    }
    return columns.size
}

private fun findTargetCardIndex(
    position: Offset,
    cards: List<CardLayoutInfo>,
): Int {
    for (index in cards.indices) {
        val bounds = cards[index].bounds
        if (position.y < bounds.center.y) {
            return index
        }
    }
    return cards.size
}

private sealed interface DragState {
    data object None : DragState
    data class Column(val columnId: Int, val index: Int, val bounds: Rect) : DragState
    data class Card(val cardId: Int, val columnId: Int, val index: Int, val bounds: Rect) : DragState
}

private data class ColumnLayoutInfo(
    val columnId: Int,
    val index: Int,
    val bounds: Rect,
)

private data class CardLayoutInfo(
    val cardId: Int,
    val columnId: Int,
    val index: Int,
    val bounds: Rect,
)

private val STANDARD_COLORS = listOf("#FFFF78", "#7878FF", "#78F1FF", "#78FF78")
