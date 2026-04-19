package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import domain.auth.UserType
import domain.project.ProjectStatus
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.project.sdk.ProjectNavItem
import ru.kaelesty.madprojects.kmp.generated.resources.Res
import ru.kaelesty.madprojects.kmp.generated.resources.activity as activityIcon
import ru.kaelesty.madprojects.kmp.generated.resources.github as githubIcon
import ru.kaelesty.madprojects.kmp.generated.resources.info as infoIcon
import ru.kaelesty.madprojects.kmp.generated.resources.kanban as kanbanIcon
import ru.kaelesty.madprojects.kmp.generated.resources.message as messageIcon
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.fields.AppTextField
import ru.kaelesty.madprojects.ui.headers.ScreenHeader
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto
import ru.kaelesty.madprojects.utils.KLogger

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ProjectScreen(
    authContext: AuthContext,
    projectId: String,
    navigator: ProjectNavItem.Navigator,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ProjectTab.Sprints) }
    var pendingChatId by rememberSaveable { mutableStateOf<Int?>(null) }
    var moderationBannerCollapsed by rememberSaveable(projectId) { mutableStateOf(false) }
    val userType by authContext.userType.collectAsState()
    koinViewModel<ProjectSocketViewModel>(parameters = { parametersOf(projectId) })
    val moderationVm = koinViewModel<ProjectModerationViewModel>(parameters = { parametersOf(projectId) })
    val moderationUi by moderationVm.uiState.collectAsState()
    val approveDialogState by moderationVm.approveDialogState.collectAsState()
    val disapproveDialogState by moderationVm.disapproveDialogState.collectAsState()

    val shouldShowModerationBanner =
        userType == UserType.Curator && moderationUi.projectStatus == ProjectStatus.Pending

    LaunchedEffect(shouldShowModerationBanner) {
        if (!shouldShowModerationBanner) {
            moderationBannerCollapsed = false
        }
    }

    Surface(color = Palette.Background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = StringResources.ProjectScreenTitle,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        ProjectTab.Info -> ProjectInfoScreen(
                            projectId = projectId,
                            authContext = authContext,
                            onProjectDeleted = navigator::back,
                        )
                        ProjectTab.Github -> ProjectGithubScreen(projectId = projectId)
                        ProjectTab.Kanban -> ProjectKanbanScreen(
                            projectId = projectId,
                            onOpenChat = { chatId ->
                                if (selectedTab != ProjectTab.Messenger) {
                                    KLogger.d(TAG) { "tab switch: ${selectedTab.name} -> Messenger (chatId=$chatId)" }
                                }
                                pendingChatId = chatId
                                selectedTab = ProjectTab.Messenger
                            }
                        )
                        ProjectTab.Messenger -> ProjectMessengerScreen(
                            projectId = projectId,
                            initialChatId = pendingChatId,
                            onChatConsumed = { pendingChatId = null }
                        )
                        ProjectTab.Sprints -> ProjectSprintsScreen(projectId = projectId)
                    }
                }
                NavigationBar(
                    containerColor = Palette.CardSurface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == ProjectTab.Sprints,
                        onClick = {
                            if (selectedTab != ProjectTab.Sprints) {
                                KLogger.d(TAG) { "tab switch: ${selectedTab.name} -> Sprints" }
                            }
                            selectedTab = ProjectTab.Sprints
                        },
                        icon = {
                            ProjectNavIcon(painter = painterResource(Res.drawable.activityIcon))
                        },
                        colors = navigationItemColors()
                    )
                    NavigationBarItem(
                        selected = selectedTab == ProjectTab.Messenger,
                        onClick = {
                            if (selectedTab != ProjectTab.Messenger) {
                                KLogger.d(TAG) { "tab switch: ${selectedTab.name} -> Messenger" }
                            }
                            selectedTab = ProjectTab.Messenger
                        },
                        icon = {
                            ProjectNavIcon(painter = painterResource(Res.drawable.messageIcon))
                        },
                        colors = navigationItemColors()
                    )
                    NavigationBarItem(
                        selected = selectedTab == ProjectTab.Kanban,
                        onClick = {
                            if (selectedTab != ProjectTab.Kanban) {
                                KLogger.d(TAG) { "tab switch: ${selectedTab.name} -> Kanban" }
                            }
                            selectedTab = ProjectTab.Kanban
                        },
                        icon = {
                            ProjectNavIcon(painter = painterResource(Res.drawable.kanbanIcon))
                        },
                        colors = navigationItemColors()
                    )
                    NavigationBarItem(
                        selected = selectedTab == ProjectTab.Github,
                        onClick = {
                            if (selectedTab != ProjectTab.Github) {
                                KLogger.d(TAG) { "tab switch: ${selectedTab.name} -> Github" }
                            }
                            selectedTab = ProjectTab.Github
                        },
                        icon = {
                            ProjectNavIcon(painter = painterResource(Res.drawable.githubIcon))
                        },
                        colors = navigationItemColors()
                    )
                    NavigationBarItem(
                        selected = selectedTab == ProjectTab.Info,
                        onClick = {
                            if (selectedTab != ProjectTab.Info) {
                                KLogger.d(TAG) { "tab switch: ${selectedTab.name} -> Info" }
                            }
                            selectedTab = ProjectTab.Info
                        },
                        icon = {
                            ProjectNavIcon(painter = painterResource(Res.drawable.infoIcon))
                        },
                        colors = navigationItemColors()
                    )
                }
            }

            if (shouldShowModerationBanner) {
                CuratorModerationBannerHost(
                    collapsed = moderationBannerCollapsed,
                    onCollapse = { moderationBannerCollapsed = true },
                    onExpand = { moderationBannerCollapsed = false },
                    onApproveClick = moderationVm::openApproveDialog,
                    onDisapproveClick = moderationVm::openDisapproveDialog,
                    isBusy = approveDialogState.isSubmitting || disapproveDialogState.isSubmitting,
                    errorMessage = moderationUi.bannerActionErrorMessage,
                    onDismissError = moderationVm::clearBannerActionError,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 84.dp)
                )
            }
        }

        if (approveDialogState.isOpen) {
            CuratorApproveProjectDialog(
                state = approveDialogState,
                onDismiss = moderationVm::closeApproveDialog,
                onConfirm = moderationVm::submitApprove,
            )
        }
        if (disapproveDialogState.isOpen) {
            CuratorDisapproveProjectDialog(
                state = disapproveDialogState,
                onDismiss = moderationVm::closeDisapproveDialog,
                onReasonChange = moderationVm::setDisapproveReason,
                onConfirm = moderationVm::submitDisapprove,
            )
        }
    }
}

private enum class ProjectTab {
    Info,
    Github,
    Kanban,
    Messenger,
    Sprints,
}

@Composable
private fun ProjectNavIcon(painter: Painter) {
    Box(
        modifier = Modifier.padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(LocalContentColor.current)
        )
    }
}

@Composable
private fun ProjectNavIcon(imageVector: ImageVector) {
    Box(
        modifier = Modifier.padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Palette.AccentBlue,
    unselectedIconColor = Palette.FieldLabel,
    indicatorColor = Palette.Background,
    disabledIconColor = Palette.FieldLabel.copy(alpha = 0.6f),
    disabledTextColor = Color.Transparent
)

@Composable
private fun CuratorModerationBannerHost(
    collapsed: Boolean,
    onCollapse: () -> Unit,
    onExpand: () -> Unit,
    onApproveClick: () -> Unit,
    onDisapproveClick: () -> Unit,
    isBusy: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !collapsed,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Palette.AccentRed,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = StringResources.ProjectModerationBannerTitle,
                            color = Palette.ButtonTextOnPrimary,
                            fontFamily = Roboto,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onCollapse,
                            enabled = !isBusy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = StringResources.ProjectModerationCollapse,
                                tint = Palette.ButtonTextOnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = StringResources.ProjectModerationBannerBody,
                        color = Palette.ButtonTextOnPrimary,
                        fontFamily = Roboto,
                        fontSize = 13.sp
                    )
                    errorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = it,
                                color = Palette.ButtonTextOnPrimary,
                                fontFamily = Roboto,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissError, enabled = !isBusy) {
                                Text(
                                    text = StringResources.ProjectInfoErrorDismiss,
                                    color = Palette.ButtonTextOnPrimary,
                                    fontFamily = Roboto
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CuratorModerationActionButton(
                            text = StringResources.ProjectModerationApproveButton,
                            onClick = onApproveClick,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        CuratorModerationActionButton(
                            text = StringResources.ProjectModerationDisapproveButton,
                            onClick = onDisapproveClick,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f),
                            filled = false,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = collapsed,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Palette.AccentRed,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onExpand)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "!",
                        color = Palette.ButtonTextOnPrimary,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CuratorModerationActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
) {
    val backgroundColor = if (filled) Color.White.copy(alpha = 0.18f) else Color.Transparent
    val borderColor = if (filled) Color.Transparent else Color.White.copy(alpha = 0.45f)
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (filled) null else androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Palette.ButtonTextOnPrimary.copy(alpha = if (enabled) 1f else 0.65f),
                fontFamily = Roboto,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}

@Composable
private fun CuratorApproveProjectDialog(
    state: ProjectModerationViewModel.ApproveDialogState,
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.ProjectModerationApproveDialogTitle,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = StringResources.ProjectModerationApproveDialogBody,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 13.sp
                )
                state.errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = Palette.AccentRed,
                        fontFamily = Roboto,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Palette.AccentBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Palette.AccentBlue
                            )
                        ) {
                            Text(StringResources.ProjectModerationCancelButton, fontFamily = Roboto)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            PrimaryActionButton(
                                text = StringResources.ProjectModerationApproveButton,
                                onClick = onConfirm
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CuratorDisapproveProjectDialog(
    state: ProjectModerationViewModel.DisapproveDialogState,
    onDismiss: () -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 440.dp),
            shape = RoundedCornerShape(16.dp),
            color = Palette.CardSurface,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.ProjectModerationDisapproveDialogTitle,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = StringResources.ProjectModerationDisapproveDialogHint,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    label = StringResources.ProjectModerationReasonLabel,
                    value = state.reason,
                    onValueChange = onReasonChange,
                    placeholder = StringResources.ProjectModerationReasonPlaceholder,
                    singleLine = false,
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${state.reason.length}/200",
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 12.sp
                )
                state.errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = Palette.AccentRed,
                        fontFamily = Roboto,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Palette.AccentBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Palette.AccentBlue
                            )
                        ) {
                            Text(StringResources.ProjectModerationCancelButton, fontFamily = Roboto)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            PrimaryActionButton(
                                text = StringResources.ProjectModerationDisapproveButton,
                                onClick = onConfirm
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val TAG = "ProjectScreen"
