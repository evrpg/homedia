package fr.nexhub.homedia.features.common.item.data.datasource

import android.graphics.Bitmap
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fr.nexhub.homedia.features.common.item.domain.model.Item
import fr.nexhub.homedia.managers.JellyfinManager
import fr.nexhub.homedia.network.error.NetworkError
import fr.nexhub.homedia.network.toGeneralError
import fr.nexhub.homedia.utils.byteReadChannelToBitmap
import fr.nexhub.homedia.utils.getItemImage
import fr.nexhub.homedia.utils.toItem
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import javax.inject.Inject

class RemoteItemDataSource @Inject constructor(): ItemDataSource {
    override suspend fun getItems(libraryId: UUID, limit: Int?): Either<NetworkError, List<Item>> {
        return try {
            val resp = JellyfinManager.api.itemsApi.getItems(
                userId = JellyfinManager.api.userApi.getCurrentUser().content.id,
                parentId = libraryId,
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                sortBy = listOf(ItemSortBy.DATE_CREATED),
                sortOrder = listOf(SortOrder.DESCENDING),
                limit = limit
            )
            val items = resp.content.items
            if (items.isNullOrEmpty()) {
                val list: List<Item> = listOf()
                list.right()
            } else {
                items.map { baseItem ->
                    var bitmap: Bitmap? = null
                    baseItem.getItemImage(350, 500)
                        .onRight {
                            bitmap = it.byteReadChannelToBitmap()
                        }
                    baseItem.toItem(bitmap, null)
                }.right()
            }
        } catch (err: InvalidStatusException) {
            err.toGeneralError().left()
        }
    }
}