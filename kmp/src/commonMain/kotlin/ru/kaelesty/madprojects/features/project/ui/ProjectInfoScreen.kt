package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import domain.auth.UserType
import domain.project.Project
import domain.project.ProjectMember
import domain.project.ProjectRepository
import domain.project.ProjectStatus
import kotlinx.coroutines.flow.collect
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.ProfileCard
import ru.kaelesty.madprojects.ui.fields.AppTextField
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto
import ru.kaelesty.madprojects.utils.KLogger

@Composable
fun ProjectInfoScreen(
    projectId: String,
    authContext: AuthContext,
    onProjectDeleted: () -> Unit,
) {
    val vm = koinViewModel<ProjectInfoViewModel>(parameters = { parametersOf(projectId) })
    val ui by vm.uiState.collectAsState()
    val editState by vm.editMetaDialogState.collectAsState()
    val inviteState by vm.inviteDialogState.collectAsState()
    val repoState by vm.repoDialogState.collectAsState()
    val deleteState by vm.deleteProjectDialogState.collectAsState()
    val userType by authContext.userType.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val canManageAsCurator = userType == UserType.Curator
    var inviteCopied by remember(inviteState.isOpen) { mutableStateOf(false) }
    var pendingMemberDelete by remember { mutableStateOf<ProjectMember?>(null) }
    var pendingRepoDelete by remember { mutableStateOf<ProjectRepository?>(null) }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            if (event is ProjectInfoViewModel.Event.ProjectDeleted) {
                KLogger.d(TAG) { "project deleted -> back" }
                onProjectDeleted()
            }
        }
    }

    if (editState.isOpen) {
        DialogCard(onDismiss = vm::closeEditMetaDialog) {
            Text(StringResources.ProjectInfoEditMetaDialogTitle, color = Palette.OnCard, fontFamily = Roboto, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            AppTextField(
                label = StringResources.CreateProjectNameLabel,
                value = editState.title,
                onValueChange = vm::setEditTitle,
                placeholder = StringResources.CreateProjectNamePlaceholder
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                label = StringResources.CreateProjectDescLabel,
                value = editState.description,
                onValueChange = vm::setEditDescription,
                placeholder = StringResources.CreateProjectDescPlaceholder,
                singleLine = false,
                minLines = 4,
                maxLines = 6,
            )
            Spacer(Modifier.height(8.dp))
            Text("${editState.title.length}/32, ${editState.description.length}/1000", color = Palette.FieldLabel, fontFamily = Roboto, fontSize = 12.sp)
            editState.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Palette.AccentRed, fontFamily = Roboto, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            if (editState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Palette.AccentBlue, strokeWidth = 2.dp)
            } else {
                DialogButtons(
                    primaryText = StringResources.ProjectInfoSaveButton,
                    onPrimary = vm::submitEditMeta,
                    secondaryText = StringResources.ProjectInfoCancelButton,
                    onSecondary = vm::closeEditMetaDialog,
                )
            }
        }
    }

    if (inviteState.isOpen) {
        DialogCard(onDismiss = vm::closeInviteDialog) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    StringResources.ProjectInfoInviteDialogTitle,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = vm::refreshInvite, enabled = !inviteState.isLoading && !inviteState.isRefreshing) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = Palette.OnCard)
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                inviteState.isLoading || inviteState.isRefreshing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Palette.AccentBlue, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (inviteState.isRefreshing) StringResources.ProjectInfoInviteRefreshing else StringResources.ProjectInfoInviteLoading,
                            color = Palette.OnCard,
                            fontFamily = Roboto
                        )
                    }
                }
                inviteState.errorMessage != null -> Text(inviteState.errorMessage!!, color = Palette.AccentRed, fontFamily = Roboto, fontSize = 13.sp)
                else -> {
                    Text(StringResources.ProjectInfoInviteHint, color = Palette.FieldLabel, fontFamily = Roboto, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Palette.Background, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            inviteState.inviteCode.orEmpty(),
                            color = Palette.OnCard,
                            fontFamily = Roboto,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (inviteCopied) {
                            Text(
                                StringResources.ProjectInfoInviteCopied,
                                color = Palette.AccentBlue,
                                fontFamily = Roboto,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        TextButton(
                            onClick = {
                                inviteState.inviteCode?.takeIf { it.isNotBlank() }?.let { code ->
                                    clipboardManager.setText(AnnotatedString(code))
                                    inviteCopied = true
                                }
                            }
                        ) {
                            Text(StringResources.ProjectInfoInviteCopyButton, fontFamily = Roboto)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = vm::closeInviteDialog, modifier = Modifier.align(Alignment.End)) {
                Text(StringResources.ProjectInfoCloseButton, fontFamily = Roboto)
            }
        }
    }

    if (repoState.isOpen) {
        DialogCard(onDismiss = vm::closeRepoDialog) {
            Text(StringResources.CreateProjectRepoLinkDialogTitle, color = Palette.OnCard, fontFamily = Roboto, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            AppTextField(
                label = StringResources.CreateProjectRepoLinkLabel,
                value = repoState.link,
                onValueChange = vm::setRepoLink,
                placeholder = StringResources.CreateProjectRepoLinkPlaceholder,
            )
            repoState.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Palette.AccentRed, fontFamily = Roboto, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            if (repoState.isValidating || repoState.isSubmitting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Palette.AccentBlue, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (repoState.isValidating) StringResources.CreateProjectRepoLinkValidating else StringResources.ProjectInfoAddRepoSubmitting,
                        color = Palette.OnCard,
                        fontFamily = Roboto
                    )
                }
            } else {
                DialogButtons(
                    primaryText = StringResources.CreateProjectRepoLinkAdd,
                    onPrimary = vm::submitAddRepository,
                    secondaryText = StringResources.CreateProjectRepoLinkCancel,
                    onSecondary = vm::closeRepoDialog,
                )
            }
        }
    }

    if (deleteState.isOpen) {
        DialogCard(onDismiss = vm::closeDeleteProjectDialog) {
            Text(StringResources.ProjectInfoDeleteProjectDialogTitle, color = Palette.AccentRed, fontFamily = Roboto, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(StringResources.ProjectInfoDeleteProjectDialogBody, color = Palette.OnCard, fontFamily = Roboto, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            AppTextField(
                label = StringResources.ProjectInfoDeleteProjectConfirmLabel,
                value = deleteState.confirmText,
                onValueChange = vm::setDeleteProjectConfirmText,
                placeholder = StringResources.ProjectInfoDeleteProjectConfirmWord,
            )
            deleteState.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Palette.AccentRed, fontFamily = Roboto, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            if (deleteState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Palette.AccentBlue, strokeWidth = 2.dp)
            } else {
                DialogButtons(
                    primaryText = StringResources.ProjectInfoDeleteProjectConfirmButton,
                    onPrimary = vm::submitDeleteProject,
                    primaryEnabled = deleteState.isDeleteAllowed,
                    secondaryText = StringResources.ProjectInfoCancelButton,
                    onSecondary = vm::closeDeleteProjectDialog,
                )
            }
        }
    }

    pendingMemberDelete?.let { member ->
        val isSubmitting = member.id in ui.removingMemberIds
        ConfirmDeleteDialog(
            title = StringResources.ProjectInfoRemoveMemberDialogTitle,
            body = "${StringResources.ProjectInfoRemoveMemberDialogBody}\n${formatMember(member)}",
            confirmText = StringResources.ProjectInfoConfirmButton,
            isSubmitting = isSubmitting,
            onDismiss = { if (!isSubmitting) pendingMemberDelete = null },
            onConfirm = {
                if (!isSubmitting) {
                    vm.removeMember(member.id)
                }
            }
        )
    }
    pendingRepoDelete?.let { repo ->
        val isSubmitting = repo.id in ui.removingRepoIds
        ConfirmDeleteDialog(
            title = StringResources.ProjectInfoRemoveRepoDialogTitle,
            body = "${StringResources.ProjectInfoRemoveRepoDialogBody}\n${repo.link}",
            confirmText = StringResources.ProjectInfoConfirmButton,
            isSubmitting = isSubmitting,
            onDismiss = { if (!isSubmitting) pendingRepoDelete = null },
            onConfirm = {
                if (!isSubmitting) {
                    vm.removeRepository(repo.id)
                }
            }
        )
    }

    LaunchedEffect(ui.removingMemberIds) {
        pendingMemberDelete?.let { member ->
            if (member.id !in ui.removingMemberIds) pendingMemberDelete = null
        }
    }
    LaunchedEffect(ui.removingRepoIds) {
        pendingRepoDelete?.let { repo ->
            if (repo.id !in ui.removingRepoIds) pendingRepoDelete = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val project = ui.project
        when {
            ui.isLoading && project == null -> LoadingCard(StringResources.ProjectInfoLoading)
            project == null -> ErrorCard(ui.errorMessage ?: StringResources.LoadError, vm::load)
            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (ui.isRefreshing) {
                        Text(StringResources.ProjectInfoRefreshing, color = Palette.FieldLabel, fontFamily = Roboto, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    ProjectInfoContent(
                        project = project,
                        ui = ui,
                        canManageProject = project.isCreator || canManageAsCurator,
                        onOpenRepo = { link -> runCatching { uriHandler.openUri(link) }.onFailure { KLogger.e(TAG, it) { "openUri failed" } } },
                        onInvite = vm::openInviteDialog,
                        onEditMeta = vm::openEditMetaDialog,
                        onAddRepo = vm::openRepoDialog,
                        onDeleteProject = vm::openDeleteProjectDialog,
                        onRemoveMember = { memberId ->
                            pendingMemberDelete = project.members.firstOrNull { it.id == memberId }
                        },
                        onRemoveRepo = { repoId ->
                            pendingRepoDelete = project.repos.firstOrNull { it.id == repoId }
                        },
                        onDismissTeamError = vm::clearTeamActionError,
                        onDismissReposError = vm::clearReposActionError,
                        onDismissSettingsError = vm::clearSettingsActionError,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ProjectInfoContent(
    project: Project,
    ui: ProjectInfoViewModel.UiState,
    canManageProject: Boolean,
    onOpenRepo: (String) -> Unit,
    onInvite: () -> Unit,
    onEditMeta: () -> Unit,
    onAddRepo: () -> Unit,
    onDeleteProject: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onRemoveRepo: (String) -> Unit,
    onDismissTeamError: () -> Unit,
    onDismissReposError: () -> Unit,
    onDismissSettingsError: () -> Unit,
) {
    ProfileCard {
        SectionHeader(StringResources.ProjectInfoSectionInfo)
        Text(project.meta.title, color = Palette.OnCard, fontFamily = Roboto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text((project.meta.desc.ifBlank { StringResources.ProjectInfoDescriptionEmpty }), color = Palette.OnCard, fontFamily = Roboto, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        MetaRow(StringResources.ProjectInfoStatusLabel, statusLabel(project.status), statusColor(project.status))
        project.mark?.let { MetaRow(StringResources.ProjectInfoMarkLabel, it.toString()) }
        MetaRow(StringResources.ProjectInfoMembersLimitLabel, project.meta.maxMembersCount.toString())
        MetaRow(StringResources.ProjectInfoCreatedAtLabel, project.meta.createDate)
    }

    ProfileCard {
        CardTitle(StringResources.ProjectInfoSectionTeam) {
            if (canManageProject) {
                IconButton(onClick = onInvite) { Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Palette.OnCard) }
            }
        }
        ui.teamActionError?.let { InlineError(it, onDismissTeamError) }
        if (project.members.isEmpty()) {
            Text(StringResources.ProjectInfoMembersEmpty, color = Palette.FieldLabel, fontFamily = Roboto, modifier = Modifier.fillMaxWidth())
        } else {
            project.members.forEachIndexed { index, member ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatMember(member), color = Palette.OnCard, fontFamily = Roboto, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (index == 0) {
                        Text(StringResources.ProjectInfoCreatorBadge, color = Palette.FieldLabel, fontFamily = Roboto, fontSize = 12.sp)
                    }
                    if (canManageProject && index != 0) {
                        Spacer(Modifier.width(6.dp))
                        if (member.id in ui.removingMemberIds) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Palette.AccentBlue)
                        } else {
                            IconButton(onClick = { onRemoveMember(member.id) }) { Icon(Icons.Filled.Delete, contentDescription = null, tint = Palette.AccentRed) }
                        }
                    }
                }
                if (index < project.members.lastIndex) ThinDivider()
            }
        }
        if (canManageProject) {
            if (project.members.isNotEmpty()) ThinDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onInvite)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    StringResources.ProjectInfoAddMemberRow,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Palette.FieldLabel)
            }
        }
    }

    ProfileCard {
        CardTitle(StringResources.ProjectInfoSectionRepositories) {
            if (canManageProject) {
                IconButton(onClick = onAddRepo) { Icon(Icons.Filled.Add, contentDescription = null, tint = Palette.OnCard) }
            }
        }
        ui.reposActionError?.let { InlineError(it, onDismissReposError) }
        if (project.repos.isEmpty()) {
            Text(StringResources.ProjectInfoRepositoriesEmpty, color = Palette.FieldLabel, fontFamily = Roboto, modifier = Modifier.fillMaxWidth())
        } else {
            project.repos.forEachIndexed { index, repo ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = Palette.FieldLabel, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f).clickable { onOpenRepo(repo.link) }) {
                        Text((repo.title.takeIf { it.isNotBlank() } ?: repo.link.substringAfterLast('/')), color = Palette.OnCard, fontFamily = Roboto, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(repo.link, color = Palette.FieldLabel, fontFamily = Roboto, fontSize = 12.sp)
                    }
                    if (canManageProject) {
                        Spacer(Modifier.width(6.dp))
                        if (repo.id in ui.removingRepoIds) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Palette.AccentBlue)
                        } else {
                            IconButton(onClick = { onRemoveRepo(repo.id) }) { Icon(Icons.Filled.Delete, contentDescription = null, tint = Palette.AccentRed) }
                        }
                    }
                }
                if (index < project.repos.lastIndex) ThinDivider()
            }
        }
        if (canManageProject) {
            if (project.repos.isNotEmpty()) ThinDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddRepo)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    StringResources.ProjectInfoAddRepoRow,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.Add, contentDescription = null, tint = Palette.FieldLabel)
            }
        }
    }

    if (canManageProject) {
        ProfileCard {
            SectionHeader(StringResources.ProjectInfoSectionSettings)
            ui.settingsActionError?.let { InlineError(it, onDismissSettingsError) }
            TextButton(onClick = onEditMeta, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = Palette.OnCard)
                Spacer(Modifier.width(8.dp))
                Text(StringResources.ProjectInfoEditMetaButton, color = Palette.OnCard, fontFamily = Roboto)
            }
            TextButton(onClick = onDeleteProject, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Palette.AccentRed)
                Spacer(Modifier.width(8.dp))
                Text(StringResources.ProjectInfoDeleteProjectButton, color = Palette.AccentRed, fontFamily = Roboto)
            }
        }
    }
}

@Composable private fun LoadingCard(text: String) = ProfileCard(modifier = Modifier.widthIn(max = 560.dp)) {
    CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Palette.AccentBlue, strokeWidth = 3.dp)
    Spacer(Modifier.height(12.dp))
    Text(text, color = Palette.OnCard, fontFamily = Roboto)
}

@Composable private fun ErrorCard(text: String, onRetry: () -> Unit) = ProfileCard(modifier = Modifier.widthIn(max = 560.dp)) {
    Text(text, color = Palette.OnCard, fontFamily = Roboto, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
    PrimaryActionButton(text = StringResources.RetryButton, onClick = onRetry)
}

@Composable
private fun DialogCard(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Palette.CardSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
        }
    }
}

@Composable
private fun DialogButtons(
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    primaryEnabled: Boolean = true,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onSecondary, modifier = Modifier.weight(1f)) { Text(secondaryText, fontFamily = Roboto) }
        Box(modifier = Modifier.weight(1f)) {
            PrimaryActionButton(text = primaryText, onClick = onPrimary, enabled = primaryEnabled)
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    body: String,
    confirmText: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    DialogCard(onDismiss = onDismiss) {
        Text(title, color = Palette.AccentRed, fontFamily = Roboto, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(body, color = Palette.OnCard, fontFamily = Roboto, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Palette.AccentBlue, strokeWidth = 2.dp)
        } else {
            DialogButtons(
                primaryText = confirmText,
                onPrimary = onConfirm,
                secondaryText = StringResources.ProjectInfoCancelButton,
                onSecondary = onDismiss,
            )
        }
    }
}

@Composable
private fun CardTitle(title: String, trailing: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionHeader(title)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = Palette.OnCard, fontFamily = Roboto, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun MetaRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = Palette.OnCard) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = Palette.FieldLabel, fontFamily = Roboto, fontSize = 13.sp, modifier = Modifier.width(136.dp))
        Text(value, color = valueColor, fontFamily = Roboto, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun InlineError(text: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Palette.AccentRed.copy(alpha = 0.08f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Palette.AccentRed, fontFamily = Roboto, fontSize = 12.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text(StringResources.ProjectInfoErrorDismiss, color = Palette.AccentRed, fontFamily = Roboto) }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ThinDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Palette.Divider))
}

private fun formatMember(member: ProjectMember): String = listOf(member.lastName, member.firstName, member.secondName).filter { it.isNotBlank() }.joinToString(" ")

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

private const val TAG = "ProjectInfoScreen"
