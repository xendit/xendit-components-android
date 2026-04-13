package co.xendit.paymentsdk.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.xendit.paymentsdk.BuildConfig
import co.xendit.paymentsdk.data.model.BffChannel
import co.xendit.paymentsdk.data.model.BffSession
import co.xendit.paymentsdk.data.model.CardDetails
import co.xendit.paymentsdk.data.model.ChannelFormField
import co.xendit.paymentsdk.data.model.InstallmentPlan
import co.xendit.paymentsdk.ui.components.DynamicForm
import co.xendit.paymentsdk.ui.style.XenditAppearance
import co.xendit.paymentsdk.ui.style.xenditAppearance

/** Card specific payment UI. This component specifically handles the Card payment flow. */
@Composable
fun CardPaymentUI(
  session: BffSession?,
  channelData: BffChannel,
  cardDetails: CardDetails?,
  installmentPlans: List<InstallmentPlan>?,
  onCardNumberChanged: (String) -> Unit,
  onFormStateChanged: (Map<String, String>, List<ChannelFormField>, Boolean) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier,
  showSaveCheckbox: Boolean = false
) {
  val appearance = xenditAppearance
  val formValues = remember { mutableStateMapOf<String, String>() }
  val isSaveChecked = remember { mutableStateOf(false) }
  val visibleFields = remember { mutableStateOf<List<ChannelFormField>>(emptyList()) }

  Column(
    modifier = modifier
      .padding(horizontal = 16.dp)
      .padding(bottom = 12.dp)
  ) {
    // Instructions if any
    channelData.instructions?.forEach { instruction ->
      Text(
        text = instruction,
        style = MaterialTheme.typography.headlineMedium,
        color = appearance.colorText ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
    }
    // Dynamic Form for Card
    channelData.form?.let { fields ->
      DynamicForm(
        fields = fields,
        cardDetails = cardDetails,
        installmentPlans = installmentPlans,
        onValuesChanged = { updatedValues ->
          formValues.clear()
          formValues.putAll(updatedValues)
          onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
        },
        onCardNumberChanged = onCardNumberChanged,
        onVisibleFieldsChanged = {
          visibleFields.value = it
          onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
        },
        bffCardInfo = channelData.card,
        mockData = mapOf(
          "card_details.card_number" to "4000000000002503", //4000000000002503 4111111111111111
          "card_details.cardholder_first_name" to "arga",
          "card_details.cardholder_last_name" to "argaar",
          "card_details.cardholder_email" to "arga@gmail.com",
          "card_details.cardholder_phone_number" to "81342532569"
        )
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (showSaveCheckbox) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
      ) {
        Checkbox(
          checked = isSaveChecked.value,
          onCheckedChange = {
            isSaveChecked.value = it
            onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
          }
        )
        Text(
          text = "Save for faster payments",
          style = MaterialTheme.typography.bodyMedium,
          color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(start = 8.dp)
        )
      }
    }
  }
}
