package mega.privacy.android.app.audioplayer

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.exoplayer2.util.Util
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.SimpleAnimatorListener
import mega.privacy.android.app.utils.autoCleared

@AndroidEntryPoint
class AudioPlayerFragment : Fragment() {
    private var binding by autoCleared<FragmentAudioPlayerBinding>()

    private lateinit var artworkContainer: CardView
    private lateinit var trackName: TextView
    private lateinit var artistName: TextView
    private lateinit var bgPlay: ImageButton
    private lateinit var bgPlayHint: TextView
    private lateinit var playlist: ImageButton

    private lateinit var playerServiceIntent: Intent
    private var playerService: AudioPlayerService? = null

    private var playlistObserved = false

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AudioPlayerServiceBinder) {
                playerService = service.service

                setupPlayer(service.service)
                tryObservePlaylist()
            }
        }
    }
    private val playerListener = object : Player.EventListener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (isResumed && mediaItem != null
                && reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            ) {
                displayMetadata(Metadata(null, null, null, mediaItem.mediaId))
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (isResumed) {
                binding.loading.isVisible = state == Player.STATE_BUFFERING
            }
        }
    }

    private var toolbarVisible = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAudioPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        artworkContainer = binding.root.findViewById(R.id.artwork_container)
        trackName = binding.root.findViewById(R.id.track_name)
        artistName = binding.root.findViewById(R.id.artist_name)
        bgPlay = binding.root.findViewById(R.id.background_play_toggle)
        bgPlayHint = binding.root.findViewById(R.id.background_play_hint)
        playlist = binding.root.findViewById(R.id.playlist)

        binding.toolbar.navigationIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back_white)!!.mutate()
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }

        binding.toolbar.inflateMenu(R.menu.audio_player)
        binding.toolbar.setOnMenuItemClickListener(object : Toolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.save_to_device -> {
                        return true
                    }
                    R.id.properties -> {
                        return true
                    }
                    R.id.send_to_chat -> {
                        return true
                    }
                    R.id.get_link -> {
                        return true
                    }
                    R.id.remove_link -> {
                        return true
                    }
                    R.id.rename -> {
                        return true
                    }
                    R.id.move -> {
                        return true
                    }
                    R.id.copy -> {
                        return true
                    }
                    R.id.move_to_trash -> {
                        return true
                    }
                }
                return false
            }
        })

        tryObservePlaylist()

        runDelay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS) {
            if (isResumed) {
                hideToolbar()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = activity?.intent?.extras ?: return
        playerServiceIntent = Intent(requireContext(), AudioPlayerService::class.java)
        playerServiceIntent.putExtras(extras)
        playerServiceIntent.setDataAndType(activity?.intent?.data, activity?.intent?.type)
        Util.startForegroundService(requireContext(), playerServiceIntent)

        requireContext().bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()

        val service = playerService
        if (service != null) {
            setupPlayer(service)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        playerService?.exoPlayer?.removeListener(playerListener)
        playerService?.mainPlayerUIClosed()
        playerService = null
        requireContext().unbindService(connection)
    }

    private fun tryObservePlaylist() {
        val service = playerService
        if (!playlistObserved && service != null) {
            playlistObserved = true

            service.viewModel.playlist.observe(viewLifecycleOwner) {
                playlist.isEnabled = it.first.size > 1
            }
        }
    }

    private fun setupPlayer(service: AudioPlayerService) {
        setupPlayerView(service.exoPlayer)
        observeMetadata(service.metadata)

        // we need setup control buttons again, because reset player would reset
        // PlayerControlView
        setupButtons()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPlayerView(player: SimpleExoPlayer) {
        binding.playerView.player = player

        binding.playerView.useController = true
        binding.playerView.controllerShowTimeoutMs = 0
        binding.playerView.controllerHideOnTouch = false
        binding.playerView.setShowShuffleButton(true)
        binding.playerView.setRepeatToggleModes(
            RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE or RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
        )

        binding.playerView.showController()

        binding.playerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (toolbarVisible) {
                    hideToolbar()
                } else {
                    showToolbar()
                }
            }
            true
        }

        binding.loading.isVisible = player.playbackState == Player.STATE_BUFFERING
        player.addListener(playerListener)

        post {
            val artworkWidth = resources.displayMetrics.widthPixels / 3 * 2
            val controllerHeight =
                resources.getDimensionPixelSize(R.dimen.audio_player_main_controller_height)

            val layoutParams = artworkContainer.layoutParams as FrameLayout.LayoutParams
            layoutParams.width = artworkWidth
            layoutParams.height = artworkWidth
            layoutParams.topMargin =
                (binding.playerView.measuredHeight - artworkWidth - controllerHeight) / 2
            artworkContainer.layoutParams = layoutParams

            artworkContainer.isVisible = true
        }
    }

    private fun observeMetadata(metadata: LiveData<Metadata>) {
        metadata.observe(viewLifecycleOwner, this::displayMetadata)
    }

    private fun displayMetadata(metadata: Metadata) {
        if (metadata.title != null && metadata.artist != null) {
            if (trackName.text.isEmpty()) {
                displayTrackAndArtist(trackName, artistName, metadata)
            } else {
                animateTrackAndArtist(trackName, false) {
                    displayTrackAndArtist(trackName, artistName, metadata)
                }

                if (artistName.isVisible) {
                    animateTrackAndArtist(artistName, false)
                }
            }
        } else {
            setTrackNameBottomMargin(trackName, false)
            val needAnimate = trackName.text != metadata.nodeName
            trackName.text = metadata.nodeName
            if (needAnimate) {
                animateTrackAndArtist(trackName, true)
            }

            artistName.isVisible = false
        }
    }

    private fun displayTrackAndArtist(
        trackName: TextView,
        artistName: TextView,
        metadata: Metadata
    ) {
        setTrackNameBottomMargin(trackName, true)
        trackName.text = metadata.title
        animateTrackAndArtist(trackName, true)

        artistName.isVisible = true
        artistName.text = metadata.artist
        animateTrackAndArtist(artistName, true)
    }

    private fun animateTrackAndArtist(
        textView: TextView,
        showing: Boolean,
        listener: (() -> Unit)? = null
    ) {
        textView.alpha = if (showing) 0F else 1F

        val animator = textView.animate()
        animator.cancel()

        if (listener != null) {
            animator.setListener(object : SimpleAnimatorListener() {
                override fun onAnimationEnd(animation: Animator?) {
                    animator.setListener(null)

                    listener()
                }
            })
        }

        animator
            .setDuration(AUDIO_PLAYER_TRACK_NAME_FADE_DURATION_MS)
            .alpha(if (showing) 1F else 0F)
            .start()
    }

    private fun setTrackNameBottomMargin(trackName: TextView, small: Boolean) {
        val params = trackName.layoutParams as ConstraintLayout.LayoutParams
        params.bottomMargin = resources.getDimensionPixelSize(
            if (small) R.dimen.audio_player_track_name_margin_bottom_small
            else R.dimen.audio_player_track_name_margin_bottom_large
        )
        trackName.layoutParams = params
    }

    private fun setupButtons() {
        setupBgPlaySetting()
        setupPlaylistButton()
    }

    private fun setupBgPlaySetting() {
        val enabled = playerService?.backgroundPlayEnabled() ?: return
        updateBgPlay(bgPlay, bgPlayHint, enabled)

        bgPlay.setOnClickListener {
            val service = playerService ?: return@setOnClickListener

            service.toggleBackgroundPlay()
            updateBgPlay(bgPlay, bgPlayHint, service.backgroundPlayEnabled())
        }
    }

    private fun updateBgPlay(bgPlay: ImageButton, bgPlayHint: TextView, enabled: Boolean) {
        bgPlay.setImageResource(
            if (enabled) R.drawable.player_play_bg_on else R.drawable.player_play_bg_off
        )

        bgPlayHint.setText(
            if (enabled) R.string.background_play_hint else R.string.not_background_play_hint
        )
        bgPlayHint.alpha = 1F

        bgPlayHint.animate()
            .setDuration(AUDIO_PLAYER_BACKGROUND_PLAY_HINT_FADE_OUT_DURATION_MS)
            .alpha(0F)
    }

    private fun setupPlaylistButton() {
        playlist.setOnClickListener {
            findNavController().navigate(R.id.action_player_to_playlist)
        }
    }

    private fun hideToolbar() {
        toolbarVisible = false

        binding.toolbar.animate()
            .translationY(-binding.toolbar.measuredHeight.toFloat())
            .setDuration(AUDIO_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
            .start()
    }

    private fun showToolbar() {
        toolbarVisible = true

        binding.toolbar.animate()
            .translationY(0F)
            .setDuration(AUDIO_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
            .start()
    }
}
