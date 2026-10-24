package dsltypes.comparison

import TriggDsl
import dsltypes.values.ComparisonCreator
import editor.objects.SimpleObject
import provider.ObjectManager
import provider.WrappedObjectManager

/**
 * A trigg dsl class used to build the branches of a [Comparison]
 */
@TriggDsl
class ComparisonBranchesBuilder(val ctx: ObjectManager<SimpleObject>, comp: ComparisonCreator): WrappedObjectManager<SimpleObject>(ctx) {
    private val comp = comp(this.ctx)

    fun then(thenClause: ConditionalBranch): ComparisonBranchesBuilder {
        this.comp.then(thenClause, this.ctx.acquireGroup())

        return this
    }

    infix fun fail(elseClause: ConditionalBranch): ComparisonBranchesBuilder {
        this.comp.fail(elseClause, this.ctx.acquireGroup())
        return this
    }

    fun elseif(elseifComp: ComparisonCreator, elseifBranch: ConditionalBranch): ComparisonBranchesBuilder {
        val elseClause = checkNotNull(this.comp.elseClause) { "You must declare else if clauses after the else branch" }
        this.comp.elseClause = { compCtx ->
            comp(elseifComp) {
                then(elseifBranch)
                fail(elseClause)
            }
        }

        return this
    }

    fun build(): Comparison =
        this.comp
}

fun ObjectManager<SimpleObject>.comp(comp: ComparisonCreator, builder: ComparisonBranchesBuilder.() -> Unit): Comparison {
    val compBuilder = ComparisonBranchesBuilder(this, comp)
    builder(compBuilder)

    val comp = compBuilder.build()
    comp.createAllObjects(this)

    return comp
}
