package ru.kaelesty.madprojects.features.auth.ui

import ru.kaelesty.madprojects.features.auth.domain.UserType

object RegisterStrings {
    const val Title = "Регистрация"
    const val UsernameLabel = "Имя пользователя"
    const val UsernamePlaceholder = "your-cool_nickname"
    const val LastNameLabel = "Фамилия"
    const val FirstNameLabel = "Имя"
    const val SecondNameLabel = "Отчество"
    const val UserTypeLabel = "Тип пользователя"
    const val GroupLabel = "Группа"
    const val GroupPlaceholder = "4215"
    const val PositionLabel = "Должность"
    const val PositionPlaceholder = "Преподаватель"
    const val EmailLabel = "Email"
    const val EmailPlaceholder = "cheeseonthemoon@email.com"
    const val PasswordLabel = "Пароль"
    const val PasswordPlaceholder = ""
    const val NextButton = "Далее"
    const val BackButton = "Назад"
    const val RegisterButton = "Зарегистрироваться"

    fun userTypeLabel(type: UserType): String = when (type) {
        UserType.Common -> "Студент"
        UserType.Curator -> "Куратор"
    }

    fun errorMessage(error: RegisterViewModel.ValidationError?): String? = when (error) {
        RegisterViewModel.ValidationError.UsernameTooShort -> "Имя пользователя минимум 3 символа"
        RegisterViewModel.ValidationError.InvalidLastName -> "Фамилия: только русские буквы, минимум 2"
        RegisterViewModel.ValidationError.InvalidFirstName -> "Имя: только русские буквы, минимум 2"
        RegisterViewModel.ValidationError.InvalidSecondName -> "Отчество: только русские буквы, минимум 2"
        RegisterViewModel.ValidationError.InvalidGroup -> "Некорректная группа"
        RegisterViewModel.ValidationError.InvalidPosition -> "Должность: только русские буквы, минимум 2"
        RegisterViewModel.ValidationError.EmptyEmail -> "Введите e-mail"
        RegisterViewModel.ValidationError.InvalidEmail -> "Некорректный e-mail"
        RegisterViewModel.ValidationError.EmptyPassword -> "Введите пароль"
        RegisterViewModel.ValidationError.InvalidPassword ->
            "Пароль: минимум 10 символов, строчные/прописные/спецсимвол"
        RegisterViewModel.ValidationError.EmailTaken -> "Этот e-mail уже зарегистрирован"
        RegisterViewModel.ValidationError.UsernameTaken -> "Имя пользователя уже занято"
        RegisterViewModel.ValidationError.WeakPassword -> "Пароль не соответствует требованиям"
        RegisterViewModel.ValidationError.Unavailable -> "Сервер недоступен, попробуйте позже"
        null -> null
    }
}
