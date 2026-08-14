package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.ui.XenditTestTags
import co.xendit.components.ui.helper.toLabelDisplay
import co.xendit.components.ui.style.xenditAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InstallmentPlanField(
  currency: String?,
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
  testTag: String = "",
) {
  val appearance = xenditAppearance
  val context = LocalContext.current
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier
      .fillMaxWidth()
      .then(
        if (testTag.isNotBlank()) Modifier.testTag(XenditTestTags.FORM_DROPDOWN_PREFIX + testTag)
        else Modifier.testTag(XenditTestTags.INSTALLMENT_PLAN_TRIGGER)
      )
  ) {
    XenditTextField(
      value = selectedPlanDesc,
      onValueChange = {},
      readOnly = true,
      label = label,
      placeholder = placeholder,
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier
        .menuAnchor(
          type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
          enabled = true
        )
        .fillMaxWidth(),
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
          text = {
            Text(
              plan.toLabelDisplay(context, currency).asString(),
              color = appearance.colorText
            )
          },
          onClick = {
            onPlanSelected(plan)
            expanded = false
          },
          modifier = Modifier.testTag(XenditTestTags.XENDIT_DROPDOWN_MENU_ITEM + plan.terms.toString())
        )
      }
    }
  }
}
