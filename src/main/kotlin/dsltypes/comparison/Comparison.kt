package dsltypes.comparison

import editor.objects.SimpleObject
import editor.objects.data.Pos
import editor.objects.triggers.counters.ItemCompareTrigger
import editor.rawstring.property.getOrThrow
import generator.TriggObject
import generator.onlyGroup
import provider.ObjectManager

typealias ConditionalBranch = ObjectManager<SimpleObject>.() -> Unit

class Comparison internal constructor(
    internal val obj: ItemCompareTrigger = ItemCompareTrigger(Pos.ZERO),
    val inlinedBranch: InlinedBranch? = null,
    thenClause: ConditionalBranch? = null,
    elseClause: ConditionalBranch? = null
): TriggObject<ItemCompareTrigger> {
    // TODO: Use these
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

fun Comparison.then(thenClause: ConditionalBranch, groupID: UInt): Comparison {
    require(this.thenClause == null) {
        "Cannot set a 'then conditional branch' twice !"
    }

    require(this.elseClause == null) {
        "Cannot set an 'then conditional branch' after an 'else' one !"
    }

    this.obj.succeededSpawnID.value = groupID
    this.thenClause = thenClause
    return this
}

fun Comparison.fail(elseClause: ConditionalBranch, groupID: UInt): Comparison {
    require(this.elseClause == null) {
        "Cannot set a 'else conditional branch' twice !"
    }

    this.obj.failedSpawnID.value = groupID
    this.elseClause = elseClause
    return this
}
