package com.pokedaisy.app.companion.data

/**
 * A Gen 5 DS game's text (Black / White), as its message files (a/0/0/2 ...) hold it: a section count, a line count,
 * then each section's offset; a section is its size and a table of (offset, length in u16s, flags) per line, then the
 * lines. A line's u16s are XORed with a key that starts at the last one XOR 0xFFFF and is rotated right by 3 bits a
 * code, walking backwards. The codes are UTF-16 but for a few: 0xFFFF ends a line, 0xFFFE breaks it, 0xF000 starts a
 * control code (its command, an argument count and the arguments, skipped), 0xF100 starts a 9-bit packed string, and
 * 0x246D / 0x246E are ♂ / ♀. Read from the player's own cart for names and the DEX's pages: none of the text is bundled.
 *
 * The Russian Black translation uses a custom glyph table: its UTF-16 values are not Unicode Cyrillic. The mapping
 * below was reconstructed by aligning the translation project's converted strings with its Russian source strings.
 * The same glyph table is used by the Russian species names, DEX descriptions, categories and ability names. In
 * particular, ª, «, º and » represent Cyrillic letters in the actual ROM, not punctuation.
 */
object Gen5Text {
    enum class RussianBank {
        SPECIES_NAMES,
        MOVE_NAMES,
        ITEM_NAMES,
        PLACE_NAMES,
        DEX_DESCRIPTIONS,
        DEX_CATEGORIES,
        ABILITIES,
    }

    /** Every line of the message file [file]'s first section (index 0 is the game's line 0). */
    fun lines(file: ByteArray, russianBank: RussianBank? = null): List<String> {
        if (file.size < 0x10) return emptyList()
        val count = u16(file, 2)
        val section = u32(file, 0x0C).toInt()
        val decoded = (0 until count).map { i ->
            val e = section + 4 + i * 8
            if (e + 8 > file.size) return@map ""
            val off = section + u32(file, e).toInt()
            val len = u16(file, e + 4)
            if (len == 0 || off < 0 || off + len * 2 > file.size) return@map ""
            val codes = IntArray(len) { u16(file, off + it * 2) }
            var key = codes[len - 1] xor 0xFFFF
            for (j in len - 1 downTo 0) {
                codes[j] = codes[j] xor key
                key = ((key ushr 3) or (key shl 13)) and 0xFFFF
            }
            decode(codes)
        }
        // The marker is distinctive to this Russian ROM's custom character table; English and other retail
        // localizations retain their normal Unicode text and must not have ASCII letters rewritten.
        if (russianBank == null || !isRussianBlackText(decoded)) return decoded
        return decoded.map(::mapRussian)
    }

    /**
     * Decode already-decrypted Gen 5 text codes. Internal visibility lets unit tests cover
     * character handling without needing a complete encrypted ROM message-file fixture.
     */
    internal fun decode(codes: IntArray): String {
        val sb = StringBuilder()
        var j = 0
        while (j < codes.size) {
            when (val c = codes[j]) {
                0xFFFF -> break
                0xFFFE -> sb.append('\n')
                0xF000 -> {
                    // {COMMAND args}: the command, its argument count, the arguments
                    j += 3 + codes.getOrElse(j + 2) { 0 }
                    continue
                }
                0xF100 -> {
                    // A packed string: 9 bits a character, low bits first, until 0x1FF.
                    var bits = 0L
                    var have = 0
                    var k = j + 1
                    while (k < codes.size) {
                        bits = bits or (codes[k].toLong() shl have)
                        have += 16
                        k++
                        while (have >= 9) {
                            val ch = (bits and 0x1FF).toInt()
                            bits = bits ushr 9
                            have -= 9
                            if (ch == 0x1FF) return sb.toString()
                            sb.append(ch.toChar())
                        }
                    }
                    return sb.toString()
                }
                0x246D -> sb.append('♂')
                0x246E -> sb.append('♀')
                else -> sb.append(c.toChar())
            }
            j++
        }
        return sb.toString()
    }

    internal fun isRussianBlackText(lines: List<String>): Boolean =
        lines.any { '\u00D0' in it }

    /** Apply the custom glyph table only when a string contains the Russian ROM's distinctive marker. */
    internal fun mapRussianIfDetected(text: String): String =
        if (isRussianBlackText(listOf(text))) mapRussian(text) else text

    internal fun mapRussian(text: String): String {
        return buildString(text.length) {
            text.forEach { append(RUSSIAN_COMMON[it.code] ?: it) }
        }
    }

    private val RUSSIAN_COMMON = mapOf(
        0x0041 to 'А', 0x0043 to 'С', 0x004F to 'О',
        0x0061 to 'а', 0x0063 to 'с', 0x006F to 'о',
        0x00C0 to 'Ё', 0x00C1 to 'Б', 0x00C2 to 'В', 0x00C3 to 'Г',
        0x00C4 to 'Д', 0x00C5 to 'Е', 0x00C7 to 'Ж', 0x00C8 to 'З',
        0x00C9 to 'И', 0x00CA to 'Й', 0x00CB to 'К', 0x00CC to 'Л',
        0x00CD to 'М', 0x00CE to 'Н', 0x00CF to 'Ц', 0x00D0 to 'П',
        0x00D1 to 'Р', 0x00D2 to 'Ю', 0x00D3 to 'Т', 0x00D4 to 'У',
        0x00D5 to 'Ф', 0x00D6 to 'Х', 0x00D8 to 'Ч', 0x00D9 to 'Ш',
        0x00DA to 'Щ', 0x00DC to 'Ы', 0x00DD to 'Ь', 0x00DE to 'Э',
        0x00E0 to 'ё', 0x00E1 to 'б', 0x00E2 to 'в', 0x00E3 to 'г',
        0x00E4 to 'д', 0x00E5 to 'е', 0x00E7 to 'ж', 0x00E8 to 'з',
        0x00E9 to 'и', 0x00EA to 'й', 0x00EB to 'к', 0x00EC to 'л',
        0x00ED to 'м', 0x00EE to 'н', 0x00EF to 'ц', 0x00F0 to 'п',
        0x00F1 to 'р', 0x00F2 to 'ю', 0x00F3 to 'т', 0x00F4 to 'у',
        0x00F5 to 'ф', 0x00F6 to 'х', 0x00F8 to 'ч', 0x00F9 to 'ш',
        0x00FA to 'щ', 0x00FB to 'ъ', 0x00FC to 'ы', 0x00FD to 'ь',
        0x00FE to 'э', 0x00AA to 'л', 0x00AB to 'м', 0x00BA to 'н', 0x00BB to 'ц',
        0x015E to 'Я', 0x015F to 'я',
    )

    private fun u16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
    private fun u32(b: ByteArray, o: Int) = (u16(b, o).toLong() or (u16(b, o + 2).toLong() shl 16))
}
