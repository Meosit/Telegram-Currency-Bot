package by.mksn.gae.easycurrbot.input

import by.mksn.gae.easycurrbot.AppConfig

/**
 * @author Mikhail Snitavets
 */
class CurrencyAliasMatcher(config: AppConfig) {

    private val currencyAliases: Map<String, String> = sequence {
        config.currencies.supported.forEach { currency ->
            currency.aliases.forEach { alias ->
                yield(alias.toLowerCase() to currency.code)
                if (alias.length > 1) {
                    yield(alias.switchKeyboardEnglishToRussian().toLowerCase() to currency.code)
                    yield(alias.switchKeyboardRussianToEnglish().toLowerCase() to currency.code)
                }
            }
        }
    }.toMap()

    /**
     * Returns the currency code of the provided alias
     * @throws IllegalArgumentException in case of unknown alias
     */
    fun matchToCode(alias: String): String =
            matchToCodeOrNull(alias) ?: throw IllegalArgumentException("Invalid alias '$alias'")


    /**
     * Returns the currency code of the provided alias or `null` in case of no match
     */
    fun matchToCodeOrNull(alias: String): String? =
            currencyAliases[alias.toLowerCase()]

    /**
     * Returns a string, contained of chars from the **russian** keyboard layout using the same *physical button* as a comparison criteria.
     * Buttons which do not contain any letter are remain the same (space, numbers, etc.)
     * Examples:
     * * "ghbdtn" -> "привет"
     * * "he,km" -> "рубль"
     * * "10 ,frcjd" -> "10 баксов"
     */
    private fun String.switchKeyboardEnglishToRussian() = mapChars {
        val idx = KEYBOARD_LETTERS_IN_ENGLISH.indexOf(it)
        if (idx == -1) it else KEYBOARD_LETTERS_IN_RUSSIAN[idx]
    }

    /**
     * Returns a string, contained of chars from the **english** keyboard layout using the same *physical button* as a comparison criteria.
     * Buttons which do not contain any letter are remain the same (space, numbers, etc.)
     * Examples:
     * * "вщддфкы" -> "dollars"
     * * "тшсу ещ ьууе нщг" -> "nice to meet you"
     * * "Хмфкшфиду_тфьуЪ" -> "{variable_name}"
     */
    private fun String.switchKeyboardRussianToEnglish() = mapChars {
        val idx = KEYBOARD_LETTERS_IN_RUSSIAN.indexOf(it)
        if (idx == -1) it else KEYBOARD_LETTERS_IN_ENGLISH[idx]
    }

    private inline fun String.mapChars(transform: (Char) -> Char): String {
        val chars = toCharArray()
        var index = 0
        for (char in chars)
            chars[index++] = transform(char)
        return String(chars)
    }

    val allAliasesRegex = currencyAliases.keys.asSequence()
            .map { alias -> Regex.escape(alias) }
            .distinct()
            .sortedByDescending { it.length }
            .joinToString("|", prefix = "(?iu)", postfix = "(?-iu)")
            .toRegex()

    companion object {
        // The 'native' typing mapping of russian symbols to the corresponding english symbols on the same keyboard buttons
        // (e.g. the person forgot to switch the keyboard to russian, but started typing and vice versa)
        // NOT ALL keyboard symbols included, just the buttons where at least one letter present (either russian or english)
        private const val KEYBOARD_LETTERS_IN_RUSSIAN = """йцукенгшщзхъфывапролджэячсмитьбюёЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮЁ"""
        private const val KEYBOARD_LETTERS_IN_ENGLISH = """qwertyuiop[]asdfghjkl;'zxcvbnm,.`QWERTYUIOP{}ASDFGHJKL:"ZXCVBNM<>~"""
    }
}