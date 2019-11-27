package helpers

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date

/**
 * Adds or subtracts the specified amount of time to the given calendar field,
 * based on the calendar's rules. For example, to subtract 5 days from
 * the current time of the calendar, you can achieve it by calling:
 * <p><code>add(Calendar.DAY_OF_MONTH, -5)</code>.
 *
 * @param field the calendar field.
 * @param amount the amount of date or time to be added to the field.
 * @see #roll(int,int)
 * @see #set(int,int)
 */
fun Date.add(field: Int, amount: Int): Date {
    return Calendar.getInstance().also {
        it.time = this
        it.add(field, amount)
    }.time
}

operator fun Date.plus(amount: Duration): Date {
    return Calendar.getInstance().also {
        it.time = this
        it.add(Calendar.SECOND, amount.seconds.toInt())
    }.time
}

operator fun Date.minus(amount: Duration): Date {
    return Calendar.getInstance().also {
        it.time = this
        it.add(Calendar.SECOND, -amount.seconds.toInt())
    }.time
}

operator fun Date.minus(amount: Date?) =
    Duration.between(this.toInstant(), (amount ?: this).toInstant())
