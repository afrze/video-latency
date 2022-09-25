package com.video.latency

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer.MetricsConstants.PLAYING
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazonaws.ivs.broadcast.BroadcastException
import com.amazonaws.ivs.broadcast.BroadcastSession
import com.amazonaws.ivs.broadcast.Presets
import com.amazonaws.ivs.player.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList


class PlayerActivity : AppCompatActivity() {
    lateinit var broadcastSession: BroadcastSession
    val URL =
        "https://ac95a9cbc2b6.ap-northeast-1.playback.live-video.net/api/video/v1/ap-northeast-1.139721864149.channel.k789KABGD7qf.m3u8"
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val requiredPermissions =
            arrayOf<String>(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this, permission!!
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // If any permissions are missing we want to just request them all.
                ActivityCompat.requestPermissions(this, requiredPermissions, 0x100)
                break
            } else {
                setupBroadcastSession()
            }
        }
        broadcastSession.start(
            "rtmps://ac95a9cbc2b6.global-contribute.live-video.net:443/app/",
            "sk_ap-northeast-1_uzUfAPPjGDfI_2aI4fhJKCsBSwwYy5WSnGY6WSV3o2X"
        );

        playerView = findViewById(R.id.playerView)

        // Load Uri to play
        playerView.player.load(Uri.parse(URL))

        // Set PlaybackRate
        setPlaybackrate()

        // Set Listener for Player callback events
        handlePlayerEvents()
    }


    override fun onPause() {
        super.onPause()
        broadcastSession.stop()
    }

    override fun onStart() {
        super.onStart()
        playerView.player.play()
    }

    override fun onStop() {
        super.onStop()
        playerView.player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerView.player.release()
    }

    private fun setPlaybackrate() {
        //val rateSpinner = findViewById(R.id.rate_spinner) as Spinner
        // Set playback rate, must be a floating point value
        // playerView.player.setPlaybackRate(0.5f)
    }

    private fun updateQuality() {
//        val qualitySpinner = findViewById(R.id.quality_spinner) as Spinner
//        var auto = "auto"
//        val currentQuality: String = playerView.player.getQuality().getName()
//        if (playerView.player.isAutoQualityMode() && !TextUtils.isEmpty(currentQuality)) {
//            auto += " ($currentQuality)"
//        }
//        var selected = 0
//        val names: ArrayList<String?> = ArrayList()
//        for (quality in playerView.player.getQualities()) {
//            names.add(quality.name)
//        }
//        names.add(0, auto)
//        if (!playerView.player.isAutoQualityMode()) {
//            for (i in 0 until names.size) {
//                if (names.get(i).equals(currentQuality)) {
//                    selected = i
//                }
//            }
//        }
//        val qualityAdapter: ArrayAdapter<String?> = ArrayAdapter<String?>(this,
//            android.R.layout.simple_spinner_item, names)
//        qualityAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
//        qualitySpinner.setOnItemSelectedListener(null)
//        qualitySpinner.setAdapter(qualityAdapter)
//        qualitySpinner.setSelection(selected, false)
//        qualitySpinner.setOnItemSelectedListener(object : OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
//                val name = qualityAdapter.getItem(position)
//                if (name != null && name.startsWith("auto")) {
//                    playerView.player.setAutoQualityMode(true)
//                } else {
//                    for (quality in playerView.player.getQualities()) {
//                        if (quality.name.equals(name, ignoreCase = true)) {
//                            Log.i("IVSPlayer", "Quality Selected: " + quality);
//                            playerView.player.setQuality(quality)
//                            break
//                        }
//                    }
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//        })
    }

    /**
     * Demonstration for what callback APIs are available to Listen for Player events.
     */
    private fun handlePlayerEvents() {
        playerView.player.apply {
            // Listen to changes on the player
            addListener(object : Player.Listener() {
                override fun onAnalyticsEvent(p0: String, p1: String) {}
                override fun onDurationChanged(p0: Long) {
                    // If the video is a VOD, you can seek to a duration in the video
                    Log.i("IVSPlayer", "New duration: $duration")
                    seekTo(p0)
                }

                override fun onError(p0: PlayerException) {}
                override fun onMetadata(type: String, data: ByteBuffer) {}
                override fun onQualityChanged(p0: Quality) {
                    Log.i("IVSPlayer", "Quality changed to " + p0);
                    updateQuality()
                }

                override fun onRebuffering() {}
                override fun onSeekCompleted(p0: Long) {}
                override fun onVideoSizeChanged(p0: Int, p1: Int) {}
                override fun onCue(cue: Cue) {
                    when (cue) {
                        is TextMetadataCue -> Log.i(
                            "IVSPlayer", "Received Text Metadata: ${cue.text}"
                        )
                    }
                }

                override fun onStateChanged(state: Player.State) {
                    Log.i("PlayerLog", "Current state: ${state}")
                    when (state) {
                        Player.State.BUFFERING, Player.State.READY -> {
                            updateQuality()
                        }
                        Player.State.IDLE, Player.State.ENDED -> {
                            // no-op
                        }
                        Player.State.PLAYING -> {
                            // Qualities will be dependent on the video loaded, and can
                            // be retrieved from the player
                            Log.i("IVSPlayer", "Available Qualities: ${qualities}")
                        }
                    }
                }
            })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )
        if (requestCode == 0x100) {
            for (result in grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    return
                }
            }
            Log.d("tag", "here")
            setupBroadcastSession()
        }
    }

    private fun setupBroadcastSession() {
        val broadcastListener: BroadcastSession.Listener = object : BroadcastSession.Listener() {
            override fun onStateChanged(state: BroadcastSession.State) {
                Log.d("TAG", "State=$state")
            }

            override fun onError(exception: BroadcastException) {
                Log.e("TAG", "Exception: $exception")
            }
        }

        val ctx: Context = applicationContext
        broadcastSession = BroadcastSession(
            ctx,
            broadcastListener,
            Presets.Configuration.STANDARD_PORTRAIT,
            Presets.Devices.BACK_CAMERA(ctx)
        )
    }
}