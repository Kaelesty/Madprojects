package ru.kaelesty.madprojects.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import domain.auth.UserType
import domain.profile.ProfileProject
import domain.projectgroups.ProjectGroup
import domain.project.ProjectStatus
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.GithubOauthBridge
import ru.kaelesty.madprojects.features.auth.domain.GithubOauthResult
import ru.kaelesty.madprojects.features.auth.domain.StartGithubOauthUseCase
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
import ru.kaelesty.madprojects.utils.KLogger

@Composable
fun ProfileScreen(
    authContext: AuthContext,
    navigator: ProfileNavItem.Navigator,
) {
    val userType by authContext.userType.collectAsState()
    val pendingOauthResult by GithubOauthBridge.pendingResult.collectAsState()
    val (oauthBanner, setOauthBanner) = remember { mutableStateOf<GithubOauthResult?>(null) }

    androidx.compose.runtime.LaunchedEffect(pendingOauthResult?.eventId) {
        pendingOauthResult?.let {
            setOauthBanner(it)
            GithubOauthBridge.consumePendingResult()
        }
    }
    androidx.compose.runtime.LaunchedEffect(oauthBanner?.eventId) {
        if (oauthBanner != null) {
            delay(5000)
            setOauthBanner(null)
        }
    }

    ProfileScaffold(title = StringResources.ProfileTitle) {
        when (userType) {
            UserType.Common -> CommonProfileContent(
                authContext = authContext,
                onCreateProject = navigator::toCreateProject,
                onProjectClick = navigator::toProject,
                oauthBanner = oauthBanner,
                onDismissOauthBanner = { setOauthBanner(null) },
            )
            UserType.Curator -> CuratorProfileContent(
                authContext = authContext,
                onGroupClick = navigator::toCuratorGroup,
                oauthBanner = oauthBanner,
                onDismissOauthBanner = { setOauthBanner(null) },
            )
            null -> ProfilePlaceholderContent(StringResources.ProfilePlaceholder)
        }
    }
}

@Composable
private fun CommonProfileContent(
    authContext: AuthContext,
    onCreateProject: () -> Unit,
    onProjectClick: (String) -> Unit,
    oauthBanner: GithubOauthResult?,
    onDismissOauthBanner: () -> Unit,
) {
    val vm = koinViewModel<CommonProfileViewModel>()
    val state by vm.state.collectAsState()
    val projectsState by vm.projectsState.collectAsState()
    val joinDialogState by vm.joinDialogState.collectAsState()
    val editDialogState by vm.editDialogState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadProjects()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    androidx.compose.runtime.LaunchedEffect(oauthBanner?.eventId) {
        if (oauthBanner?.status == GithubOauthResult.Status.Success) {
            vm.load()
        }
    }

    if (joinDialogState.isOpen) {
        JoinProjectDialog(
            state = joinDialogState,
            onDismiss = vm::closeJoinDialog,
            onInviteChange = vm::setInviteCode,
            onConfirm = vm::submitJoin,
        )
    }
    if (editDialogState.isOpen) {
        ProfileEditDialog(
            title = StringResources.ProfileEditDialogTitle,
            extraFieldLabel = StringResources.ProfileEditGroupLabel,
            extraFieldPlaceholder = StringResources.ProfileEditGroupPlaceholder,
            firstName = editDialogState.firstName,
            secondName = editDialogState.secondName,
            lastName = editDialogState.lastName,
            extraValue = editDialogState.group,
            errorMessage = editDialogState.errorMessage,
            isSubmitting = editDialogState.isSubmitting,
            onDismiss = vm::closeEditDialog,
            onFirstNameChange = vm::setEditFirstName,
            onSecondNameChange = vm::setEditSecondName,
            onLastNameChange = vm::setEditLastName,
            onExtraValueChange = vm::setEditGroup,
            onConfirm = vm::submitEdit,
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        oauthBanner?.let { banner ->
            GithubOauthBanner(
                result = banner,
                onDismiss = onDismissOauthBanner,
            )
        }
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
                ProfileIdentityCard(
                    avatarUrl = profile.githubMeta?.githubAvatar,
                    fullName = fullName(profile.lastName, profile.firstName, profile.secondName),
                    email = profile.email,
                    secondaryLabel = StringResources.ProfileGroupLabel,
                    secondaryValue = profile.group,
                    onEdit = vm::openEditDialog,
                    onLogout = authContext::logout,
                    isGithubConnected = profile.githubMeta != null,
                    onConnectGithub = {
                        scope.launch {
                            when (val result = vm.buildGithubOauthStartUrl()) {
                                is StartGithubOauthUseCase.Result.Success -> {
                                    runCatching { uriHandler.openUri(result.url) }
                                        .onFailure { KLogger.e("ProfileScreen", it) { "openUri failed (common profile)" } }
                                }
                                is StartGithubOauthUseCase.Result.Fail -> {
                                    KLogger.w("ProfileScreen") { "buildStartUrl failed (common profile): ${result.message}" }
                                }
                            }
                        }
                    },
                )
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
            onJoinProject = vm::openJoinDialog,
            onProjectClick = onProjectClick
        )
    }
}

@Composable
private fun CuratorProfileContent(
    authContext: AuthContext,
    onGroupClick: (String) -> Unit,
    oauthBanner: GithubOauthResult?,
    onDismissOauthBanner: () -> Unit,
) {
    val vm = koinViewModel<CuratorProfileViewModel>()
    val state by vm.state.collectAsState()
    val editDialogState by vm.editDialogState.collectAsState()
    val createGroupDialogState by vm.createGroupDialogState.collectAsState()
    val deleteGroupDialogState by vm.deleteGroupDialogState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshSilently()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    androidx.compose.runtime.LaunchedEffect(oauthBanner?.eventId) {
        if (oauthBanner?.status == GithubOauthResult.Status.Success) {
            vm.load()
        }
    }

    if (editDialogState.isOpen) {
        ProfileEditDialog(
            title = StringResources.ProfileEditDialogTitle,
            extraFieldLabel = StringResources.ProfileEditGradeLabel,
            extraFieldPlaceholder = StringResources.ProfileEditGradePlaceholder,
            firstName = editDialogState.firstName,
            secondName = editDialogState.secondName,
            lastName = editDialogState.lastName,
            extraValue = editDialogState.grade,
            errorMessage = editDialogState.errorMessage,
            isSubmitting = editDialogState.isSubmitting,
            onDismiss = vm::closeEditDialog,
            onFirstNameChange = vm::setEditFirstName,
            onSecondNameChange = vm::setEditSecondName,
            onLastNameChange = vm::setEditLastName,
            onExtraValueChange = vm::setEditGrade,
            onConfirm = vm::submitEdit,
        )
    }
    if (createGroupDialogState.isOpen) {
        CreateProjectGroupDialog(
            state = createGroupDialogState,
            onDismiss = vm::closeCreateGroupDialog,
            onTitleChange = vm::setCreateGroupTitle,
            onConfirm = vm::submitCreateGroup,
        )
    }
    if (deleteGroupDialogState.isOpen) {
        DeleteProjectGroupDialog(
            state = deleteGroupDialogState,
            onDismiss = vm::closeDeleteGroupDialog,
            onConfirm = vm::submitDeleteGroup,
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        oauthBanner?.let { banner ->
            GithubOauthBanner(
                result = banner,
                onDismiss = onDismissOauthBanner,
            )
        }
        when (val current = state) {
            CuratorProfileViewModel.State.Loading -> {
                ProfileCard {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Palette.AccentBlue,
                        strokeWidth = 3.dp
                    )
                }
            }
            is CuratorProfileViewModel.State.Loaded -> {
                val profile = current.profile
                ProfileIdentityCard(
                    avatarUrl = profile.githubMeta?.githubAvatar,
                    fullName = fullName(profile.lastName, profile.firstName, profile.secondName),
                    email = profile.email,
                    secondaryLabel = StringResources.ProfileGradeLabel,
                    secondaryValue = profile.grade,
                    onEdit = vm::openEditDialog,
                    onLogout = authContext::logout,
                    isGithubConnected = profile.githubMeta != null,
                    onConnectGithub = {
                        scope.launch {
                            when (val result = vm.buildGithubOauthStartUrl()) {
                                is StartGithubOauthUseCase.Result.Success -> {
                                    runCatching { uriHandler.openUri(result.url) }
                                        .onFailure { KLogger.e("ProfileScreen", it) { "openUri failed (curator profile)" } }
                                }
                                is StartGithubOauthUseCase.Result.Fail -> {
                                    KLogger.w("ProfileScreen") { "buildStartUrl failed (curator profile): ${result.message}" }
                                }
                            }
                        }
                    },
                )
                ProjectGroupsCard(
                    groups = profile.projectGroups,
                    onCreateGroup = vm::openCreateGroupDialog,
                    onOpenGroup = { onGroupClick(it.id) },
                    onDeleteGroup = { vm.openDeleteGroupDialog(it.id, it.title) }
                )
            }
            is CuratorProfileViewModel.State.Error -> {
                ProfileCard {
                    Text(
                        text = current.message ?: StringResources.LoadError,
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
    }
}

@Composable
private fun DeleteProjectGroupDialog(
    state: CuratorProfileViewModel.DeleteGroupDialogState,
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = StringResources.CuratorProfileDeleteGroupDialogTitle,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = StringResources.CuratorProfileDeleteGroupDialogBodyPrefix + state.groupTitle,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 14.sp
                )
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Roboto,
                        textAlign = TextAlign.Center
                    )
                }
                if (state.isSubmitting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Palette.AccentBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = StringResources.CuratorProfileDeleteGroupSubmitting,
                            color = Palette.FieldLabel,
                            fontFamily = Roboto,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                PrimaryActionButton(
                    text = StringResources.CuratorProfileDeleteGroupConfirmButton,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting
                )
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = Palette.AccentBlue
                    )
                ) {
                    Text(
                        text = StringResources.CuratorProfileDeleteGroupCancelButton,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateProjectGroupDialog(
    state: CuratorProfileViewModel.CreateGroupDialogState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = StringResources.CuratorProfileCreateGroupDialogTitle,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = StringResources.CuratorProfileCreateGroupDialogHint,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 14.sp
                )
                AppTextField(
                    label = StringResources.CuratorProfileCreateGroupTitleLabel,
                    value = state.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = StringResources.CuratorProfileCreateGroupTitlePlaceholder
                )
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Roboto,
                        textAlign = TextAlign.Center
                    )
                }
                if (state.isSubmitting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Palette.AccentBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = StringResources.CuratorProfileCreateGroupSubmitting,
                            color = Palette.FieldLabel,
                            fontFamily = Roboto,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                PrimaryActionButton(
                    text = StringResources.CuratorProfileCreateGroupConfirmButton,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting
                )
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = Palette.AccentBlue
                    )
                ) {
                    Text(
                        text = StringResources.CuratorProfileCreateGroupCancelButton,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
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
private fun ProfileIdentityCard(
    avatarUrl: String?,
    fullName: String,
    email: String,
    secondaryLabel: String,
    secondaryValue: String,
    isGithubConnected: Boolean,
    onConnectGithub: () -> Unit,
    onEdit: () -> Unit,
    onLogout: () -> Unit,
) {
    val (menuExpanded, setMenuExpanded) = remember { mutableStateOf(false) }
    val menuItems = listOf(
        AppDropdownMenuItem(
            text = StringResources.ProfileMenuEdit,
            onClick = {
                setMenuExpanded(false)
                onEdit()
            }
        ),
        AppDropdownMenuItem(
            text = StringResources.ProfileMenuLogout,
            onClick = {
                setMenuExpanded(false)
                onLogout()
            }
        )
    )
    ProfileCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            if (avatarUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.Center)
                        .background(Palette.FieldBorder, CircleShape)
                )
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = StringResources.ProfileAvatar,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = { setMenuExpanded(true) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = StringResources.ProfileMenu,
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
        Spacer(Modifier.height(16.dp))
        Text(
            text = fullName,
            style = TextStyle(
                color = Palette.OnCard,
                fontFamily = Roboto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "${StringResources.ProfileEmailLabel}$email",
            color = Palette.OnCard,
            fontFamily = Roboto
        )
        Text(
            text = "$secondaryLabel$secondaryValue",
            color = Palette.OnCard,
            fontFamily = Roboto,
            textAlign = TextAlign.Center
        )
        if (!isGithubConnected) {
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = onConnectGithub,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = Palette.AccentBlue
                )
            ) {
                Text(
                    text = StringResources.GithubAuthConnectButton,
                    fontFamily = Roboto
                )
            }
        }
    }
}

@Composable
private fun GithubOauthBanner(
    result: GithubOauthResult,
    onDismiss: () -> Unit,
) {
    val isSuccess = result.status == GithubOauthResult.Status.Success
    val background = if (isSuccess) {
        Palette.AccentGreen.copy(alpha = 0.14f)
    } else {
        Palette.AccentRed.copy(alpha = 0.14f)
    }
    val contentColor = if (isSuccess) Palette.AccentGreen else Palette.AccentRed
    val message = if (isSuccess) {
        StringResources.GithubAuthBannerSuccess
    } else {
        val reason = result.reason
        if (reason.isNullOrBlank()) {
            StringResources.GithubAuthBannerError
        } else {
            "${StringResources.GithubAuthBannerError}: $reason"
        }
    }

    ProfileCard {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = background,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    color = contentColor,
                    fontFamily = Roboto,
                    fontSize = 13.sp
                )
                TextButton(
                    onClick = onDismiss,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = contentColor
                    )
                ) {
                    Text(
                        text = StringResources.GithubAuthBannerDismiss,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileEditDialog(
    title: String,
    extraFieldLabel: String,
    extraFieldPlaceholder: String,
    firstName: String,
    secondName: String,
    lastName: String,
    extraValue: String,
    errorMessage: String?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onSecondNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onExtraValueChange: (String) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    label = StringResources.ProfileEditFirstNameLabel,
                    value = firstName,
                    onValueChange = onFirstNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = StringResources.ProfileEditFirstNamePlaceholder
                )
                AppTextField(
                    label = StringResources.ProfileEditLastNameLabel,
                    value = lastName,
                    onValueChange = onLastNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = StringResources.ProfileEditLastNamePlaceholder
                )
                AppTextField(
                    label = StringResources.ProfileEditSecondNameLabel,
                    value = secondName,
                    onValueChange = onSecondNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = StringResources.ProfileEditSecondNamePlaceholder
                )
                AppTextField(
                    label = extraFieldLabel,
                    value = extraValue,
                    onValueChange = onExtraValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = extraFieldPlaceholder
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Roboto,
                        textAlign = TextAlign.Center
                    )
                }

                if (isSubmitting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Palette.AccentBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = StringResources.ProfileEditSubmitting,
                            color = Palette.FieldLabel,
                            fontFamily = Roboto,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))
                PrimaryActionButton(
                    text = StringResources.ProfileEditSaveButton,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                )
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = Palette.AccentBlue
                    )
                ) {
                    Text(
                        text = StringResources.ProfileEditCancelButton,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectGroupsCard(
    groups: List<ProjectGroup>,
    onCreateGroup: () -> Unit,
    onOpenGroup: (ProjectGroup) -> Unit,
    onDeleteGroup: (ProjectGroup) -> Unit,
) {
    ProfileCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = StringResources.CuratorProfileGroupsTitle,
                style = TextStyle(
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onCreateGroup,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = StringResources.CuratorProfileGroupsCreateButton,
                    tint = Palette.OnCard
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (groups.isEmpty()) {
            Text(
                text = StringResources.CuratorProfileGroupsEmpty,
                color = Palette.FieldLabel,
                fontFamily = Roboto,
                fontSize = 14.sp
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groups.forEach { group ->
                    ProjectGroupItem(
                        group = group,
                        onOpen = { onOpenGroup(group) },
                        onDelete = { onDeleteGroup(group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectGroupItem(
    group: ProjectGroup,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val (menuExpanded, setMenuExpanded) = remember { mutableStateOf(false) }
    val menuItems = listOf(
        AppDropdownMenuItem(
            text = StringResources.CuratorProfileDeleteGroupButton,
            onClick = {
                setMenuExpanded(false)
                onDelete()
            }
        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
            .clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Palette.AccentBlue)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = group.title,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (group.pendingProjectsCount > 0) {
            Spacer(Modifier.width(10.dp))
            Surface(
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                color = Palette.AccentRed,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.pendingProjectsCount.toString(),
                        color = Palette.CardSurface,
                        fontFamily = Roboto,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Box {
            IconButton(
                onClick = { setMenuExpanded(true) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = StringResources.ProfileMenu,
                    tint = Palette.FieldLabel,
                    modifier = Modifier.size(18.dp)
                )
            }
            AppDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { setMenuExpanded(false) },
                items = menuItems,
            )
        }
    }
}

@Composable
private fun ProjectsCard(
    state: CommonProfileViewModel.ProjectsState,
    onRetry: () -> Unit,
    onCreateProject: () -> Unit,
    onJoinProject: () -> Unit,
    onProjectClick: (String) -> Unit,
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
                            ProjectItem(project, onClick = { onProjectClick(project.id) })
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
private fun ProjectItem(
    project: ProfileProject,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
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

private fun fullName(lastName: String, firstName: String, secondName: String): String {
    return listOf(lastName, firstName, secondName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

@Composable
internal fun ProfileScaffold(
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
