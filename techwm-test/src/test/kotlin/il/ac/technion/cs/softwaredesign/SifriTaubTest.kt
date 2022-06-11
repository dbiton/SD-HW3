package il.ac.technion.cs.softwaredesign

import com.google.inject.Guice
import com.google.inject.Injector
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.misfitlabs.kotlinguice4.getInstance
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SifriTaubTests {
    private val injector: Injector = Guice.createInjector(TestModule())
    private lateinit var manager: SifriTaub
    private val username1: String = "user-a"
    private val username2: String = "user-b"
    private val password: String = "123456"
    private val age: Int = 24
    private val isFromCS: Boolean = false
    private lateinit var token: String

    @Nested
    inner class UserTests {
        @BeforeEach
        fun initManager() {
            manager = injector.getInstance<SifriTaub>()
        }

        @Test
        fun `a non-existing user throws exception on authenticate`() {
            val username = "non-existing"
            val password = "non-existing"

            assertThrows<IllegalArgumentException> {
                manager.authenticate(username, password)
            }
        }

        @Test
        fun `user is successfully registered`() {
            assertDoesNotThrow {
                manager.register(username1, password, isFromCS, age)
            }
        }

        @Test
        fun `user isn't registered - illegal age`() {

            assertThrows<IllegalArgumentException> {
                manager.register(username1, password, isFromCS, -1)
            }
        }

        @Test
        fun `registered user isn't authenticated - wrong password`() {
            manager.register(username1, password, isFromCS, age)

            assertThrows<IllegalArgumentException> {
                manager.authenticate(username1, password + "a");
            }
        }

        @Test
        fun `user is successfully authenticated`() {
            manager.register(username1, password, isFromCS, age)

            assertDoesNotThrow {
                manager.authenticate(username1, password);
            }
        }

        @Test
        fun `user token is saved successfully`() {
            manager.register(username1, password, isFromCS, age)
            val token = manager.authenticate(username1, password)

            manager.register(username2, password, isFromCS, age)

            assertDoesNotThrow {
                manager.userInformation(token, username2)
            }
        }

        @Test
        fun `user information was successfully saved in storage`() {
            manager.register(username1, password, isFromCS, age)
            val token = manager.authenticate(username1, password)

            manager.register(username2, password, isFromCS, age)

            assertThat(manager.userInformation(token, username2)?.username, equalTo(username2))
        }

        @Test
        fun `token changes on user's second authentication`() {
            manager.register(username1, password, isFromCS, age)
            val token1 = manager.authenticate(username1, password)
            val token2 = manager.authenticate(username1, password)
            assertThat(token1, !equalTo(token2))
        }

        @Test
        fun `different users have different tokens`() {
            manager.register(username1, password, isFromCS, age)
            manager.register(username2, password, isFromCS, age)
            val t1 = manager.authenticate(username1, password)
            val t2 = manager.authenticate(username2, password)
            assertThat(t1, !equalTo(t2))
        }

        // This test registers two users - one to one SifriTaub instance and the other to another.
        // Then, we check that user2 doesn't exist in the first instance.
        @Test
        fun `registering a user to a SifriTaub doesn't register in another instance of SifriTaub`() {
            val manager2 = injector.getInstance<SifriTaub>()
            manager.register(username1, password, isFromCS, age)

            assertThrows<IllegalArgumentException> { manager2.authenticate(username1, password) }
        }
    }

    @Nested
    inner class BookTests {
        private val bookID1 = "dor"
        private val bookID2 = "hamor"
        private val bookDesc = "dor went to the gym yesterday"
        private val copies = 60

        @BeforeEach
        fun initManager() {
            manager = injector.getInstance<SifriTaub>()
            manager.register(username1, password, isFromCS, age)
            manager.register(username2, password, isFromCS, age)
            token = manager.authenticate(username1, password)
        }

        @Test
        fun `bad token cant add book`() {
            assertThrows<PermissionException> {
                manager.addBookToCatalog(username2, "", "", 0)
            }
        }

        @Test
        fun `authenticated user can add book`() {
            assertDoesNotThrow {
                manager.addBookToCatalog(token, bookID1, bookDesc, copies)
            }
        }

        @Test
        fun `book successfully added to catalog`() {
            manager.addBookToCatalog(token, bookID1, bookDesc, copies)
            assertThat(manager.getBookDescription(token, bookID1), equalTo(bookDesc))
        }

        @Test
        fun `book not added does no exist in catalog`() {
            manager.addBookToCatalog(token, bookID1, bookDesc, copies)
            assertThrows<IllegalArgumentException> { manager.getBookDescription(token, bookID2) }
        }

        @Test
        fun `successfully get all book ids from catalog`() {
            val ids: MutableList<String> = mutableListOf()
            for (i in 1..8) {
                ids.add(bookID1 + i.toString())
                manager.addBookToCatalog(token, bookID1 + i.toString(), bookDesc + i.toString(), copies + i)
            }
            assertThat(manager.listBookIds(token), equalTo(ids))
        }

        @Test
        fun `successfully get first 5 book ids from catalog`() {
            val ids: MutableList<String> = mutableListOf()
            for (i in 1..8) {
                ids.add(bookID1 + i.toString())
                manager.addBookToCatalog(token, bookID1 + i.toString(), bookDesc + i.toString(), copies + i)
            }
            val bookIds = manager.listBookIds(token, 5)
            assertThat(bookIds, equalTo(ids.slice(0..4)))
        }
    }


}