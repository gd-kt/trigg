package provider

import TriggDsl

@TriggDsl
interface GroupHandler {
    /**
     * The next group that is free for use. It starts at `1`
     * To acquire this group use [acquireGroup]
     * @see acquireGroup
     */
    val nextFreeGroup: UInt
    val nextFreeLabel: UInt

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
}