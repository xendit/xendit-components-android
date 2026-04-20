package co.xendit.paymentsdk.ui.components.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import co.xendit.paymentsdk.data.model.InstallmentPlan
import co.xendit.paymentsdk.ui.helper.toLabelDisplay
import co.xendit.paymentsdk.ui.style.XenditAppearance
import co.xendit.paymentsdk.ui.style.xenditAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InstallmentPlanField(
  plans: List<InstallmentPlan>,
  selectedPlanDesc: String,
  onPlanSelected: (InstallmentPlan) -> Unit,
  modifier: Modifier = Modifier,
  label: String = "Installment Plan",
  placeholder: String = "Select an installment plan",
  isError: Boolean = false,
  errorMessage: String? = null,
  shape: Shape? = null,
  noBorder: Boolean = false,
) {
  val appearance = xenditAppearance
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier.fillMaxWidth()
  ) {
    XenditTextField(
      value = selectedPlanDesc,
      onValueChange = {},
      readOnly = true,
      label = label,
      placeholder = placeholder,
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier.menuAnchor().fillMaxWidth(),
      isError = isError,
      errorMessage = errorMessage,
      shape = shape,
      noBorder = noBorder
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      plans.forEach { plan ->
        DropdownMenuItem(
          text = { Text(plan.toLabelDisplay().asString(), color = appearance.colorText) },
          onClick = {
            onPlanSelected(plan)
            expanded = false
          }
        )
      }
    }
  }
}
