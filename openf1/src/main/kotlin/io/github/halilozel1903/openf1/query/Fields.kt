package io.github.halilozel1903.openf1.query

import kotlin.time.Instant

/**
 * A filterable attribute of an endpoint. Each query block exposes only the fields its endpoint has, with the value
 * type of that attribute, so `client.laps { lapNumber lte 10 }` compiles and `client.laps { compound eq "SOFT" }`
 * does not.
 */
public sealed class Field<V>(
    /** The attribute name in OpenF1, such as `lap_number`. */
    public val name: String,
    private val sink: MutableList<Filter>,
) {
    internal abstract fun encode(value: V): String

    internal fun add(operator: Operator, value: V) {
        sink += Filter(name, operator, encode(value))
    }

    /** `name=value`. */
    public infix fun eq(value: V): Unit = add(Operator.Equal, value)

    /** Matches any of [values]: `name=a&name=b`. */
    public fun oneOf(vararg values: V): Unit = values.forEach { add(Operator.Equal, it) }

    /** Matches any of [values]: `name=a&name=b`. */
    public infix fun oneOf(values: Iterable<V>): Unit = values.forEach { add(Operator.Equal, it) }
}

/** A field that can also be compared: numbers, dates and text. */
public sealed class ComparableField<V>(name: String, sink: MutableList<Filter>) : Field<V>(name, sink) {
    /** `name>value`. */
    public infix fun gt(value: V): Unit = add(Operator.GreaterThan, value)

    /** `name>=value`. */
    public infix fun gte(value: V): Unit = add(Operator.GreaterThanOrEqual, value)

    /** `name<value`. */
    public infix fun lt(value: V): Unit = add(Operator.LessThan, value)

    /** `name<=value`. */
    public infix fun lte(value: V): Unit = add(Operator.LessThanOrEqual, value)

    /** `name>=from&name<=to`, both ends included. */
    public fun between(from: V, to: V) {
        gte(from)
        lte(to)
    }
}

/** A whole number attribute, such as `lap_number` or `driver_number`. */
public open class IntField internal constructor(name: String, sink: MutableList<Filter>) : ComparableField<Int>(name, sink) {
    override fun encode(value: Int): String = value.toString()

    /** `name>=first&name<=last`. */
    public infix fun inRange(range: IntRange): Unit = between(range.first, range.last)
}

/** `meeting_key` or `session_key`: a number, or `latest` for the current or most recent one. */
public class KeyField internal constructor(name: String, private val sink: MutableList<Filter>) : IntField(name, sink) {
    /** `name=latest`: the current or most recent meeting or session. Live sessions need a sponsor token. */
    public fun latest() {
        sink += Filter(name, Operator.Equal, "latest")
    }
}

/** A decimal attribute, such as `lap_duration` (seconds) or `track_temperature` (°C). */
public class DoubleField internal constructor(name: String, sink: MutableList<Filter>) : ComparableField<Double>(name, sink) {
    override fun encode(value: Double): String =
        if (value == kotlin.math.floor(value) && kotlin.math.abs(value) < 1e15) value.toLong().toString() else value.toString()

    /** `name>=start&name<=endInclusive`. */
    public infix fun inRange(range: ClosedFloatingPointRange<Double>): Unit = between(range.start, range.endInclusive)
}

/** A text attribute, such as `team_name` or `compound`. Matching is exact and case sensitive. */
public class StringField internal constructor(name: String, sink: MutableList<Filter>) : ComparableField<String>(name, sink) {
    override fun encode(value: String): String = value
}

/** A true or false attribute, such as `is_pit_out_lap`. */
public class BooleanField internal constructor(name: String, sink: MutableList<Filter>) : Field<Boolean>(name, sink) {
    override fun encode(value: Boolean): String = value.toString()
}

/**
 * A UTC timestamp attribute, such as `date` or `date_start`. Compare it with an [Instant], or with any text OpenF1
 * accepts: `2025-12-07`, `2025-12-07T14:20:00` or `2025-12-07T14:20:00+00:00`.
 */
public class DateField internal constructor(name: String, sink: MutableList<Filter>) : ComparableField<String>(name, sink) {
    override fun encode(value: String): String = value

    /** `name>value`. */
    public infix fun gt(value: Instant): Unit = gt(value.toString())

    /** `name>=value`. */
    public infix fun gte(value: Instant): Unit = gte(value.toString())

    /** `name<value`. */
    public infix fun lt(value: Instant): Unit = lt(value.toString())

    /** `name<=value`. */
    public infix fun lte(value: Instant): Unit = lte(value.toString())

    /** `name>=from&name<=to`. */
    public fun between(from: Instant, to: Instant): Unit = between(from.toString(), to.toString())
}
