package me.zhangls.entry

data class AuthenticationConfig(val origin: String?, val passkeysEnabled: Boolean)

internal fun configuredAuthEndpoint(): String? = passwordlessEndpoint(BuildKonfig.AUTH_BASE_URL)
