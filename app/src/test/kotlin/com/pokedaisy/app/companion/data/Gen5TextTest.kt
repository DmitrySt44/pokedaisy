package com.pokedaisy.app.companion.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Gen5TextTest {
    @Test
    fun decodesCyrillicUtf16Characters() {
        assertEquals("Привет, Ёж!", Gen5Text.decode("Привет, Ёж!".map { it.code }.toIntArray() + 0xFFFF))
    }

    @Test
    fun decodesLineBreaksAndStopsAtEndMarker() {
        val codes = intArrayOf('Д'.code, 0xFFFE, 'Н'.code, 0xFFFF, '!'.code)
        assertEquals("Д\nН", Gen5Text.decode(codes))
    }

    @Test
    fun skipsControlCodeAndItsArguments() {
        val codes = intArrayOf('А'.code, 0xF000, 0x0001, 1, 0x1234, 'Б'.code, 0xFFFF)
        assertEquals("АБ", Gen5Text.decode(codes))
    }

    @Test
    fun decodesGenderSymbols() {
        val codes = intArrayOf(0x246D, 0x246E, 0xFFFF)
        assertEquals("♂♀", Gen5Text.decode(codes))
    }

    @Test
    fun decodesRussianPokemonCategory() {
        assertEquals(
            "Покемон Семечко",
            Gen5Text.mapRussian("Ðoëå«oº Cå«åøëo"),
        )
    }

    @Test
    fun decodesRussianDexDescriptionAndKeepsQuotationMarks() {
        val encoded = "Îåëoóoñoå âñåíş ðocìå ñoçäåîéş, oî\nñacóàó, ðoìôøaş ðéóaóåìýîüå âåúåcóâa\néè cåíåøëa îa åão cðéîå."
        val expected = "Некоторое время после рождения, он\nрастёт, получая питательные вещества\nиз семечка на его спине."
        assertEquals(expected, Gen5Text.mapRussian(encoded))
        assertEquals(
            "«Хулиган Морей»",
            Gen5Text.mapRussian("«Öôìéãaî Íoñåê»"),
        )
    }

    @Test
    fun decodesRussianAbilityAndSpeciesName() {
        assertEquals(
            "ПАХУЧЕСТЬ",
            Gen5Text.mapRussian("ÐAÖÔØÅCÓÝ"),
        )
        assertEquals(
            "Бульбазавр",
            Gen5Text.mapRussian("Áôìýáaèaâñ"),
        )
    }

    @Test
    fun russianDetectionDoesNotTreatEnglishAsRussian() {
        assertFalse(Gen5Text.isRussianBlackText(listOf("Stench", "Seed Pokémon")))
        assertTrue(Gen5Text.isRussianBlackText(listOf("Ðoëå«oº Cå«åøëo")))
    }
}
