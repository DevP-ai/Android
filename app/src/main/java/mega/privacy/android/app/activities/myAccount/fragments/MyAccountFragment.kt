package mega.privacy.android.app.activities.myAccount.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.SMSVerificationActivity
import mega.privacy.android.app.activities.myAccount.MyAccountViewModel
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.databinding.FragmentMyAccountBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.fragments.managerFragments.myAccount.editProfile.EditProfileActivity
import mega.privacy.android.app.listeners.GetUserDataListener
import mega.privacy.android.app.listeners.ResetPhoneNumberListener
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil
import mega.privacy.android.app.modalbottomsheet.phoneNumber.PhoneNumberBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.phoneNumber.PhoneNumberCallback
import mega.privacy.android.app.utils.AlertsAndWarnings.showRemoveOrModifyPhoneNumberConfirmDialog
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.ChangeApiServerUtil.showChangeApiServerDialog
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.ColorUtils.tintIcon
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.TimeUtils.formatDate
import mega.privacy.android.app.utils.Util.*
import nz.mega.sdk.MegaApiJava.BUSINESS_STATUS_EXPIRED
import nz.mega.sdk.MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaUser
import java.io.File

@AndroidEntryPoint
class MyAccountFragment : BaseFragment(), Scrollable, PhoneNumberCallback {

    companion object {
        private const val ANIMATION_DURATION = 200L
        private const val ANIMATION_DELAY = 500L
        private const val PHONE_NUMBER_CHANGE_DELAY = 3000L
    }

    private val viewModel: MyAccountViewModel by activityViewModels()

    private lateinit var binding: FragmentMyAccountBinding

    private var phoneNumberBottomSheet: PhoneNumberBottomSheetDialogFragment? = null

    private var gettingInfo = StringResourcesUtils.getString(R.string.recovering_info)

    private var isModify = false
    private var removeOrModifyPhoneNumberDialog: AlertDialog? = null
    private var changeApiServerDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyAccountBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
        setUpObservers()
    }

    private fun setUpView() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)

        (activity as AppCompatActivity).supportActionBar?.apply {
            title = null
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        ListenScrollChangesHelper().addViewToListen(
            binding.scrollView
        ) { _, _, _, _, _ -> checkScroll() }

        checkScroll()

        setUpAvatar(true)

        binding.myAccountThumbnail.setOnClickListener {
            (mActivity as ManagerActivityLollipop).checkBeforeOpeningQR(false)
        }

        binding.nameText.text = viewModel.getName()
        binding.emailText.text = viewModel.getEmail()

        setUpPhoneNumber()
        setUpAccountDetails()

        binding.backupRecoveryKeyLayout.setOnClickListener { viewModel.exportMK(context) }

        setUpAchievements()
        setUpLastSession()
        setUpContactConnections()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(SCROLLING_UP_DIRECTION)
        val isDark = isDarkMode(requireContext())
        val darkAndElevation = withElevation && isDark
        val background = ContextCompat.getColor(requireContext(), R.color.grey_020_grey_087)

        if (darkAndElevation) {
            changeStatusBarColorForElevation(requireActivity(), true)
        } else {
            requireActivity().window.statusBarColor = background
        }

        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        binding.toolbar.setBackgroundColor(
            if (darkAndElevation) getColorForElevation(
                requireContext(),
                elevation
            ) else background
        )

        (activity as AppCompatActivity).supportActionBar?.elevation =
            if (withElevation && !isDark) elevation else 0F
    }

    private fun setupEditProfile(editable: Boolean) {
        binding.nameText.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            if (editable) R.drawable.ic_view_edit_profile else 0,
            0
        )

        binding.myAccountTextInfoLayout.setOnClickListener {
            if (editable) {
                startActivity(Intent(context, EditProfileActivity::class.java))
            }
        }
    }

    fun setUpAccountDetails() {
        binding.lastSessionSubtitle.text =
            if (viewModel.getLastSession().isNotEmpty()) viewModel.getLastSession()
            else gettingInfo

        if (megaApi.isBusinessAccount) {
            setUpBusinessAccount()
            return
        }

        setupEditProfile(true)

        binding.upgradeButton.apply {
            isEnabled = true
            text = StringResourcesUtils.getString(R.string.my_account_upgrade_pro)

            setOnClickListener { viewModel.upgradeAccount(context) }
        }

        binding.accountTypeText.isVisible = true
        binding.upgradeButton.isVisible = true

        binding.accountTypeText.text = StringResourcesUtils.getString(
            when (viewModel.getAccountType()) {
                FREE -> R.string.free_account
                PRO_I -> R.string.pro1_account
                PRO_II -> R.string.pro2_account
                PRO_III -> R.string.pro3_account
                PRO_LITE -> R.string.prolite_account
                else -> R.string.recovering_info
            }
        )

        binding.accountTypeLayout.background = tintIcon(
            context, R.drawable.background_account_type, ContextCompat.getColor(
                context,
                when (viewModel.getAccountType()) {
                    FREE -> R.color.green_400_green_300
                    PRO_I -> R.color.orange_600_orange_300
                    PRO_II, PRO_III, PRO_LITE -> R.color.red_300_red_200
                    else -> R.color.white_black
                }
            )
        )

        setUpPaymentDetails()

        binding.storageProgressPercentage.isVisible = true
        binding.storageProgressBar.isVisible = true
        binding.businessStorageImage.isVisible = false
        binding.transferProgressPercentage.isVisible = true
        binding.transferProgressBar.isVisible = true
        binding.businessTransferImage.isVisible = false

        binding.businessAccountManagementText.isVisible = false

        binding.transferLayout.isVisible = !viewModel.isFreeAccount()

        if (viewModel.getUsedStorage().isEmpty()) {
            binding.storageProgressPercentage.isVisible = false
            binding.storageProgressBar.progress = 0
            binding.storageProgress.text = gettingInfo
        } else {
            binding.storageProgressPercentage.isVisible = true
            binding.storageProgressPercentage.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer_percentage,
                viewModel.getUsedStoragePercentage()
            )

            binding.storageProgressBar.progress = viewModel.getUsedStoragePercentage()
            binding.storageProgress.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer,
                viewModel.getUsedStorage(),
                viewModel.getTotalStorage()
            )
        }

        if (viewModel.getUsedTransfer().isEmpty()) {
            binding.transferProgressPercentage.isVisible = false
            binding.transferProgressBar.progress = 0
            binding.transferProgress.text = gettingInfo
        } else {
            binding.transferProgressPercentage.isVisible = true
            binding.transferProgressPercentage.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer_percentage,
                viewModel.getUsedTransferPercentage()
            )

            binding.transferProgressBar.progress = viewModel.getUsedTransferPercentage()
            binding.transferProgress.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer,
                viewModel.getUsedTransfer(),
                viewModel.getTotalTransfer()
            )
        }
    }

    private fun setUpBusinessAccount() {
        binding.accountTypeText.text = StringResourcesUtils.getString(R.string.business_label)
        binding.upgradeButton.apply {
            isEnabled = false
            text = StringResourcesUtils.getString(
                if (megaApi.isMasterBusinessAccount) R.string.admin_label
                else R.string.user_label
            )
        }

        binding.accountTypeLayout.background = tintIcon(
            context, R.drawable.background_account_type, ContextCompat.getColor(
                context,
                R.color.blue_400_blue_300
            )
        )

        if (megaApi.isMasterBusinessAccount) {
            when (megaApi.businessStatus) {
                BUSINESS_STATUS_EXPIRED, BUSINESS_STATUS_GRACE_PERIOD -> {
                    binding.renewExpiryText.isVisible = false
                    binding.renewExpiryDateText.isVisible = false
                    binding.businessStatusText.apply {
                        isVisible = true

                        text = StringResourcesUtils.getString(
                            if (megaApi.businessStatus == BUSINESS_STATUS_EXPIRED) R.string.payment_overdue_label
                            else R.string.payment_required_label
                        )
                    }

                    expandPaymentInfoIfNeeded()
                }
                else -> setUpPaymentDetails() //BUSINESS_STATUS_ACTIVE
            }

            binding.businessAccountManagementText.isVisible = true
            setupEditProfile(true)
        } else {
            binding.businessAccountManagementText.isVisible = false
            setupEditProfile(false)
        }

        binding.storageProgressPercentage.isVisible = false
        binding.storageProgressBar.isVisible = false
        binding.businessStorageImage.isVisible = true

        binding.storageProgress.text =
            if (viewModel.getUsedStorage().isEmpty()) gettingInfo
            else viewModel.getUsedStorage()

        binding.transferProgressPercentage.isVisible = false
        binding.transferProgressBar.isVisible = false
        binding.businessTransferImage.isVisible = true

        binding.transferProgress.text =
            if (viewModel.getUsedTransfer().isEmpty()) gettingInfo
            else viewModel.getUsedTransfer()

        binding.achievementsLayout.isVisible = false
    }

    private fun setUpPaymentDetails() {
        binding.businessStatusText.isVisible = false

        if (viewModel.hasRenewableSubscription() || viewModel.hasExpirableSubscription()) {
            binding.renewExpiryText.isVisible = true
            binding.renewExpiryDateText.isVisible = true

            binding.renewExpiryText.text = StringResourcesUtils.getString(
                if (viewModel.hasRenewableSubscription()) R.string.renews_on else R.string.expires_on
            )

            binding.renewExpiryDateText.text = formatDate(
                if (viewModel.hasRenewableSubscription()) viewModel.getRenewTime() else viewModel.getExpirationTime(),
                TimeUtils.DATE_MM_DD_YYYY_FORMAT
            )

            expandPaymentInfoIfNeeded()
        }
    }

    fun expandPaymentInfoIfNeeded() {
        if (!viewModel.shouldShowPaymentInfo())
            return

        val v = binding.paymentInfoLayout
        v.isVisible = false

        val matchParentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)

        val wrapContentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)

        val targetHeight = v.measuredHeight

        v.layoutParams.height = 1

        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                v.layoutParams.height =
                    if (interpolatedTime == 1f) LinearLayout.LayoutParams.WRAP_CONTENT
                    else (targetHeight * interpolatedTime).toInt()

                v.isVisible = true
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        a.duration = ANIMATION_DURATION
        a.startOffset = ANIMATION_DELAY
        v.startAnimation(a)
    }

    private fun setUpPhoneNumber() {
        val registeredPhoneNumber = megaApi.smsVerifiedPhoneNumber()
        val alreadyRegisteredPhoneNumber = !isTextEmpty(registeredPhoneNumber)
        val canVerifyPhoneNumber = canVoluntaryVerifyPhoneNumber()

        binding.phoneText.apply {
            if (alreadyRegisteredPhoneNumber) {
                isVisible = true
                text = registeredPhoneNumber
            } else {
                isVisible = false
            }
        }

        val addPhoneNumberVisible = canVerifyPhoneNumber && !alreadyRegisteredPhoneNumber

        binding.addPhoneNumberLayout.apply {
            isVisible = addPhoneNumberVisible

            setOnClickListener {
                if (canVoluntaryVerifyPhoneNumber()) {
                    startActivity(Intent(context, SMSVerificationActivity::class.java))
                } else if (!ModalBottomSheetUtil.isBottomSheetDialogShown(phoneNumberBottomSheet) && activity != null) {
                    phoneNumberBottomSheet = PhoneNumberBottomSheetDialogFragment()
                    phoneNumberBottomSheet!!.show(
                        (mActivity as ManagerActivityLollipop).supportFragmentManager,
                        phoneNumberBottomSheet!!.tag
                    )
                }
            }
        }

        if (addPhoneNumberVisible) {
            binding.addPhoneSubtitle.text =
                if (megaApi.isAchievementsEnabled) StringResourcesUtils.getString(
                    R.string.sms_add_phone_number_dialog_msg_achievement_user,
                    (mActivity as ManagerActivityLollipop).bonusStorageSMS
                ) else StringResourcesUtils.getString(
                    R.string.sms_add_phone_number_dialog_msg_non_achievement_user
                )
        }
    }

    private fun setUpAchievements() {
        binding.achievementsLayout.apply {
            isVisible = megaApi.isAchievementsEnabled

            if (isVisible) {
                setOnClickListener {
                    if (!isOnline(context)) {
                        (mActivity as ManagerActivityLollipop).showSnackbar(
                            SNACKBAR_TYPE,
                            getString(R.string.error_server_connection_problem),
                            MEGACHAT_INVALID_HANDLE
                        )
                    } else {
                        startActivity(Intent(context, AchievementsActivity::class.java))
                    }
                }
            }
        }
    }

    private fun setUpLastSession() {
        binding.lastSessionLayout.setOnClickListener {
            if (viewModel.incrementLastSessionClick()) {
                changeApiServerDialog = showChangeApiServerDialog(requireActivity(), megaApi)
            }
        }
    }

    fun resetPass() {
        AccountController(context).resetPass(megaApi.myEmail)
    }

    fun updateNameView(fullName: String) {
        binding.nameText.text = fullName
    }

    fun setUpAvatar(retry: Boolean) {
        val avatar = buildAvatarFile(context, megaApi.myEmail + JPG_EXTENSION)

        if (avatar != null) {
            setProfileAvatar(avatar, retry)
        } else {
            setDefaultAvatar()
        }
    }

    fun setProfileAvatar(avatar: File, retry: Boolean) {
        val avatarBitmap: Bitmap?

        if (avatar.exists() && avatar.length() > 0) {
            avatarBitmap = BitmapFactory.decodeFile(avatar.absolutePath, BitmapFactory.Options())

            if (avatarBitmap == null) {
                avatar.delete()
            } else {
                binding.myAccountThumbnail.setImageBitmap(avatarBitmap)
                return
            }
        }

        if (retry) viewModel.getAvatar(requireContext()) else setDefaultAvatar()
    }

    private fun setDefaultAvatar() {
        binding.myAccountThumbnail.setImageBitmap(
            getDefaultAvatar(
                getColorAvatar(megaApi.myUser),
                viewModel.getName(),
                AVATAR_SIZE,
                true
            )
        )
    }

    fun setUpContactConnections() {
        val contacts = megaApi.contacts
        val visibleContacts = ArrayList<MegaUser>()

        for (contact in contacts.indices) {
            if (contacts[contact].visibility == MegaUser.VISIBILITY_VISIBLE
                || megaApi.getInShares(contacts[contact]).size > 0
            ) {
                visibleContacts.add(contacts[contact])
            }
        }

        binding.contactsSubtitle.text = StringResourcesUtils.getQuantityString(
            R.plurals.my_account_connections,
            visibleContacts.size,
            visibleContacts.size
        )
    }

    fun updateMailView(email: String) {
        binding.emailText.text = email

        if (!isFileAvailable(buildAvatarFile(context, email + JPG_EXTENSION))) {
            setDefaultAvatar()
        }
    }

    fun updateAddPhoneNumberLabel() {
        runDelay(PHONE_NUMBER_CHANGE_DELAY) {
            //work around - it takes time for megaApi.smsVerifiedPhoneNumber() to return value
            val registeredPhoneNumber = megaApi.smsVerifiedPhoneNumber()
            LogUtil.logDebug("updateAddPhoneNumberLabel $registeredPhoneNumber")

            binding.phoneText.apply {
                if (!isTextEmpty(registeredPhoneNumber)) {
                    isVisible = true
                    text = registeredPhoneNumber
                } else {
                    isVisible = false
                }
            }
        }
    }

    override fun showConfirmation(isModify: Boolean) {
        this.isModify = isModify
        removeOrModifyPhoneNumberDialog =
            showRemoveOrModifyPhoneNumberConfirmDialog(context, isModify, this)
    }

    override fun reset() {
        megaApi.resetSmsVerifiedPhoneNumber(ResetPhoneNumberListener(context, this))
    }

    override fun onReset(error: MegaError) {        /*
          Reset phone number successfully or the account has reset the phone number,
          but user data hasn't refreshed successfully need to refresh user data again.
        */
        if (error.errorCode == MegaError.API_OK || error.errorCode == MegaError.API_ENOENT) {
            // Have to getUserData to refresh, otherwise, phone number remains previous value.
            megaApi.getUserData(GetUserDataListener(context, this))
        } else {
            binding.phoneText.isClickable = true
            showSnackbar(context, getString(R.string.remove_phone_number_fail))
            logWarning("Reset phone number failed: " + getTranslatedErrorString(error))
        }
    }

    override fun onUserDataUpdate(error: MegaError) {
        binding.phoneText.isClickable = true

        if (error.errorCode == MegaError.API_OK) {
            if (canVoluntaryVerifyPhoneNumber()) {
                binding.phoneText.text =
                    StringResourcesUtils.getString(R.string.add_phone_number_label)
                binding.phoneText.isVisible = true

                if (context is ManagerActivityLollipop) {
                    (context as ManagerActivityLollipop).showAddPhoneNumberInMenu();
                }

                if (isModify) {
                    startActivity(Intent(context, SMSVerificationActivity::class.java))
                } else {
                    showSnackbar(context, getString(R.string.remove_phone_number_success))
                }
            }
        } else {
            showSnackbar(context, getString(R.string.remove_phone_number_fail));
            logWarning(
                "Get user data for updating phone number failed: " + getTranslatedErrorString(
                    error
                )
            )
        }
    }

    private fun setUpObservers() {
        viewModel.onUpdateVersionsInfoFinished().observe(viewLifecycleOwner, ::refreshVersionsInfo)
        viewModel.onGetAvatarFinished().observe(viewLifecycleOwner, ::setAvatar)
    }

    private fun refreshVersionsInfo(error: MegaError) {
        if (error.errorCode == MegaError.API_OK) {
            //Update versions info
        }
    }

    private fun setAvatar(error: MegaError) {
        if (error.errorCode == API_ENOENT) {
            setDefaultAvatar()
        } else {
            setUpAvatar(false)
        }
    }
}