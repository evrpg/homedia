package fr.nexhub.homedia.features.home.data.datasource

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fr.nexhub.homedia.features.common.item.domain.usecase.GetItemsUseCase
import fr.nexhub.homedia.features.home.domain.model.Library
import fr.nexhub.homedia.managers.JellyfinManager
import fr.nexhub.homedia.network.error.NetworkError
import fr.nexhub.homedia.network.toGeneralError
import fr.nexhub.homedia.utils.toLibrary
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import javax.inject.Inject

class RemoteLibraryDataSource @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase
): LibraryDataSource {
    override suspend fun getLibraries(): Either<NetworkError, List<Library>> {
        return try {
            val resp = JellyfinManager.api.itemsApi.getItems(
                userId = JellyfinManager.api.userApi.getCurrentUser().content.id,
            )
            val items = resp.content.items
            val list: MutableList<Library> = mutableListOf()
            if (items.isNullOrEmpty()) {
                list.right()
            } else {
                items.forEach { item ->
                    val (recentItems, _) = getItemsUseCase.execute(item.id, 10)
                    list.add(item.toLibrary(recentItems))
                }
                list.right()
            }
        } catch (err: InvalidStatusException) {
            err.toGeneralError().left()
        }
    }
}