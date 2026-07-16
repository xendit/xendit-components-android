package co.xendit.components.data.model

import java.math.BigDecimal

internal enum class AmountAvailabilityStatus {
  AVAILABLE,
  BELOW_MIN,
  ABOVE_MAX
}

internal fun BffChannel.amountAvailabilityStatus(
  amount: BigDecimal?,
  sessionType: BffSessionType? = null
): AmountAvailabilityStatus {
  if (amount == null || sessionType == BffSessionType.SAVE) return AmountAvailabilityStatus.AVAILABLE
  return when {
    minAmount != null && amount.compareTo(minAmount) < 0 -> AmountAvailabilityStatus.BELOW_MIN
    maxAmount != null && amount.compareTo(maxAmount) > 0 -> AmountAvailabilityStatus.ABOVE_MAX
    else -> AmountAvailabilityStatus.AVAILABLE
  }
}

internal fun BffChannel.isAvailableForAmount(
  amount: BigDecimal?,
  sessionType: BffSessionType? = null
): Boolean {
  return amountAvailabilityStatus(amount, sessionType) == AmountAvailabilityStatus.AVAILABLE
}

internal fun List<BffChannel>.groupAmountAvailabilityStatus(
  amount: BigDecimal?,
  sessionType: BffSessionType? = null
): AmountAvailabilityStatus? {
  if (isEmpty() || amount == null) return null

  val firstStatus = first().amountAvailabilityStatus(amount, sessionType)

  return if (all { it.amountAvailabilityStatus(amount, sessionType) == firstStatus }) firstStatus else null
}
