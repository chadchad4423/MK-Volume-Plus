package com.mkvolumeplus

import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
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

                var currentScreen by remember {
                    mutableStateOf(AppScreen.MAIN)
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
                            getString(
                                R.string.error_read_failed,
                                error.message ?: getString(
                                    R.string.error_unknown_detail
                                )
                            )
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
                            getString(
                                R.string.error_earpiece_change_failed,
                                error.message ?: getString(
                                    R.string.error_unknown_detail
                                )
                            )
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
                            getString(
                                R.string.error_speakerphone_change_failed,
                                error.message ?: getString(
                                    R.string.error_unknown_detail
                                )
                            )
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

                BackHandler(
                    enabled = currentScreen != AppScreen.MAIN
                ) {
                    currentScreen = AppScreen.MAIN
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    contentColor = Color.Black
                ) {
                    when (currentScreen) {
                        AppScreen.MAIN -> Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            MuditaTopAppBar(
                                title = stringResource(
                                    R.string.product_name
                                ),
                                onMenuSelection =
                                    if (pendingSafetyUnlock == null) {
                                        { destination ->
                                            currentScreen = destination
                                        }
                                    } else {
                                        null
                                    }
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
                                    text = stringResource(
                                        R.string.volume_unlock_title
                                    )
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text =
                                        if (reapplyPending) {
                                            stringResource(
                                                R.string.volume_restore_wait
                                            )
                                        } else {
                                            stringResource(
                                                R.string.volume_unlock_description
                                            )
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
                                    title = stringResource(
                                        R.string.earpiece
                                    ),
                                    detail =
                                        if (reapplyPending) {
                                            stringResource(
                                                R.string.restoring_after_reboot
                                            )
                                        } else {
                                            when (
                                                audioState.earpiece
                                            ) {
                                                ProfileState.UNKNOWN ->
                                                    stringResource(
                                                        R.string.status_unknown
                                                    )

                                                else ->
                                                    stringResource(
                                                        R.string.earpiece_gain
                                                    )
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
                                    title = stringResource(
                                        R.string.speakerphone
                                    ),
                                    detail =
                                        if (reapplyPending) {
                                            stringResource(
                                                R.string.restoring_after_reboot
                                            )
                                        } else {
                                            when (
                                                audioState.speakerphone
                                            ) {
                                                ProfileState.UNKNOWN ->
                                                    stringResource(
                                                        R.string.status_unknown
                                                    )

                                                else ->
                                                    stringResource(
                                                        R.string.speakerphone_gain
                                                    )
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
                                    text = stringResource(
                                        R.string.startup
                                    )
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(10.dp)
                                )

                                SettingsSwitchRow(
                                    title = stringResource(
                                        R.string.reapply_after_reboot
                                    ),
                                    detail =
                                        stringResource(
                                            R.string.reapply_description
                                        ),
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

                        AppScreen.ABOUT -> InformationScreen(
                            onBack = {
                                currentScreen = AppScreen.MAIN
                            }
                        )

                        AppScreen.LIMITATIONS -> LimitationsScreen(
                            onBack = {
                                currentScreen = AppScreen.MAIN
                            }
                        )

                        AppScreen.PRIVACY -> PrivacyPolicyScreen(
                            onBack = {
                                currentScreen = AppScreen.MAIN
                            }
                        )

                        AppScreen.LICENSE -> LicenseScreen(
                            onBack = {
                                currentScreen = AppScreen.MAIN
                            }
                        )
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
private fun InformationScreen(
    onBack: () -> Unit
) {
    ThemeMMD {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            MmdDocumentTopAppBar(
                title = stringResource(R.string.about_title),
                onBack = onBack
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextMMD(
                        text = stringResource(
                            R.string.about_version,
                            stringResource(R.string.version_name)
                        ),
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                    TextMMD(
                        text = stringResource(
                            R.string.about_contact,
                            stringResource(R.string.contact_email)
                        ),
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                ) {
                    TextMMD(
                        text = stringResource(R.string.made_in_louisiana),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                    Image(
                        painter = painterResource(
                            R.drawable.louisiana_bayou
                        ),
                        contentDescription = stringResource(
                            R.string.louisiana_scene_description
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(BAYOU_ASPECT_RATIO),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}

@Composable
private fun LimitationsScreen(
    onBack: () -> Unit
) {
    val paragraphs = listOf(
        stringResource(
            R.string.limitations_paragraph_1,
            stringResource(R.string.product_name)
        ),
        stringResource(R.string.limitations_paragraph_2),
        stringResource(
            R.string.limitations_paragraph_3,
            stringResource(R.string.reapply_after_reboot)
        ),
        stringResource(R.string.limitations_paragraph_4)
    )

    ThemeMMD {
        Column(modifier = Modifier.fillMaxSize()) {
            MmdDocumentTopAppBar(
                title = stringResource(R.string.limitations_title),
                onBack = onBack
            )

            LazyColumnMMD(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        top = 8.dp,
                        end = 20.dp,
                        bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                scrollStep = 1
            ) {
                items(paragraphs) { paragraph ->
                    MmdDocumentParagraph(text = paragraph)
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    val productName = stringResource(R.string.product_name)
    val paragraphs = listOf(
        stringResource(R.string.privacy_effective_date),
        stringResource(
            R.string.privacy_paragraph_1,
            productName
        ),
        stringResource(R.string.privacy_paragraph_2),
        stringResource(R.string.privacy_paragraph_3),
        stringResource(R.string.privacy_paragraph_4),
        stringResource(
            R.string.privacy_paragraph_5,
            productName
        ),
        stringResource(
            R.string.privacy_paragraph_6,
            productName
        ),
        stringResource(
            R.string.privacy_paragraph_7,
            productName
        ),
        stringResource(R.string.privacy_paragraph_8)
    )

    ThemeMMD {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            MmdDocumentTopAppBar(
                title = stringResource(R.string.privacy_title),
                onBack = onBack
            )

            LazyColumnMMD(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        top = 8.dp,
                        end = 4.dp,
                        bottom = 8.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(paragraphs) { paragraph ->
                    MmdDocumentParagraph(text = paragraph)
                }
            }
        }
    }
}

@Composable
private fun LicenseScreen(
    onBack: () -> Unit
) {
    ThemeMMD {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            MmdDocumentTopAppBar(
                title = stringResource(R.string.license_title),
                onBack = onBack
            )

            val licenseChunks = splitIntoDocumentItems(
                stringResource(R.string.mit_license_text)
            )

            LazyColumnMMD(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        top = 8.dp,
                        end = 4.dp,
                        bottom = 8.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(licenseChunks) { chunk ->
                    MmdDocumentParagraph(text = chunk)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MmdDocumentTopAppBar(
    title: String,
    onBack: () -> Unit
) {
    val backDescription = stringResource(R.string.navigation_back)

    TopAppBarMMD(
        title = {
            TextMMD(
                text = title,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics {
                    contentDescription = backDescription
                }
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    val centerY = size.height / 2f
                    val startX = size.width * 0.18f
                    val endX = size.width * 0.82f
                    val headX = size.width * 0.46f
                    val headOffset = size.height * 0.27f
                    val strokeWidth = 2.dp.toPx()

                    drawLine(
                        color = Color.Black,
                        start = Offset(startX, centerY),
                        end = Offset(endX, centerY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Square
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(startX, centerY),
                        end = Offset(headX, centerY - headOffset),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Square
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(startX, centerY),
                        end = Offset(headX, centerY + headOffset),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Square
                    )
                }
            }
        }
    )
}

@Composable
private fun MmdDocumentParagraph(
    text: String
) {
    TextMMD(
        text = text,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    )
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
                text = stringResource(R.string.warning_title),
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = stringResource(
                    R.string.safety_warning,
                    stringResource(R.string.product_name)
                ),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            WarningActionButton(
                text = stringResource(R.string.action_cancel),
                primary = false,
                onClick = onCancel
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            WarningActionButton(
                text = stringResource(R.string.action_acknowledge),
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
    onBack: (() -> Unit)? = null,
    onMenuSelection: ((AppScreen) -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
            if (onBack != null) {
                TopBarAction(
                    symbol = "←",
                    description = stringResource(
                        R.string.navigation_back
                    ),
                    onClick = onBack
                )

                Spacer(modifier = Modifier.size(8.dp))
            }

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 24.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold
            )

            if (onMenuSelection != null) {
                Spacer(modifier = Modifier.size(8.dp))

                Box {
                    TopBarAction(
                        symbol = "⋮",
                        description = stringResource(
                            R.string.top_bar_menu_description
                        ),
                        onClick = { menuExpanded = true }
                    )
                    DropdownMenuMMD(
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded = false
                        },
                        modifier = Modifier.widthIn(
                            min = 112.dp,
                            max = 280.dp
                        )
                    ) {
                        listOf(
                            R.string.about_title to AppScreen.ABOUT,
                            R.string.limitations_title to
                                    AppScreen.LIMITATIONS,
                            R.string.privacy_menu_title to
                                    AppScreen.PRIVACY,
                            R.string.license_menu_title to
                                    AppScreen.LICENSE
                        ).forEach { (label, destination) ->
                            DropdownMenuItemMMD(
                                text = {
                                    TextMMD(
                                        text = stringResource(label)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onMenuSelection(destination)
                                }
                            )
                        }
                    }
                }
            }
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
private fun TopBarAction(
    symbol: String,
    description: String,
    onClick: () -> Unit
) {
    val interactionSource =
        remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics {
                contentDescription = description
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (symbol == "⋮") {
            Canvas(modifier = Modifier.size(24.dp)) {
                val centerX = size.width / 2f
                val radius = 2.dp.toPx()
                val offset = 7.dp.toPx()

                drawCircle(
                    color = Color.Black,
                    radius = radius,
                    center = Offset(centerX, size.height / 2f - offset)
                )
                drawCircle(
                    color = Color.Black,
                    radius = radius,
                    center = Offset(centerX, size.height / 2f)
                )
                drawCircle(
                    color = Color.Black,
                    radius = radius,
                    center = Offset(centerX, size.height / 2f + offset)
                )
            }
        } else {
            Text(
                text = symbol,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
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

private enum class AppScreen {
    MAIN,
    ABOUT,
    LIMITATIONS,
    PRIVACY,
    LICENSE
}

private const val BAYOU_ASPECT_RATIO = 1983f / 597f
private const val MAX_DOCUMENT_ITEM_CHARS = 150

private fun splitIntoDocumentItems(text: String): List<String> =
    text.split("\n\n").flatMap { paragraph ->
        if (paragraph.length <= MAX_DOCUMENT_ITEM_CHARS) {
            listOf(paragraph)
        } else {
            paragraph.split(Regex("(?<=[.!?])\\s+"))
        }
    }
