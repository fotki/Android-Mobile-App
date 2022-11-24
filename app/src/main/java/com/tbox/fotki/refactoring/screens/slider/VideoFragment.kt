package com.tbox.fotki.refactoring.screens.slider

import android.graphics.Color
import android.media.session.PlaybackState.STATE_SKIPPING_TO_NEXT
import android.media.session.PlaybackState.STATE_SKIPPING_TO_PREVIOUS
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.tbox.fotki.R
import com.tbox.fotki.model.entities.ParcelableItem
import com.tbox.fotki.refactoring.api.backFragment
import com.tbox.fotki.util.L
import com.tbox.fotki.util.Utility
import com.tbox.fotki.util.sync_files.PreferenceHelper
import kotlinx.android.synthetic.main.fragment_video.*


class VideoFragment : Fragment(), View.OnTouchListener {

    private val utils = Utility.instance
    private var startedWidth: Int = 0
    private var startedHeigth: Int = 0

    private lateinit var controller: MediaController
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        L.print(this, "Video view onCreateView!!!")
        return inflater.inflate(R.layout.fragment_video, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        L.print(this, "Video view onActivityCreated")
        super.onActivityCreated(savedInstanceState)
        clContainer.setOnTouchListener { _, event -> gesture.onTouchEvent(event) }
        playerView.setOnTouchListener { _, event -> gesture.onTouchEvent(event) }

        arguments?.let { args ->
            val item = args.getParcelable<ParcelableItem>("item")
            initializePlayer(item!!)
        }
    }

    val gesture: GestureDetector = GestureDetector(
        activity,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return false
            }

            override fun onFling(
                e1: MotionEvent, e2: MotionEvent, velocityX: Float,
                velocityY: Float
            ): Boolean {

                val position = arguments?.getInt("position") ?: 0
                val size = arguments?.getInt("size") ?: 0


                L.print(this, "onFling has been called! position - $position size - $size")
                val SWIPE_MIN_DISTANCE = 120
                val SWIPE_MAX_OFF_PATH = 250
                val SWIPE_THRESHOLD_VELOCITY = 200
                try {
                    val ph = PreferenceHelper(requireContext())
                    val selected = ph.getInt("mSelectedPosition")
                    if (Math.abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) return false
                    if (e1.x - e2.x > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                    ) {
                        if (position < (size - 1)) {
                            clContainer.setOnTouchListener { _, event -> false }
                            ph.applyPrefs(
                                hashMapOf("mSelectedPosition" to selected + 1)
                            )
                        }
                        backFragment()
                        L.print(this, "Right to Left $selected")

                    } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                    ) {
                        if (position > 0) {
                            clContainer.setOnTouchListener { _, event -> false }
                            ph.applyPrefs(
                                hashMapOf("mSelectedPosition" to selected - 1)
                            )
                        }
                        backFragment()
                        L.print(this, "Left to Right $selected")
                    }
                } catch (e: Exception) {
                    // nothing
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        return false
    }

    private val TAG = "TEST12345"

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var mediaDataSourceFactory: DataSource.Factory
    private var currentPlayingVodPosition = -1

    private fun initializePlayer(parcelableItem: ParcelableItem) {
        val url = parcelableItem.mVideoUrl
        if (parcelableItem.mInaccessable == 0) {
            Utility.instance.showOkDialog(
                requireContext(),
                "The video is processing... Please open again later"
            ) {
                backFragment()
            }
            return
        }
        if (parcelableItem.mInaccessable == 2) {
            Utility.instance.showOkDialog(requireContext(), "The video didn't convert fine.") {
                backFragment()
            }
            return
        }
        /*if (parcelableItem.mInaccessable == 1) {
            Utility.instance.showOkDialog(
                requireContext(),
                "The video is processing... Please open again later"
            ) {
                backFragment()
            }
            return
        }*/

        utils.showProgressDialog(requireContext(), "Loading video...")
        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(requireActivity())

        mediaDataSourceFactory = DefaultDataSourceFactory(
            requireContext(),
            Util.getUserAgent(requireContext(), "mediaPlayerSample")
        )


        val mediaSource =
            ProgressiveMediaSource.Factory(mediaDataSourceFactory).createMediaSource(Uri.parse(url))

        simpleExoPlayer.prepare(mediaSource, false, false)
        simpleExoPlayer.playWhenReady = true

        playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        playerView.player = simpleExoPlayer
        playerView.requestFocus()
        val next_bt = playerView.findViewById<ImageButton>(R.id.exo_next);
        next_bt.setOnClickListener {
            val ph = PreferenceHelper(requireContext())
            val selected = ph.getInt("mSelectedPosition")
            val position = arguments?.getInt("position") ?: 0
            val size = arguments?.getInt("size") ?: 0

            if (position < (size - 1)) {
                clContainer.setOnTouchListener { _, event -> false }
                ph.applyPrefs(
                    hashMapOf("mSelectedPosition" to selected + 1)
                )
            }
            backFragment()
           Log.d("skipToNext","next bt clicked")

        }

        simpleExoPlayer.addListener(object : Player.EventListener {

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                Log.d(TAG, "onPlaybackParametersChanged: ")
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray?,
                trackSelections: TrackSelectionArray?
            ) {
                currentPlayingVodPosition =  simpleExoPlayer.currentWindowIndex
                Log.d(TAG, "onTracksChanged: ")
                if (currentPlayingVodPosition-1 == simpleExoPlayer.currentWindowIndex){
                    Toast.makeText(requireContext(),"prev clicked",Toast.LENGTH_SHORT).show()

                }
                if (currentPlayingVodPosition+1 == simpleExoPlayer.currentWindowIndex){
                    Toast.makeText(requireContext(),"next clicked",Toast.LENGTH_SHORT).show()

                }
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                Log.d(TAG, "onPlayerError: ")
            }

            /** 4 playbackState exists */
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    STATE_BUFFERING -> {
                        progressBar?.let {
                            progressBar.visibility = View.VISIBLE
                        }
                        Log.d(TAG, "onPlayerStateChanged - STATE_BUFFERING")
                        //requireContext().toast("onPlayerStateChanged - STATE_BUFFERING")
                    }
                    STATE_READY -> {
                        progressBar?.let {
                            progressBar.visibility = View.INVISIBLE
                        }
                        utils.dismissProgressDialog()
                        Log.d(TAG, "onPlayerStateChanged - STATE_READY")
                        //requireContext().toast("onPlayerStateChanged - STATE_READY")
                    }
                    STATE_IDLE -> {
                        Log.d(TAG, "onPlayerStateChanged - STATE_IDLE")
                        //requireContext().toast("onPlayerStateChanged - STATE_IDLE")
                    }
                    STATE_ENDED -> {
                        Log.d(TAG, "onPlayerStateChanged - STATE_ENDED")
                        //requireContext().toast("onPlayerStateChanged - STATE_ENDED")
                    }
                    STATE_SKIPPING_TO_NEXT ->{
                        val ph = PreferenceHelper(requireContext())
                        val selected = ph.getInt("mSelectedPosition")
                        val position = arguments?.getInt("position") ?: 0
                        val size = arguments?.getInt("size") ?: 0

                        if (position < (size - 1)) {
                            clContainer.setOnTouchListener { _, event -> false }
                            ph.applyPrefs(
                                hashMapOf("mSelectedPosition" to selected + 1)
                            )
                        }
                        backFragment()
                        L.print(this, "Right to Left $selected")


                    }
                    STATE_SKIPPING_TO_PREVIOUS ->{
                        val ph = PreferenceHelper(requireContext())
                        val selected = ph.getInt("mSelectedPosition")
                        val position = arguments?.getInt("position") ?: 0
                        if (position > 0) {
                            clContainer.setOnTouchListener { _, event -> false }
                            ph.applyPrefs(
                                hashMapOf("mSelectedPosition" to selected - 1)
                            )
                        }
                        backFragment()

                    }
                }
            }

            override fun onLoadingChanged(isLoading: Boolean) {
                Log.d(TAG, "onLoadingChanged: ")
            }

            override fun onPositionDiscontinuity(reason: Int) {
                Log.d(TAG, "onPositionDiscontinuity: ")
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                Log.d(TAG, "onRepeatModeChanged: ")

                //Toast.makeText(baseContext, "repeat mode changed", Toast.LENGTH_SHORT).show()
            }

            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                Log.d(TAG, "onTimelineChanged: ")
            }
        })

    }

    override fun onStop() {
        try{
            releasePlayer()
        } catch (ex:Exception){ }
        super.onStop()
    }

    private fun releasePlayer() {
        simpleExoPlayer.release()
    }

}