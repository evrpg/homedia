package fr.nexhub.homedia.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
import android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT
import android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fr.nexhub.homedia.features.common.item.domain.model.Item
import fr.nexhub.homedia.features.episodes.domain.model.Episode
import fr.nexhub.homedia.features.home.domain.model.Library
import fr.nexhub.homedia.features.overview.domain.model.Details
import fr.nexhub.homedia.features.overview.domain.model.Season
import fr.nexhub.homedia.features.settings.screens.profile.domain.model.User
import fr.nexhub.homedia.managers.JellyfinManager
import fr.nexhub.homedia.network.error.NetworkError
import fr.nexhub.homedia.network.toGeneralError
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.UserDto

fun <T> StateFlow<T>.toMutable() = this as MutableStateFlow

private val DPadEventsKeyCodes = listOf(
    KEYCODE_DPAD_LEFT,
    KEYCODE_SYSTEM_NAVIGATION_LEFT,
    KEYCODE_DPAD_RIGHT,
    KEYCODE_SYSTEM_NAVIGATION_RIGHT,
    KEYCODE_DPAD_CENTER,
    KEYCODE_ENTER,
    KEYCODE_NUMPAD_ENTER,
)

fun Modifier.handleDPadKeyEvents(
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
) = onPreviewKeyEvent {
    fun onActionUp(block: () -> Unit) {
        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) block()
    }

    if (!DPadEventsKeyCodes.contains(it.nativeKeyEvent.keyCode)) return@onPreviewKeyEvent false

    when (it.nativeKeyEvent.keyCode) {
        KEYCODE_ENTER,
        KEYCODE_DPAD_CENTER,
        KEYCODE_NUMPAD_ENTER,
        -> {
            onEnter?.apply {
                onActionUp(::invoke)
                return@onPreviewKeyEvent true
            }
        }

        KEYCODE_DPAD_LEFT,
        KEYCODE_SYSTEM_NAVIGATION_LEFT,
        -> {
            onLeft?.apply {
                onActionUp(::invoke)
                return@onPreviewKeyEvent true
            }
        }

        KEYCODE_DPAD_RIGHT,
        KEYCODE_SYSTEM_NAVIGATION_RIGHT,
        -> {
            onRight?.apply {
                onActionUp(::invoke)
                return@onPreviewKeyEvent true
            }
        }
    }
    false
}

suspend fun ByteReadChannel.byteReadChannelToBitmap(): Bitmap? {
    val limit: Long = 1024 * 1024
//    val headerSizeHint = 1024
    val byteArray = withContext(Dispatchers.IO) {
        this@byteReadChannelToBitmap.readRemaining(limit).readBytes()
    }
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
}

suspend fun BaseItemDto.getItemImage(width: Int, height: Int): Either<NetworkError, ByteReadChannel> {
    return try {
        return JellyfinManager.api.imageApi.getItemImage(
            itemId = this.id,
            imageType = ImageType.PRIMARY,
            width = width,
            height = height
        ).content.right()
    } catch (err: InvalidStatusException) {
        err.toGeneralError().left()
    }
}

fun  BaseItemDto.toLibrary(recentItems: List<Item>): Library {
    return Library(
        id = this.id,
        title = this.name ?: "Unknown",
        recentItems = recentItems
    )
}

fun  BaseItemDto.toItem(image: Bitmap?, details: Details?): Item {
    return Item(
        id = this.id,
        image = image,
        title = this.name ?: "Unknown",
        details = details
    )
}

fun  BaseItemDto.toDetails(): Details {
    val totalSeconds = this.runTimeTicks?.div(10_000_000)
    val hours = totalSeconds?.div(3600)
    val minutes = (totalSeconds?.rem(3600))?.div(60)
    return Details(
        type = this.type,
        productionYear = this.productionYear,
        communityRating = this.communityRating,
        overview = this.overview,
        duration = "${hours}h${minutes}",
        genres = this.genres
    )
}

fun BaseItemDto.toSeason(image: Bitmap?): Season {
    return Season (
        id = this.id,
        title = this.name ?: "???",
        image = image
    )
}

fun BaseItemDto.toEpisode(image: Bitmap?): Episode {
    return Episode (
        id = this.id,
        title = this.name ?: "???",
        image = image,
        overview = this.overview
    )
}

fun UserDto.toUser(): User {
    return User(
        name = this.name ?: "Unknown",
        lastActivityDate = this.lastActivityDate
    )
}