package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class FakeStorage public constructor() : SecureStorage {
    private var data : MutableMap<ByteArrayWrapper, ByteArray> = mutableMapOf(); //Dictionary

    @Throws(InterruptedException::class)
    private fun readFuture(key: ByteArray): CompletableFuture<ByteArray?> {
        val completableFuture = CompletableFuture<ByteArray?>()
        Executors.newCachedThreadPool().submit<Any?> {
            Thread.sleep(100)
            completableFuture.complete(data[ByteArrayWrapper(key)])
            null
        }
        return completableFuture
    }

    @Throws(InterruptedException::class)
    private fun writeFuture(key: ByteArray, value: ByteArray): CompletableFuture<Unit> {
        val completableFuture = CompletableFuture<Unit>()
        Executors.newCachedThreadPool().submit<Any?> {
            Thread.sleep(100)
            data[ByteArrayWrapper(key)] = value
            null
        }
        return completableFuture
    }

    override fun read(key: ByteArray): CompletableFuture<ByteArray?> {
        return readFuture(key)
    }

    override fun write(key: ByteArray, value: ByteArray): CompletableFuture<Unit> {
        return writeFuture(key, value)
    }
}