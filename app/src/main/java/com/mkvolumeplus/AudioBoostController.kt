package com.mkvolumeplus

import android.media.AudioManager
import android.os.Handler
import android.os.Looper

class AudioBoostController(
    private val audioManager: AudioManager
) {

    private val handler =
        Handler(Looper.getMainLooper())

    fun setEarpieceBoosted(boosted: Boolean) {
        applyAndVerify(
            profile = PROFILE_RECEIVER,
            table = if (boosted) {
                RECEIVER_BOOST_TABLE
            } else {
                RECEIVER_STOCK_TABLE
            }
        )
    }

    fun setSpeakerphoneBoosted(boosted: Boolean) {
        applyAndVerify(
            profile = PROFILE_SPEAKER,
            table = if (boosted) {
                SPEAKER_BOOST_TABLE
            } else {
                SPEAKER_STOCK_TABLE
            }
        )
    }

    fun restoreAll() {
        setEarpieceBoosted(false)
        setSpeakerphoneBoosted(false)
    }

    fun readCurrentState(): CallVolumeState {
        val receiverTables =
            readTables(PROFILE_RECEIVER)

        val speakerTables =
            readTables(PROFILE_SPEAKER)

        return CallVolumeState(
            earpiece = determineProfileState(
                tables = receiverTables,
                stockTable = RECEIVER_STOCK_TABLE,
                boostTable = RECEIVER_BOOST_TABLE
            ),
            speakerphone = determineProfileState(
                tables = speakerTables,
                stockTable = SPEAKER_STOCK_TABLE,
                boostTable = SPEAKER_BOOST_TABLE
            )
        )
    }

    private fun applyAndVerify(
        profile: String,
        table: String
    ) {
        BANDS.forEach { band ->
            audioManager.setParameters(
                "APP_SET_PARAM=SpeechVol#" +
                        "Band,$band,Profile,$profile,Network,GSM#" +
                        "dl_gain#$table"
            )
        }

        audioManager.setParameters(
            "UpdateHALCustGainTable=1"
        )

        val actualTables =
            readTables(profile)

        check(
            actualTables.values.all { it == table }
        ) {
            "$profile table verification failed."
        }

        pulseVoiceCallVolumeIfActive()
    }

    private fun pulseVoiceCallVolumeIfActive() {
        val mode =
            audioManager.mode

        if (
            mode != AudioManager.MODE_IN_CALL &&
            mode != AudioManager.MODE_IN_COMMUNICATION
        ) {
            return
        }

        val streamType =
            AudioManager.STREAM_VOICE_CALL

        val currentVolume =
            audioManager.getStreamVolume(streamType)

        val minimumVolume =
            audioManager.getStreamMinVolume(streamType)

        val maximumVolume =
            audioManager.getStreamMaxVolume(streamType)

        val temporaryVolume =
            when {
                currentVolume > minimumVolume ->
                    currentVolume - 1

                currentVolume < maximumVolume ->
                    currentVolume + 1

                else ->
                    return
            }

        audioManager.setStreamVolume(
            streamType,
            temporaryVolume,
            0
        )

        handler.postDelayed(
            {
                audioManager.setStreamVolume(
                    streamType,
                    currentVolume,
                    0
                )
            },
            VOLUME_PULSE_DELAY_MS
        )
    }

    private fun readTables(
        profile: String
    ): Map<String, String> {
        return BANDS.associateWith { band ->
            val response =
                audioManager.getParameters(
                    "APP_GET_PARAM#SpeechVol#" +
                            "Band,$band,Profile,$profile," +
                            "Network,GSM#dl_gain"
                )

            response.substringAfter(
                delimiter = "=",
                missingDelimiterValue = response
            )
        }
    }

    private fun determineProfileState(
        tables: Map<String, String>,
        stockTable: String,
        boostTable: String
    ): ProfileState {
        return when {
            tables.values.all { it == stockTable } ->
                ProfileState.STOCK

            tables.values.all { it == boostTable } ->
                ProfileState.BOOSTED

            else ->
                ProfileState.UNKNOWN
        }
    }

    companion object {
        private const val PROFILE_RECEIVER = "RCV"
        private const val PROFILE_SPEAKER = "SPK"

        private const val RECEIVER_STOCK_TABLE =
            "21,18,15,12,9,6,3"

        private const val RECEIVER_BOOST_TABLE =
            "21,18,15,12,9,0,3"

        private const val SPEAKER_STOCK_TABLE =
            "22,19,16,13,10,7,4"

        private const val SPEAKER_BOOST_TABLE =
            "22,19,16,13,10,0,4"

        private const val VOLUME_PULSE_DELAY_MS =
            75L

        private val BANDS =
            listOf("NB", "WB", "SWB")
    }
}

data class CallVolumeState(
    val earpiece: ProfileState,
    val speakerphone: ProfileState
)

enum class ProfileState(
    val displayName: String
) {
    STOCK("STOCK"),
    BOOSTED("BOOSTED"),
    UNKNOWN("UNKNOWN")
}
