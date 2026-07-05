package com.kyrx.mypresence.core.utils

object Constants {
    const val DISCORD_API_BASE = "https://discord.com/api/v10"
    const val DISCORD_WS_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
    const val DISCORD_OAUTH2_URL = "https://discord.com/api/oauth2/authorize"
    const val DISCORD_TOKEN_URL = "https://discord.com/api/oauth2/token"

    const val CLIENT_ID = "" // Set your Discord application client ID here
    const val REDIRECT_URI = "mypresence://oauth2/callback"
    const val SCOPE = "identify%20rpc"

    const val DATABASE_NAME = "mypresence_db"
    const val PREFS_NAME = "mypresence_prefs"
}
