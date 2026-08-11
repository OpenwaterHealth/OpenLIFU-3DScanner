package health.openwater.openlifu3dscanner.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import health.openwater.openlifu3dscanner.R

@OptIn(ExperimentalTextApi::class)
private fun interTightWeight(weight: FontWeight) = Font(
    resId = R.font.inter_tight_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val InterTight = FontFamily(
    interTightWeight(FontWeight.Normal),
    interTightWeight(FontWeight.Medium),
    interTightWeight(FontWeight.SemiBold),
    interTightWeight(FontWeight.Bold),
)

val AppTypography = Typography(
    displayLarge   = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Normal,   fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium  = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Normal,   fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall   = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Normal,   fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall     = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium    = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall     = TextStyle(fontFamily = InterTight, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp),
)
