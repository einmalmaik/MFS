package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MessageEntity
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.TextParchment

@Composable
fun EditMessageDialog(
  message: MessageEntity,
  onConfirmEdit: (newContent: String) -> Unit,
  onDismiss: () -> Unit
) {
  var editInput by remember { mutableStateOf(message.content) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Nachricht bearbeiten & Zukunft neu berechnen", color = TextParchment) },
    text = {
      Column {
        Text(
          text = "Alle nachfolgenden Nachrichten und Antworten werden aus der Datenbank gelöscht. Die Geschichte wird ab diesem Punkt mit deinen neuen Worten fortgesetzt.",
          style = MaterialTheme.typography.bodySmall,
          color = CrimsonDanger
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = editInput,
          onValueChange = { editInput = it },
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SlateDark900,
            unfocusedContainerColor = SlateDark900,
            focusedTextColor = TextParchment,
            unfocusedTextColor = TextParchment,
            focusedBorderColor = AmberGoldPrimary,
            unfocusedBorderColor = SlateDark600
          ),
          shape = RoundedCornerShape(8.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val edited = editInput.trim()
          if (edited.isNotBlank()) {
            onConfirmEdit(edited)
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = AmberGoldPrimary,
          contentColor = SlateDark900
        )
      ) {
        Text("Neu berechnen & absenden", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Abbrechen", color = TextParchment)
      }
    },
    containerColor = SlateDark800
  )
}
