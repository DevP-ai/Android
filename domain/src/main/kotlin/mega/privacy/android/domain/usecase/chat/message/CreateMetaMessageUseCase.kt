package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import javax.inject.Inject

internal class CreateMetaMessageUseCase @Inject constructor(
    private val createInvalidMessageUseCase: CreateInvalidMessageUseCase,
) : CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = when (message.containsMeta?.type) {
        ContainsMetaType.RICH_PREVIEW -> RichPreviewMessage(
            msgId = message.msgId,
            time = message.timestamp,
            isMine = isMine,
            userHandle = message.userHandle,
            preview = message.containsMeta.richPreview
        )

        ContainsMetaType.GEOLOCATION -> LocationMessage(
            msgId = message.msgId,
            time = message.timestamp,
            isMine = isMine,
            userHandle = message.userHandle
        )

        ContainsMetaType.GIPHY -> GiphyMessage(
            msgId = message.msgId,
            time = message.timestamp,
            isMine = isMine,
            userHandle = message.userHandle
        )

        else -> createInvalidMessageUseCase(message, isMine)
    }
}