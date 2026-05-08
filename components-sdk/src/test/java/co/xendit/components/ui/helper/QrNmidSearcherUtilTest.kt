package co.xendit.components.ui.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrNmidSearcherUtilsTest {
  @Test
  fun `test extract NMID from Tag 51 - National Standard`() {
    // PERBAIKAN: ID.CO.QRIS.WWW itu 15 karakter (0015), bukan 0014.
    // Jika 0014, maka huruf 'W' terakhir akan dianggap sebagai awal tag berikutnya dan MERUSAK parsing.
    val qrisData = "00020101021126610014COM.GO-JEK.WWW" +
        "01189360091435456007810210G5456007810303UMI51440014ID.CO.QRIS.WWW" +
        "0215ID10190000023280303UMI5204581253033605802ID5916Kantin Ibu Lilik6013Jakarta Pusat61051031062070703A0163044C6B"

    val result = QrNmidSearcherUtil.getNationalMerchantID(qrisData)
    assertEquals("ID1019000002328", result)
  }

  @Test
  fun `test extract NMID from Tag 51 - Simple Corrected`() {
    // PERBAIKAN: ID1234567890123 itu 15 karakter (ID + 13 angka). Jadi kodenya 0215.
    // Tag 51 total: 0015... (19) + 0215... (19) = 38.
    val qris = "00020101021226680016ID.CO.TELKOM.WWW" +
        "011893600898029003487302150001952900348730303UMI51440014ID.CO.QRIS.WWW" +
        "0215ID10221477541080303UMI5204549953033605802ID5924Ini Nayla Bukan Reinhart6013Jakarta Pusat61051026062370511100027433310611100027433310703A17630488A8"

    val result = QrNmidSearcherUtil.getNationalMerchantID(qris)
    assertEquals("ID1022147754108", result)
  }

  @Test
  fun `test return null when NMID not present`() {
    val invalidQris = "00020101021126160014ID.CO.QRIS.WWW520459995802ID5908Toko ABC63041234"
    val result = QrNmidSearcherUtil.getNationalMerchantID(invalidQris)
    assertNull(result)
  }

  @Test
  fun `test return null when data is corrupted`() {
    val result = QrNmidSearcherUtil.getNationalMerchantID("INI_BUKAN_QRIS_12345")
    assertNull(result)
  }

  @Test
  fun `test handle length mismatch gracefully`() {
    val result = QrNmidSearcherUtil.getNationalMerchantID("26990205ID123")
    assertNull(result)
  }
}
