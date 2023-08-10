package mega.privacy.android.app.di.transfers

import mega.privacy.android.domain.di.TransferModule as DomainTransferModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.AreAllTransfersPaused
import mega.privacy.android.app.presentation.transfers.notification.DefaultDownloadNotificationMapper
import mega.privacy.android.data.mapper.transfer.DownloadNotificationMapper
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.DefaultMonitorTransfersSize
import mega.privacy.android.domain.usecase.GetNumPendingTransfers
import mega.privacy.android.domain.usecase.MonitorTransfersSize
import mega.privacy.android.domain.usecase.ResetTotalDownloads

/**
 * Use cases to check on transfer status
 */
@Module(includes = [DomainTransferModule::class])
@InstallIn(SingletonComponent::class, ViewModelComponent::class, ServiceComponent::class)
abstract class TransfersModule {
    /**
     * Binds the Use Case [MonitorTransfersSize] to its default implementation [DefaultMonitorTransfersSize]
     *
     * @param useCase [DefaultMonitorTransfersSize]
     * @return [MonitorTransfersSize]
     */
    @Binds
    abstract fun bindMonitorTransfersSize(useCase: DefaultMonitorTransfersSize): MonitorTransfersSize

    /**
     * Binds [DownloadNotificationMapper] to its default implementation [DefaultDownloadNotificationMapper]
     * @param mapper [DefaultDownloadNotificationMapper]
     * @return default [DownloadNotificationMapper]
     */
    @Binds
    abstract fun bindDownloadNotificationMapper(mapper: DefaultDownloadNotificationMapper): DownloadNotificationMapper

    companion object {

        /**
         * Provides the [GetNumPendingTransfers] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [GetNumPendingTransfers]
         */
        @Provides
        fun provideGetNumPendingTransfers(transfersRepository: TransferRepository): GetNumPendingTransfers =
            GetNumPendingTransfers(transfersRepository::getNumPendingTransfers)

        /**
         * Provides the [AreAllTransfersPaused] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [AreAllTransfersPaused]
         */
        @Provides
        fun provideAreAllTransfersPaused(transfersRepository: TransferRepository): AreAllTransfersPaused =
            AreAllTransfersPaused(transfersRepository::areAllTransfersPaused)

        /**
         * Provides the [ResetTotalDownloads] implementation
         *
         * @param transferRepository [TransferRepository]
         */
        @Provides
        fun provideResetTotalDownloads(transferRepository: TransferRepository):
                ResetTotalDownloads =
            ResetTotalDownloads(transferRepository::resetTotalDownloads)
    }
}
