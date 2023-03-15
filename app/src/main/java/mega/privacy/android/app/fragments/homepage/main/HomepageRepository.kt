package mega.privacy.android.app.fragments.homepage.main

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.facebook.common.executors.UiThreadImmediateExecutorService
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaBanner
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUtilsAndroid
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomepageRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context,
) {

    private val bannerList = MutableLiveData<MutableList<MegaBanner>?>()

    /** The last time of getting banners */
    private var lastGetBannerTime = 0L

    /** Time threshold of getting banners from the server again */
    private val getBannerThreshold = 6 * TimeUtils.HOUR

    /** Whether the previous getting banners was successful or not */
    private var getBannerSuccess = false

    fun getBannerListLiveData(): MutableLiveData<MutableList<MegaBanner>?> {
        return bannerList
    }

    /**
     * Retrieve the latest banner list from the server if time is over.
     * Or return the memory cached data
     * The time threshold is set to 6 hours for preventing too frequent
     * API requests
     */
    suspend fun loadBannerList() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGetBannerTime < getBannerThreshold && getBannerSuccess) {
            bannerList.value?.let {
                bannerList.value = it
            }

            return
        }

        lastGetBannerTime = currentTime
        getBannerSuccess = false

        withContext(Dispatchers.IO) {
            megaApi.getBanners(OptionalMegaRequestListenerInterface(onRequestFinish = { megaRequest, megaError ->
                if (megaError.errorCode == MegaError.API_OK) {
                    MegaUtilsAndroid.bannersToArray(megaRequest.megaBannerList)?.also {
                        prefetchBannerImages(it)
                    }
                } else if (megaError.errorCode == MegaError.API_ENOENT) {
                    bannerList.value = null
                }
            }))
        }
    }

    /**
     * Prefetch all the images of the banners by virtue of Fresco disk cache mechanism.
     * Notify to display the banner view pager only if all images were successfully downloaded
     *
     * @param banners the list of MegaBanner
     */
    private fun prefetchBannerImages(banners: MutableList<MegaBanner>) {
        val bannerImageUrls = mutableListOf<String>()

        banners.forEach {
            bannerImageUrls.add(it.imageLocation.plus(it.backgroundImage))
            bannerImageUrls.add(it.imageLocation.plus(it.image))
        }

        var count = bannerImageUrls.size

        bannerImageUrls.forEach {
            val ds = Fresco.getImagePipeline()
                .prefetchToDiskCache(ImageRequest.fromUri(it), null)
            ds.subscribe(object : BaseDataSubscriber<Void>() {
                override fun onNewResultImpl(dataSource: DataSource<Void>) {
                    count--
                    if (count == 0) {
                        getBannerSuccess = true
                        bannerList.value = banners
                    }
                }

                override fun onFailureImpl(dataSource: DataSource<Void>) {
                    Timber.w("Get banner image failed")
                }

            }, UiThreadImmediateExecutorService.getInstance())
        }
    }

    fun isRootNodeNull() = (megaApi.rootNode == null)

    fun dismissBanner(id: Int) {
        megaApi.dismissBanner(id)
    }

    /**
     * Resets values after logout.
     */
    fun logout() {
        getBannerSuccess = false
        bannerList.value = null
    }
}