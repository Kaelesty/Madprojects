package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.cards.ProfileCard
import ru.kaelesty.madprojects.ui.fields.AppTextField
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun ProjectMessengerScreen(
    projectId: String,
    modifier: Modifier = Modifier,
) {
    val vm = koinViewModel<ProjectMessengerViewModel>(parameters = { parametersOf(projectId) })
    val state by vm.chatsState.collectAsState()
    val createChatState by vm.createChatDialogState.collectAsState()
    val (selectedChat, setSelectedChat) = remember { mutableStateOf<ProjectMessengerViewModel.ChatItem?>(null) }

    if (createChatState.isOpen) {
        CreateChatDialog(
            state = createChatState,
            onDismiss = vm::closeCreateChatDialog,
            onTitleChange = vm::setCreateChatTitle,
            onConfirm = vm::submitCreateChat,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (selectedChat != null) {
            ProjectChatScreen(
                projectId = projectId,
                chatId = selectedChat.id,
                chatTitle = selectedChat.title,
                onBack = { setSelectedChat(null) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(min = 280.dp, max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MessengerHeader(onAddClick = vm::openCreateChatDialog)
                when (state) {
                    ProjectMessengerViewModel.ChatsState.Loading -> {
                        ProfileCard {
                            RowWithLoader(text = StringResources.ProjectMessengerChatsLoading)
                        }
                    }
                    is ProjectMessengerViewModel.ChatsState.Error -> {
                        ProfileCard {
                            Text(
                                text = (state as ProjectMessengerViewModel.ChatsState.Error).message
                                    ?: StringResources.ProjectMessengerChatsError,
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = Roboto,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is ProjectMessengerViewModel.ChatsState.Loaded -> {
                        val chats = (state as ProjectMessengerViewModel.ChatsState.Loaded).chats
                        if (chats.isEmpty()) {
                            ProfileCard {
                                Text(
                                    text = StringResources.ProjectMessengerChatsEmpty,
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
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                items(chats) { chat ->
                                    ChatCard(
                                        chat = chat,
                                        onClick = { setSelectedChat(chat) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessengerHeader(onAddClick: () -> Unit) {
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
            text = StringResources.ProjectMessengerTitle,
            color = Palette.OnCard,
            fontFamily = Roboto,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = StringResources.ProjectMessengerAddChat,
                tint = Palette.OnCard
            )
        }
    }
}

@Composable
private fun RowWithLoader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.width(18.dp).height(18.dp),
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
private fun ChatCard(
    chat: ProjectMessengerViewModel.ChatItem,
    onClick: () -> Unit,
) {
    ProfileCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatAvatar(title = chat.title)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chat.title,
                    color = Palette.OnCard,
                    fontFamily = Roboto,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chat.lastMessage,
                    color = Palette.FieldLabel,
                    fontFamily = Roboto,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (chat.unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                UnreadBadge(count = chat.unreadCount)
            }
        }
    }
}

@Composable
private fun ChatAvatar(title: String) {
    val letter = title.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Palette.AccentBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Palette.AccentBlue,
            fontFamily = Roboto,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    val label = if (count > 9) "9+" else count.toString()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.AccentBlue)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Palette.ButtonTextOnPrimary,
            fontFamily = Roboto,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CreateChatDialog(
    state: ProjectMessengerViewModel.CreateChatDialogState,
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
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = StringResources.ProjectMessengerCreateTitle,
                    style = TextStyle(
                        color = Palette.OnCard,
                        fontFamily = Roboto,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    label = StringResources.ProjectMessengerCreateLabel,
                    value = state.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = StringResources.ProjectMessengerCreatePlaceholder
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
                    RowWithLoader(text = StringResources.ProjectMessengerCreateProcessing)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryActionButton(
                    text = StringResources.ProjectMessengerCreateButton,
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
                        text = StringResources.ProjectMessengerCreateCancel,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}
