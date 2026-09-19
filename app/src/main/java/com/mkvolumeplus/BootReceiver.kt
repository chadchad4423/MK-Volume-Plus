package com.mkvolumeplus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val preferences =
            BoostPreferences(context)

        if (!preferences.reapplyAfterBoot) {
            return
        }

        val currentBootCount =
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.BOOT_COUNT,
                -1
            )

        val audioManager =
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        val audioController =
            AudioBoostController(audioManager)

        runCatching {
            if (preferences.earpieceBoostWanted) {
                audioController.setEarpieceBoosted(true)
            }

            if (preferences.speakerphoneBoostWanted) {
                audioController.setSpeakerphoneBoosted(true)
            }

            preferences.lastSuccessfulReapplyBoot =
                currentBootCount
        }.onFailure { error ->
            Log.e(
                TAG,
                "Boot-time volume unlock reapply failed.",
                error
            )
        }
    }

    companion object {
        private const val TAG = "MKVolumePlusBoot"
    }
}

class BoostPreferences(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    var reapplyAfterBoot: Boolean
        get() =
            preferences.getBoolean(
                KEY_REAPPLY_AFTER_BOOT,
                false
            )

        set(value) {
            preferences
                .edit()
                .putBoolean(
                    KEY_REAPPLY_AFTER_BOOT,
                    value
                )
                .apply()
        }

    var earpieceBoostWanted: Boolean
        get() =
            preferences.getBoolean(
                KEY_EARPIECE_BOOST_WANTED,
                false
            )

        set(value) {
            preferences
                .edit()
                .putBoolean(
                    KEY_EARPIECE_BOOST_WANTED,
                    value
                )
                .apply()
        }

    var speakerphoneBoostWanted: Boolean
        get() =
            preferences.getBoolean(
                KEY_SPEAKERPHONE_BOOST_WANTED,
                false
            )

        set(value) {
            preferences
                .edit()
                .putBoolean(
                    KEY_SPEAKERPHONE_BOOST_WANTED,
                    value
                )
                .apply()
        }

    var lastSuccessfulReapplyBoot: Int
        get() =
            preferences.getInt(
                KEY_LAST_SUCCESSFUL_REAPPLY_BOOT,
                -1
            )

        set(value) {
            preferences
                .edit()
                .putInt(
                    KEY_LAST_SUCCESSFUL_REAPPLY_BOOT,
                    value
                )
                .apply()
        }

    var safetyWarningAcknowledged: Boolean
        get() =
            preferences.getBoolean(
                KEY_SAFETY_WARNING_ACKNOWLEDGED,
                false
            )

        set(value) {
            preferences
                .edit()
                .putBoolean(
                    KEY_SAFETY_WARNING_ACKNOWLEDGED,
                    value
                )
                .apply()
        }

    companion object {
        private const val PREFERENCES_NAME =
            "kompakt_call_volume_preferences"

        private const val KEY_REAPPLY_AFTER_BOOT =
            "reapply_after_boot"

        private const val KEY_EARPIECE_BOOST_WANTED =
            "earpiece_boost_wanted"

        private const val KEY_SPEAKERPHONE_BOOST_WANTED =
            "speakerphone_boost_wanted"

        private const val KEY_LAST_SUCCESSFUL_REAPPLY_BOOT =
            "last_successful_reapply_boot"

        private const val KEY_SAFETY_WARNING_ACKNOWLEDGED =
            "safety_warning_acknowledged"
    }
}
