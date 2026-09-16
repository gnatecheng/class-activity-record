package com.classrecord.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.MemberStatus

data class TypeTint(
    val color: Color,
    val container: Color,
    val onContainer: Color
)

data class AppColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val money: Color,
    val onMoney: Color,
    val moneyContainer: Color,
    val attendance: TypeTint,
    val payment: TypeTint,
    val split: TypeTint,
    val checklist: TypeTint,
    val classScope: TypeTint,
    val groupScope: TypeTint,
    val memberShortcut: Color,
    val groupShortcut: Color,
    val ledgerShortcut: Color
) {
    fun typeTint(type: ActivityType): TypeTint = when (type) {
        ActivityType.ATTENDANCE -> attendance
        ActivityType.PAYMENT -> payment
        ActivityType.SPLIT -> split
        ActivityType.CHECKLIST -> checklist
    }

    fun statusTint(status: MemberStatus): TypeTint = when (status) {
        MemberStatus.DONE -> TypeTint(success, successContainer, onSuccess)
        MemberStatus.PENDING -> TypeTint(warning, warningContainer, onWarning)
        MemberStatus.EXCUSED -> TypeTint(info, infoContainer, onInfo)
    }

    fun scopeTint(isClass: Boolean): TypeTint = if (isClass) classScope else groupScope
}

private val LightAppColors = AppColors(
    success = Color(0xFF1B7A3D),
    onSuccess = Color(0xFF0D3B1E),
    successContainer = Color(0xFFC8E6C9),
    warning = Color(0xFFC43E00),
    onWarning = Color(0xFF5A2100),
    warningContainer = Color(0xFFFFE0B2),
    info = Color(0xFF1565C0),
    onInfo = Color(0xFF00315C),
    infoContainer = Color(0xFFBBDEFB),
    money = Color(0xFFB45309),
    onMoney = Color(0xFF4A2800),
    moneyContainer = Color(0xFFFFECB3),
    attendance = TypeTint(Color(0xFF00796B), Color(0xFFB2DFDB), Color(0xFF004D40)),
    payment = TypeTint(Color(0xFFC77800), Color(0xFFFFE082), Color(0xFF5D3A00)),
    split = TypeTint(Color(0xFF7B1FA2), Color(0xFFE1BEE7), Color(0xFF4A0072)),
    checklist = TypeTint(Color(0xFF2E7D32), Color(0xFFC8E6C9), Color(0xFF1B5E20)),
    classScope = TypeTint(Color(0xFF2962FF), Color(0xFFD6E2FF), Color(0xFF001A41)),
    groupScope = TypeTint(Color(0xFFE65100), Color(0xFFFFCCBC), Color(0xFF5D1800)),
    memberShortcut = Color(0xFFD6E2FF),
    groupShortcut = Color(0xFFFFCCBC),
    ledgerShortcut = Color(0xFFFFECB3)
)

private val DarkAppColors = AppColors(
    success = Color(0xFF81C784),
    onSuccess = Color(0xFFC8E6C9),
    successContainer = Color(0xFF1B5E20),
    warning = Color(0xFFFFB74D),
    onWarning = Color(0xFFFFE0B2),
    warningContainer = Color(0xFF5D3100),
    info = Color(0xFF90CAF9),
    onInfo = Color(0xFFBBDEFB),
    infoContainer = Color(0xFF0D47A1),
    money = Color(0xFFFFD54F),
    onMoney = Color(0xFFFFECB3),
    moneyContainer = Color(0xFF5D4300),
    attendance = TypeTint(Color(0xFF80CBC4), Color(0xFF004D40), Color(0xFFB2DFDB)),
    payment = TypeTint(Color(0xFFFFD54F), Color(0xFF5D3A00), Color(0xFFFFE082)),
    split = TypeTint(Color(0xFFCE93D8), Color(0xFF4A0072), Color(0xFFE1BEE7)),
    checklist = TypeTint(Color(0xFFA5D6A7), Color(0xFF1B5E20), Color(0xFFC8E6C9)),
    classScope = TypeTint(Color(0xFFB4C5FF), Color(0xFF0037A0), Color(0xFFD6E2FF)),
    groupScope = TypeTint(Color(0xFFFFAB91), Color(0xFF8A2A00), Color(0xFFFFCCBC)),
    memberShortcut = Color(0xFF1A3A7A),
    groupShortcut = Color(0xFF6A2A12),
    ledgerShortcut = Color(0xFF5D4300)
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

private val LightColors = lightColorScheme(
    primary = Color(0xFF2962FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF8E24AA),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3D8FF),
    onTertiaryContainer = Color(0xFF3B0053),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF3F6FF),
    onBackground = Color(0xFF1A1C22),
    surface = Color(0xFFF3F6FF),
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFE1E6F4),
    onSurfaceVariant = Color(0xFF434655),
    outline = Color(0xFF737686),
    inversePrimary = Color(0xFFB4C5FF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002B75),
    primaryContainer = Color(0xFF0037A0),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFE1BEE7),
    onTertiary = Color(0xFF4A0072),
    tertiaryContainer = Color(0xFF6A1B9A),
    onTertiaryContainer = Color(0xFFF3D8FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    background = Color(0xFF11131A),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF11131A),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF434655),
    onSurfaceVariant = Color(0xFFC4C6D4),
    outline = Color(0xFF8D90A0),
    outlineVariant = Color(0xFF434655),
    inversePrimary = Color(0xFF2962FF)
)

@Composable
fun ClassRecordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                content()
            }
        }
    }
}
