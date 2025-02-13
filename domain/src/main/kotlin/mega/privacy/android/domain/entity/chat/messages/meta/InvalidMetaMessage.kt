package mega.privacy.android.domain.entity.chat.messages.meta

/**
 * Invalid meta message
 */
data class InvalidMetaMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : MetaMessage