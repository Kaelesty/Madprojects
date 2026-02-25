package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kaelesty.madprojects.ui.cards.ProfileCard
import ru.kaelesty.madprojects.ui.fields.AppDropdownField
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun ProjectGithubScreen(
    projectId: String,
    modifier: Modifier = Modifier,
) {
    val vm = koinViewModel<ProjectViewModel>(parameters = { parametersOf(projectId) })
    val branchesState by vm.repoBranchesState.collectAsState()
    val selectedBranch by vm.selectedBranch.collectAsState()
    val monthOptions by vm.monthOptions.collectAsState()
    val selectedMonth by vm.selectedMonth.collectAsState()
    val commitsState by vm.commitsState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 420.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GithubFiltersCard(
                branchesState = branchesState,
                selectedBranch = selectedBranch,
                monthOptions = monthOptions,
                selectedMonth = selectedMonth,
                commitsState = commitsState,
                onSelectBranch = vm::selectBranch,
                onSelectMonth = vm::selectMonth,
            )
            GithubCalendarCard(
                selectedMonth = selectedMonth,
                commitsState = commitsState,
            )
            GithubCommitsCard(commitsState = commitsState)
        }
    }
}

@Composable
private fun GithubFiltersCard(
    branchesState: ProjectViewModel.RepoBranchesState,
    selectedBranch: ProjectViewModel.RepoBranchOption?,
    monthOptions: List<ProjectViewModel.MonthOption>,
    selectedMonth: ProjectViewModel.MonthOption?,
    commitsState: ProjectViewModel.CommitsState,
    onSelectBranch: (ProjectViewModel.RepoBranchOption) -> Unit,
    onSelectMonth: (ProjectViewModel.MonthOption) -> Unit,
) {
    val options = (branchesState as? ProjectViewModel.RepoBranchesState.Loaded)?.options.orEmpty()
    val dropdownPlaceholder = when (branchesState) {
        ProjectViewModel.RepoBranchesState.Loading -> StringResources.ProjectGithubBranchesLoading
        is ProjectViewModel.RepoBranchesState.Error -> StringResources.LoadError
        is ProjectViewModel.RepoBranchesState.Loaded -> if (options.isEmpty()) {
            StringResources.ProjectGithubBranchesEmpty
        } else {
            StringResources.ProjectGithubBranchPlaceholder
        }
    }
    val dropdownEnabled = branchesState is ProjectViewModel.RepoBranchesState.Loaded && options.isNotEmpty()
    val monthPlaceholder = when {
        selectedBranch == null -> StringResources.ProjectGithubMonthSelectBranch
        commitsState is ProjectViewModel.CommitsState.Loading -> StringResources.ProjectGithubMonthLoading
        commitsState is ProjectViewModel.CommitsState.Error -> StringResources.LoadError
        commitsState is ProjectViewModel.CommitsState.Loaded && monthOptions.isEmpty() -> StringResources.ProjectGithubMonthEmpty
        else -> StringResources.ProjectGithubMonthPlaceholder
    }
    val monthEnabled = commitsState is ProjectViewModel.CommitsState.Loaded && monthOptions.isNotEmpty()

    ProfileCard {
        SectionTitle(text = StringResources.ProjectGithubFiltersTitle)
        Spacer(Modifier.height(12.dp))
        AppDropdownField(
            label = StringResources.ProjectGithubBranchLabel,
            options = options,
            selected = selectedBranch,
            onSelect = onSelectBranch,
            optionLabel = { it.displayName },
            modifier = Modifier.fillMaxWidth(),
            placeholder = dropdownPlaceholder,
            enabled = dropdownEnabled
        )

        if (branchesState is ProjectViewModel.RepoBranchesState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = branchesState.message ?: StringResources.LoadError,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error,
                fontFamily = Roboto,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(12.dp))
        AppDropdownField(
            label = StringResources.ProjectGithubMonthLabel,
            options = monthOptions,
            selected = selectedMonth,
            onSelect = onSelectMonth,
            optionLabel = { it.label },
            modifier = Modifier.fillMaxWidth(),
            placeholder = monthPlaceholder,
            enabled = monthEnabled
        )
    }
}

@Composable
private fun GithubCalendarCard(
    selectedMonth: ProjectViewModel.MonthOption?,
    commitsState: ProjectViewModel.CommitsState,
) {
    ProfileCard {
        SectionTitle(text = StringResources.ProjectGithubCalendarTitle)
        Spacer(Modifier.height(12.dp))
        when (commitsState) {
            ProjectViewModel.CommitsState.Idle -> {
                Text(
                    text = StringResources.ProjectGithubMonthSelectBranch,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ProjectViewModel.CommitsState.Loading -> {
                RowWithLoader(text = StringResources.ProjectGithubCommitsLoading)
            }
            is ProjectViewModel.CommitsState.Error -> {
                Text(
                    text = commitsState.message ?: StringResources.LoadError,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = Roboto,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is ProjectViewModel.CommitsState.Loaded -> {
                if (selectedMonth == null) {
                    Text(
                        text = StringResources.ProjectGithubMonthPlaceholder,
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CommitCalendar(
                        month = selectedMonth,
                        commits = commitsState.commits,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GithubCommitsCard(
    commitsState: ProjectViewModel.CommitsState,
) {
    ProfileCard {
        SectionTitle(text = StringResources.ProjectGithubCommitsTitle)
        Spacer(Modifier.height(12.dp))
        when (commitsState) {
            ProjectViewModel.CommitsState.Idle -> {
                Text(
                    text = StringResources.ProjectGithubCommitsPlaceholder,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 14.sp
                )
            }
            ProjectViewModel.CommitsState.Loading -> {
                RowWithLoader(text = StringResources.ProjectGithubCommitsLoading)
            }
            is ProjectViewModel.CommitsState.Error -> {
                Text(
                    text = commitsState.message ?: StringResources.LoadError,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = Roboto,
                    fontSize = 14.sp
                )
            }
            is ProjectViewModel.CommitsState.Loaded -> {
                if (commitsState.commits.isEmpty()) {
                    Text(
                        text = StringResources.ProjectGithubCommitsEmpty,
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        itemsIndexed(commitsState.commits) { index, commit ->
                            CommitListItem(
                                commit = commit,
                                showDivider = index < commitsState.commits.lastIndex
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

@Composable
private fun CommitListItem(
    commit: ProjectViewModel.CommitItem,
    showDivider: Boolean,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(shape)
                .background(Palette.CardSurface, shape)
                .border(1.dp, Palette.FieldBorder, shape)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Palette.AccentBlue)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commit.message,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${StringResources.ProjectGithubCommitAuthorLabel}${commit.authorName}",
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 12.sp
                )
                Text(
                    text = commit.formattedDate,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 12.sp
                )
            }
        }
        if (showDivider) {
            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.height(8.dp))
        }
    }
}
