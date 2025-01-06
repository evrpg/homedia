package fr.nexhub.homedia.features.login.withQuickConnect.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nexhub.homedia.features.login.withQuickConnect.domain.usecase.AuthenticateUseCase
import fr.nexhub.homedia.features.login.withQuickConnect.domain.usecase.ConnectUseCase
import fr.nexhub.homedia.managers.JellyfinManager
import fr.nexhub.homedia.managers.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.authenticateWithQuickConnect
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.api.AuthenticationResult
import org.jellyfin.sdk.model.api.QuickConnectResult
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class QuickConnectViewModel @Inject constructor(
    private val connectUseCase: ConnectUseCase,
    private val authenticateUseCase: AuthenticateUseCase,
    private val preferencesManager: PreferencesManager,
): ViewModel() {

    private val _state = MutableStateFlow(QuickConnectViewState())
    val state = _state.asStateFlow()

    fun authenticate(authHasSucceeded: () -> Unit) {
        viewModelScope.launch {
            authenticateUseCase.execute()
            .onRight { quickConnectState ->
                _state.update { quickConnectViewState ->
                    quickConnectViewState.copy(
                        code = quickConnectState.content.code,
                        status = quickConnectState.status
                    )
                }
                connect(quickConnectState = quickConnectState, authHasSucceeded)
            }
            .onLeft { error ->
                _state.update { quickConnectViewState ->
                    quickConnectViewState.copy(
                        status = error.details.code,
                    )
                }
                Timber.tag("QUICK_CONNECT_AUTH_ERROR").d(error.t?.message ?: "")
            }
        }
    }

    private suspend fun connect(quickConnectState: Response<QuickConnectResult>, authHasSucceeded: () -> Unit) {
        connectUseCase.execute(secret = quickConnectState.content.secret) { secret, status ->
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                val authenticationResult by JellyfinManager.api.userApi.authenticateWithQuickConnect(
                    secret = secret
                )
                storeAccessToken(authenticationResult = authenticationResult)
                storeUserId(authenticationResult = authenticationResult)
                storeBaseUrl()
                val mainScope = CoroutineScope(Dispatchers.Main)
                mainScope.launch {
                    authHasSucceeded()
                }
                Timber.tag("QUICK_CONNECT_AUTH_STATUS").d(status.toString())
            }
        }
    }

    private fun storeAccessToken(authenticationResult: AuthenticationResult) {
        JellyfinManager.api.update(accessToken = authenticationResult.accessToken)
        authenticationResult.accessToken?.let {
            preferencesManager.saveData("ACCESS_TOKEN", it, true)
        }
    }

    private fun storeUserId(authenticationResult: AuthenticationResult) {
//        JellyfinManager.api.userId = authenticationResult.user?.id
        authenticationResult.user?.id.let {
            preferencesManager.saveData("USER_ID", it.toString())
        }
    }

    private fun storeBaseUrl() {
        JellyfinManager.api.baseUrl?.let {
            preferencesManager.saveData("BASE_URL", it)
        }
    }
}