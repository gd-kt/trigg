package fr.geming400.gddotkt.trigg

data class LabelSpace(var memory: UInt = 100u, var script: UInt = 100u, var baseOffset: UInt = 0u) {
    init {
        require(this.memory >= 1u)
        require(this.script >= 1u)
    }

    fun toImmutable(): ImmutableLabelSpace = ImmutableLabelSpace(this.memory, this.script, this.baseOffset)
}

data class ImmutableLabelSpace(val memory: UInt, val script: UInt, val baseOffset: UInt)
