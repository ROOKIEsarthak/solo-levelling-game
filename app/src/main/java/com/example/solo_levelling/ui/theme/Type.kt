package com.example.solo_levelling.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.solo_levelling.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/** Kept for any legacy references; maps to JetBrains Mono (system data font). */
val CascadiaCode = JetBrainsMono

private val DefaultMaterialTypography = Typography()

/** Dual-font Sovereign OS type scale: Inter for human content, Mono for system data. */
val Typography = Typography(
    displayLarge = DefaultMaterialTypography.displayLarge.copy(
        fontFamily = Inter,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.03).sp,
    ),
    displayMedium = DefaultMaterialTypography.displayMedium.copy(
        fontFamily = Inter,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.03).sp,
    ),
    displaySmall = DefaultMaterialTypography.displaySmall.copy(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).sp,
    ),
    headlineLarge = DefaultMaterialTypography.headlineLarge.copy(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.02).sp,
    ),
    headlineMedium = DefaultMaterialTypography.headlineMedium.copy(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = DefaultMaterialTypography.headlineSmall.copy(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = DefaultMaterialTypography.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    titleMedium = DefaultMaterialTypography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    titleSmall = DefaultMaterialTypography.titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    bodyLarge = DefaultMaterialTypography.bodyLarge.copy(fontFamily = Inter, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = DefaultMaterialTypography.bodyMedium.copy(fontFamily = Inter),
    bodySmall = DefaultMaterialTypography.bodySmall.copy(fontFamily = Inter),
    labelLarge = DefaultMaterialTypography.labelLarge.copy(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.05.sp,
    ),
    labelMedium = DefaultMaterialTypography.labelMedium.copy(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.05.sp,
    ),
    labelSmall = DefaultMaterialTypography.labelSmall.copy(
        fontFamily = JetBrainsMono,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.05.sp,
    ),
)

/** Human-content styles (Inter). */
fun allInterTypographyStyles(): List<TextStyle> = listOf(
    Typography.displayLarge,
    Typography.displayMedium,
    Typography.displaySmall,
    Typography.headlineLarge,
    Typography.headlineMedium,
    Typography.headlineSmall,
    Typography.titleLarge,
    Typography.titleMedium,
    Typography.titleSmall,
    Typography.bodyLarge,
    Typography.bodyMedium,
    Typography.bodySmall,
)

/** System-data styles (JetBrains Mono). */
fun allMonoTypographyStyles(): List<TextStyle> = listOf(
    Typography.labelLarge,
    Typography.labelMedium,
    Typography.labelSmall,
)

/** All Material3 typography styles for tests / verification. */
fun allTypographyStyles(): List<TextStyle> = allInterTypographyStyles() + allMonoTypographyStyles()
