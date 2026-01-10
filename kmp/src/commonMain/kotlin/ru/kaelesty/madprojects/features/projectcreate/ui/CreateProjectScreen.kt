package ru.kaelesty.madprojects.features.projectcreate.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import domain.project.AvailableCurator
import org.koin.compose.viewmodel.koinViewModel
import ru.kaelesty.madprojects.features.projectcreate.sdk.ProjectCreateNavItem
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.ProfileCard
import ru.kaelesty.madprojects.ui.fields.AppDropdownField
import ru.kaelesty.madprojects.ui.fields.AppTextField
import ru.kaelesty.madprojects.ui.headers.ScreenHeader
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun CreateProjectScreen(
    navigator: ProjectCreateNavItem.Navigator,
) {
    val vm = koinViewModel<CreateProjectViewModel>()
    val state by vm.state.collectAsState()
    val curatorsState by vm.curatorsState.collectAsState()
    val groupsState by vm.groupsState.collectAsState()

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            if (event is CreateProjectViewModel.Event.Successful) {
                navigator.back()
            }
        }
    }

    if (state.isRepoDialogOpen) {
        RepoLinkDialog(
            state = state,
            onDismiss = vm::closeRepoDialog,
            onInputChange = vm::setRepoInput,
            onConfirm = vm::confirmRepoInput,
        )
    }

    Surface(color = Palette.Background) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = StringResources.CreateProjectTitle,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                ProfileCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 280.dp, max = 420.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        val curators = (curatorsState as? CreateProjectViewModel.ListState.Loaded)?.items.orEmpty()
                        val groups = (groupsState as? CreateProjectViewModel.ListState.Loaded)?.items.orEmpty()
                        val curatorPlaceholder = when (curatorsState) {
                            CreateProjectViewModel.ListState.Loading -> StringResources.CreateProjectCuratorsLoading
                            is CreateProjectViewModel.ListState.Error -> StringResources.LoadError
                            is CreateProjectViewModel.ListState.Loaded -> if (curators.isEmpty()) {
                                StringResources.CreateProjectCuratorsEmpty
                            } else {
                                StringResources.CreateProjectCuratorPlaceholder
                            }
                            CreateProjectViewModel.ListState.Disabled -> StringResources.CreateProjectCuratorPlaceholder
                        }
                        val groupPlaceholder = when (groupsState) {
                            CreateProjectViewModel.ListState.Disabled -> StringResources.CreateProjectGroupDisabled
                            CreateProjectViewModel.ListState.Loading -> StringResources.CreateProjectGroupsLoading
                            is CreateProjectViewModel.ListState.Error -> StringResources.LoadError
                            is CreateProjectViewModel.ListState.Loaded -> if (groups.isEmpty()) {
                                StringResources.CreateProjectGroupsEmpty
                            } else {
                                StringResources.CreateProjectGroupPlaceholder
                            }
                        }
                        val curatorEnabled = curatorsState is CreateProjectViewModel.ListState.Loaded && curators.isNotEmpty()
                        val groupEnabled = groupsState is CreateProjectViewModel.ListState.Loaded && groups.isNotEmpty()

                        AppTextField(
                            label = StringResources.CreateProjectNameLabel,
                            value = state.title,
                            onValueChange = vm::setTitle,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = StringResources.CreateProjectNamePlaceholder
                        )

                        Spacer(Modifier.height(12.dp))

                        AppTextField(
                            label = StringResources.CreateProjectDescLabel,
                            value = state.desc,
                            onValueChange = vm::setDesc,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = StringResources.CreateProjectDescPlaceholder,
                            singleLine = false,
                            minLines = 5
                        )

                        Spacer(Modifier.height(12.dp))

                        AppTextField(
                            label = StringResources.CreateProjectMaxMembersLabel,
                            value = state.maxMembersCount,
                            onValueChange = vm::setMaxMembersCount,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(Modifier.height(12.dp))

                        AppDropdownField(
                            label = StringResources.CreateProjectCuratorLabel,
                            options = curators,
                            selected = state.selectedCurator,
                            onSelect = vm::setCurator,
                            optionLabel = ::curatorLabel,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = curatorPlaceholder,
                            enabled = curatorEnabled
                        )

                        if (curatorsState is CreateProjectViewModel.ListState.Error) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = listErrorMessage((curatorsState as CreateProjectViewModel.ListState.Error).message),
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = Roboto,
                                textAlign = TextAlign.Center
                            )
                            TextButton(
                                onClick = vm::loadCurators,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Palette.AccentBlue
                                )
                            ) {
                                Text(
                                    text = StringResources.RetryButton,
                                    fontFamily = Roboto
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        AppDropdownField(
                            label = StringResources.CreateProjectGroupLabel,
                            options = groups,
                            selected = state.selectedGroup,
                            onSelect = vm::setGroup,
                            optionLabel = { it.title },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = groupPlaceholder,
                            enabled = groupEnabled
                        )

                        if (groupsState is CreateProjectViewModel.ListState.Error) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = listErrorMessage((groupsState as CreateProjectViewModel.ListState.Error).message),
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = Roboto,
                                textAlign = TextAlign.Center
                            )
                            TextButton(
                                onClick = vm::loadGroups,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Palette.AccentBlue
                                )
                            ) {
                                Text(
                                    text = StringResources.RetryButton,
                                    fontFamily = Roboto
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        RepoLinksSection(
                            repoLinks = state.repoLinks,
                            onAddClick = vm::openRepoDialog,
                            onRemoveClick = vm::removeRepoLink,
                        )

                        if (state.errorMessage != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = state.errorMessage ?: "",
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = Roboto,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        PrimaryActionButton(
                            text = StringResources.CreateProjectSubmit,
                            onClick = vm::submit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun curatorLabel(curator: AvailableCurator): String {
    return listOf(curator.lastName, curator.firstName, curator.secondName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

private fun listErrorMessage(message: String?): String {
    return message?.takeIf { it.isNotBlank() } ?: StringResources.LoadError
}

@Composable
private fun RepoLinksSection(
    repoLinks: List<String>,
    onAddClick: () -> Unit,
    onRemoveClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = StringResources.CreateProjectRepoLinksLabel,
            color = Palette.FieldLabel,
            fontFamily = Roboto,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))
        if (repoLinks.isEmpty()) {
            Text(
                text = StringResources.CreateProjectRepoLinksEmpty,
                color = Palette.FieldPlaceholder,
                fontFamily = Roboto,
                fontSize = 14.sp
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repoLinks.forEach { link ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = link,
                            modifier = Modifier.weight(1f),
                            color = Palette.FieldText,
                            fontFamily = Roboto,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { onRemoveClick(link) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Palette.AccentRed
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = Palette.AccentBlue
            )
        ) {
            Text(
                text = StringResources.CreateProjectRepoLinksAdd,
                fontFamily = Roboto
            )
        }
    }
}

@Composable
private fun RepoLinkDialog(
    state: CreateProjectViewModel.State,
    onDismiss: () -> Unit,
    onInputChange: (String) -> Unit,
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
                    text = StringResources.CreateProjectRepoLinkDialogTitle,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    label = StringResources.CreateProjectRepoLinkLabel,
                    value = state.repoInput,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = StringResources.CreateProjectRepoLinkPlaceholder
                )
                if (state.repoInputError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.repoInputError,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Roboto,
                        textAlign = TextAlign.Center
                    )
                }
                if (state.isRepoValidating) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Palette.AccentBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = StringResources.CreateProjectRepoLinkValidating,
                            color = Palette.FieldLabel,
                            fontFamily = Roboto,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                PrimaryActionButton(
                    text = StringResources.CreateProjectRepoLinkAdd,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRepoValidating
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isRepoValidating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Palette.AccentBlue
                    )
                ) {
                    Text(
                        text = StringResources.CreateProjectRepoLinkCancel,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}
