package ru.kaelesty.madprojects.features.project.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kaelesty.madprojects.ui.buttons.PrimaryActionButton
import ru.kaelesty.madprojects.ui.strings.StringResources
import ru.kaelesty.madprojects.ui.theme.Palette
import ru.kaelesty.madprojects.ui.theme.Roboto

@Composable
fun ProjectChatScreen(
    projectId: String,
    chatId: Int,
    chatTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = koinViewModel<ProjectChatViewModel>(
        key = "chat-$projectId-$chatId",
        parameters = { parametersOf(projectId, chatId) }
    )
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(min = 280.dp, max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ChatHeader(title = chatTitle, onBack = onBack)
            when {
                state.isLoading && state.messages.isEmpty() -> {
                    RowWithLoader(text = StringResources.ProjectMessengerChatLoading)
                }
                state.errorMessage != null && state.messages.isEmpty() -> {
                    Text(
                        text = state.errorMessage ?: StringResources.ProjectMessengerChatError,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Roboto,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryActionButton(
                        text = StringResources.RetryButton,
                        onClick = vm::reload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                state.messages.isEmpty() -> {
                    Text(
                        text = StringResources.ProjectMessengerChatEmpty,
                        color = Palette.FieldLabel,
                        fontFamily = Roboto,
                        fontSize = 14.sp
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages) { message ->
                    MessageBubble(message = message)
                }
            }
            ChatInputBar(
                value = state.input,
                onValueChange = vm::setInput,
                onSend = vm::sendMessage,
                enabled = !state.isSending
            )
        }
    }
}

@Composable
private fun ChatHeader(
    title: String,
    onBack: () -> Unit,
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
private fun MessageBubble(
    message: ProjectChatViewModel.MessageItem,
) {
    val bubbleColor = if (message.isMine) Palette.AccentBlue else Palette.FieldBg
    val textColor = if (message.isMine) Palette.ButtonTextOnPrimary else Palette.FieldText
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontFamily = Roboto,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp),
            placeholder = {
                Text(
                    text = StringResources.ProjectMessengerChatInputPlaceholder,
                    color = Palette.FieldPlaceholder,
                    fontFamily = Roboto
                )
            },
            textStyle = TextStyle(
                color = Palette.FieldText,
                fontFamily = Roboto,
                fontSize = 15.sp
            ),
            shape = shape,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (enabled && value.isNotBlank()) onSend() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Palette.FieldBg,
                unfocusedContainerColor = Palette.FieldBg,
                disabledContainerColor = Palette.FieldBg,
                focusedIndicatorColor = Palette.FieldBorderFocused,
                unfocusedIndicatorColor = Palette.FieldBorder,
                disabledIndicatorColor = Palette.FieldBorder,
                cursorColor = Palette.Cursor,
                focusedTextColor = Palette.FieldText,
                unfocusedTextColor = Palette.FieldText,
                focusedPlaceholderColor = Palette.FieldPlaceholder,
                unfocusedPlaceholderColor = Palette.FieldPlaceholder
            )
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = StringResources.ProjectMessengerChatSend,
                tint = if (enabled && value.isNotBlank()) Palette.AccentBlue else Palette.FieldLabel
            )
        }
    }
}
