package io.github.halilozel1903.openf1.query

/** A comparison OpenF1 understands in a query string, such as `lap_number<=3` or `date>=2025-12-07`. */
public enum class Operator(public val symbol: String) {
    /** `field=value`. Repeating it for one field matches any of the values. */
    Equal("="),

    /** `field>value`. */
    GreaterThan(">"),

    /** `field>=value`. */
    GreaterThanOrEqual(">="),

    /** `field<value`. */
    LessThan("<"),

    /** `field<=value`. */
    LessThanOrEqual("<="),
}

/**
 * One condition of a query: [field] compared to [value] with [operator]. Build them through the typed fields of a
 * query block, such as `lapNumber lte 10` in `client.laps { ... }`, rather than by hand.
 */
public data class Filter(
    /** The attribute name, such as `lap_number`. */
    public val field: String,
    public val operator: Operator,
    /** The value as OpenF1 expects it in the url, before percent-encoding. */
    public val value: String,
) {
    /**
     * This filter as one query string parameter, percent-encoded: `lap_number%3C=3` for `lap_number<=3`.
     * `<` and `>` are encoded the way browsers and HTTP libraries send them; the `=` stays as the separator.
     */
    public fun toQueryParameter(): String = buildString {
        append(encode(field))
        when (operator) {
            Operator.Equal -> append('=')
            Operator.GreaterThan -> append("%3E")
            Operator.GreaterThanOrEqual -> append("%3E=")
            Operator.LessThan -> append("%3C")
            Operator.LessThanOrEqual -> append("%3C=")
        }
        append(encode(value))
    }

    override fun toString(): String = "$field${operator.symbol}$value"

    internal companion object {
        private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:"

        /** RFC 3986 percent-encoding of a query component (UTF-8). `:` stays readable in timestamps. */
        fun encode(text: String): String = buildString {
            for (byte in text.encodeToByteArray()) {
                val c = byte.toInt() and 0xFF
                if (c < 0x80 && UNRESERVED.indexOf(c.toChar()) >= 0) {
                    append(c.toChar())
                } else {
                    append('%')
                    append(HEX[c shr 4])
                    append(HEX[c and 0x0F])
                }
            }
        }

        private const val HEX = "0123456789ABCDEF"
    }
}
