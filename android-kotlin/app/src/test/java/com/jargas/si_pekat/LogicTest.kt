package com.jargas.si_pekat

import com.jargas.si_pekat.core.CsvParser
import com.jargas.si_pekat.core.SheetColumns
import com.jargas.si_pekat.core.normalizeComparable
import com.jargas.si_pekat.core.sanitizeForSheet
import com.jargas.si_pekat.data.MeterOcrParser
import com.jargas.si_pekat.model.Ticket
import com.jargas.si_pekat.model.VisitCustomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uji logika murni (tanpa Android). Padanan perilaku Dart-nya diverifikasi saat porting. */
class LogicTest {
    @Test fun csvQuotedFieldsAndEscapedQuotes() {
        assertEquals(listOf("a@x.id", "p,w", "Budi \"B\" S"), CsvParser.parseRow("a@x.id,\"p,w\",\"Budi \"\"B\"\" S\""))
    }

    @Test fun csvLegacyModeKunjungan() {
        assertEquals(listOf("ab", "c"), CsvParser.parseRow("\"a\"\"b\",c", handleEscapedQuotes = false))
    }

    @Test fun sanitizeBlocksFormulaInjection() {
        assertEquals("BOCOR PIPA", sanitizeForSheet("  bocor pipa "))
        listOf("=1+1", "+62", "-5", "@x").forEach { assertTrue(sanitizeForSheet(it).startsWith("'")) }
    }

    @Test fun normalizeComparable() {
        assertEquals("budi santoso", normalizeComparable("  BUDI   Santoso "))
    }

    @Test fun ticketAliasesAndDefaults() {
        val t = Ticket.fromMap(mapOf("ticket" to "T-1", "status" to null, "idpel" to " 61 ", "telp" to "0812", "keluhan" to "gas bau", "latitude" to "-7.4"))
        assertEquals("61", t.idPelanggan)
        assertEquals("OPEN", t.status)
        assertEquals(-7.4, t.lat!!, 0.0)
    }

    @Test fun ocrPrefersReadingAtOrAbovePreviousStand() {
        assertEquals("12345", MeterOcrParser.parseRawText("12345 98765", previousStand = 12300).digits)
        assertEquals("98765", MeterOcrParser.parseRawText("12345 98765", previousStand = 98000).digits)
    }

    @Test fun ocrConfidenceDependsOnHistory() {
        assertFalse(MeterOcrParser.parseRawText("01234", previousStand = 50000).isConfident)
        assertTrue(MeterOcrParser.parseRawText("01234", previousStand = 1000).isConfident)
        assertFalse(MeterOcrParser.parseRawText("123", null).hasDigits)
    }

    @Test fun missingRequiredColumnsAreReported() {
        val missing = SheetColumns.missing(listOf("Foto_Ses_RP", "Status", "no mgrt"), listOf("STATUS_RP", "foto_ses_rp"))
        assertEquals(listOf("Foto_Ses_RP"), missing)
        assertTrue(SheetColumns.warning(emptyList()) == null)
        assertTrue(SheetColumns.warning(missing)!!.contains("Foto_Ses_RP"))
    }

    @Test fun visitListHidesArrearsBelowTwoMonths() {
        fun c(bln: Int) = VisitCustomer("1", "N", "A", "M", bln, 0.0, "Y")
        assertFalse(c(0).eligibleForVisit)   // BLN kosong diparse sebagai 0
        assertFalse(c(1).eligibleForVisit)
        assertTrue(c(2).eligibleForVisit)
        assertTrue(c(7).eligibleForVisit)
    }
}
