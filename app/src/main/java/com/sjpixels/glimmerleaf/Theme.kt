package com.sjpixels.glimmerleaf

/** A full colour palette for one theme in day or night mode. */
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
    val dark: Boolean,
) {
    val danger: Int get() = if (dark) 0xFFF08A80.toInt() else 0xFFB3392F.toInt()
}

data class ThemeSpec(val id: String, val nameKey: String, val light: Pal, val night: Pal) {
    fun pal(dark: Boolean): Pal = if (dark) night else light
}

private fun c(hex: Long): Int = (0xFF000000L or hex).toInt()

private fun pal(dark: Boolean, vararg h: Long) = Pal(
    bg = c(h[0]), bg2 = c(h[1]), surface = c(h[2]), surfaceAlt = c(h[3]), ink = c(h[4]), muted = c(h[5]),
    accent = c(h[6]), onAccent = c(h[7]), accentSoft = c(h[8]), line = c(h[9]), glow = c(h[10]), dark = dark,
)

/**
 * Seven hues built on the warm, candle-lit parchment mood of the cover:
 * sepia ink, aged paper and a golden glow, each shifted toward its own colour.
 */
object Themes {
    //                          bg        bg2       surface   surfAlt   ink       muted     accent    onAccent  accSoft   line      glow
    val all = listOf(
        ThemeSpec(
            "gold", "theme_gold",
            pal(false, 0xF4E9D0, 0xE6CFA2, 0xFFF9EC, 0xF8EEDA, 0x3A2416, 0x7D604A, 0xA26B2A, 0xFFFFFF, 0xF0DDB5, 0xE3D0AE, 0xF6C66A),
            pal(true, 0x1A120D, 0x2C1D13, 0x271B13, 0x31231A, 0xF5E8CF, 0xC2A98A, 0xE6B866, 0x2A1A0C, 0x4A3520, 0x3E2E22, 0xE8B455),
        ),
        ThemeSpec(
            "crimson", "theme_crimson",
            pal(false, 0xF6EADF, 0xEBCDBF, 0xFFF8F2, 0xF9ECE4, 0x3B1D19, 0x80574F, 0x9B2B2C, 0xFFFFFF, 0xF2D3CD, 0xE8D2C8, 0xF0A08A),
            pal(true, 0x1B0F10, 0x301718, 0x281617, 0x33201F, 0xF6E4DE, 0xC9A29B, 0xE9857A, 0x2B0E0D, 0x4C2526, 0x432A29, 0xE57865),
        ),
        ThemeSpec(
            "amber", "theme_amber",
            pal(false, 0xF8EBD7, 0xF2CF9F, 0xFFF8ED, 0xFBEEDC, 0x3C2210, 0x845F42, 0xBF5E17, 0xFFFFFF, 0xF7DCBA, 0xEDD5B6, 0xFFB45E),
            pal(true, 0x1C1209, 0x352010, 0x2A1B10, 0x342416, 0xF8E7D2, 0xCCA788, 0xF4A757, 0x2E1806, 0x4F3219, 0x45301E, 0xF59A3C),
        ),
        ThemeSpec(
            "rose", "theme_rose",
            pal(false, 0xF7EAE7, 0xECCFD3, 0xFFF8F7, 0xFAEEEE, 0x3A1F27, 0x835C67, 0xA44B68, 0xFFFFFF, 0xF1D4DD, 0xEAD4D8, 0xF2A7BC),
            pal(true, 0x1B1215, 0x311D24, 0x291A1F, 0x34232A, 0xF7E5EA, 0xCBA4B0, 0xE99CB3, 0x2E1019, 0x4B2834, 0x432D35, 0xE28AA6),
        ),
        ThemeSpec(
            "violet", "theme_violet",
            pal(false, 0xF1EBF3, 0xDCCDE7, 0xFBF7FD, 0xF2ECF7, 0x2B2039, 0x6D5F80, 0x6B4A9C, 0xFFFFFF, 0xE3D7F1, 0xE0D6EA, 0xC3A6F0),
            pal(true, 0x15111C, 0x251C35, 0x211A2C, 0x2B2338, 0xEEE6F8, 0xB6A8CB, 0xBBA2EA, 0x1F1233, 0x3B2E54, 0x382E48, 0xA685E6),
        ),
        ThemeSpec(
            "sapphire", "theme_sapphire",
            pal(false, 0xEBF0F4, 0xCCDAE7, 0xF7FAFD, 0xEDF2F7, 0x1C2836, 0x5A6B7E, 0x2D5D8D, 0xFFFFFF, 0xD4E1EF, 0xD7E1EB, 0x8EBBEA),
            pal(true, 0x0E141B, 0x172331, 0x16202B, 0x1F2A37, 0xE4EDF7, 0xA2B3C6, 0x8FBAEA, 0x0C1D30, 0x233B55, 0x2A3746, 0x6FA6E3),
        ),
        ThemeSpec(
            "emerald", "theme_emerald",
            pal(false, 0xEDF1E7, 0xD2E0C9, 0xF8FBF4, 0xEEF4E8, 0x1F2B21, 0x5D6F5F, 0x3D7250, 0xFFFFFF, 0xD7E8D6, 0xDAE5D5, 0x9FD6A8),
            pal(true, 0x0F160F, 0x1A2719, 0x192319, 0x222E22, 0xE6F1E5, 0xA7BCA7, 0x90CC9C, 0x0F2414, 0x27412C, 0x2B3A2B, 0x77C287),
        ),
    )

    fun get(id: String): ThemeSpec = all.firstOrNull { it.id == id } ?: all[0]
}
