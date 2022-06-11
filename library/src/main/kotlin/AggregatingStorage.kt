package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture

class AggregatingStorage(f: SecureStorageFactory) {
    private var factory: SecureStorageFactory? = f
    private var table: SecureStorage? = null
    private var bytesPerEntry: Int? = null
    private val bytesPerChunk = 100

    fun open(name: String) {
        table = factory?.open(name.toByteArray())?.get()
    }

    fun setBytesPerEntry(n: Int) {
        bytesPerEntry = n
    }

    fun write(key: Int, data: ByteArray): CompletableFuture<Unit> {
        val entriesPerChunk = bytesPerChunk / bytesPerEntry!!
        val aggregateKey = key / entriesPerChunk
        val begin = key - aggregateKey * entriesPerChunk
        val end = begin + data.size // changed bytesPerEntry to data.size
        var dataStored: ByteArray? = null
        val future = table!!.read(byteArrayOfInts(aggregateKey)).thenApply {
            dataStored = it?.copyOf()
            if (dataStored == null) {
                dataStored = ByteArray(bytesPerChunk)
            }
            for (i in begin until end) {
                dataStored!![i] = data[i - begin]
            }

        }
        return future.thenCompose { table!!.write(byteArrayOfInts(aggregateKey), dataStored!!) }
    }

    fun read(key: Int): CompletableFuture<ByteArray?> {
        val entriesPerChunk = bytesPerChunk / bytesPerEntry!!
        val aggregateKey = key / entriesPerChunk
        val begin = key - aggregateKey * entriesPerChunk
        val end = begin + bytesPerEntry!!
        val futureData = table!!.read(byteArrayOfInts(aggregateKey))
        return futureData.thenApply { data -> data?.slice(begin until end)?.toByteArray() }
    }
}
