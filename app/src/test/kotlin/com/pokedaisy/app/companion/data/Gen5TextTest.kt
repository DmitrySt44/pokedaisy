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
    fun linesMapsRussianBanksButLeavesEnglishTextAlone() {
        assertEquals(
            listOf("Покемон Семечко"),
            Gen5Text.lines(messageFile("Ðoëå«oº Cå«åøëo"), Gen5Text.RussianBank.DEX_CATEGORIES),
        )
        assertEquals(
            listOf("Seed Pokémon"),
            Gen5Text.lines(messageFile("Seed Pokémon"), Gen5Text.RussianBank.DEX_CATEGORIES),
        )
    }

    private fun messageFile(text: String): ByteArray {
        val codes = text.map { if (it == '\\n') 0xFFFE else it.code } + 0xFFFF
        val lineOffset = 0x1C
        val file = ByteArray(lineOffset + codes.size * 2)
        fun put16(offset: Int, value: Int) {
            file[offset] = value.toByte()
            file[offset + 1] = (value ushr 8).toByte()
        }
        fun put32(offset: Int, value: Int) {
            put16(offset, value and 0xFFFF)
            put16(offset + 2, value ushr 16)
        }
        put16(2, 1)
        put32(0x0C, 0x10)
        put32(0x14, lineOffset - 0x10)
        put16(0x18, codes.size)
        put16(0x1A, 0)
        // An encrypted last word of zero makes the rolling XOR key 0xFFFF for this fixture.
        codes.forEachIndexed { index, code ->
            put16(lineOffset + index * 2, if (index == codes.lastIndex) 0 else code xor 0xFFFF)
        }
        return file
    }

    @Test
    fun decodesRussianPokemonCategory() {
        assertEquals(
            "Покемон Семечко",
            Gen5Text.mapRussian("Ðoëå«oº Cå«åøëo"),
        )
    }

    @Test
    fun decodesRussianDexDescriptionAndSpecialLetters() {
        val encoded = "Îåëoóoñoå âñå«ş ðocªå ñoçäåºéş, oº\nñacóàó, ðoªôøaş ðéóaóåªýºüå âåúåcóâa\néè cå«åøëa ºa åão cðéºå."
        val expected = "Некоторое время после рождения, он\nрастёт, получая питательные вещества\nиз семечка на его спине."
        assertEquals(expected, Gen5Text.mapRussian(encoded))
    }

    @Test
    fun decodesRussianAbilityAndSpeciesName() {
        assertEquals(
            "Пахучесть",
            Gen5Text.mapRussian("Ðaöôøåcóý"),
        )
        assertEquals("Боевая броня", Gen5Text.mapRussian("Áoåâaş áñoîş"))
        assertEquals("Подзарядка", Gen5Text.mapRussian("Ðoäèañşäëa"))
        assertEquals("Покебол", Gen5Text.mapRussian("Ðoëåáoì"))
        assertEquals("Затрещина", Gen5Text.mapRussian("Èaóñåúéîa"))
        assertEquals("Таинственная Зона", Gen5Text.mapRussian("Óaéºcóâåººaş Èoºa"))
        assertEquals(
            "Бульбазавр",
            Gen5Text.mapRussian("Áôìýáaèaâñ"),
        )
    }

    @Test
    fun russianDetectionDoesNotTreatEnglishAsRussian() {
        assertFalse(Gen5Text.isRussianBlackText(listOf("Stench", "Seed Pokémon", "Árbol")))
        assertTrue(Gen5Text.isRussianBlackText(listOf("Ðoëå«oº Cå«åøëo")))
    }
}
