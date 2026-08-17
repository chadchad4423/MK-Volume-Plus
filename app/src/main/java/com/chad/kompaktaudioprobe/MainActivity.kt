package com.chad.kompaktaudioprobe

import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioManager =
            getSystemService(AUDIO_SERVICE) as AudioManager

        val audioController =
            AudioBoostController(audioManager)

        val preferences =
            BoostPreferences(this)

        val currentBootCount =
            Settings.Global.getInt(
                contentResolver,
                Settings.Global.BOOT_COUNT,
                -1
            )

        val initialAudioState =
            runCatching {
                audioController.readCurrentState()
            }.getOrElse {
                CallVolumeState(
                    earpiece = ProfileState.UNKNOWN,
                    speakerphone = ProfileState.UNKNOWN
                )
            }

        setContent {
            MaterialTheme {
                var audioState by remember {
                    mutableStateOf(initialAudioState)
                }

                var reapplyAfterBoot by remember {
                    mutableStateOf(
                        preferences.reapplyAfterBoot
                    )
                }

                var lastSuccessfulReapplyBoot by remember {
                    mutableStateOf(
                        preferences.lastSuccessfulReapplyBoot
                    )
                }

                var pendingSafetyUnlock by remember {
                    mutableStateOf<PendingUnlock?>(null)
                }

                var message by remember {
                    mutableStateOf("")
                }

                val reapplyPending =
                    reapplyAfterBoot &&
                            currentBootCount >= 0 &&
                            lastSuccessfulReapplyBoot !=
                            currentBootCount

                fun refreshState() {
                    runCatching {
                        audioController.readCurrentState()
                    }.onSuccess { newState ->
                        audioState = newState

                        reapplyAfterBoot =
                            preferences.reapplyAfterBoot

                        lastSuccessfulReapplyBoot =
                            preferences.lastSuccessfulReapplyBoot

                        message = ""
                    }.onFailure { error ->
                        message =
                            "Read failed: ${error.message}"
                    }
                }

                fun setEarpieceUnlock(
                    enabled: Boolean
                ) {
                    runCatching {
                        audioController.setEarpieceBoosted(
                            enabled
                        )

                        preferences.earpieceBoostWanted =
                            enabled
                    }.onSuccess {
                        refreshState()
                    }.onFailure { error ->
                        message =
                            "Earpiece change failed: " +
                                    "${error.message}"
                    }
                }

                fun setSpeakerphoneUnlock(
                    enabled: Boolean
                ) {
                    runCatching {
                        audioController.setSpeakerphoneBoosted(
                            enabled
                        )

                        preferences.speakerphoneBoostWanted =
                            enabled
                    }.onSuccess {
                        refreshState()
                    }.onFailure { error ->
                        message =
                            "Speakerphone change failed: " +
                                    "${error.message}"
                    }
                }

                LaunchedEffect(reapplyPending) {
                    if (!reapplyPending) {
                        return@LaunchedEffect
                    }

                    while (
                        preferences.lastSuccessfulReapplyBoot !=
                        currentBootCount
                    ) {
                        delay(STARTUP_CHECK_INTERVAL_MS)
                    }

                    lastSuccessfulReapplyBoot =
                        preferences.lastSuccessfulReapplyBoot

                    refreshState()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    contentColor = Color.Black
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        MuditaTopAppBar(
                            title = "MK Volume+",
                            version = "v1.1"
                        )

                        if (pendingSafetyUnlock != null) {
                            SafetyWarningContent(
                                onCancel = {
                                    pendingSafetyUnlock = null
                                },
                                onAcknowledge = {
                                    val requestedUnlock =
                                        pendingSafetyUnlock

                                    preferences
                                        .safetyWarningAcknowledged =
                                        true

                                    pendingSafetyUnlock = null

                                    when (requestedUnlock) {
                                        PendingUnlock.EARPIECE ->
                                            setEarpieceUnlock(true)

                                        PendingUnlock.SPEAKERPHONE ->
                                            setSpeakerphoneUnlock(true)

                                        null ->
                                            Unit
                                    }
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 20.dp
                                    )
                            ) {
                                SectionHeading(
                                    text = "Volume Unlock"
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text =
                                        if (reapplyPending) {
                                            "Please wait while call volume settings are restored."
                                        } else {
                                            "Unlocks a louder maximum volume for calls."
                                        },
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    fontWeight =
                                        FontWeight.Normal
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(14.dp)
                                )

                                SettingsSwitchRow(
                                    title = "Earpiece",
                                    detail =
                                        if (reapplyPending) {
                                            "Restoring after reboot"
                                        } else {
                                            when (
                                                audioState.earpiece
                                            ) {
                                                ProfileState.UNKNOWN ->
                                                    "Status unknown"

                                                else ->
                                                    "Up to +6 dB"
                                            }
                                        },
                                    checked =
                                        audioState.earpiece ==
                                                ProfileState.BOOSTED,
                                    enabled =
                                        !reapplyPending &&
                                                audioState.earpiece !=
                                                ProfileState.UNKNOWN,
                                    onCheckedChange = { enabled ->
                                        if (
                                            enabled &&
                                            !preferences
                                                .safetyWarningAcknowledged
                                        ) {
                                            pendingSafetyUnlock =
                                                PendingUnlock.EARPIECE
                                        } else {
                                            setEarpieceUnlock(
                                                enabled
                                            )
                                        }
                                    }
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                SettingsSwitchRow(
                                    title = "Speakerphone",
                                    detail =
                                        if (reapplyPending) {
                                            "Restoring after reboot"
                                        } else {
                                            when (
                                                audioState.speakerphone
                                            ) {
                                                ProfileState.UNKNOWN ->
                                                    "Status unknown"

                                                else ->
                                                    "Up to +7 dB"
                                            }
                                        },
                                    checked =
                                        audioState.speakerphone ==
                                                ProfileState.BOOSTED,
                                    enabled =
                                        !reapplyPending &&
                                                audioState.speakerphone !=
                                                ProfileState.UNKNOWN,
                                    onCheckedChange = { enabled ->
                                        if (
                                            enabled &&
                                            !preferences
                                                .safetyWarningAcknowledged
                                        ) {
                                            pendingSafetyUnlock =
                                                PendingUnlock.SPEAKERPHONE
                                        } else {
                                            setSpeakerphoneUnlock(
                                                enabled
                                            )
                                        }
                                    }
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(18.dp)
                                )

                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = Color.Black
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(18.dp)
                                )

                                SectionHeading(
                                    text = "Startup"
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(10.dp)
                                )

                                SettingsSwitchRow(
                                    title = "Reapply after reboot",
                                    detail =
                                        "Reapply enabled volume unlocks",
                                    checked = reapplyAfterBoot,
                                    enabled = true,
                                    onCheckedChange = { enabled ->
                                        preferences.reapplyAfterBoot =
                                            enabled

                                        if (
                                            enabled &&
                                            currentBootCount >= 0
                                        ) {
                                            preferences
                                                .earpieceBoostWanted =
                                                audioState.earpiece ==
                                                        ProfileState.BOOSTED

                                            preferences
                                                .speakerphoneBoostWanted =
                                                audioState.speakerphone ==
                                                        ProfileState.BOOSTED

                                            preferences
                                                .lastSuccessfulReapplyBoot =
                                                currentBootCount

                                            lastSuccessfulReapplyBoot =
                                                currentBootCount
                                        }

                                        reapplyAfterBoot =
                                            enabled
                                    }
                                )

                                if (message.isNotBlank()) {
                                    Spacer(
                                        modifier =
                                            Modifier.height(20.dp)
                                    )

                                    Text(
                                        text = message,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        fontWeight =
                                            FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val STARTUP_CHECK_INTERVAL_MS =
            500L
    }
}

private enum class PendingUnlock {
    EARPIECE,
    SPEAKERPHONE
}

@Composable
private fun SafetyWarningContent(
    onCancel: () -> Unit,
    onAcknowledge: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 20.dp
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Warning!",
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text =
                    "MK Volume+ allows call volume to exceed the device’s stock maximum. " +
                            "High volume may damage the speaker or even damage your hearing. " +
                            "Use with caution.",
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            WarningActionButton(
                text = "Cancel",
                primary = false,
                onClick = onCancel
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            WarningActionButton(
                text = "I understand",
                primary = true,
                onClick = onAcknowledge
            )
        }
    }
}

@Composable
private fun WarningActionButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    val interactionSource =
        remember { MutableInteractionSource() }

    val shape =
        RoundedCornerShape(4.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color =
                    if (primary) {
                        Color.Black
                    } else {
                        Color.White
                    },
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color =
                if (primary) {
                    Color.White
                } else {
                    Color.Black
                }
        )
    }
}

@Composable
private fun MuditaTopAppBar(
    title: String,
    version: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(67.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = version,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.Black)
        )
    }
}

@Composable
private fun SectionHeading(
    text: String
) {
    Text(
        text = text,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = detail,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }

        MuditaSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun MuditaSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource =
        remember { MutableInteractionSource() }

    val trackShape =
        RoundedCornerShape(percent = 50)

    val accessibilityModifier =
        if (enabled) {
            Modifier
        } else {
            Modifier.semantics {
                disabled()
            }
        }

    Box(
        modifier = Modifier
            .size(56.dp)
            .then(accessibilityModifier)
            .then(
                if (enabled) {
                    Modifier.toggleable(
                        value = checked,
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Switch,
                        onValueChange = onCheckedChange
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = 48.dp,
                    height = 30.dp
                )
                .background(
                    color =
                        if (enabled && checked) {
                            Color.Black
                        } else {
                            Color.White
                        },
                    shape = trackShape
                )
                .border(
                    width = 1.dp,
                    color = Color.Black,
                    shape = trackShape
                )
        ) {
            if (enabled) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 5.dp),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        if (checked) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color =
                                    if (checked) {
                                        Color.White
                                    } else {
                                        Color.Black
                                    },
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}