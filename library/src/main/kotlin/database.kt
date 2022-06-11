package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import java.util.concurrent.CompletableFuture


interface DB {
    fun getUserData(username: String): CompletableFuture<UserData?>
    fun changeTokenForUser(data: UserData): CompletableFuture<String>
    fun addUser(username: String, isFromCS: Boolean, age: Int, password: String): CompletableFuture<Unit>
    fun isTokenValid(token: String): CompletableFuture<Boolean>
    fun getBookData(id: String): CompletableFuture<BookData?>
    fun addBook(data: BookData): CompletableFuture<Unit>
    fun getFirstBookIDs(n: Int): CompletableFuture<List<String>>
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

    private fun parseBookData(book_id: String, byteArray: ByteArray?): BookData? {
        if (byteArray == null)
            return null
        val str = String(byteArray)
        val descriptionBegin = str.indexOf("X")
        val descriptionEnd = str.lastIndexOf("X")
        val strs = str.substring(0, descriptionBegin).split(" ")
        return BookData(book_id, str.substring(descriptionBegin + 1, descriptionEnd), strs[1].toInt())
    }

    private fun parseUserData(username: String, byteArray: ByteArray?): UserData? {
        if (byteArray == null)
            return null
        val str = String(byteArray)
        val passwordBegin = str.indexOf("X")
        val passwordEnd = str.lastIndexOf("X")
        val strs = str.substring(0, passwordBegin).split(" ")
        val password = str.substring(passwordBegin + 1, passwordEnd)
        return UserData(
            username, strs[1].toBoolean(), strs[2].toInt(), password,
            strs[0].toInt()
        )
    }

    private fun generateBookTimestamp(): CompletableFuture<Int> {
        var last_timestamp = -1
        return tableMetadata.read(1).thenCompose {
            last_timestamp = it!!.toInt()
            tableMetadata.write(1, byteArrayOfInts(last_timestamp + 1))
        }.thenApply { last_timestamp + 1 }
    }

    private fun generateToken(): CompletableFuture<Int> {
        var last_token = -1
        return tableMetadata.read(0).thenCompose {
            last_token = it!!.toInt()
            tableMetadata.write(0, byteArrayOfInts(last_token + 1))
        }.thenApply { last_token + 1 }
    }

    override fun getUserData(username: String): CompletableFuture<UserData?> {
        return tableUsers.read(username).thenApply { data -> parseUserData(username, data) }
    }

    // can be changed to only modify the first chunk - too lazy to implement
    override fun changeTokenForUser(data: UserData): CompletableFuture<String> {
        //val username = data.username
        //val userData = getUserData(username)
        var future = tableTokens.write(data.token, byteArrayOf(0))
        var userStr = ""
        var tokenStr = ""
        val futureToken = generateToken()
        future = future.thenCompose {
            futureToken.thenCompose {
                tokenStr = it.toString()
                userStr = tokenStr + " " + data.isFromCS.toString() + " " +
                        data.age.toString() + "X" + data.password + "X"
                tableTokens.write(it, byteArrayOf(1))
            }
        }
        future = future.thenCompose { tableUsers.write(data.username, userStr.toByteArray()) }

        return future.thenApply { tokenStr }
    }

    override fun addUser(username: String, isFromCS: Boolean, age: Int, password: String): CompletableFuture<Unit> {
        val futureToken = generateToken()
        var tokenStr = ""
        val future = futureToken.thenCompose {
            tokenStr = it.toString()
            tableTokens.write(it, byteArrayOf(1))
        }
        val data = tokenStr + " " + isFromCS.toString() + " " + age.toString() + "X" + password + "X"
        return future.thenCompose { tableUsers.write(username, data.toByteArray()) }
    }

    override fun isTokenValid(token: String): CompletableFuture<Boolean> {
        var num_token: Int = -1
        try {
            num_token = token.toInt()
        } catch (e: NumberFormatException) {
            return CompletableFuture.completedFuture(false)
        }
        return tableTokens.read(num_token).thenApply { tokenValue -> tokenValue != null && tokenValue.toInt() == 1 }
    }

    override fun getBookData(id: String): CompletableFuture<BookData?> {
        return tableBooks.read(id).thenApply { data -> parseBookData(id, data) }
    }

    override fun addBook(data: BookData): CompletableFuture<Unit> {
        val ts = generateBookTimestamp()
        return tableBooks.write(
            data.id,
            (ts.toString() + " " + data.numOfCopies.toString() + "X" + data.description + "X")
                .toByteArray()
        ).thenCompose {
            tableBooksOrder.write(ts.toString(), data.id.toByteArray())
        }
    }

    override fun getFirstBookIDs(n: Int): CompletableFuture<List<String>> {
        val bookIds: MutableList<String> = mutableListOf()
        var shouldBreak = false
        if (n < 1) return CompletableFuture.completedFuture(bookIds)
        var futureData: CompletableFuture<List<String>> = tableBooksOrder.read(1.toString())
            .thenApply {
                if (it == null)
                    shouldBreak = true
                else
                    bookIds.add(String(it))
                bookIds
            }
        for (i in 2..n) {
            futureData = futureData.thenCompose {
                tableBooksOrder.read(i.toString()).thenApply {
                    if (it == null)
                        shouldBreak = true
                    else
                        bookIds.add(String(it))
                    bookIds
                }
            }
            if (shouldBreak)
                break
        }
        return futureData
    }
}


