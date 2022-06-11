package il.ac.technion.cs.softwaredesign

import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

// hi there! we didn't use provider cuz it broke sum stuff ~~~ have an ice day!
class TestModule : KotlinModule(){
    override fun configure() {
        bind<DB>().to<LibDB>()
        bind<SecureStorageFactory>().to<FakeFactory>()
        bind<SecureStorage>().to<FakeStorage>()
    }
}