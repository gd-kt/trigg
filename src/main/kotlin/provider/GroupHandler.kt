package provider

import TriggDsl

@TriggDsl
interface GroupHandler {
    /**
     * The next group that is free for use.
     * To acquire a group use [acquireGroup]
     * @see acquireGroup
     */
    val nextFreeGroup: UInt

    /**
     * The next label that is free for use.
     * To acquire a label use [acquireLabel]
     * @see acquireLabel
     */
    val nextFreeLabel: UInt

    /**
     * The next register that is free for use.
     * To acquire a register use [acquireRegister] and to
     * release one use [releaseRegister]
     * @see acquireLabel
     * @see releaseRegister
     */
    val acquiredRegisters: Set<UInt>

    /**
     * Acquires a group ID, incrementing [nextFreeGroup]
     * @return the acquired group ID
     * @throws IllegalStateException if there aren't enough allocated groups to allocate a new one
     * @see nextFreeGroup
     */
    fun acquireGroup(): UInt

    /**
     * Acquires a label ID, incrementing [nextFreeLabel]
     * @return the acquired label ID
     * @throws IllegalStateException if there aren't enough allocated labels to allocate a new one
     */
    fun acquireLabel(): UInt

    /**
     * Acquires a new register to be used.
     * Registers are a bit different from labels since they can be released.
     *
     * For example, in a loop we need a **temporary** counter, when created
     * we [acquire][acquireRegister] a register and once the loops end, we can [release][releaseRegister] it
     * @return the acquired register
     * @throws IndexOutOfBoundsException
     * @see releaseRegister
     */
    fun acquireRegister(): UInt

    /**
     * Releases a given [register].
     * Registers are a bit different from labels since they can be released.
     *
     * For an example, look at [acquireRegister]'s KDoc
     * @throws IllegalArgumentException if the given [register] was never acquired
     * @see acquireRegister
     */
    fun releaseRegister(register: UInt)
}