package com.github.infinitescroller.ui.util

import androidx.compose.ui.graphics.Color

/** Colors from GitHub Linguist for common languages; others get a stable hashed hue. */
object LanguageColors {

    private val known = mapOf(
        "kotlin" to 0xFFA97BFF, "java" to 0xFFB07219, "python" to 0xFF3572A5,
        "javascript" to 0xFFF1E05A, "typescript" to 0xFF3178C6, "go" to 0xFF00ADD8,
        "rust" to 0xFFDEA584, "swift" to 0xFFF05138, "c++" to 0xFFF34B7D,
        "c" to 0xFF555555, "c#" to 0xFF178600, "ruby" to 0xFF701516,
        "php" to 0xFF4F5D95, "dart" to 0xFF00B4AB, "shell" to 0xFF89E051,
        "html" to 0xFFE34C26, "css" to 0xFF563D7C, "vue" to 0xFF41B883,
        "svelte" to 0xFFFF3E00, "jupyter notebook" to 0xFFDA5B0B, "objective-c" to 0xFF438EFF,
        "scala" to 0xFFC22D40, "lua" to 0xFF000080, "zig" to 0xFFEC915C,
        "elixir" to 0xFF6E4A7E, "haskell" to 0xFF5E5086, "r" to 0xFF198CE7,
        "clojure" to 0xFFDB5855, "nix" to 0xFF7E7EFF, "hcl" to 0xFF844FBA,
        "mdx" to 0xFFFCB32C, "astro" to 0xFFFF5A03, "vim script" to 0xFF199F4B,
    )

    fun of(language: String): Color {
        known[language.lowercase()]?.let { return Color(it) }
        val hue = ((language.lowercase().hashCode() % 360) + 360) % 360
        return Color.hsl(hue.toFloat(), 0.55f, 0.55f)
    }
}
