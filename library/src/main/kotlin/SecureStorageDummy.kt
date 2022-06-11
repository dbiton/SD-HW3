package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class FakeFactory : SecureStorageFactory {
    private var tables: MutableMap<ByteArrayWrapper, FakeStorage> = mutableMapOf(); //Dictionary
    override fun open(name: ByteArray): SecureStorage {
        tables[ByteArrayWrapper(name)] = FakeStorage()
        return tables[ByteArrayWrapper(name)]!!
    }
}

