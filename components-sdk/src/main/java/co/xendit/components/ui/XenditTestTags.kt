package co.xendit.components.ui

/**
 * Centralized test tag constants for every user-interactive element in Xendit Components.
 *
 * Naming convention: `<domain>:<action_or_kind>:<identifier>` — human-readable, stable across
 * refactors, and always used via the constant (never hard-coded strings in tests).
 *
 * Rules:
 *  - When a composable accepts an `identifier: String` parameter (form key, channel code,
 *    province value, etc.), concatenate it with the appropriate [FORM_FIELD_PREFIX] /
 *    [CHANNEL_PREFIX] / [OPTION_PREFIX] helper at the call-site.
 *  - When a composable is a static single-instance button/sheet/etc., use the raw constant below.
 *  - Always prefer these constants to string literals — both in the composable and in tests.
 */
internal object XenditTestTags {

  // ── Prefixes for dynamically-tagged form fields & options ──────────────────────────
  /** Form input field (text, card, phone, expiry, cvc). Append the form property key. */
  const val FORM_FIELD_PREFIX = "form:field:"
  /** Form dropdown trigger anchor (Country, Province, Installment, generic Dropdown). Append the form property key. */
  const val FORM_DROPDOWN_PREFIX = "form:dropdown:"
  /** A selectable item inside a dropdown / sheet (country row, province row, dropdown menu item). Append the option value/code. */
  const val OPTION_PREFIX = "option:item:"
  /** Search input field inside a picker sheet (country search, province search). Append sheet kind. */
  const val PICKER_SEARCH_PREFIX = "picker:search:"
  /** Channel / payment method row in the selector (UI group header or specific channel card). Append channel code. */
  const val CHANNEL_PREFIX = "channel:row:"

  // ── PaymentContainerHost: global sheet/dialog & CTA ────────────────────────────────
  const val PAYMENT_SHEET = "payment:sheet:root"
  const val PAYMENT_DIALOG = "payment:dialog:root"
  const val DIALOG_SUBMIT_BUTTON = "button:submit:pay"
  const val DIALOG_ERROR_CLOSE_BUTTON = "button:error:close"
  const val GENERIC_HEADER_LEADING_BUTTON = "button:header:leading"
  const val GENERIC_HEADER_TRAILING_BUTTON = "button:header:trailing"

  // ── PaymentMethodsUI: channel selector UI groups & rows ───────────────────────────
  const val SAVE_CARD_CHECKBOX = "checkbox:save_card"
  const val AWAITING_PAYMENT_DIALOG_CLOSE = "button:awaiting_payment:close"

  // ── Google Pay ─────────────────────────────────────────────────────────────────────
  const val GOOGLE_PAY_BUTTON = "button:google_pay:pay"

  // ── Country & Province pickers ────────────────────────────────────────────────────
  const val COUNTRY_PICKER_TRIGGER = "picker:country:trigger"
  const val COUNTRY_PICKER_SHEET = "picker:country:sheet"
  const val COUNTRY_PICKER_SHEET_CLOSE = "button:picker:country:close"
  const val COUNTRY_PICKER_SEARCH = PICKER_SEARCH_PREFIX + "country"

  const val PROVINCE_PICKER_SHEET = "picker:province:sheet"
  const val PROVINCE_PICKER_SHEET_CLOSE = "button:picker:province:close"
  const val PROVINCE_PICKER_SEARCH = PICKER_SEARCH_PREFIX + "province"

  // ── Installment & generic dropdown ─────────────────────────────────────────────────
  const val INSTALLMENT_PLAN_TRIGGER = "form:dropdown:installment_plan"
  const val XENDIT_DROPDOWN_TRIGGER = "form:dropdown:anchor"
  const val XENDIT_DROPDOWN_MENU_ITEM = "dropdown:menu:item:"
  const val XENDIT_DROPDOWN_MENU = "dropdown:menu:sheet"

  // ── Phone number country-code chip ─────────────────────────────────────────────────
  const val PHONE_COUNTRY_CODE_TRIGGER = "picker:phone_country_code:trigger"
}
