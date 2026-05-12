package com.example.diplom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange

@Composable
fun ReportDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val reasons = listOf("Спам", "Оскорбления", "Неуместный контент", "Нарушение авторских прав", "Другое")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пожаловаться", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily) },
        text = {
            Column {
                Text("Выберите причину:", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                reasons.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { reason = r }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(r, fontSize = 14.sp, fontFamily = InterFontFamily)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Дополнительное описание (опционально)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (reason.isNotBlank()) onSubmit(reason, description) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
            ) {
                Text("Отправить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
