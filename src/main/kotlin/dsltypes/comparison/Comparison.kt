package dsltypes.comparison

import editor.objects.SimpleObject
import editor.objects.data.Pos
import editor.objects.triggers.counters.ItemCompareTrigger
import editor.rawstring.property.getOrThrow
import generator.TriggObject
import generator.onlyGroup
import provider.ObjectManager

typealias ConditionalBranch = ObjectManager<SimpleObject>.() -> Unit

/**
 * A comparison between 2 [GDValues][dsltypes.values.GDValue].
 * To create a comparison you need to use [ObjectManager.comp]
 * @see ObjectManager.comp
 */
class Comparison internal constructor(
    internal val obj: ItemCompareTrigger = ItemCompareTrigger(Pos.ZERO),
    val inlinedBranch: InlinedBranch? = null,
    thenClause: ConditionalBranch? = null,
    elseClause: ConditionalBranch? = null
): TriggObject<ItemCompareTrigger> {
    var thenClause: ConditionalBranch? = thenClause
        internal set

    var elseClause: ConditionalBranch? = elseClause
        internal set

    override fun createObject(context: ObjectManager<SimpleObject>): ItemCompareTrigger? {
        checkNotNull(this.thenClause) {
            "A then branch is required to build a condition"
        }

        if (this.inlinedBranch != null)
            return null

        return this.obj.apply {
            pos = context.getNextPos()
        }
    }

    override fun createAllObjects(context: ObjectManager<SimpleObject>) {
        checkNotNull(this.thenClause) {
            "A then branch is required to build a condition"
        }

        if (this.inlinedBranch != null) {
           when (this.inlinedBranch) {
               InlinedBranch.INLINE_THEN ->
                   this@Comparison.thenClause!!(context)

               InlinedBranch.INLINE_ELSE ->
                   if (this.elseClause != null)
                       this@Comparison.elseClause!!(context)
           }
        } else {
            onlyGroup(this.obj.succeededSpawnID.getOrThrow()) {
                this@Comparison.thenClause!!()
            }

            if (this.elseClause != null)
                onlyGroup(this.obj.failedSpawnID.getOrThrow()) {
                    this@Comparison.elseClause!!()
                }
        }

        val compTrigger = this.createObject(context)
        if (compTrigger != null)
            context.add(compTrigger)
    }

    fun then(thenClause: ConditionalBranch, groupAcquirer: () -> UInt): Comparison {
        check(this.thenClause == null) {
            "Cannot set a 'then conditional branch' twice !"
        }

        check(this.elseClause == null) {
            "Cannot set an 'then conditional branch' after an 'else' one !"
        }

        if (this.inlinedBranch == null)
            this.obj.succeededSpawnID.value = groupAcquirer()
        this.thenClause = thenClause
        return this
    }

    fun fail(elseClause: ConditionalBranch, groupAcquirer: () -> UInt): Comparison {
        check(this.elseClause == null) {
            "Cannot set a 'else conditional branch' twice !"
        }

        if (this.inlinedBranch == null)
            this.obj.failedSpawnID.value = groupAcquirer()
        this.elseClause = elseClause
        return this
    }

    /**
     * Represents the comparison branch that should be inlined.
     *
     * Geometry dash doesn't natively support comparing 2
     * numbers, and even if it did, that would do unnecessary
     * calculations, this is why a [Comparison] inlines
     * the executed branch
     */
    enum class InlinedBranch {
        /**
         * If the [Comparison] was precomputed and the result was
         * `true` then we inline the `then` branch
         */
        INLINE_THEN,
        /**
         * If the [Comparison] was precomputed and the result was
         * `false` then we inline the `else` branch
         */
        INLINE_ELSE;

        companion object {
            @JvmStatic
            fun fromEvaluation(result: Boolean): InlinedBranch =
                if (result)
                    INLINE_THEN
                else
                    INLINE_ELSE
        }
    }
}
