package il.ac.technion.cs.softwaredesign

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnboundedStorageTests {
    private lateinit var s : UnboundedStorage
    private var sName = "unboundedStorageTest"

    @BeforeAll
    fun init() {
        s = UnboundedStorage(FakeFactory())
        s.open(sName)
    }

    @Test
    fun `write to unbounded storage`() {
        for (i in 1..64){
            val data = "DATA".repeat(i*i)
            assertDoesNotThrow {
                s.write(i.toString(), data.toByteArray())
            }
        }
    }

    @Test
    fun `read from unbounded storage`() {
        for (i in 64 downTo 1){
            assertDoesNotThrow {
                s.read(i.toString())
            }
        }
    }

    @Test
    fun `read from unbounded storage returns correct value`() {
        for (i in 1..64){
            val data = "DATA".repeat(i*i)
            assertDoesNotThrow {
                s.write(i.toString(), data.toByteArray())
                val dataRead = s.read(i.toString())
                assert(data.toByteArray().contentEquals(dataRead))
            }
        }
    }
}