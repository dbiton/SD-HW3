package il.ac.technion.cs.softwaredesign

import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule

class StorageModule : KotlinModule() {
    override fun configure() {
        install(SecureStorageModule())
    }
}