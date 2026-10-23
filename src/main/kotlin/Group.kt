import editor.objects.propertycontainers.TriggerProperties

class Group(val value: UInt) : Number(), Comparable<Group> {
    companion object {
        inline val Int.group
            get() = Group(this.toUInt())

        inline val UInt.group
            get() = Group(this)
    }

    init {
        require(this.value <= TriggerProperties.MAX_GROUPS) {
            "Group ${this.value} must be <= ${TriggerProperties.MAX_GROUPS}"
        }
    }

    override fun toByte(): Byte =
        this.value.toByte()

    override fun toShort(): Short =
        this.value.toShort()

    override fun toInt(): Int =
        this.value.toInt()

    override fun toLong(): Long =
        this.value.toLong()

    override fun toFloat(): Float =
        this.value.toFloat()

    override fun toDouble(): Double =
        this.value.toDouble()

    fun toUByte(): UByte =
        this.value.toUByte()

    fun toUShort(): UShort =
        this.value.toUShort()

    fun toUInt(): UInt =
        this.value

    fun toULong(): ULong =
        this.value.toULong()

    override fun compareTo(other: Group): Int =
        this.value.compareTo(other.value)

    operator fun plus(other: Group): Group =
        Group(this.value + other.value)

    operator fun minus(other: Group): Group =
        Group(this.value - other.value)

    operator fun times(other: Group): Group =
        Group(this.value * other.value)

    operator fun div(other: Group): Group =
        Group(this.value / other.value)

    operator fun rem(other: Group): Group =
        Group(this.value % other.value)

    operator fun plus(other: UInt): Group =
        Group(this.value + other)

    operator fun minus(other: UInt): Group =
        Group(this.value - other)

    operator fun times(other: UInt): Group =
        Group(this.value * other)

    operator fun div(other: UInt): Group =
        Group(this.value / other)

    operator fun rem(other: UInt): Group =
        Group(this.value % other)

    operator fun unaryPlus(): Group =
        this

    operator fun inc(): Group =
        Group(this.value + 1u)

    operator fun dec(): Group =
        Group(this.value - 1u)

    override fun equals(other: Any?): Boolean =
        other is Group && this.value == other.value

    override fun hashCode(): Int =
        this.value.hashCode()

    override fun toString(): String =
        this.value.toString()
}
