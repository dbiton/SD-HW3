package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeStorageTests {
    private lateinit var db : SecureStorage

    @BeforeAll
    fun init() {
        db = FakeFactory().open("test".toByteArray()).get()
    }

    @Test
    fun `can write to db`() {
        assertDoesNotThrow { db.write("test".toByteArray(), "value".toByteArray()) }
    }

    @Test
    fun `can read from db`() {
        assertDoesNotThrow { db.read("test".toByteArray()) }
    }

    @Test
    fun `value read from db is right`() {
        db.write("test".toByteArray(), "value".toByteArray())
        assert("value".toByteArray().contentEquals((db.read("test".toByteArray())).get()))
    }

}