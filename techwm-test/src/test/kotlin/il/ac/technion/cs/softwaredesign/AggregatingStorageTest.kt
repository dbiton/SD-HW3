package il.ac.technion.cs.softwaredesign

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregatingStorageTests {
    private lateinit var s1 : AggregatingStorage
    private lateinit var s17 : AggregatingStorage
    private var s1Name = "aggregatingStorageTestS1"
    private var s17Name = "aggregatingStorageTestS17"


    @BeforeAll
    fun init() {
        s1 = AggregatingStorage(FakeFactory())
        s1.open(s1Name)
        s1.setBytesPerEntry(1)

        s17 = AggregatingStorage(FakeFactory())
        s17.open(s17Name)
        s17.setBytesPerEntry(17)
    }

    @Test
    fun `write to aggregating storage 1 byte per entry`() {
        for (i in 1..256){
            assertDoesNotThrow { s1.write(i, byteArrayOfInts(i%3)) }
        }
    }

    @Test
    fun `read from aggregating storage 1 byte per entry`() {
        for (i in 1..256){
            assertDoesNotThrow {
                s1.read(i)
            }
        }
    }

    @Test
    fun `write to aggregating storage 17 bytes per entry`() {
        for (i in 1..256){
            s1.write(i, byteArrayOfInts(i))
        }
    }

    @Test
    fun `read from aggregating storage 17 bytes per entry`() {
        for (i in 1..256){
            assertDoesNotThrow {
                s1.read(i)
            }
        }
    }

    @Test
    fun `write to aggregating storage 1 byte per entry returns correct value`() {
        for (i in 1..256){
            assertDoesNotThrow {
                s1.write(i, byteArrayOfInts(i%3))
                val dataRead = s1.read(i)
                assert(dataRead.get().contentEquals(byteArrayOfInts(i%3)))
            }
        }
    }

    @Test
    fun `write to aggregating storage 17 bytes per entry returns correct value`() {
        for (i in 1..256){
            assertDoesNotThrow {
                val dataWrite = ByteArray(17)
                dataWrite[0] = i.toByte()
                s17.write(i, dataWrite)
                val dataRead = s17.read(i)
                assert(dataRead.get().contentEquals(dataWrite))
            }
        }
    }
}