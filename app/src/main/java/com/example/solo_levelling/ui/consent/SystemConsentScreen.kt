package com.example.solo_levelling.ui.consent

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemTertiary

internal const val ConsentEyebrow = "SYSTEM"
internal const val ConsentHeading = "TURN INTENTION INTO PROGRESS"
internal const val ConsentWhat =
    "You choose the areas of your life you want to improve. The system turns those intentions into clear actions you can take each day."
internal const val ConsentWhy =
    "Progress is easier to keep when you can see it. Everyday effort becomes something you can understand, measure, and improve."
internal const val ConsentHow =
    "Only the areas you select are tracked. Work on one, or several — progress is measured within what you choose, and nothing else becomes part of the plan."
internal const val ConsentQuestion = "DO YOU CHOOSE TO ACCEPT?"
internal const val ConsentAgreeLine =
    "By continuing, you allow the system to use the information you provide to build and track your personal progression."
internal const val ConsentContinueLabel = "CONTINUE"
internal const val ConsentDeclineLabel = "NOT FOR ME"
internal const val ConsentDeclineTitle = "THE SYSTEM IS NOT FOR YOU"
internal const val ConsentDeclineBody =
    "This System is designed for people who want to actively track their progress and work toward meaningful improvement.\n\nYou can leave at any time."
internal const val ConsentExitLabel = "EXIT SYSTEM"
internal const val ConsentGoBackLabel = "GO BACK"

@Composable
fun SystemConsentScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var continuing by remember { mutableStateOf(false) }
    var showDecline by remember { mutableStateOf(false) }

    BackHandler {
        showDecline = true
    }

    if (showDecline) {
        AlertDialog(
            onDismissRequest = { showDecline = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            title = {
                Text(
                    text = "[ SYSTEM NOTICE ]",
                    fontFamily = JetBrainsMono,
                    color = SystemTertiary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = ConsentDeclineTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = ConsentDeclineBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GlassSurface(level = GlassLevel.Level1) {
                        Text(
                            text = "> STATUS: ACCESS_DECLINED\n> ACTION: EXIT_OR_RETURN",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemTertiary.copy(alpha = 0.8f),
                        )
                    }
                }
            },
            confirmButton = {
                SystemActionButton(
                    label = ConsentExitLabel,
                    onClick = {
                        showDecline = false
                        (context as? Activity)?.finish()
                    },
                )
            },
            dismissButton = {
                GhostTextButton(
                    label = ConsentGoBackLabel,
                    onClick = { showDecline = false },
                )
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            SystemSectionHeader(tag = ConsentEyebrow)
            Text(
                text = ConsentHeading,
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = SystemPrimary,
            )
            GlassSurface(
                level = GlassLevel.Level2,
                modifier = Modifier.semantics {
                    contentDescription = "$ConsentWhat $ConsentWhy $ConsentHow"
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = ConsentWhat,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = ConsentWhy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = ConsentHow,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = ConsentQuestion,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = SystemPrimary,
            )
            Text(
                text = ConsentAgreeLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            SystemActionButton(
                label = ConsentContinueLabel,
                onClick = {
                    if (continuing) return@SystemActionButton
                    continuing = true
                    onContinue()
                },
                enabled = !continuing,
                modifier = Modifier.fillMaxWidth(),
            )
            SystemActionButton(
                label = ConsentDeclineLabel,
                onClick = { showDecline = true },
                primary = false,
                enabled = !continuing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
