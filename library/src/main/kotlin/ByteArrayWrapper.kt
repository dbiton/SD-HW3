package il.ac.technion.cs.softwaredesign

data class ByteArrayWrapper(private val data: ByteArray?) {

    override fun equals(other: Any?): Boolean {
        return if (other !is ByteArrayWrapper) {
            false
        } else data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}