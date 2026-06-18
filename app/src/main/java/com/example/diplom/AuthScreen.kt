package com.example.diplom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.diplom.data.AuthState
import com.example.diplom.data.AuthViewModel
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiCream
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthModal(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPasswordRecovery by remember { mutableStateOf(false) }
    var recoveryStep by remember { mutableIntStateOf(1) }

    val state by viewModel.state
    val verificationState = state as? AuthState.VerificationRequired

    LaunchedEffect(state) {
        if (state is AuthState.Success) onSuccess()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = UmamiCream,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (verificationState != null) {
                        "Подтверждение почты"
                    } else if (isPasswordRecovery) {
                        if (recoveryStep == 1) "Восстановление пароля" else "Новый пароль"
                    } else if (isLogin) {
                        "Вход в профиль"
                    } else {
                        "Регистрация"
                    },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (verificationState != null) {
                    Text(
                        "На почту ${verificationState.email} отправлен код. Введите его для завершения регистрации.",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        placeholder = { Text("Код подтверждения", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    )
                    TextButton(
                        onClick = { viewModel.resendCode(verificationState.email) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Переотправить код", color = UmamiOrange, fontSize = 12.sp)
                    }
                } else {
                    if (isPasswordRecovery) {
                        if (recoveryStep == 1) {
                            Text(
                                "Введите email вашего аккаунта для получения кода восстановления.",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { Text("Email", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                        } else {
                            Text(
                                "Код отправлен на $email. Введите код и новый пароль.",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = verificationCode,
                                onValueChange = { verificationCode = it },
                                placeholder = { Text("Код из письма", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("Новый пароль", color = Color.Gray) },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = UmamiOrange
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = { Text("Повторите новый пароль", color = Color.Gray) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                    } else {
                        if (!isLogin) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = { Text("Логин", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("Имя", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("Email", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Пароль", color = Color.Gray) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = UmamiOrange
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        if (!isLogin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = { Text("Повторите пароль", color = Color.Gray) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                        }

                        if (isLogin) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    isPasswordRecovery = true
                                    recoveryStep = 1
                                },
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    "Забыли пароль?",
                                    color = Color.Gray,
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (state is AuthState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text((state as AuthState.Error).message, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        when {
                            verificationState != null -> {
                                viewModel.verifyEmail(
                                    email = verificationState.email,
                                    code = verificationCode,
                                    passwordForAutoLogin = verificationState.password
                                )
                            }
                            isPasswordRecovery -> {
                                if (recoveryStep == 1) {
                                    if (email.isNotBlank()) {
                                        viewModel.requestPasswordRecovery(email) {
                                            recoveryStep = 2
                                        }
                                    }
                                } else {
                                    if (verificationCode.isNotBlank() && password.isNotBlank() && password == confirmPassword) {
                                        viewModel.resetPassword(email, verificationCode, password) {
                                            isPasswordRecovery = false
                                            recoveryStep = 1
                                            android.widget.Toast.makeText(context, "Пароль успешно изменен", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            isLogin -> viewModel.login(email, password)
                            password == confirmPassword -> viewModel.register(username, name, email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange),
                    shape = RoundedCornerShape(28.dp),
                    enabled = state !is AuthState.Loading
                ) {
                    if (state is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            when {
                                verificationState != null -> "Подтвердить"
                                isPasswordRecovery -> if (recoveryStep == 1) "Отправить код" else "Сбросить пароль"
                                isLogin -> "Войти"
                                else -> "Создать аккаунт"
                            },
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                if (verificationState == null) {
                    if (isPasswordRecovery) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { 
                            isPasswordRecovery = false 
                            recoveryStep = 1
                        }) {
                            Text("Назад к входу", color = UmamiOrange, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (isLogin) "Ещё нет аккаунта?" else "Уже есть аккаунт?", color = Color.Gray, fontFamily = InterFontFamily, fontSize = 14.sp)
                        TextButton(onClick = { isLogin = !isLogin }) {
                            Text(if (isLogin) "Зарегистрироваться" else "Войти в профиль", color = UmamiOrange, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
