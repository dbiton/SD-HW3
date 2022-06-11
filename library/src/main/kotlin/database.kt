package il.ac.technion.cs.softwaredesign
import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max


interface DB {
    fun getUserData(username: String): UserData?
    fun changeTokenForUser(data: UserData): String
    fun addUser(username: String, isFromCS: Boolean, age: Int, password: String)
    fun isTokenValid(token: String): Boolean
    fun getBookData(id: String): BookData?
    fun addBook(data: BookData)
    fun getFirstBookIDs(n: Int): List<String>
}

class LibDB// at most 1,000,000 books therefore 20 bits needed
// last_token
// last_book_timestamp
    @Inject constructor(f: SecureStorageFactory, name: String = "DB") : DB {
    private var tableTokens: AggregatingStorage
    private var tableUsers: UnboundedStorage
    private var tableBooks: UnboundedStorage
    private var tableBooksOrder: UnboundedStorage
    private var tableMetadata: AggregatingStorage

    private val maxCountBooks = 1000000
    private val maxCountUsers = 1000000
    private val bytesPerToken = ceil(ceil(log2(maxCountUsers.toDouble())) / 8).toInt()
    private val bytesPerBookTimestamp = ceil(ceil(log2(maxCountBooks.toDouble())) / 8).toInt()


    init {
        tableTokens = AggregatingStorage(f)
        tableUsers = UnboundedStorage(f)
        tableBooks = UnboundedStorage(f)
        tableBooksOrder = UnboundedStorage(f)

        tableMetadata = AggregatingStorage(f)
        tableTokens.open("$name tokens")
        tableUsers.open("$name users")
        tableBooks.open("$name books")
        tableBooksOrder.open("$name order")
        tableMetadata.open("$name metadata")
        tableTokens.setBytesPerEntry(1)
        tableMetadata.setBytesPerEntry(max(bytesPerToken, bytesPerBookTimestamp))
        tableMetadata.write(0, byteArrayOfInts(0)) // lastToken
        tableMetadata.write(1, byteArrayOfInts(0)) // lastBookTimestamp
    }

    private fun parseBookData(book_id: String, byteArray: ByteArray): BookData {
        val str = String(byteArray)
        val descriptionBegin = str.indexOf("X")
        val descriptionEnd = str.lastIndexOf("X")
        val strs = str.substring(0, descriptionBegin).split(" ")
        return BookData(book_id, str.substring(descriptionBegin+1, descriptionEnd), strs[1].toInt())
    }

    private fun parseUserData(username: String, byteArray: ByteArray): UserData {
        val str = String(byteArray)
        val passwordBegin = str.indexOf("X")
        val passwordEnd = str.lastIndexOf("X")
        val strs = str.substring(0, passwordBegin).split(" ")
        val password = str.substring(passwordBegin+1, passwordEnd)
        return UserData(
            username, strs[1].toBoolean(), strs[2].toInt(), password,
            strs[0].toInt()
        )
    }

    private fun generateBookTimestamp(): Int {
        val last_timestamp = tableMetadata.read(1)?.toInt()
        tableMetadata.write(1, byteArrayOfInts(last_timestamp!! + 1))
        return last_timestamp + 1
    }

    private fun generateToken(): Int {
        val last_token = tableMetadata.read(0)!!.toInt()
        tableMetadata.write(0, byteArrayOfInts(last_token + 1))
        return last_token + 1
    }

    override fun getUserData(username: String): UserData? {
        val byteArray = tableUsers.read(username) ?: return null
        return parseUserData(username, byteArray)
    }

    // can be changed to only modify the first chunk - too lazy to implement
    override fun changeTokenForUser(data: UserData): String {
        //val username = data.username
        //val userData = getUserData(username)
        tableTokens.write(data.token, byteArrayOf(0))

        val token = generateToken()
        tableTokens.write(token, byteArrayOf(1))
        val value =
            token.toString() + " " + data.isFromCS.toString() + " " + data.age.toString() + "X" + data.password + "X"
        tableUsers.write(data.username, value.toByteArray())

        return token.toString()
    }

    override fun addUser(username: String, isFromCS: Boolean, age: Int, password: String) {
        val token = generateToken()
        tableTokens.write(token, byteArrayOf(1))
        val data = token.toString() + " " + isFromCS.toString() + " " + age.toString() + "X" + password + "X"
        tableUsers.write(username, data.toByteArray())
    }

    override fun isTokenValid(token: String): Boolean {
        try {
            val tokenValue = tableTokens.read(token.toInt())
            return tokenValue != null && tokenValue.toInt() == 1
        } catch (e: NumberFormatException) {
            return false
        }
    }

    override fun getBookData(id: String): BookData? {
        val data = tableBooks.read(id) ?: return null
        return parseBookData(id, data)
    }

    override fun addBook(data: BookData) {
        val ts = generateBookTimestamp()
        tableBooks.write(
            data.id,
            (ts.toString() + " " + data.numOfCopies.toString() + "X" + data.description + "X")
                .toByteArray()
        )
        tableBooksOrder.write(ts.toString(), data.id.toByteArray())
    }

    override fun getFirstBookIDs(n: Int): List<String> {
        val bookIds: MutableList<String> = mutableListOf()
        for (i in 1 .. n) {
            val data = tableBooksOrder.read(i.toString()) ?: break
            bookIds.add(String(data))
        }
        return bookIds
    }
}


