package com.example.github

import java.security.MessageDigest

object SkillSecurityInspector {

    /**
     * Permissions a third-party skill may ever be granted. Anything outside this
     * set is never installable, even if the user approves it — fail closed.
     */
    val INSTALLABLE_PERMISSIONS = setOf(
        "NETWORK",
        "INTERNET",
        "DEVICE_ACTIONS",
        "DEVICE_INTENTS",
        "STORAGE_ACCESS_FRAMEWORK",
        "NOTIFICATIONS",
        "ALARM"
    )

    /**
     * High-risk permissions no third-party skill may hold. Requesting any of
     * these blocks installation outright (see the check in [inspectRepository]).
     */
    val DISALLOWED_HIGH_RISK_PERMISSIONS = setOf(
        "ROOT",
        "READ_SMS",
        "SEND_SMS",
        "SMS",
        "READ_CONTACTS",
        "WRITE_CONTACTS",
        "READ_CALL_LOG",
        "WRITE_CALL_LOG",
        "CALL_PHONE",
        "RECORD_AUDIO",
        "CAMERA",
        "ACCESS_FINE_LOCATION",
        "ACCESS_COARSE_LOCATION",
        "ACCESS_BACKGROUND_LOCATION",
        "READ_EXTERNAL_STORAGE",
        "WRITE_EXTERNAL_STORAGE",
        "MANAGE_EXTERNAL_STORAGE",
        "SYSTEM_ALERT_WINDOW",
        "BIND_ACCESSIBILITY_SERVICE",
        "DEVICE_ADMIN",
        "INSTALL_PACKAGES",
        "READ_PHONE_STATE",
        "READ_PHONE_NUMBERS",
        "BIND_NOTIFICATION_LISTENER_SERVICE",
        "WRITE_SETTINGS"
    )

    private val SUSPICIOUS_BINARY_EXTENSIONS = listOf(
        ".dex", ".apk", ".so", ".exe", ".sh", ".bat", ".cmd", ".vbs", ".dll", ".jar"
    )

    private val SUSPICIOUS_PROMPT_PATTERNS = listOf(
        "ignore previous instructions",
        "ignore all prior instructions",
        "system prompt override",
        "bypass security",
        "reveal api key",
        "exfiltrate",
        "send api key",
        "print credentials",
        "jailbreak",
        "dan mode",
        "dump memory"
    )

    private val EMBEDDED_CREDENTIAL_PATTERNS = listOf(
        Regex("AIzaSy[A-Za-z0-9_-]{33}"), // Google API key
        Regex("sk-[A-Za-z0-9]{32,}"),      // OpenAI key
        Regex("ghp_[A-Za-z0-9]{36}"),     // GitHub Personal Access Token
        Regex("gho_[A-Za-z0-9]{36}"),     // GitHub OAuth Access Token
        Regex("AKIA[0-9A-Z]{16}")         // AWS Access Key ID
    )

    fun inspectRepository(
        repoFullName: String,
        branch: String,
        files: List<GitHubContentItem>,
        manifest: SkillManifest?,
        readmeContent: String,
        compatibilityType: CompatibilityType
    ): SecurityAuditReport {
        val findings = mutableListOf<SecurityFinding>()
        var isBlocked = false
        var hasWarning = false
        var needsPermission = false

        // 1. File extension and binary checks
        for (file in files) {
            val lower = file.name.lowercase()

            // Check for path traversal attempts
            if (file.path.contains("..") || file.path.contains("./")) {
                findings.add(
                    SecurityFinding("CRITICAL", "Path traversal attempt detected in file path: ${file.path}", file.name)
                )
                isBlocked = true
            }

            // Check for suspicious binaries
            for (ext in SUSPICIOUS_BINARY_EXTENSIONS) {
                if (lower.endsWith(ext)) {
                    findings.add(
                        SecurityFinding(
                            "WARNING",
                            "Executable binary file found ($ext). JARVIS prohibits dynamic code execution.",
                            file.name
                        )
                    )
                    hasWarning = true
                }
            }
        }

        // 2. Scan Manifest & System Prompt for Prompt Injections
        val textToScan = buildString {
            append(readmeContent)
            append("\n")
            if (manifest != null) {
                append(manifest.systemPrompt)
                append("\n")
                append(manifest.description)
            }
        }.lowercase()

        for (pattern in SUSPICIOUS_PROMPT_PATTERNS) {
            if (textToScan.contains(pattern)) {
                findings.add(
                    SecurityFinding(
                        "WARNING",
                        "Potentially adversarial prompt instruction detected: \"$pattern\". Verify instructions before execution.",
                        manifest?.name
                    )
                )
                hasWarning = true
            }
        }

        // 2b. Scan tool definitions for embedded action tags / prompt-injection
        // patterns. A tool that smuggles "<<ACTION:...>>" into its description
        // tries to make the assistant execute an action the user never asked for.
        for (tool in manifest?.tools.orEmpty()) {
            val toolText = "${tool.name} ${tool.description} ${tool.handler}"
            if (toolText.contains("<<ACTION:", ignoreCase = true)) {
                findings.add(
                    SecurityFinding(
                        "WARNING",
                        "Potentially adversarial prompt instruction detected: embedded action tag in tool \"${tool.name}\". Verify instructions before execution.",
                        manifest?.name
                    )
                )
                hasWarning = true
            } else {
                val lowerToolText = toolText.lowercase()
                for (pattern in SUSPICIOUS_PROMPT_PATTERNS) {
                    if (lowerToolText.contains(pattern)) {
                        findings.add(
                            SecurityFinding(
                                "WARNING",
                                "Potentially adversarial prompt instruction detected: \"$pattern\" in tool \"${tool.name}\". Verify instructions before execution.",
                                manifest?.name
                            )
                        )
                        hasWarning = true
                        break
                    }
                }
            }
        }

        // 3. Scan for accidentally embedded private credentials
        for (regex in EMBEDDED_CREDENTIAL_PATTERNS) {
            if (regex.containsMatchIn(textToScan)) {
                findings.add(
                    SecurityFinding(
                        "WARNING",
                        "Detected pattern resembling an embedded API key or token in package text.",
                        "Credentials"
                    )
                )
                hasWarning = true
            }
        }

        // 4. Requested Permissions Analysis
        val requestedPermissions = manifest?.allowedPermissions ?: emptyList()
        if (requestedPermissions.isNotEmpty()) {
            needsPermission = true
            findings.add(
                SecurityFinding(
                    "INFO",
                    "This skill requests access to: ${requestedPermissions.joinToString(", ")}. User approval required.",
                    "Permissions"
                )
            )
            // High-risk permissions block installation outright, no matter what
            // the user approved — a skill must never hold ROOT, SMS access, etc.
            val disallowed = requestedPermissions.filter {
                it.uppercase() in DISALLOWED_HIGH_RISK_PERMISSIONS
            }
            if (disallowed.isNotEmpty()) {
                findings.add(
                    SecurityFinding(
                        "CRITICAL",
                        "Disallowed high-risk permissions: ${disallowed.joinToString(", ")}. This skill cannot be installed.",
                        "Permissions"
                    )
                )
                isBlocked = true
            }
        }

        // 5. Compute SHA-256 integrity hash of combined files
        val integrityCombined = files.sortedBy { it.path }.joinToString(";") { "${it.path}:${it.sha}" } + ";${manifest?.systemPrompt ?: ""}"
        val sha256 = computeSha256(integrityCombined)

        // 6. Assign final audit status
        val status = when {
            isBlocked -> AuditStatus.BLOCKED
            compatibilityType == CompatibilityType.UNSUPPORTED -> AuditStatus.UNSUPPORTED
            hasWarning -> AuditStatus.WARNING
            needsPermission -> AuditStatus.NEEDS_PERMISSION
            compatibilityType == CompatibilityType.DEV_SOURCE_PROJECT -> AuditStatus.SAFE_TO_REVIEW
            else -> AuditStatus.COMPATIBLE
        }

        return SecurityAuditReport(
            repoUrl = "https://github.com/$repoFullName",
            repoFullName = repoFullName,
            branch = branch,
            commitSha = files.firstOrNull()?.sha?.take(7) ?: "HEAD",
            compatibilityType = compatibilityType,
            status = status,
            integritySha256 = sha256,
            detectedManifestName = if (manifest != null) manifest.type else null,
            requestedPermissions = requestedPermissions,
            findings = findings,
            skillManifest = manifest,
            isInstallable = !isBlocked && (compatibilityType == CompatibilityType.JARVIS_SKILL || compatibilityType == CompatibilityType.AGENT_INSTRUCTION || compatibilityType == CompatibilityType.PROMPT_PACK),
            isDeveloperSource = compatibilityType == CompatibilityType.DEV_SOURCE_PROJECT,
            rawReadme = readmeContent
        )
    }

    private fun computeSha256(content: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
