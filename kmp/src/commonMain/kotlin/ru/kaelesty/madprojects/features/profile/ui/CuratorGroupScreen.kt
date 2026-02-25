package ru.kaelesty.madprojects.features.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.projectgroups.ProjectInGroupView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.ProfileCard
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun CuratorGroupScreen(
    groupId: String,
    onBack: () -> Unit,
    onProjectClick: (String) -> Unit,
) {
    val vm = koinViewModel<CuratorGroupViewModel>(parameters = { parametersOf(groupId) })
    val state by vm.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshSilently()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val loadedState = state as? CuratorGroupViewModel.State.Loaded
    val headerTitle = loadedState?.groupTitle ?: StringResources.CuratorGroupScreenTitle

    ProfileScaffold(title = headerTitle) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CuratorGroupHeaderCard(
                onBack = onBack,
                pendingCount = loadedState?.pendingProjects?.size,
                approvedCount = loadedState?.approvedProjects?.size,
            )

            when (val current = state) {
                CuratorGroupViewModel.State.Loading -> {
                    ProfileCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Palette.AccentBlue,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = StringResources.LoadError.replace(StringResources.LoadError, "Загрузка проектов группы..."),
                                color = Palette.FieldLabel,
                                fontFamily = Roboto,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is CuratorGroupViewModel.State.Error -> {
                    ProfileCard {
                        Text(
                            text = current.message,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = Roboto,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        PrimaryActionButton(
                            text = StringResources.RetryButton,
                            onClick = vm::load,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is CuratorGroupViewModel.State.Loaded -> {
                    if (current.pendingProjects.isNotEmpty()) {
                        CuratorGroupProjectsCard(
                            title = StringResources.CuratorGroupPendingProjectsTitle,
                            projects = current.pendingProjects,
                            accentColor = Palette.AccentRed,
                            emptyText = null,
                            onProjectClick = onProjectClick,
                        )
                    }
                    CuratorGroupProjectsCard(
                        title = StringResources.CuratorGroupApprovedProjectsTitle,
                        projects = current.approvedProjects,
                        accentColor = Palette.AccentBlue,
                        emptyText = StringResources.CuratorGroupApprovedProjectsEmpty,
                        onProjectClick = onProjectClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun CuratorGroupHeaderCard(
    onBack: () -> Unit,
    pendingCount: Int?,
    approvedCount: Int?,
) {
    ProfileCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Palette.FieldBg,
                border = BorderStroke(1.dp, Palette.FieldBorder)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = StringResources.BackButton,
                        tint = Palette.OnCard,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = StringResources.CuratorGroupProjectsListSubtitle,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Списки по статусам",
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 12.sp
                )
            }
        }

        if (pendingCount != null && approvedCount != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GroupStatPill(
                    label = StringResources.CuratorGroupPendingProjectsTitle,
                    value = pendingCount,
                    accentColor = Palette.AccentRed,
                    modifier = Modifier.weight(1f)
                )
                GroupStatPill(
                    label = StringResources.CuratorGroupApprovedProjectsTitle,
                    value = approvedCount,
                    accentColor = Palette.AccentBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GroupStatPill(
    label: String,
    value: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Palette.FieldBg,
        border = BorderStroke(1.dp, Palette.FieldBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accentColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = Palette.FieldLabel,
                fontFamily = Roboto,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = value.toString(),
                color = Palette.OnCard,
                fontFamily = Roboto,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CuratorGroupProjectsCard(
    title: String,
    projects: List<ProjectInGroupView>,
    accentColor: Color,
    emptyText: String?,
    onProjectClick: (String) -> Unit,
) {
    ProfileCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 20.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = TextStyle(
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.weight(1f)
            )
            SectionCountBadge(
                count = projects.size,
                accentColor = accentColor
            )
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Palette.Divider)
        Spacer(Modifier.height(10.dp))

        if (projects.isEmpty()) {
            if (emptyText != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Palette.FieldBg,
                    border = BorderStroke(1.dp, Palette.FieldBorder)
                ) {
                    Text(
                        text = emptyText,
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                projects.forEach { project ->
                    CuratorGroupProjectItem(
                        project = project,
                        accentColor = accentColor,
                        onClick = { onProjectClick(project.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCountBadge(
    count: Int,
    accentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = 0.12f)
    ) {
        Text(
            text = count.toString(),
            color = accentColor,
            fontFamily = Roboto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CuratorGroupProjectItem(
    project: ProjectInGroupView,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Palette.FieldBg,
        border = BorderStroke(1.dp, Palette.FieldBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(44.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = project.title,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            project.mark?.let { mark ->
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Palette.AccentBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = mark.toString(),
                        color = Palette.AccentBlue,
                        fontFamily = Roboto,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaPill(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = Palette.CardSurface,
        border = BorderStroke(1.dp, Palette.FieldBorder)
    ) {
        Text(
            text = text,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
