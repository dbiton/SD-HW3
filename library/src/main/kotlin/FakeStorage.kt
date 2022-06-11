package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage


class FakeStorage public constructor() : SecureStorage {
    private var data : MutableMap<ByteArrayWrapper, ByteArray> = mutableMapOf(); //Dictionary
    override fun read(key: ByteArray): ByteArray? {
        return data[ByteArrayWrapper(key)]
    }

    override fun write(key: ByteArray, value: ByteArray) {
        data[ByteArrayWrapper(key)] = value
    }
}