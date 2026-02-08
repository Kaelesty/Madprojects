package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.sprints.ProfileSprint
import domain.sprints.SprintMeta
import domain.sprints.SprintView
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.ProfileCard
import ru.kaelesty.madprojects.ui.fields.AppTextField
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto
import ru.kaelesty.madprojects.utils.nowMillis
import shared_domain.entities.KanbanState

@Composable
fun ProjectSprintsScreen(
    projectId: String,
    modifier: Modifier = Modifier,
) {
    val listVm = koinViewModel<ProjectSprintsViewModel>(
        key = "sprints-$projectId",
        parameters = { parametersOf(projectId) }
    )
    val listState by listVm.state.collectAsState()
    var page by remember { mutableStateOf<SprintsPage>(SprintsPage.List) }

    LaunchedEffect(page) {
        if (page is SprintsPage.List) {
            listVm.load()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val current = page) {
            SprintsPage.List -> SprintsListContent(
                state = listState,
                onCreate = { page = SprintsPage.Create },
                onOpenSprint = { sprintId -> page = SprintsPage.Details(sprintId) },
                onRetry = listVm::load
            )
            is SprintsPage.Details -> ProjectSprintDetailsScreen(
                projectId = projectId,
                sprintId = current.sprintId,
                onBack = { page = SprintsPage.List },
                onEdit = { sprintId, sprint -> page = SprintsPage.Edit(sprintId, sprint) },
                onFinished = { page = SprintsPage.List }
            )
            SprintsPage.Create -> ProjectSprintCreateScreen(
                projectId = projectId,
                onBack = { page = SprintsPage.List },
                onCreated = { page = SprintsPage.List }
            )
            is SprintsPage.Edit -> ProjectSprintEditScreen(
                projectId = projectId,
                sprintId = current.sprintId,
                sprint = current.sprint,
                onBack = { page = SprintsPage.List },
                onSaved = { page = SprintsPage.List }
            )
        }
    }
}

private sealed interface SprintsPage {
    data object List : SprintsPage
    data class Details(val sprintId: String) : SprintsPage
    data object Create : SprintsPage
    data class Edit(val sprintId: String, val sprint: SprintView) : SprintsPage
}

@Composable
private fun SprintsListContent(
    state: ProjectSprintsViewModel.State,
    onCreate: () -> Unit,
    onOpenSprint: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(min = 280.dp, max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(
            title = StringResources.ProjectSprintsTitle,
            actionIcon = Icons.Filled.Add,
            actionDescription = StringResources.ProjectSprintsCreate,
            onActionClick = onCreate
        )

        when (state) {
            ProjectSprintsViewModel.State.Loading -> {
                ProfileCard {
                    RowWithLoader(text = StringResources.ProjectSprintsLoading)
                }
            }
            is ProjectSprintsViewModel.State.Error -> {
                ProfileCard {
                    Text(
                        text = state.message ?: StringResources.ProjectSprintsError,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Roboto,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton(
                        text = StringResources.RetryButton,
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is ProjectSprintsViewModel.State.Loaded -> {
                if (state.sprints.isEmpty()) {
                    ProfileCard {
                        Text(
                            text = StringResources.ProjectSprintsEmpty,
                            color = Palette.FieldLabel,
                            fontFamily = Roboto,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.sprints) { sprint ->
                            SprintCard(sprint = sprint, onClick = { onOpenSprint(sprint.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectSprintDetailsScreen(
    projectId: String,
    sprintId: String,
    onBack: () -> Unit,
    onEdit: (String, SprintView) -> Unit,
    onFinished: () -> Unit,
) {
    val vm = koinViewModel<ProjectSprintDetailsViewModel>(
        key = "sprint-$projectId-$sprintId",
        parameters = { parametersOf(sprintId) }
    )
    val state by vm.state.collectAsState()

    LaunchedEffect(state.finishSuccess) {
        if (state.finishSuccess) {
            vm.consumeFinishSuccess()
            onFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(min = 280.dp, max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val sprint = state.sprint
        SprintHeader(
            title = sprint?.meta?.title?.let { "${StringResources.ProjectSprintDetailsTitle} / $it" }
                ?: StringResources.ProjectSprintDetailsTitle,
            canEdit = sprint?.meta?.actualEndDate == null,
            onBack = onBack,
            onEdit = { sprint?.let { onEdit(sprintId, it) } }
        )

        when {
            state.isLoading && sprint == null -> {
                ProfileCard {
                    RowWithLoader(text = StringResources.ProjectSprintsLoading)
                }
            }
            state.errorMessage != null && sprint == null -> {
                ProfileCard {
                    Text(
                        text = state.errorMessage ?: StringResources.ProjectSprintsError,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Roboto,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryActionButton(
                        text = StringResources.RetryButton,
                        onClick = vm::load,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            sprint != null -> {
                SprintMetaCard(
                    meta = sprint.meta,
                    isFinishing = state.isFinishing,
                    finishError = state.finishError,
                    onFinish = vm::finishSprint
                )
                if (sprint.meta.desc.isNotBlank()) {
                    SprintDescriptionCard(desc = sprint.meta.desc)
                }
                SprintGoalsCard(columns = sprint.kanban.columns)
            }
        }
    }
}

@Composable
private fun ProjectSprintCreateScreen(
    projectId: String,
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    val vm = koinViewModel<ProjectSprintCreateViewModel>(
        key = "sprint-create-$projectId",
        parameters = { parametersOf(projectId) }
    )
    val state by vm.state.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) {
            onCreated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(min = 280.dp, max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SprintHeader(
            title = StringResources.ProjectSprintCreateTitle,
            canEdit = false,
            onBack = onBack,
            onEdit = {}
        )

        SprintFormCard(
            title = state.title,
            desc = state.desc,
            endDate = state.endDate,
            onTitleChange = vm::setTitle,
            onDescChange = vm::setDesc,
            onEndDateChange = vm::setEndDate,
            showEndDate = true
        )

        SprintTasksCard(
            kards = state.kards,
            selectedIds = state.selectedKardIds,
            lockedIds = emptySet(),
            isLoading = state.isLoadingKards,
            onToggle = vm::toggleKard
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage ?: StringResources.ProjectSprintsError,
                color = MaterialTheme.colorScheme.error,
                fontFamily = Roboto,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.isSubmitting) {
            RowWithLoader(text = StringResources.ProjectSprintFinishing)
        }

        PrimaryActionButton(
            text = StringResources.ProjectSprintSubmitCreate,
            onClick = vm::submit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting,
            colors = ButtonDefaults.textButtonColors(contentColor = Palette.AccentBlue)
        ) {
            Text(text = StringResources.ProjectSprintSubmitCancel, fontFamily = Roboto)
        }
    }
}

@Composable
private fun ProjectSprintEditScreen(
    projectId: String,
    sprintId: String,
    sprint: SprintView,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val vm = koinViewModel<ProjectSprintEditViewModel>(
        key = "sprint-edit-$projectId-$sprintId",
        parameters = { parametersOf(projectId, sprintId, sprint) }
    )
    val state by vm.state.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) {
            onSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(min = 280.dp, max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SprintHeader(
            title = StringResources.ProjectSprintEditTitle,
            canEdit = false,
            onBack = onBack,
            onEdit = {}
        )

        SprintFormCard(
            title = state.title,
            desc = state.desc,
            endDate = "",
            onTitleChange = vm::setTitle,
            onDescChange = vm::setDesc,
            onEndDateChange = {},
            showEndDate = false
        )

        SprintTasksCard(
            kards = state.kards,
            selectedIds = state.selectedKardIds,
            lockedIds = state.lockedKardIds,
            isLoading = state.isLoadingKards,
            onToggle = vm::toggleKard
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage ?: StringResources.ProjectSprintsError,
                color = MaterialTheme.colorScheme.error,
                fontFamily = Roboto,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.isSubmitting) {
            RowWithLoader(text = StringResources.ProjectSprintFinishing)
        }

        PrimaryActionButton(
            text = StringResources.ProjectSprintSubmitSave,
            onClick = vm::submit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting,
            colors = ButtonDefaults.textButtonColors(contentColor = Palette.AccentBlue)
        ) {
            Text(text = StringResources.ProjectSprintSubmitCancel, fontFamily = Roboto)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionDescription: String,
    onActionClick: () -> Unit,
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
            text = title,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onActionClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = actionIcon,
                contentDescription = actionDescription,
                tint = Palette.OnCard
            )
        }
    }
}

@Composable
private fun SprintHeader(
    title: String,
    canEdit: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = StringResources.ProjectMessengerChatBack,
                tint = Palette.OnCard
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (canEdit) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = StringResources.ProjectSprintEdit,
                    tint = Palette.OnCard
                )
            }
        }
    }
}

@Composable
private fun SprintCard(
    sprint: ProfileSprint,
    onClick: () -> Unit,
) {
    val status = sprintStatus(sprint)
    ProfileCard(modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text = sprint.title,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = sprint.supposedEndDate,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = status.text,
            color = status.color,
            fontFamily = Roboto,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SprintMetaCard(
    meta: SprintMeta,
    isFinishing: Boolean,
    finishError: String?,
    onFinish: () -> Unit,
) {
    ProfileCard {
        SectionTitle(text = StringResources.ProjectSprintMetaTitle)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "${StringResources.ProjectSprintStartDateLabel}${meta.startDate}",
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${StringResources.ProjectSprintSupposedEndDateLabel}${meta.supposedEndDate}",
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
        meta.actualEndDate?.let { actual ->
            Text(
                text = "${StringResources.ProjectSprintActualEndDateLabel}$actual",
                color = Palette.OnCard,
                fontFamily = Roboto,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (meta.actualEndDate == null) {
            Spacer(Modifier.height(12.dp))
            if (isFinishing) {
                RowWithLoader(text = StringResources.ProjectSprintFinishing)
            } else {
                PrimaryActionButton(
                    text = StringResources.ProjectSprintFinishButton,
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (finishError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = finishError,
                color = MaterialTheme.colorScheme.error,
                fontFamily = Roboto,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SprintDescriptionCard(desc: String) {
    ProfileCard {
        SectionTitle(text = StringResources.ProjectSprintDescriptionTitle)
        Spacer(Modifier.height(12.dp))
        Text(
            text = desc,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SprintGoalsCard(columns: List<KanbanState.Column>) {
    ProfileCard {
        SectionTitle(text = StringResources.ProjectSprintGoalsTitle)
        Spacer(Modifier.height(12.dp))
        if (columns.isEmpty()) {
            Text(
                text = StringResources.ProjectSprintGoalsEmpty,
                color = Palette.FieldLabel,
                fontFamily = Roboto,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                columns.forEach { column ->
                    SprintColumnCard(column = column)
                }
            }
        }
    }
}

@Composable
private fun SprintColumnCard(column: KanbanState.Column) {
    val shape = RoundedCornerShape(12.dp)
    val columnColor = parseHexColor(column.color, Palette.FieldBorder)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Palette.FieldBorder, shape)
            .background(Palette.CardSurface, shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(columnColor)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = column.name,
                color = Palette.OnCard,
                fontFamily = Roboto,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (column.kards.isEmpty()) {
                Text(
                    text = StringResources.ProjectSprintGoalsEmpty,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 12.sp
                )
            } else {
                column.kards.forEach { kard ->
                    Text(
                        text = kard.title,
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
private fun SprintFormCard(
    title: String,
    desc: String,
    endDate: String,
    onTitleChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    showEndDate: Boolean,
) {
    ProfileCard {
        SectionTitle(text = StringResources.ProjectSprintMetaTitle)
        Spacer(Modifier.height(12.dp))
        AppTextField(
            label = StringResources.ProjectSprintFieldTitleLabel,
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = StringResources.ProjectSprintFieldTitlePlaceholder
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            label = StringResources.ProjectSprintFieldDescLabel,
            value = desc,
            onValueChange = onDescChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = StringResources.ProjectSprintFieldDescPlaceholder,
            singleLine = false,
            minLines = 3,
            maxLines = 6
        )
        if (showEndDate) {
            Spacer(Modifier.height(12.dp))
            AppTextField(
                label = StringResources.ProjectSprintFieldEndDateLabel,
                value = endDate,
                onValueChange = onEndDateChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = StringResources.ProjectSprintFieldEndDatePlaceholder
            )
        }
    }
}

@Composable
private fun SprintTasksCard(
    kards: List<KanbanState.Kard>,
    selectedIds: Set<Int>,
    lockedIds: Set<Int>,
    isLoading: Boolean,
    onToggle: (Int) -> Unit,
) {
    ProfileCard {
        SectionTitle(text = StringResources.ProjectSprintFieldTasksLabel)
        Spacer(Modifier.height(12.dp))
        when {
            isLoading -> {
                RowWithLoader(text = StringResources.ProjectSprintTasksLoading)
            }
            kards.isEmpty() -> {
                Text(
                    text = StringResources.ProjectSprintTasksEmpty,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(kards) { kard ->
                        val isLocked = lockedIds.contains(kard.id)
                        val isChecked = selectedIds.contains(kard.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Palette.FieldBg)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggle(kard.id) },
                                enabled = !isLocked,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Palette.AccentBlue,
                                    uncheckedColor = Palette.FieldBorder,
                                    checkmarkColor = Palette.ButtonTextOnPrimary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = kard.title,
                                color = if (isLocked) Palette.FieldLabel else Palette.OnCard,
                                fontFamily = Roboto,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
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
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RowWithLoader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
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

private data class SprintStatusInfo(
    val text: String,
    val color: Color,
)

private fun sprintStatus(sprint: ProfileSprint): SprintStatusInfo {
    val supposed = parseDate(sprint.supposedEndDate)
    val actual = sprint.actualEndDate?.let { parseDate(it) }
    if (supposed == null) {
        return SprintStatusInfo(sprint.supposedEndDate, Palette.FieldLabel)
    }
    return if (actual != null) {
        val overdue = actual.toEpochDays() - supposed.toEpochDays()
        if (overdue > 0) {
            SprintStatusInfo(
                text = "${StringResources.ProjectSprintStatusDoneLatePrefix}${formatDays(overdue)}",
                color = Palette.AccentRed
            )
        } else {
            SprintStatusInfo(
                text = StringResources.ProjectSprintStatusDoneOnTime,
                color = Palette.AccentBlue
            )
        }
    } else {
        val today = today()
        val daysLeft = supposed.toEpochDays() - today.toEpochDays()
        when {
            daysLeft > 0 -> SprintStatusInfo(
                text = "${formatDays(daysLeft)} ${StringResources.ProjectSprintStatusDaysLeftSuffix}",
                color = Palette.FieldLabel
            )
            daysLeft == 0L -> SprintStatusInfo(
                text = StringResources.ProjectSprintStatusDueToday,
                color = Color(0xFFFFC00A)
            )
            else -> SprintStatusInfo(
                text = "${StringResources.ProjectSprintStatusOverduePrefix}${formatDays(-daysLeft)}",
                color = Palette.AccentRed
            )
        }
    }
}

private fun formatDays(days: Long): String {
    val number = kotlin.math.abs(days)
    val n1 = (number % 10).toInt()
    val ending = when {
        number % 100 in 11L..19L -> "дней"
        n1 == 1 -> "день"
        n1 in 2..4 -> "дня"
        else -> "дней"
    }
    return "$number $ending"
}

private fun parseDate(value: String): LocalDate? {
    val parts = value.split(".")
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return runCatching { LocalDate(year, month, day) }.getOrNull()
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun today(): LocalDate {
    return Instant.fromEpochMilliseconds(nowMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
}

private fun parseHexColor(value: String, fallback: Color): Color {
    val cleaned = value.removePrefix("#").trim()
    val parsed = cleaned.toLongOrNull(16) ?: return fallback
    return Color((0xFF000000 or parsed).toInt())
}
