package ru.kaelesty.madprojects.features.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kaelesty.madprojects.features.project.domain.GithubActivityUseCase
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.utils.KLogger
import domain.profile.SharedProfile
import shared_domain.entities.BranchCommits
import shared_domain.entities.RepoView

class ProjectViewModel(
    private val projectId: String,
    private val githubUseCase: GithubActivityUseCase,
    private val str: StringResources = StringResources,
) : ViewModel() {

    data class RepoBranchOption(
        val repoName: String,
        val branchName: String,
        val sha: String,
    ) {
        val displayName: String
            get() = "$repoName / ${branchName.substringAfterLast('/')}"
    }

    data class MonthOption(
        val year: Int,
        val month: Int,
    ) {
        val label: String
            get() = "${month.toString().padStart(2, '0')}.$year"
    }

    data class CommitItem(
        val sha: String,
        val message: String,
        val rawDate: String,
        val formattedDate: String,
        val authorName: String,
    )

    sealed interface RepoBranchesState {
        data object Loading : RepoBranchesState
        data class Loaded(val options: List<RepoBranchOption>) : RepoBranchesState
        data class Error(val message: String?) : RepoBranchesState
    }

    sealed interface CommitsState {
        data object Idle : CommitsState
        data object Loading : CommitsState
        data class Loaded(val commits: List<CommitItem>) : CommitsState
        data class Error(val message: String?) : CommitsState
    }

    private val _repoBranchesState = MutableStateFlow<RepoBranchesState>(RepoBranchesState.Loading)
    val repoBranchesState = _repoBranchesState.asStateFlow()

    private val _selectedBranch = MutableStateFlow<RepoBranchOption?>(null)
    val selectedBranch = _selectedBranch.asStateFlow()

    private val _monthOptions = MutableStateFlow<List<MonthOption>>(emptyList())
    val monthOptions = _monthOptions.asStateFlow()

    private val _selectedMonth = MutableStateFlow<MonthOption?>(null)
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _commitsState = MutableStateFlow<CommitsState>(CommitsState.Idle)
    val commitsState = _commitsState.asStateFlow()

    private var loadedCommits: List<CommitItem> = emptyList()

    init {
        loadRepoBranches()
    }

    fun loadRepoBranches() {
        KLogger.d(TAG) { "loadRepoBranches start" }
        _repoBranchesState.value = RepoBranchesState.Loading
        viewModelScope.launch {
            when (val result = githubUseCase.loadRepoBranches(projectId)) {
                is GithubActivityUseCase.RepoBranchesResult.Success -> {
                    val options = result.repos.toBranchOptions()
                    KLogger.i(TAG) { "loadRepoBranches success: options=${options.size}" }
                    _repoBranchesState.value = RepoBranchesState.Loaded(options)
                }
                is GithubActivityUseCase.RepoBranchesResult.Fail -> {
                    val message = result.message?.takeIf { it.isNotBlank() } ?: str.LoadError
                    KLogger.w(TAG) { "loadRepoBranches failed: status=${result.status} message=$message" }
                    _repoBranchesState.value = RepoBranchesState.Error(message)
                }
            }
        }
    }

    fun selectBranch(option: RepoBranchOption) {
        KLogger.d(TAG) { "selectBranch: repo=${option.repoName} sha=${option.sha}" }
        _selectedBranch.value = option
        _monthOptions.value = emptyList()
        _selectedMonth.value = null
        loadedCommits = emptyList()
        loadCommits(option)
    }

    fun selectMonth(option: MonthOption) {
        KLogger.d(TAG) { "selectMonth: ${option.label}" }
        _selectedMonth.value = option
        applyMonthFilter()
    }

    private fun loadCommits(option: RepoBranchOption) {
        _commitsState.value = CommitsState.Loading
        viewModelScope.launch {
            when (val result = githubUseCase.loadRepoBranchContent(option.sha, option.repoName)) {
                is GithubActivityUseCase.BranchCommitsResult.Success -> {
                    loadedCommits = result.commits.toCommitItems()
                    val options = loadedCommits.toMonthOptions()
                    _monthOptions.value = options
                    _selectedMonth.value = options.firstOrNull()
                    KLogger.i(TAG) { "loadCommits success: count=${loadedCommits.size} months=${options.size}" }
                    applyMonthFilter()
                }
                is GithubActivityUseCase.BranchCommitsResult.Fail -> {
                    val message = result.message?.takeIf { it.isNotBlank() } ?: str.LoadError
                    KLogger.w(TAG) { "loadCommits failed: status=${result.status} message=$message" }
                    _commitsState.value = CommitsState.Error(message)
                }
            }
        }
    }

    private fun List<RepoView>.toBranchOptions(): List<RepoBranchOption> {
        return flatMap { repo ->
            repo.repoBranches.map { branch ->
                RepoBranchOption(
                    repoName = repo.name,
                    branchName = branch.name,
                    sha = branch.sha,
                )
            }
        }
    }

    private fun applyMonthFilter() {
        val month = _selectedMonth.value
        val filtered = if (month == null) {
            emptyList()
        } else {
            loadedCommits.filter { commit ->
                val parsed = parseYearMonth(commit.rawDate)
                parsed?.first == month.year && parsed.second == month.month
            }
        }
        _commitsState.value = CommitsState.Loaded(filtered)
    }

    private fun List<CommitItem>.toMonthOptions(): List<MonthOption> {
        return mapNotNull { commit ->
            parseYearMonth(commit.rawDate)?.let { (year, month) -> MonthOption(year, month) }
        }
            .distinctBy { it.year to it.month }
            .sortedWith(compareByDescending<MonthOption> { it.year }.thenByDescending { it.month })
    }

    private fun BranchCommits.toCommitItems(): List<CommitItem> {
        val authorNames = authors.mapNotNull { commiter ->
            val githubId = commiter.githubMeta?.githubId
            val profile = commiter.profile
            if (githubId == null || profile == null) return@mapNotNull null
            val name = formatAuthorName(profile)
            if (name.isBlank()) return@mapNotNull null
            githubId to name
        }.toMap()
        return commits.map { commit ->
            CommitItem(
                sha = commit.sha,
                message = commit.message,
                rawDate = commit.date,
                formattedDate = formatCommitDate(commit.date),
                authorName = authorNames[commit.authorGithubId] ?: str.ProjectGithubCommitAuthorUnknown,
            )
        }
    }

    private fun formatAuthorName(profile: SharedProfile): String {
        return listOf(profile.lastName, profile.firstName, profile.secondName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun parseYearMonth(date: String): Pair<Int, Int>? {
        if (date.length < 7) return null
        val year = date.substring(0, 4).toIntOrNull() ?: return null
        val month = date.substring(5, 7).toIntOrNull() ?: return null
        if (month !in 1..12) return null
        return year to month
    }

    private fun formatCommitDate(raw: String): String {
        val parts = raw.split("T")
        if (parts.size < 2) return raw
        val dateParts = parts[0].split("-")
        if (dateParts.size != 3) return raw
        val time = parts[1].take(5)
        if (time.length < 5) return raw
        val (year, month, day) = dateParts
        return "$day.$month.$year $time"
    }

    private companion object {
        private const val TAG = "ProjectViewModel"
    }
}
