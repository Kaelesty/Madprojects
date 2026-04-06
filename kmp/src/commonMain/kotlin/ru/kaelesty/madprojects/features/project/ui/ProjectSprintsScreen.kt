package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.CommiterModel
import domain.activity.Activity
import domain.activity.ActivityType
import domain.profile.SharedProfile
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
    val activityState by listVm.activityState.collectAsState()
    val analyticsState by listVm.analyticsState.collectAsState()
    val uriHandler = LocalUriHandler.current
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
                analyticsState = analyticsState,
                activityState = activityState,
                onCreate = { page = SprintsPage.Create },
                onOpenSprint = { sprintId -> page = SprintsPage.Details(sprintId) },
                onOpenRepo = uriHandler::openUri,
                onRetry = listVm::load,
                onRetryAnalytics = listVm::reloadAnalytics,
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
    analyticsState: ProjectSprintsViewModel.AnalyticsState,
    activityState: ProjectSprintsViewModel.ActivityState,
    onCreate: () -> Unit,
    onOpenSprint: (String) -> Unit,
    onOpenRepo: (String) -> Unit,
    onRetry: () -> Unit,
    onRetryAnalytics: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(min = 280.dp, max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            AnalyticsCommitsCard(
                state = analyticsState,
                onRetry = onRetryAnalytics,
            )
        }
        item {
            ActivityFeedCard(
                state = activityState,
                onRetry = onRetry,
                onOpenSprint = onOpenSprint,
                onOpenRepo = onOpenRepo
            )
        }
        item {
            SectionHeader(
                title = StringResources.ProjectSprintsTitle,
                actionIcon = Icons.Filled.Add,
                actionDescription = StringResources.ProjectSprintsCreate,
                onActionClick = onCreate
            )
        }

        when (state) {
            ProjectSprintsViewModel.State.Loading -> {
                item {
                    ProfileCard {
                        RowWithLoader(text = StringResources.ProjectSprintsLoading)
                    }
                }
            }
            is ProjectSprintsViewModel.State.Error -> {
                item {
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
            }
            is ProjectSprintsViewModel.State.Loaded -> {
                if (state.sprints.isEmpty()) {
                    item {
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
                    }
                } else {
                    items(state.sprints) { sprint ->
                        SprintCard(sprint = sprint, onClick = { onOpenSprint(sprint.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsCommitsCard(
    state: ProjectSprintsViewModel.AnalyticsState,
    onRetry: () -> Unit,
) {
    ProfileCard {
        SectionTitle(text = "Аналитика")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Распределение коммитов по участникам",
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        when (state) {
            ProjectSprintsViewModel.AnalyticsState.Loading -> {
                RowWithLoader(text = "Загрузка аналитики...")
            }
            is ProjectSprintsViewModel.AnalyticsState.Error -> {
                Text(
                    text = state.message ?: "Ошибка загрузки аналитики",
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = Roboto,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                PrimaryActionButton(
                    text = StringResources.RetryButton,
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is ProjectSprintsViewModel.AnalyticsState.Loaded -> {
                val commiters = state.commiters
                    .filter { it.commitsCount > 0 }
                    .sortedByDescending { it.commitsCount }
                if (commiters.isEmpty()) {
                    Text(
                        text = "Пока нет данных по коммитам",
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CommitAnalyticsContent(commiters = commiters)
                }
            }
        }
    }
}

@Composable
private fun CommitAnalyticsContent(commiters: List<CommiterModel>) {
    val totalCommits = commiters.sumOf { it.commitsCount }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(152.dp),
            contentAlignment = Alignment.Center
        ) {
            CommitAnalyticsDonut(
                commiters = commiters,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Palette.CardSurface)
                    .border(
                        width = 1.dp,
                        color = Palette.FieldBorder.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalCommits.toString(),
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "коммитов",
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commiters.forEachIndexed { index, commiter ->
                CommitAnalyticsLegendRow(
                    commiter = commiter,
                    color = analyticsChartColors[index % analyticsChartColors.size],
                    totalCommits = totalCommits,
                )
            }
        }
    }
}

@Composable
private fun CommitAnalyticsDonut(
    commiters: List<CommiterModel>,
    modifier: Modifier = Modifier,
) {
    val totalCommits = commiters.sumOf { it.commitsCount }.coerceAtLeast(1)
    Canvas(modifier = modifier) {
        val ringWidth = 16.dp.toPx()
        val diameter = size.minDimension - ringWidth
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        val arcSize = Size(width = diameter, height = diameter)

        drawArc(
            color = Palette.FieldBorder.copy(alpha = 0.28f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = ringWidth)
        )

        var startAngle = -90f
        commiters.forEachIndexed { index, commiter ->
            val sweepAngle = if (commiters.size == 1) {
                360f
            } else {
                (commiter.commitsCount.toFloat() / totalCommits.toFloat()) * 360f
            }
            drawArc(
                color = analyticsChartColors[index % analyticsChartColors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = ringWidth, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CommitAnalyticsLegendRow(
    commiter: CommiterModel,
    color: Color,
    totalCommits: Int,
) {
    val percent = ((commiter.commitsCount * 100f) / totalCommits.coerceAtLeast(1)).toInt()
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
        Spacer(Modifier.width(10.dp))
        Text(
            text = commiter.fullName.ifBlank { "Без имени" },
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${commiter.commitsCount} (${percent}%)",
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 12.sp,
        )
    }
}

private val analyticsChartColors = listOf(
    Color(0xFF3B82F6),
    Color(0xFF06B6D4),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFF14B8A6),
)

@Composable
private fun ActivityFeedCard(
    state: ProjectSprintsViewModel.ActivityState,
    onRetry: () -> Unit,
    onOpenSprint: (String) -> Unit,
    onOpenRepo: (String) -> Unit,
) {
    ProfileCard {
        SectionTitle(text = StringResources.ProjectActivityFeedTitle)
        Spacer(Modifier.height(12.dp))
        when (state) {
            ProjectSprintsViewModel.ActivityState.Loading -> {
                RowWithLoader(text = StringResources.ProjectActivityFeedLoading)
            }
            is ProjectSprintsViewModel.ActivityState.Error -> {
                Text(
                    text = state.message ?: StringResources.ProjectActivityFeedError,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = Roboto,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                PrimaryActionButton(
                    text = StringResources.RetryButton,
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is ProjectSprintsViewModel.ActivityState.Loaded -> {
                val activities = state.response.activities.asReversed()
                if (activities.isEmpty()) {
                    Text(
                        text = StringResources.ProjectActivityFeedEmpty,
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        activities.forEachIndexed { index, activity ->
                            val actor = activity.actorId?.let(state.response.actors::get)
                            val formatted = formatProjectActivity(activity = activity, actor = actor)
                            ActivityFeedRow(
                                formatted = formatted,
                                onOpenSprint = onOpenSprint,
                                onOpenRepo = onOpenRepo,
                            )
                            if (index < activities.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Palette.FieldBorder.copy(alpha = 0.55f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class FormattedProjectActivity(
    val timestamp: String,
    val message: String,
    val sprintId: String? = null,
    val repoUrl: String? = null,
)

@Composable
private fun ActivityFeedRow(
    formatted: FormattedProjectActivity,
    onOpenSprint: (String) -> Unit,
    onOpenRepo: (String) -> Unit,
) {
    val clickAction = when {
        formatted.sprintId != null -> ({ onOpenSprint(formatted.sprintId) })
        formatted.repoUrl != null -> ({ onOpenRepo(formatted.repoUrl) })
        else -> null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickAction != null) Modifier.clickable(onClick = clickAction) else Modifier)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = formatted.timestamp,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 11.sp
        )
        Text(
            text = formatted.message,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            modifier = Modifier.fillMaxWidth()
        )
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
    var showEndDatePicker by remember { mutableStateOf(false) }

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
            showEndDate = true,
            onOpenEndDatePicker = { showEndDatePicker = true }
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

    if (showEndDatePicker) {
        SprintEndDatePickerDialog(
            currentValue = state.endDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { value ->
                vm.setEndDate(value)
                showEndDatePicker = false
            }
        )
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
    onOpenEndDatePicker: (() -> Unit)? = null,
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
                placeholder = StringResources.ProjectSprintFieldEndDatePlaceholder,
                readOnly = onOpenEndDatePicker != null,
                onClick = onOpenEndDatePicker,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SprintEndDatePickerDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initialDateMillis = remember(currentValue) {
        parseDate(currentValue)?.let(::localDateToPickerUtcMillis)
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )
    val pickerColorScheme = MaterialTheme.colorScheme.copy(
        primary = Palette.AccentBlue,
        onPrimary = Palette.ButtonTextOnPrimary,
        surface = Palette.CardSurface,
        onSurface = Palette.OnCard,
        surfaceVariant = Palette.FieldBg,
        onSurfaceVariant = Palette.FieldLabel,
        outline = Palette.FieldBorder,
    )

    MaterialTheme(colorScheme = pickerColorScheme) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            colors = DatePickerDefaults.colors(
                containerColor = Palette.CardSurface,
                titleContentColor = Palette.OnCard,
                headlineContentColor = Palette.OnCard,
                weekdayContentColor = Palette.FieldLabel,
                subheadContentColor = Palette.FieldLabel,
                yearContentColor = Palette.FieldLabel,
                currentYearContentColor = Palette.AccentBlue,
                selectedYearContentColor = Palette.ButtonTextOnPrimary,
                selectedYearContainerColor = Palette.AccentBlue,
                dayContentColor = Palette.OnCard,
                disabledDayContentColor = Palette.FieldLabel.copy(alpha = 0.4f),
                selectedDayContentColor = Palette.ButtonTextOnPrimary,
                disabledSelectedDayContentColor = Palette.ButtonTextOnPrimary.copy(alpha = 0.5f),
                selectedDayContainerColor = Palette.AccentBlue,
                todayContentColor = Palette.AccentBlue,
                todayDateBorderColor = Palette.AccentBlue,
                dayInSelectionRangeContentColor = Palette.OnCard,
                dayInSelectionRangeContainerColor = Palette.AccentBlue.copy(alpha = 0.18f),
                dividerColor = Palette.FieldBorder,
                navigationContentColor = Palette.OnCard,
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = state.selectedDateMillis ?: return@TextButton
                        onConfirm(formatSprintDateFromPickerMillis(selected))
                    },
                    enabled = state.selectedDateMillis != null,
                    colors = ButtonDefaults.textButtonColors(contentColor = Palette.AccentBlue)
                ) {
                    Text(StringResources.ProjectInfoConfirmButton, fontFamily = Roboto)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Palette.AccentBlue)
                ) {
                    Text(StringResources.ProjectSprintSubmitCancel, fontFamily = Roboto)
                }
            }
        ) {
            DatePicker(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = StringResources.ProjectSprintFieldEndDateLabel,
                        fontFamily = Roboto,
                        color = Palette.OnCard,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                },
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Palette.CardSurface,
                    titleContentColor = Palette.OnCard,
                    headlineContentColor = Palette.OnCard,
                    weekdayContentColor = Palette.FieldLabel,
                    subheadContentColor = Palette.FieldLabel,
                    yearContentColor = Palette.FieldLabel,
                    currentYearContentColor = Palette.AccentBlue,
                    selectedYearContentColor = Palette.ButtonTextOnPrimary,
                    selectedYearContainerColor = Palette.AccentBlue,
                    dayContentColor = Palette.OnCard,
                    disabledDayContentColor = Palette.FieldLabel.copy(alpha = 0.4f),
                    selectedDayContentColor = Palette.ButtonTextOnPrimary,
                    disabledSelectedDayContentColor = Palette.ButtonTextOnPrimary.copy(alpha = 0.5f),
                    selectedDayContainerColor = Palette.AccentBlue,
                    todayContentColor = Palette.AccentBlue,
                    todayDateBorderColor = Palette.AccentBlue,
                    dayInSelectionRangeContentColor = Palette.OnCard,
                    dayInSelectionRangeContainerColor = Palette.AccentBlue.copy(alpha = 0.18f),
                    dividerColor = Palette.FieldBorder,
                    navigationContentColor = Palette.OnCard,
                )
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
private fun localDateToPickerUtcMillis(date: LocalDate): Long {
    // noon UTC avoids edge cases around timezone offsets when converting back to LocalDate
    val iso = "${date.year.toString().padStart(4, '0')}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}T12:00:00Z"
    return Instant.parse(iso).toEpochMilliseconds()
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatSprintDateFromPickerMillis(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.UTC)
        .date
    return buildString {
        append(date.dayOfMonth.toString().padStart(2, '0'))
        append('.')
        append(date.monthNumber.toString().padStart(2, '0'))
        append('.')
        append(date.year)
    }
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

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatActivityTimestamp(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dt.date.dayOfMonth.toString().padStart(2, '0'))
        append('.')
        append(dt.date.monthNumber.toString().padStart(2, '0'))
        append('.')
        append(dt.date.year)
        append(' ')
        append(dt.time.hour.toString().padStart(2, '0'))
        append(':')
        append(dt.time.minute.toString().padStart(2, '0'))
    }
}

private fun formatActorName(actor: SharedProfile?): String {
    if (actor == null) return StringResources.ProjectActivityFeedActorUnknown
    return listOf(actor.lastName, actor.firstName, actor.secondName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { StringResources.ProjectActivityFeedActorUnknown }
}

private fun formatProjectActivity(
    activity: Activity,
    actor: SharedProfile?,
): FormattedProjectActivity {
    val actorName = formatActorName(actor)
    val timestamp = formatActivityTimestamp(activity.timeMillis)
    return when (activity.type) {
        ActivityType.SprintStart -> FormattedProjectActivity(
            timestamp = timestamp,
            message = "$actorName начал спринт ${activity.targetTitle}",
            sprintId = activity.targetId
        )
        ActivityType.SprintFinish -> FormattedProjectActivity(
            timestamp = timestamp,
            message = "$actorName завершил спринт ${activity.targetTitle}",
            sprintId = activity.targetId
        )
        ActivityType.RepoBind -> FormattedProjectActivity(
            timestamp = timestamp,
            message = "$actorName привязал репозиторий ${activity.targetTitle}",
            repoUrl = activity.targetTitle
        )
        ActivityType.RepoUnbind -> FormattedProjectActivity(
            timestamp = timestamp,
            message = "$actorName отвязал репозиторий ${activity.targetTitle}",
            repoUrl = activity.targetTitle
        )
        ActivityType.MemberAdd -> FormattedProjectActivity(
            timestamp = timestamp,
            message = "${activity.targetTitle} присоединился к проекту"
        )
        ActivityType.MemberRemove -> FormattedProjectActivity(
            timestamp = timestamp,
            message = "$actorName удалил участника ${activity.targetTitle} из проекта"
        )
        ActivityType.KardMove -> FormattedProjectActivity(
            timestamp = timestamp,
            message = buildString {
                append(actorName)
                append(" переместил карточку ")
                append(activity.targetTitle)
                activity.secondaryTargetTitle?.takeIf { it.isNotBlank() }?.let {
                    append(" в столбец ")
                    append(it)
                }
            }
        )
    }
}
