package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import kotlin.math.min

class UnboundedStorage(f: SecureStorageFactory) {
    private val bytesPerChunk = 100
    private var factory: SecureStorageFactory? = f
    private var table: SecureStorage? = null

    fun open(name: String) {
        table = factory?.open(name.toByteArray())
    }

    fun write(key: String, data: ByteArray): Unit {
        for (chunkIndex in 0..data.size/bytesPerChunk) {
            val chunkKey = "$key $chunkIndex"
            val chunk = data.slice(chunkIndex*bytesPerChunk until
                    min(data.size, (1+chunkIndex)*bytesPerChunk))
            table?.write(chunkKey.toByteArray(),chunk.toByteArray())
        }
    }

    fun read(key: String): ByteArray? {
        // read in loop
        var data : ByteArray = table?.read("$key 0".toByteArray()) ?: return null
        var chunk_index = 1
        while (true){
            val chunk = table?.read("$key $chunk_index".toByteArray()) ?: break
            data += chunk
            chunk_index += 1
        }
        return data
    }
}
