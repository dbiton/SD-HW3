package il.ac.technion.cs.softwaredesign

import dev.misfitlabs.kotlinguice4.KotlinModule

class SifriTaubModule: KotlinModule() {
    override fun configure() {
        bind<DB>().to<LibDB>()
        install(StorageModule())
    }
}