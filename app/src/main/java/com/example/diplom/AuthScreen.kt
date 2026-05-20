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
                    text = if (verificationState != null) "Подтверждение почты" else if (isLogin) "Вход в профиль" else "Регистрация",
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Забыли пароль?", color = Color.Gray, fontFamily = InterFontFamily, fontSize = 14.sp)
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
