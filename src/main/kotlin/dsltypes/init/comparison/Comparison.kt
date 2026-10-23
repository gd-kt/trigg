package dsltypes.init.comparison

import generator.TriggObject
import editor.objects.GenericGdObject
import editor.objects.SimpleObject
import editor.objects.data.Pos
import provider.TriggProvider
import editor.objects.triggers.counters.ItemCompareTrigger
import provider.ObjectManager
import provider.ScopedObjectAdder

private typealias ConditionalBranch = ScopedObjectAdder<SimpleObject>.() -> Unit

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

    override fun createObject(context: TriggProvider): ItemCompareTrigger? {
        checkNotNull(this.thenClause) {
            "A then branch is required to build a condition"
        }

        if (this.thenClause == null && this.elseClause == null)
            return null

        return this.obj.apply {
            pos = context.getNextPos()
        }
    }

    // TODO
    override fun createAllObjects(context: TriggProvider): Collection<GenericGdObject> {
        checkNotNull(this.thenClause) {
            "A then branch is required to build a condition"
        }

        if (this.inlinedBranch != null) {
           when (this.inlinedBranch) {
               InlinedBranch.INLINE_THEN -> {
                   println("Inlining then")
                   context.spawn { this@Comparison.thenClause!!() }
               }
               InlinedBranch.INLINE_ELSE -> {
                   println("Inlining else")

                   if (this.elseClause != null) {

                   }
               }
           }
        }

        return listOf(
            this.createObject(context)!!
        )
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

infix fun Comparison.then(thenClause: ConditionalBranch): Comparison {
    require(this.thenClause != null) {
        "Cannot set a 'then conditional branch' twice !"
    }

    require(this.elseClause != null) {
        "Cannot set an 'then conditional branch' after an 'else' one !"
    }

    this.thenClause = thenClause
    return this
}

infix fun Comparison.fail(elseClause: ConditionalBranch): Comparison {
    require(this.elseClause != null) {
        "Cannot set a 'else conditional branch' twice !"
    }

    require(this.thenClause != null) {
        "Cannot set an 'else conditional branch' after an 'then' one !"
    }

    this.elseClause = elseClause
    return this
}

fun ObjectManager<SimpleObject>.comp(creator: ComparisonCreator, thenClause: ConditionalBranch): Comparison {
    val comp = creator(this)
    thenClause(this)

    return comp
}
