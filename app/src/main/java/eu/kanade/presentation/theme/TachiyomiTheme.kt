package eu.kanade.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.EInkProfile
import eu.kanade.domain.ui.model.EInkThemeMode
import eu.kanade.presentation.theme.colorscheme.AuroraColorScheme
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme
import eu.kanade.presentation.theme.colorscheme.CloudflareColorScheme
import eu.kanade.presentation.theme.colorscheme.CottoncandyColorScheme
import eu.kanade.presentation.theme.colorscheme.DoomColorScheme
import eu.kanade.presentation.theme.colorscheme.EventHorizonColorScheme
import eu.kanade.presentation.theme.colorscheme.GreenAppleColorScheme
import eu.kanade.presentation.theme.colorscheme.LavenderColorScheme
import eu.kanade.presentation.theme.colorscheme.MatrixColorScheme
import eu.kanade.presentation.theme.colorscheme.MidnightDuskColorScheme
import eu.kanade.presentation.theme.colorscheme.MochaColorScheme
import eu.kanade.presentation.theme.colorscheme.MonetColorScheme
import eu.kanade.presentation.theme.colorscheme.MonochromeColorScheme
import eu.kanade.presentation.theme.colorscheme.NebulaTideColorScheme
import eu.kanade.presentation.theme.colorscheme.NordColorScheme
import eu.kanade.presentation.theme.colorscheme.OnyxGoldColorScheme
import eu.kanade.presentation.theme.colorscheme.SakuraNoirColorScheme
import eu.kanade.presentation.theme.colorscheme.SapphireColorScheme
import eu.kanade.presentation.theme.colorscheme.StrawberryColorScheme
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme
import eu.kanade.presentation.theme.colorscheme.TakoColorScheme
import eu.kanade.presentation.theme.colorscheme.TealTurqoiseColorScheme
import eu.kanade.presentation.theme.colorscheme.TidalWaveColorScheme
import eu.kanade.presentation.theme.colorscheme.VoidRedColorScheme
import eu.kanade.presentation.theme.colorscheme.YinYangColorScheme
import eu.kanade.presentation.theme.colorscheme.YotsubaColorScheme
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun TachiyomiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val uiPreferences = Injekt.get<UiPreferences>()
    val appUiFontId = uiPreferences.appUiFontId().get()
    val coverTitleFontId = uiPreferences.coverTitleFontId().get()
    val eInkProfile = uiPreferences.eInkProfile().collectAsState().value
    val eInkThemeMode = uiPreferences.eInkThemeMode().collectAsState().value
    val isDark = resolveEInkThemeIsDark(
        eInkProfile = eInkProfile,
        eInkThemeMode = eInkThemeMode,
        isSystemDarkTheme = isSystemInDarkTheme(),
    )
    BaseTachiyomiTheme(
        appTheme = appTheme ?: uiPreferences.appTheme().get(),
        isAmoled = amoled ?: uiPreferences.themeDarkAmoled().get(),
        eInkProfile = eInkProfile,
        isDark = isDark,
        appUiFontId = appUiFontId,
        coverTitleFontId = coverTitleFontId,
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseTachiyomiTheme(
    appTheme = appTheme,
    isAmoled = isAmoled,
    eInkProfile = EInkProfile.OFF,
    isDark = isSystemInDarkTheme(),
    appUiFontId = UiPreferences.DEFAULT_APP_UI_FONT_ID,
    coverTitleFontId = UiPreferences.DEFAULT_COVER_TITLE_FONT_ID,
    content = content,
)

@Composable
private fun BaseTachiyomiTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    eInkProfile: EInkProfile,
    isDark: Boolean,
    appUiFontId: String,
    coverTitleFontId: String,
    content: @Composable () -> Unit,
) {
    val isEInkMode = eInkProfile.isEnabled
    val colorScheme = if (appTheme == AppTheme.AURORA_PRIME) {
        val context = LocalContext.current
        val payload =
            remember {
                eu.kanade.domain.easteregg.aurora.AuroraQuest(
                    context.applicationContext as android.app.Application,
                ).unlockedPayload()
            }
        remember(payload, isDark, isAmoled) {
            buildAuroraPrimeColorScheme(payload, isDark, isAmoled)
        }
    } else {
        getThemeColorScheme(
            appTheme = appTheme,
            isAmoled = isAmoled,
            eInkProfile = eInkProfile,
            isDark = isDark,
        )
    }
    val appFontFamily = rememberAppFontFamily(appUiFontId)
    val coverTitleFontFamily = rememberAppFontFamily(coverTitleFontId)
    val typography = remember(appFontFamily) {
        Typography().withDefaultFontFamily(appFontFamily)
    }

    val auroraColors = AuroraColors.fromColorScheme(
        colorScheme = colorScheme,
        isDark = isDark,
        isAmoled = isAmoled,
        eInkProfile = eInkProfile,
    )

    CompositionLocalProvider(
        LocalIsEInkMode provides isEInkMode,
        LocalAuroraColors provides auroraColors,
        LocalIsAuroraTheme provides appTheme.isAuroraStyle,
        LocalIsDefaultAppUiFont provides (appUiFontId == UiPreferences.DEFAULT_APP_UI_FONT_ID),
        LocalCoverTitleFontFamily provides coverTitleFontFamily,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun getThemeColorScheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    eInkProfile: EInkProfile,
    isDark: Boolean,
): ColorScheme {
    if (eInkProfile == EInkProfile.MONOCHROME) {
        return MonochromeColorScheme.getColorScheme(
            isDark = isDark,
            isAmoled = false,
        )
    }
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(LocalContext.current)
    } else {
        colorSchemes.getOrDefault(appTheme, TachiyomiColorScheme)
    }
    return colorScheme.getColorScheme(
        isDark,
        isAmoled,
    )
}

internal fun resolveEInkThemeIsDark(
    eInkProfile: EInkProfile,
    eInkThemeMode: EInkThemeMode,
    isSystemDarkTheme: Boolean,
): Boolean {
    return when {
        !eInkProfile.isEnabled -> isSystemDarkTheme
        eInkThemeMode == EInkThemeMode.LIGHT -> false
        eInkThemeMode == EInkThemeMode.DARK -> true
        else -> isSystemDarkTheme
    }
}

private const val RIPPLE_DRAGGED_ALPHA = .1f
private const val RIPPLE_FOCUSED_ALPHA = .1f
private const val RIPPLE_HOVERED_ALPHA = .1f
private const val RIPPLE_PRESSED_ALPHA = .1f

val playerRippleConfiguration
    @Composable get() = RippleConfiguration(
        color = if (isSystemInDarkTheme()) Color.White else Color.Black,
        rippleAlpha = RippleAlpha(
            draggedAlpha = RIPPLE_DRAGGED_ALPHA,
            focusedAlpha = RIPPLE_FOCUSED_ALPHA,
            hoveredAlpha = RIPPLE_HOVERED_ALPHA,
            pressedAlpha = RIPPLE_PRESSED_ALPHA,
        ),
    )

val LocalIsAuroraTheme = staticCompositionLocalOf { false }
val LocalIsEInkMode = staticCompositionLocalOf { false }
val LocalIsDefaultAppUiFont = staticCompositionLocalOf { true }

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to AuroraColorScheme,
    AppTheme.CLOUDFLARE to CloudflareColorScheme,
    AppTheme.COTTONCANDY to CottoncandyColorScheme,
    AppTheme.DOOM to DoomColorScheme,
    AppTheme.GREEN_APPLE to GreenAppleColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    AppTheme.MATRIX to MatrixColorScheme,
    AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
    AppTheme.MONOCHROME to MonochromeColorScheme,
    AppTheme.MOCHA to MochaColorScheme,
    AppTheme.SAPPHIRE to SapphireColorScheme,
    AppTheme.NORD to NordColorScheme,
    AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
    AppTheme.TAKO to TakoColorScheme,
    AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
    AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
    AppTheme.YINYANG to YinYangColorScheme,
    AppTheme.YOTSUBA to YotsubaColorScheme,
    AppTheme.AURORA to AuroraColorScheme,
    AppTheme.ONYX_GOLD to OnyxGoldColorScheme,
    AppTheme.SAKURA_NOIR to SakuraNoirColorScheme,
    AppTheme.NEBULA_TIDE to NebulaTideColorScheme,
    AppTheme.EVENT_HORIZON to EventHorizonColorScheme,
    AppTheme.VOID_RED to VoidRedColorScheme,
)

private fun buildAuroraPrimeColorScheme(
    payload: eu.kanade.domain.easteregg.aurora.AuroraPayload?,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    fun parseColor(hex: String?, fallback: Color): Color {
        if (hex == null) return fallback
        return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
    }

    val primaryColor = parseColor(payload?.themeColors?.get("primary"), Color(0xFF64FFDA))
    val secondaryColor = parseColor(payload?.themeColors?.get("secondary"), Color(0xFF7C4DFF))
    val accentColor = parseColor(payload?.themeColors?.get("accent"), Color(0xFFFF6E9C))
    val backgroundColor = parseColor(payload?.themeColors?.get("background"), Color(0xFF050B14))
    val surfaceColor = parseColor(payload?.themeColors?.get("surface"), Color(0xFF0A1626))

    val baseScheme = androidx.compose.material3.darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.Black,
        primaryContainer = primaryColor.copy(alpha = 0.2f),
        onPrimaryContainer = primaryColor,
        secondary = secondaryColor,
        onSecondary = Color.White,
        secondaryContainer = secondaryColor.copy(alpha = 0.2f),
        onSecondaryContainer = secondaryColor,
        tertiary = accentColor,
        onTertiary = Color.Black,
        tertiaryContainer = accentColor.copy(alpha = 0.2f),
        onTertiaryContainer = accentColor,
        background = if (isAmoled) Color.Black else backgroundColor,
        onBackground = Color(0xFFDCEBFF),
        surface = if (isAmoled) Color.Black else surfaceColor,
        onSurface = Color(0xFFDCEBFF),
        surfaceVariant = surfaceColor.copy(alpha = 0.8f),
        onSurfaceVariant = Color(0xCCDCEBFF),
        surfaceTint = primaryColor,
        outline = primaryColor.copy(alpha = 0.5f),
        outlineVariant = primaryColor.copy(alpha = 0.2f),
    )

    return if (isAmoled) {
        baseScheme.copy(
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF0C0C0C),
            surfaceContainerLowest = Color(0xFF0C0C0C),
            surfaceContainerLow = Color(0xFF0C0C0C),
            surfaceContainer = Color(0xFF0C0C0C),
            surfaceContainerHigh = Color(0xFF131313),
            surfaceContainerHighest = Color(0xFF1B1B1B),
        )
    } else {
        baseScheme
    }
}
