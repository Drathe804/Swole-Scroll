import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dravenmiller.swolescroll.R // This connects to your res/font folder

// 📜 THE CINZEL FONT FAMILY DEFINITION
val CinzelFontFamily = FontFamily(
    Font(R.font.cinzel_regular, FontWeight.Normal),
    Font(R.font.cinzel_medium, FontWeight.Medium),
    Font(R.font.cinzel_semibold, FontWeight.SemiBold),
    Font(R.font.cinzel_bold, FontWeight.Bold),
    Font(R.font.cinzel_extrabold, FontWeight.ExtraBold),
    Font(R.font.cinzel_black, FontWeight.Black)
)

// Grab the baseline Material 3 sizing so we don't have to build it from scratch
private val defaultTypography = Typography()

// ⚔️ THE MASTER APP TYPOGRAPHY
val SwoleTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = CinzelFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = CinzelFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = CinzelFontFamily),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = CinzelFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = CinzelFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = CinzelFontFamily),

    // Your custom Scroll styles are preserved here!
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = CinzelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = CinzelFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = CinzelFontFamily),

    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = CinzelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = CinzelFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = CinzelFontFamily),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = CinzelFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = CinzelFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = CinzelFontFamily)
)
