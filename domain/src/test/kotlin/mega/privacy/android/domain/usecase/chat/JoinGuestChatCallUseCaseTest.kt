package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class JoinGuestChatCallUseCaseTest {
    private val createEphemeralAccountUseCase: CreateEphemeralAccountUseCase = mock()
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase = mock()
    private val joinChatCallUseCase: JoinChatCallUseCase = mock()

    private lateinit var underTest: JoinGuestChatCallUseCase

    @Before
    fun setup() {
        underTest = JoinGuestChatCallUseCase(
            createEphemeralAccountUseCase,
            initGuestChatSessionUseCase,
            joinChatCallUseCase
        )
    }

    @Test
    fun `test that all methods are called in correct order`() = runBlocking {
        val chatLink = "chatLink"
        val firstName = "firstName"
        val lastName = "lastName"

        underTest.invoke(chatLink, firstName, lastName)

        verify(initGuestChatSessionUseCase).invoke(false)
        verify(createEphemeralAccountUseCase).invoke(firstName, lastName)
        verify(joinChatCallUseCase).invoke(chatLink)
    }

    @Test(expected = ChatRoomDoesNotExistException::class)
    fun `test that exception is thrown when chat room does not exist`() = runBlocking {
        val chatLink = "chatLink"
        val firstName = "firstName"
        val lastName = "lastName"

        whenever(joinChatCallUseCase.invoke(chatLink)).thenThrow(ChatRoomDoesNotExistException())

        underTest.invoke(chatLink, firstName, lastName)
    }
}
