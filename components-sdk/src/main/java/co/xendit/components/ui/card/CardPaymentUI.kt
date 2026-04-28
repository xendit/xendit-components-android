package co.xendit.components.ui.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffSession
import co.xendit.components.data.model.CardDetails
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.ui.components.DynamicForm
import co.xendit.components.ui.style.xenditAppearance

/** Card specific payment UI. This component specifically handles the Card payment flow. */
@Composable
internal fun CardPaymentUI(
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
        color = appearance.colorText,
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
        bffCardInfo = channelData.card
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
          text = "Save card information for future use",
          style = MaterialTheme.typography.bodyMedium,
          color = appearance.colorText,
          modifier = Modifier.padding(start = 8.dp)
        )
      }
    }
  }
}
