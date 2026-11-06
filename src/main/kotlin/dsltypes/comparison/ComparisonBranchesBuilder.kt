package dsltypes.comparison

import Group
import Group.Companion.group
import TriggDsl
import dsltypes.values.ComparisonCreator
import editor.objects.SimpleObject
import generator.TriggFunction
import generator.TriggFunctionType
import generator.onlyGroup
import provider.ObjectManager
import provider.WrappedObjectManager

/**
 * A trigg dsl class used to build the branches of a [Comparison].
 * To create a [ComparisonBranchesBuilder], use [ObjectManager.comp]
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
        this.comp.then(thenClause) { this.ctx.acquireGroup() }
        return this
    }

    /**
     * Sets the `then` branch of this comparison to be a given [group]
     * @param group the group which value's is used to set the success group ID
     */
    infix fun then(group: Group): ComparisonBranchesBuilder {
        this.comp.then(group.value)
        return this
    }

    /**
     * Sets the `then` branch of this comparison to be a given [TriggFunction]
     * @param triggFunction the [TriggFunction] to spawn when the condition succeeds
     */
    infix fun then(triggFunction: TriggFunctionType): ComparisonBranchesBuilder =
        this.then(TriggFunction.getGroupID(triggFunction).group)

    /**
     * Sets the `else` branch of this comparison on the furthest branch.
     * It must be the last call in this builder.
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
        this.latestCompBranch.fail(elseClause) { this.ctx.acquireGroup() }
        return this
    }

    /**
     * Sets the `else` branch of this comparison to be a given [group]
     * @param group the group which value's is used to set the success group ID
     */
    infix fun fail(group: Group): ComparisonBranchesBuilder {
        this.comp.fail(group.value)
        return this
    }

    /**
     * Sets the `else` branch of this comparison to be a given [TriggFunction]
     * @param triggFunction the [TriggFunction] to spawn when the condition fails
     */
    infix fun fail(triggFunction: TriggFunctionType): ComparisonBranchesBuilder =
        this.fail(TriggFunction.getGroupID(triggFunction).group)


    private fun elseifHelper(elseifComp: ComparisonCreator, thenSetter: (compBuilder: ComparisonBranchesBuilder) -> Unit): ComparisonBranchesBuilder {
        check(this.comp.thenClause != null) {
            "Cannot make an else-if branch before a then branch"
        }

        check(this.latestCompBranch.elseClause == null) {
            "Cannot make an else-if branch after an else branch"
        }

        val compBuilder = ComparisonBranchesBuilder(this, elseifComp)
        thenSetter(compBuilder)

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
     * Sets the `else if` branch of this comparison on the furthest branch.
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
    fun elseif(elseifComp: ComparisonCreator, elseifBranch: ConditionalBranch): ComparisonBranchesBuilder =
        this.elseifHelper(elseifComp) { it.then(elseifBranch) }

    /**
     * Sets the `else if` branch of this comparison on the furthest branch.
     * @param group the group to spawn when this branch gets executed
     */
    fun elseif(elseifComp: ComparisonCreator, group: Group) =
        this.elseifHelper(elseifComp) { it.then(group) }

    /**
     * Sets the `else if` branch of this comparison on the furthest branch.
     * @param triggFunction the [TriggFunction] to spawn when this branch gets executed
     */
    fun elseif(elseifComp: ComparisonCreator, triggFunction: TriggFunctionType) =
        this.elseifHelper(elseifComp) { it.then(triggFunction) }

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
