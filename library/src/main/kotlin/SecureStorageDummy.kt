package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class FakeFactory : SecureStorageFactory {
    private var tables: MutableMap<ByteArrayWrapper, FakeStorage> = mutableMapOf(); //Dictionary

    @Throws(InterruptedException::class)
    private fun openFuture(name: ByteArray): CompletableFuture<SecureStorage> {
        val completableFuture = CompletableFuture<SecureStorage>()
        Executors.newCachedThreadPool().submit<Any?> {
            Thread.sleep(100)
            tables[ByteArrayWrapper(name)] = FakeStorage()
            completableFuture.complete(tables[ByteArrayWrapper(name)]!!)
            null
        }
        return completableFuture
    }

    override fun open(name: ByteArray): CompletableFuture<SecureStorage> {
        return openFuture(name)
    }
}

