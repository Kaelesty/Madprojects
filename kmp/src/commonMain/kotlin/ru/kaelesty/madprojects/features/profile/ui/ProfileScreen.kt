package ru.kaelesty.madprojects.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import domain.auth.UserType
import domain.profile.ProfileProject
import domain.project.ProjectStatus
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.viewmodel.koinViewModel
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.profile.sdk.ProfileNavItem
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.ProfileCard
import ru.kaelesty.madprojects.ui.fields.AppTextField
import ru.kaelesty.madprojects.ui.headers.ScreenHeader
import ru.kaelesty.madprojects.ui.menus.AppDropdownMenu
import ru.kaelesty.madprojects.ui.menus.AppDropdownMenuItem
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun ProfileScreen(
    authContext: AuthContext,
    navigator: ProfileNavItem.Navigator,
) {
    val userType by authContext.userType.collectAsState()
    ProfileScaffold(title = StringResources.ProfileTitle) {
        when (userType) {
            UserType.Common -> CommonProfileContent(
                onCreateProject = navigator::toCreateProject
            )
            UserType.Curator -> CuratorProfileContent()
            null -> ProfilePlaceholderContent(StringResources.ProfilePlaceholder)
        }
    }
}

@Composable
private fun CommonProfileContent(
    onCreateProject: () -> Unit,
) {
    val vm = koinViewModel<CommonProfileViewModel>()
    val state by vm.state.collectAsState()
    val projectsState by vm.projectsState.collectAsState()
    val joinDialogState by vm.joinDialogState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadProjects()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (joinDialogState.isOpen) {
        JoinProjectDialog(
            state = joinDialogState,
            onDismiss = vm::closeJoinDialog,
            onInviteChange = vm::setInviteCode,
            onConfirm = vm::submitJoin,
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val current = state) {
            CommonProfileViewModel.State.Loading -> {
                ProfileCard {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Palette.AccentBlue,
                        strokeWidth = 3.dp
                    )
                }
            }
            is CommonProfileViewModel.State.Loaded -> {
                val profile = current.profile
                val avatarUrl = profile.githubMeta?.githubAvatar
                ProfileCard {
                    if (avatarUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(Palette.FieldBorder, CircleShape)
                        )
                    } else {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = StringResources.ProfileAvatar,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "${profile.lastName} ${profile.firstName} ${profile.secondName}",
                        style = TextStyle(
                            color = Palette.OnCard,
                            fontFamily = Roboto,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(text = "${StringResources.ProfileEmailLabel}${profile.email}", color = Palette.OnCard, fontFamily = Roboto)
                    Text(text = "${StringResources.ProfileGroupLabel}${profile.group}", color = Palette.OnCard, fontFamily = Roboto)
                }
            }
            CommonProfileViewModel.State.Error -> {
                ProfileCard {
                    Text(
                        text = StringResources.LoadError,
                        style = TextStyle(
                            color = Palette.OnCard,
                            fontFamily = Roboto,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryActionButton(
                        text = StringResources.RetryButton,
                        onClick = vm::load,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        ProjectsCard(
            state = projectsState,
            onRetry = vm::loadProjects,
            onCreateProject = onCreateProject,
            onJoinProject = vm::openJoinDialog
        )
    }
}

@Composable
private fun CuratorProfileContent() {
    ProfilePlaceholderContent(StringResources.CuratorProfilePlaceholder)
}

@Composable
private fun ProfilePlaceholderContent(title: String) {
    ProfileCard {
        Text(
            text = title,
            style = TextStyle(
                color = Palette.OnCard,
                fontFamily = Roboto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ProjectsCard(
    state: CommonProfileViewModel.ProjectsState,
    onRetry: () -> Unit,
    onCreateProject: () -> Unit,
    onJoinProject: () -> Unit,
) {
    val (menuExpanded, setMenuExpanded) = remember { mutableStateOf(false) }
    val menuItems = listOf(
        AppDropdownMenuItem(
            text = StringResources.ProfileProjectsCreate,
            onClick = {
                setMenuExpanded(false)
                onCreateProject()
            }
        ),
        AppDropdownMenuItem(
            text = StringResources.ProfileProjectsJoin,
            onClick = {
                setMenuExpanded(false)
                onJoinProject()
            }
        ),
    )
    ProfileCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = StringResources.ProfileProjectsTitle,
                style = TextStyle(
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            )
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(
                    onClick = { setMenuExpanded(true) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = StringResources.ProfileProjectsMenu,
                        tint = Palette.OnCard
                    )
                }
                AppDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { setMenuExpanded(false) },
                    items = menuItems,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        when (state) {
            CommonProfileViewModel.ProjectsState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Palette.AccentBlue,
                    strokeWidth = 3.dp
                )
            }
            is CommonProfileViewModel.ProjectsState.Loaded -> {
                if (state.projects.isEmpty()) {
                    Text(
                        text = StringResources.ProfileProjectsEmpty,
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 14.sp
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.projects.forEach { project ->
                            ProjectItem(project)
                        }
                    }
                }
            }
            CommonProfileViewModel.ProjectsState.Error -> {
                Text(
                    text = StringResources.LoadError,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 16.sp
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
}

@Composable
private fun JoinProjectDialog(
    state: CommonProfileViewModel.JoinDialogState,
    onDismiss: () -> Unit,
    onInviteChange: (String) -> Unit,
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
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = StringResources.JoinProjectDialogTitle,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    label = StringResources.JoinProjectInviteLabel,
                    value = state.inviteCode,
                    onValueChange = onInviteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = StringResources.JoinProjectInvitePlaceholder
                )
                if (state.errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Roboto,
                        textAlign = TextAlign.Center
                    )
                }
                if (state.isSubmitting) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Palette.AccentBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = StringResources.JoinProjectInviteProcessing,
                            color = Palette.FieldLabel,
                            fontFamily = Roboto,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                PrimaryActionButton(
                    text = StringResources.JoinProjectInviteButton,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = Palette.AccentBlue
                    )
                ) {
                    Text(
                        text = StringResources.JoinProjectInviteCancel,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectItem(project: ProfileProject) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Palette.AccentBlue)
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = project.title,
                color = Palette.OnCard,
                fontFamily = Roboto,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = statusLabel(project.status),
                color = statusColor(project.status),
                fontFamily = Roboto,
                fontSize = 13.sp
            )
        }
        project.mark?.let { mark ->
            Text(
                text = mark.toString(),
                color = Palette.OnCard,
                fontFamily = Roboto,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun statusLabel(status: ProjectStatus): String = when (status) {
    ProjectStatus.Pending -> StringResources.ProjectStatusPending
    ProjectStatus.Approved -> StringResources.ProjectStatusApproved
    ProjectStatus.Unapproved -> StringResources.ProjectStatusUnapproved
}

private fun statusColor(status: ProjectStatus) = when (status) {
    ProjectStatus.Pending -> Palette.FieldLabel
    ProjectStatus.Approved -> Palette.AccentBlue
    ProjectStatus.Unapproved -> Palette.AccentRed
}

@Composable
private fun ProfileScaffold(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(color = Palette.Background) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = title,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 280.dp, max = 420.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content()
                }
            }
        }
    }
}
