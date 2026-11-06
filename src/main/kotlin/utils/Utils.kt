package utils

import kotlin.time.Duration
import kotlin.time.DurationUnit

fun UIntRange.groupClamping(includeZero: Boolean = false): UIntRange {
    val firstValue = if (includeZero) 0u else 1u

    val first = this.first.coerceIn(firstValue, 9999u)
    return UIntRange(
        first,
        this.last.coerceAtMost(9999u)
    )
}

fun ClosedRange<UInt>.overlaps(other: ClosedRange<UInt>): Boolean =
    other.endInclusive >= this.start && other.start <= this.endInclusive

fun Duration.toFloat(unit: DurationUnit) =
    this.toDouble(unit).toFloat()
