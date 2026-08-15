package com.example.solo_levelling.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.solo_levelling.R

val CascadiaCode = FontFamily(
    Font(R.font.cascadia_code_regular, FontWeight.W300),
    Font(R.font.cascadia_code_regular, FontWeight.Normal),
    Font(R.font.cascadia_code_regular, FontWeight.Medium),
    Font(R.font.cascadia_code_bold, FontWeight.SemiBold),
    Font(R.font.cascadia_code_bold, FontWeight.Bold),
    Font(R.font.cascadia_code_bold, FontWeight.ExtraBold),
)

private fun TextStyle.withCascadia(): TextStyle = copy(fontFamily = CascadiaCode)

private val DefaultMaterialTypography = Typography()

/** App-wide Material3 type scale — Cascadia Code on every slot. */
val Typography = Typography(
    displayLarge = DefaultMaterialTypography.displayLarge.withCascadia(),
    displayMedium = DefaultMaterialTypography.displayMedium.withCascadia(),
    displaySmall = DefaultMaterialTypography.displaySmall.withCascadia(),
    headlineLarge = DefaultMaterialTypography.headlineLarge.withCascadia(),
    headlineMedium = DefaultMaterialTypography.headlineMedium.withCascadia(),
    headlineSmall = DefaultMaterialTypography.headlineSmall.withCascadia(),
    titleLarge = DefaultMaterialTypography.titleLarge.withCascadia(),
    titleMedium = DefaultMaterialTypography.titleMedium.withCascadia(),
    titleSmall = DefaultMaterialTypography.titleSmall.withCascadia(),
    bodyLarge = DefaultMaterialTypography.bodyLarge.withCascadia(),
    bodyMedium = DefaultMaterialTypography.bodyMedium.withCascadia(),
    bodySmall = DefaultMaterialTypography.bodySmall.withCascadia(),
    labelLarge = DefaultMaterialTypography.labelLarge.withCascadia(),
    labelMedium = DefaultMaterialTypography.labelMedium.withCascadia(),
    labelSmall = DefaultMaterialTypography.labelSmall.withCascadia(),
)

/** All Material3 typography styles for tests / verification. */
fun allTypographyStyles(): List<TextStyle> = listOf(
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
    Typography.labelLarge,
    Typography.labelMedium,
    Typography.labelSmall,
)
