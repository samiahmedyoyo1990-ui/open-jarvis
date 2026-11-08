package com.example.github

import java.security.MessageDigest

object SkillManifestParser {

    /**
     * Attempts to parse files list and file contents into a SkillManifest.
     */
    suspend fun parseRepositorySkill(
        repoFullName: String,
        defaultBranch: String,
        files: List<GitHubContentItem>,
        fetchContentLambda: suspend (String) -> String?
    ): ParsedSkillResult {
        val fileNames = files.map { it.name.lowercase() }

        // 1. Check for native jarvis.skill.json
        if (fileNames.contains("jarvis.skill.json")) {
            val content = fetchContentLambda("jarvis.skill.json")
            if (!content.isNullOrBlank()) {
                val manifest = SkillManifest.fromJson(content)
                if (manifest != null) {
                    val hash = computeSha256(content)
                    return ParsedSkillResult(
                        manifest = manifest.copy(
                            sourceRepoUrl = "https://github.com/$repoFullName",
                            integrityHash = hash
                        ),
                        compatibilityType = CompatibilityType.JARVIS_SKILL,
                        detectedFile = "jarvis.skill.json",
                        isInstallable = true
                    )
                }
            }
        }

        // 2. Check for SKILL.md or skill.md
        val skillMdName = files.firstOrNull { it.name.equals("SKILL.md", ignoreCase = true) }?.name
        if (skillMdName != null) {
            val content = fetchContentLambda(skillMdName)
            if (!content.isNullOrBlank()) {
                val manifest = parseSkillMd(content, repoFullName)
                val hash = computeSha256(content)
                return ParsedSkillResult(
                    manifest = manifest.copy(
                        sourceRepoUrl = "https://github.com/$repoFullName",
                        integrityHash = hash
                    ),
                    compatibilityType = CompatibilityType.AGENT_INSTRUCTION,
                    detectedFile = skillMdName,
                    isInstallable = true
                )
            }
        }

        // 3. Check for prompt packs (prompts.json)
        if (fileNames.contains("prompts.json") || fileNames.contains("prompt_pack.json")) {
            val pName = if (fileNames.contains("prompts.json")) "prompts.json" else "prompt_pack.json"
            val content = fetchContentLambda(pName)
            if (!content.isNullOrBlank()) {
                val manifest = parsePromptPack(content, repoFullName)
                val hash = computeSha256(content)
                return ParsedSkillResult(
                    manifest = manifest.copy(
                        sourceRepoUrl = "https://github.com/$repoFullName",
                        integrityHash = hash
                    ),
                    compatibilityType = CompatibilityType.PROMPT_PACK,
                    detectedFile = pName,
                    isInstallable = true
                )
            }
        }

        // 4. Check for Developer Source Project (Android / Java / Kotlin / Node / Python)
        val isDevProject = fileNames.any {
            it in listOf("build.gradle", "build.gradle.kts", "pom.xml", "package.json", "cargo.toml", "requirements.txt", "makefile", "go.mod")
        }
        if (isDevProject) {
            return ParsedSkillResult(
                manifest = null,
                compatibilityType = CompatibilityType.DEV_SOURCE_PROJECT,
                detectedFile = files.firstOrNull { it.name in listOf("build.gradle.kts", "build.gradle", "package.json", "pom.xml") }?.name ?: "source_project",
                isInstallable = false
            )
        }

        // 5. Fallback: Check if README has substantial instructions that can be imported as Agent Instructions
        val readmeName = files.firstOrNull { it.name.startsWith("readme", ignoreCase = true) }?.name
        if (readmeName != null) {
            val content = fetchContentLambda(readmeName)
            if (!content.isNullOrBlank() && (content.contains("JARVIS", ignoreCase = true) || content.contains("system prompt", ignoreCase = true))) {
                val manifest = SkillManifest(
                    id = "skill_${repoFullName.replace('/', '_').lowercase()}",
                    name = repoFullName.substringAfter('/'),
                    banglaName = "${repoFullName.substringAfter('/')} স্কিল",
                    version = "1.0.0",
                    author = repoFullName.substringBefore('/'),
                    description = content.lineSequence().firstOrNull { it.isNotBlank() && !it.startsWith("#") }?.take(200) ?: "Agent instructions derived from repository README",
                    category = "Agent",
                    type = "AGENT_INSTRUCTION",
                    systemPrompt = content.take(3000),
                    allowedPermissions = emptyList(),
                    sourceRepoUrl = "https://github.com/$repoFullName",
                    integrityHash = computeSha256(content)
                )
                return ParsedSkillResult(
                    manifest = manifest,
                    compatibilityType = CompatibilityType.AGENT_INSTRUCTION,
                    detectedFile = readmeName,
                    isInstallable = true
                )
            }
        }

        return ParsedSkillResult(
            manifest = null,
            compatibilityType = CompatibilityType.UNSUPPORTED,
            detectedFile = null,
            isInstallable = false
        )
    }

    /**
     * Parses standard SKILL.md with YAML frontmatter:
     * ---
     * name: "Web Search"
     * description: "Searches the web"
     * ---
     * Markdown body instructions
     */
    fun parseSkillMd(content: String, repoFullName: String): SkillManifest {
        var name = repoFullName.substringAfter('/')
        var description = "Imported AI skill from $repoFullName"
        val trimmed = content.trim()
        var body = trimmed

        if (trimmed.startsWith("---")) {
            val secondDelimiter = trimmed.indexOf("---", 3)
            if (secondDelimiter != -1) {
                val yaml = trimmed.substring(3, secondDelimiter)
                body = trimmed.substring(secondDelimiter + 3).trim()

                for (line in yaml.lines()) {
                    val trimmedLine = line.trim()
                    if (!line.startsWith(" ") && !line.startsWith("\t")) {
                        if (trimmedLine.startsWith("name:", ignoreCase = true)) {
                            name = trimmedLine.substringAfter(':').trim().trim('"', '\'')
                        } else if (trimmedLine.startsWith("description:", ignoreCase = true)) {
                            description = trimmedLine.substringAfter(':').trim().trim('"', '\'')
                        }
                    }
                }
            }
        }

        // Infer permissions from body keywords
        val requestedPerms = mutableListOf<String>()
        if (body.contains("internet", ignoreCase = true) || body.contains("search", ignoreCase = true) || body.contains("http", ignoreCase = true)) {
            requestedPerms.add("NETWORK")
        }
        if (body.contains("device", ignoreCase = true) || body.contains("intent", ignoreCase = true) || body.contains("open app", ignoreCase = true)) {
            requestedPerms.add("DEVICE_ACTIONS")
        }

        return SkillManifest(
            id = "skill_${repoFullName.replace('/', '_').lowercase()}",
            name = name,
            banglaName = name,
            version = "1.0.0",
            author = repoFullName.substringBefore('/'),
            description = description,
            category = "AI Skill",
            type = "AGENT_INSTRUCTION",
            systemPrompt = body.take(4000),
            allowedPermissions = requestedPerms,
            sourceRepoUrl = "https://github.com/$repoFullName"
        )
    }

    private fun parsePromptPack(jsonStr: String, repoFullName: String): SkillManifest {
        val repoShort = repoFullName.substringAfter('/')
        return SkillManifest(
            id = "promptpack_${repoFullName.replace('/', '_').lowercase()}",
            name = "$repoShort Prompt Pack",
            banglaName = "$repoShort প্রম্পট প্যাক",
            version = "1.0.0",
            author = repoFullName.substringBefore('/'),
            description = "Structured collection of curated AI prompts and behaviors.",
            category = "Prompts",
            type = "PROMPT_PACK",
            systemPrompt = jsonStr.take(3500),
            allowedPermissions = emptyList(),
            sourceRepoUrl = "https://github.com/$repoFullName"
        )
    }

    fun computeSha256(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

data class ParsedSkillResult(
    val manifest: SkillManifest?,
    val compatibilityType: CompatibilityType,
    val detectedFile: String?,
    val isInstallable: Boolean
)
