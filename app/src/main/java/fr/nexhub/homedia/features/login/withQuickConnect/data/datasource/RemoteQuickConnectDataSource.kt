package fr.nexhub.homedia.features.login.withQuickConnect.data.datasource

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fr.nexhub.homedia.managers.JellyfinManager
import fr.nexhub.homedia.network.error.NetworkError
import fr.nexhub.homedia.network.toGeneralError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.model.api.QuickConnectResult
import timber.log.Timber
import javax.inject.Inject

class RemoteQuickConnectDataSource @Inject constructor() : QuickConnectDataSource {
    override suspend fun initiate(): Either<NetworkError, Response<QuickConnectResult>> {
        return try {
            val quickConnectState = JellyfinManager.api.quickConnectApi.initiateQuickConnect()
            quickConnectState.right()
        } catch (err: InvalidStatusException) {
            err.toGeneralError().left()
        }
    }

    override suspend fun connect(secret: String, onSuccess: (String, Boolean) -> Unit) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            // This loop keep running forever
            // todo: find a way to stop it
            while (isActive) {
//                val quickConnectResult = JellyfinManager.api.quickConnectApi.authorizeQuickConnect(
//                    secret = secret
//                )
                val quickConnectResult = JellyfinManager.api.quickConnectApi.getQuickConnectState(
                    secret = secret
                )
                if (quickConnectResult.content.authenticated) {
                    scope.cancel()
                    onSuccess(secret, true)
                }
                Timber.tag("QUICK_CONNECT_STATE").i("getQuickConnectState still running")
                delay(5000)
            }
        }
    }
}