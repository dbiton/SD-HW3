package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.math.min

class UnboundedStorage(f: SecureStorageFactory) {
    private val bytesPerChunk = 100
    private var factory: SecureStorageFactory? = f
    private var table: SecureStorage? = null


    fun open(name: String) {
        table = factory?.open(name.toByteArray())?.get()
    }

    fun write(key: String, data: ByteArray): CompletableFuture<Unit> {
        var future_writes : CompletableFuture<Unit>? = null
        for (chunkIndex in 0..data.size / bytesPerChunk) {
            val chunkKey = "$key $chunkIndex"
            val chunk = data.slice(
                chunkIndex * bytesPerChunk until
                        min(data.size, (1 + chunkIndex) * bytesPerChunk)
            )
            future_writes = if (future_writes == null) {
                table?.write(chunkKey.toByteArray(), chunk.toByteArray())
            } else {
                future_writes.thenCompose { table?.write(chunkKey.toByteArray(), chunk.toByteArray()) }
            }
        }
        return future_writes ?: CompletableFuture.completedFuture(null)
    }

    fun read(key: String): CompletableFuture<ByteArray?> {
        // read in loop
        val result = table?.read("$key 0".toByteArray())?.thenApply {
            if (it == null) return@thenApply null
            var data = it.copyOf()
            var chunk_index = 1
            while (true) {
                table?.read("$key $chunk_index".toByteArray())?.thenApply {
                    if (it == null) return@thenApply null
                    val chunk = it.copyOf()
                    data += chunk
                    chunk_index += 1
                    chunk
                }?.get() ?: break
            }
            data
        }
        return result ?: CompletableFuture.completedFuture(null)
    }
}
