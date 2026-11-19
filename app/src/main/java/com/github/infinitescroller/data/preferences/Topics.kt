package com.github.infinitescroller.data.preferences

data class TopicCategory(val title: String, val topics: List<String>)

object Topics {

    private const val MAX_LENGTH = 50
    private val VALID = Regex("^[a-z0-9][a-z0-9-]*$")

    val categories = listOf(
        TopicCategory("Mobile", listOf("android", "ios", "jetpack-compose", "kotlin-multiplatform", "flutter", "react-native", "swiftui")),
        TopicCategory("Web", listOf("react", "nextjs", "vue", "svelte", "angular", "tailwindcss", "nodejs", "webassembly")),
        TopicCategory("Languages", listOf("kotlin", "rust", "go", "python", "typescript", "swift", "java", "zig")),
        TopicCategory("AI & data", listOf("llm", "machine-learning", "deep-learning", "pytorch", "rag", "data-science", "computer-vision")),
        TopicCategory("Infrastructure", listOf("docker", "kubernetes", "terraform", "devops", "serverless", "database", "observability")),
        TopicCategory("Tools & more", listOf("cli", "developer-tools", "neovim", "game-development", "security", "self-hosted", "awesome")),
    )

    /**
     * Turns user input into a GitHub topic slug (lowercase letters, digits and hyphens,
     * starting with a letter or digit, max 50 chars). Returns null when it can't be made valid.
     */
    fun normalize(raw: String): String? {
        val slug = raw.trim()
            .removePrefix("#")
            .lowercase()
            .replace(Regex("[\\s_]+"), "-")
            .replace(Regex("-{2,}"), "-")
            .trim('-')
        return slug.takeIf { it.length in 1..MAX_LENGTH && VALID.matches(it) }
    }
}
