package com.example.ui.components

import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog

@Composable
fun EditMessageDialog(
  message: MessageEntity,
  onConfirmEdit: (newContent: String) -> Unit,
  onDismiss: () -> Unit
) {
  var editInput by remember { mutableStateOf(message.content) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DnaColors.SurfaceContainer,
      border = BorderStroke(1.dp, DnaColors.Border),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Nachricht bearbeiten",
          fontFamily = DnaTypography.ManropeFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 18.sp,
          color = DnaColors.OnSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Alles danach wird gelöscht und ab hier neu erzählt.",
          style = MaterialTheme.typography.bodySmall,
          fontFamily = DnaTypography.InterFamily,
          color = DnaColors.StatusDestructive
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = editInput,
          onValueChange = { editInput = it },
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DnaColors.Surface,
            unfocusedContainerColor = DnaColors.Surface,
            focusedTextColor = DnaColors.OnSurface,
            unfocusedTextColor = DnaColors.OnSurface,
            focusedBorderColor = DnaColors.Primary,
            unfocusedBorderColor = DnaColors.Border
          ),
          shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DnaColors.BorderFaint)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          DnaButton(
            text = "Abbrechen",
            onClick = onDismiss,
            variant = DnaButtonVariant.GHOST,
            modifier = Modifier.padding(end = 8.dp),
            testTag = "cancel_edit_message_button"
          )

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
        }
      }
    }
  }
}
