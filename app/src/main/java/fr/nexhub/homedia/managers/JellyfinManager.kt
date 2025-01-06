package fr.nexhub.homedia.managers

import android.content.Context
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.UUID

object JellyfinManager {

    private lateinit var _api: ApiClient
    val api: ApiClient
        get() = _api


    fun initSDK(currentContext: Context, baseUrl: String, accessToken: String?, userId: UUID?) {
        val jellyfin =  createJellyfin {
            context = currentContext
            clientInfo = ClientInfo(name = "jellyfin_androidTV_client", version = "1.63")
        }

        _api = jellyfin.createApi(
            baseUrl = baseUrl,
            accessToken = accessToken,
//            userId = userId
        )
    }
}