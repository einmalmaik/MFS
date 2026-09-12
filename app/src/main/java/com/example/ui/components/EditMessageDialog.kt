package com.example.ui.components

import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography
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
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MessageEntity

@Composable
fun EditMessageDialog(
  message: MessageEntity,
  onConfirmEdit: (newContent: String) -> Unit,
  onDismiss: () -> Unit
) {
  var editInput by remember { mutableStateOf(message.content) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Nachricht bearbeiten", color = DnaColors.OnSurface) },
    text = {
      Column {
        Text(
          text = "Alles danach wird gelöscht und ab hier neu erzählt.",
          style = MaterialTheme.typography.bodySmall,
          color = DnaColors.StatusDestructive
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = editInput,
          onValueChange = { editInput = it },
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DnaColors.Surface,
            unfocusedContainerColor = DnaColors.Surface,
            focusedTextColor = DnaColors.OnSurface,
            unfocusedTextColor = DnaColors.OnSurface,
            focusedBorderColor = DnaColors.Primary,
            unfocusedBorderColor = DnaColors.Border
          ),
          shape = RoundedCornerShape(8.dp)
        )
      }
    },
    confirmButton = {
      DnaButton(
        text = "Neu erzählen",
        onClick = {
          val edited = editInput.trim()
          if (edited.isNotBlank()) {
            onConfirmEdit(edited)
          }
        },
        variant = DnaButtonVariant.PRIMARY,
        testTag = "confirm_edit_message_button"
      )
    },
    dismissButton = {
      DnaButton(
        text = "Abbrechen",
        onClick = onDismiss,
        variant = DnaButtonVariant.GHOST,
        testTag = "cancel_edit_message_button"
      )
    },
    containerColor = DnaColors.SurfaceContainer
  )
}
