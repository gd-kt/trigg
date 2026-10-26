package dsltypes.comparison

import TriggDsl
import dsltypes.values.ComparisonCreator
import editor.objects.SimpleObject
import generator.onlyGroup
import provider.ObjectManager
import provider.WrappedObjectManager

/**
 * A trigg dsl class used to build the branches of a [Comparison]
 */
@TriggDsl
class ComparisonBranchesBuilder(val ctx: ObjectManager<SimpleObject>, comp: ComparisonCreator): WrappedObjectManager<SimpleObject>(ctx) {
    private val comp = comp(this.ctx)
    private var latestCompBranch: Comparison = this.comp

    /**
     * Sets the `then` branch of this comparison.
     *
     * For example
     * ```kotlin
     * comp(attempts() greaterThan const(15)) {
     *     then {
     *         // ...
     *     }
     * }
     * ```
     *
     * is equivalent to:
     * ```kotlin
     * if (atts >= 15) {
     *     // ...
     * }
     * ```
     */
    fun then(thenClause: ConditionalBranch): ComparisonBranchesBuilder {
        this.comp.then(thenClause, this.ctx.acquireGroup())
        return this
    }

    /**
     * Sets the `else` branch of this comparison on the furthest branch.
     *
     * For example
     * ```kotlin
     * comp(attempts() greaterThan const(15)) {
     *     then {
     *         // ...
     *     }
     *
     *     fail {
     *         // ...
     *     }
     * }
     * ```
     *
     * is equivalent to:
     * ```kotlin
     * if (atts >= 15) {
     *     // ...
     * } else {
     *     // ...
     * }
     * ```
     */
    infix fun fail(elseClause: ConditionalBranch): ComparisonBranchesBuilder {
        this.latestCompBranch.fail(elseClause, this.ctx.acquireGroup())
        return this
    }

    /**
     * Sets the `else if` branch of this comparison on the furthest branch.
     *
     *
     * For example
     * ```kotlin
     * comp(attempts() greaterThan const(15)) {
     *     then {
     *         // ...
     *     }
     *
     *     elseif(attempts() equalsTo const(7)) {
     *         // ...
     *     }
     * }
     * ```
     *
     * is equivalent to:
     * ```kotlin
     * if (atts >= 15) {
     *     // ...
     * } else if (atts == 7) {
     *     // ...
     * }
     *
     * // Internally it gets translated to:
     * if (atts >= 15) {
     *     // ...
     * } else {
     *     if (atts == 7) {
     *         // ...
     *     }
     * }
     * ```
     */
    fun elseif(elseifComp: ComparisonCreator, elseifBranch: ConditionalBranch): ComparisonBranchesBuilder {
        val compBuilder = ComparisonBranchesBuilder(this, elseifComp)
        compBuilder.then(elseifBranch)

        val latestComp = this@ComparisonBranchesBuilder.latestCompBranch
        latestComp.elseClause = { compCtx ->
            val acquiredGroup = this.ctx.acquireGroup()
            latestComp.obj.failedSpawnID.value = acquiredGroup

            onlyGroup(acquiredGroup) {
                val comp = compBuilder.build()
                comp.createAllObjects(this)
            }
        }

        this.latestCompBranch = compBuilder.comp
        return this
    }

    /**
     * Builds the comparison of this [ComparisonBranchesBuilder]
     */
    fun build(): Comparison =
        this.comp
}

/**
 * Creates a comparison between 2 [GDValues][dsltypes.values.GDValue]
 * @param comp the comparison. It is created like this: `gdValue1 operator gdValue2`.
 *             For example, to compare if the number of attempts is equal to `8`, you can do `attempts() equalsTo const(8)`
 * @param builder the builder used to set the conditional branches of the comparison
 * @return the created comparison
 * @see dsltypes.values.GDValue
 * @see Comparison
 */
fun ObjectManager<SimpleObject>.comp(comp: ComparisonCreator, builder: ComparisonBranchesBuilder.() -> Unit): Comparison {
    val compBuilder = ComparisonBranchesBuilder(this, comp)
    builder(compBuilder)

    val comp = compBuilder.build()
    comp.createAllObjects(this)

    return comp
}
