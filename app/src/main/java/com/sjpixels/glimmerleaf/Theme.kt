package com.sjpixels.glimmerleaf

/** A full colour palette for one theme in day or night mode. Values come from design/kit/tokens.json. */
data class Pal(
    val bg: Int,
    val bg2: Int,
    val surface: Int,
    val surfaceAlt: Int,
    val ink: Int,
    val muted: Int,
    val accent: Int,
    val onAccent: Int,
    val accentSoft: Int,
    val line: Int,
    val glow: Int,
    val danger: Int,
    val dark: Boolean,
)

data class ThemeSpec(val id: String, val nameKey: String, val light: Pal, val night: Pal) {
    fun pal(dark: Boolean): Pal = if (dark) night else light
}

private fun c(hex: Long): Int = (0xFF000000L or hex).toInt()

private fun pal(dark: Boolean, vararg h: Long) = Pal(
    bg = c(h[0]), bg2 = c(h[1]), surface = c(h[2]), surfaceAlt = c(h[3]), ink = c(h[4]), muted = c(h[5]),
    accent = c(h[6]), onAccent = c(h[7]), accentSoft = c(h[8]), line = c(h[9]), glow = c(h[10]), danger = c(h[11]), dark = dark,
)

/**
 * Seven hues on the warm parchment of the Glimmerleaf logo. Golden Parchment uses the logo's
 * deep brown for actions in the day and its gold at night. All text pairs meet WCAG AA
 * (see design/kit/contrast.csv).
 */
object Themes {
    //                bg        bg2       surface   surfAlt   ink       muted     accent    onAccent  accSoft   line      glow      danger
    val all = listOf(
        ThemeSpec(
            "gold", "theme_gold",
            pal(false, 0xF5E2CA, 0xF9ECDD, 0xFBF3EA, 0xF7E7D2, 0x321209, 0x69554B, 0x321209, 0xFFFFFF, 0xFAD386, 0xC6B09C, 0xFEC74E, 0x9C2825),
            pal(true, 0x1A120D, 0x221A15, 0x2A231E, 0x332C28, 0xFDF4E5, 0xD6C4B4, 0xFEC74E, 0x25170F, 0x433319, 0x513D1D, 0xFEC74E, 0xFFACA4),
        ),
        ThemeSpec(
            "crimson", "theme_crimson",
            pal(false, 0xF6EADF, 0xF9F2EB, 0xFBF7F2, 0xF7EDE4, 0x321209, 0x69554B, 0x9B2B2C, 0xFFFFFF, 0xEBD3CA, 0xE0BCB4, 0x9B2B2C, 0x9C2825),
            pal(true, 0x1B0F10, 0x231718, 0x2B2021, 0x34292A, 0xFDF4E5, 0xD6C4B4, 0xE9857A, 0x25170F, 0x402423, 0x4C2B29, 0xE9857A, 0xFFACA4),
        ),
        ThemeSpec(
            "amber", "theme_amber",
            pal(false, 0xF8EBD7, 0xFBF2E5, 0xFCF7EF, 0xF9EEDD, 0x321209, 0x69554B, 0x99450C, 0xFFFFFF, 0xEDD7BF, 0xE1C3A6, 0x99450C, 0x9C2825),
            pal(true, 0x1C1209, 0x241A12, 0x2C231A, 0x352C24, 0xFDF4E5, 0xD6C4B4, 0xF4A757, 0x25170F, 0x432D17, 0x50361C, 0xF4A757, 0xFFACA4),
        ),
        ThemeSpec(
            "rose", "theme_rose",
            pal(false, 0xF7EAE7, 0xFAF2F0, 0xFCF7F5, 0xF8EDEB, 0x321209, 0x69554B, 0x99415E, 0xFFFFFF, 0xECD6D7, 0xE0C1C6, 0x99415E, 0x9C2825),
            pal(true, 0x1B1215, 0x231A1D, 0x2B2325, 0x342C2F, 0xFDF4E5, 0xD6C4B4, 0xE99CB3, 0x25170F, 0x402B31, 0x4C333B, 0xE99CB3, 0xFFACA4),
        ),
        ThemeSpec(
            "violet", "theme_violet",
            pal(false, 0xF1EBF3, 0xF6F2F7, 0xF9F7FA, 0xF3EEF5, 0x321209, 0x69554B, 0x6B4A9C, 0xFFFFFF, 0xE1D8E9, 0xD1C4DE, 0x6B4A9C, 0x9C2825),
            pal(true, 0x15111C, 0x1D1924, 0x25222C, 0x2F2B35, 0xFDF4E5, 0xD6C4B4, 0xBBA2EA, 0x25170F, 0x332B41, 0x3D344D, 0xBBA2EA, 0xFFACA4),
        ),
        ThemeSpec(
            "sapphire", "theme_sapphire",
            pal(false, 0xEBF0F4, 0xF2F5F8, 0xF7F9FB, 0xEEF2F6, 0x321209, 0x69554B, 0x2D5D8D, 0xFFFFFF, 0xD4DEE8, 0xBDCDDB, 0x2D5D8D, 0x9C2825),
            pal(true, 0x0E141B, 0x161C23, 0x1F242B, 0x292E34, 0xFDF4E5, 0xD6C4B4, 0x8FBAEA, 0x25170F, 0x253240, 0x2D3C4D, 0x8FBAEA, 0xFFACA4),
        ),
        ThemeSpec(
            "emerald", "theme_emerald",
            pal(false, 0xEDF1E7, 0xF3F6F0, 0xF8F9F5, 0xF0F3EB, 0x321209, 0x69554B, 0x386647, 0xFFFFFF, 0xD7E0D4, 0xC2D0C1, 0x386647, 0x9C2825),
            pal(true, 0x0F160F, 0x171E17, 0x202620, 0x293029, 0xFDF4E5, 0xD6C4B4, 0x90CC9C, 0x25170F, 0x263728, 0x2E4231, 0x90CC9C, 0xFFACA4),
        ),
    )

    fun get(id: String): ThemeSpec = all.firstOrNull { it.id == id } ?: all[0]
}
