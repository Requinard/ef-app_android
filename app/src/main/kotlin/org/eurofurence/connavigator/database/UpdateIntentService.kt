package org.eurofurence.connavigator.database

import android.app.IntentService
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.serialization.ImplicitReflectionSerializer
import org.eurofurence.connavigator.BuildConfig
import org.eurofurence.connavigator.broadcast.EventFavoriteBroadcast
import org.eurofurence.connavigator.net.imageService
import org.eurofurence.connavigator.pref.DebugPreferences
import org.eurofurence.connavigator.pref.RemotePreferences
import org.eurofurence.connavigator.util.v2.convert
import org.eurofurence.connavigator.util.v2.internalSpec
import org.eurofurence.connavigator.webapi.apiService
import org.jetbrains.anko.*
import org.joda.time.DateTime
import java.util.*
import kotlinx.serialization.Serializable
import org.eurofurence.connavigator.util.extensions.*
import org.eurofurence.connavigator.util.v2.DateSerializer

@Serializable
data class UpdateComplete(val success: Boolean,
                          @Serializable(with = DateSerializer::class)
                          val time: Date?, val exception: Throwable?)

@ImplicitReflectionSerializer
val updateComplete = internalSpec<UpdateComplete>()

/**
 * Dispatches an update
 * @param context The host context for the service
 */
fun AnkoLogger.dispatchUpdate(context: Context,
                              showToastOnCompletion: Boolean = false,
                              preloadChangedImages: Boolean = true) {
    info { "Dispatching update" }

    val intent = Intent(context, UpdateIntentService::class.java)
    intent.putExtra("showToastOnCompletion", showToastOnCompletion)
    intent.putExtra("preloadChangedImages", preloadChangedImages)

    context.startService(intent)
}

/**
 * Updates the database on request.
 */
class UpdateIntentService : IntentService("UpdateIntentService"), HasDb, AnkoLogger {
    companion object {
        const val UPDATE_COMPLETE = "org.eurofurence.connavigator.driver.UPDATE_COMPLETE"
    }

    override val db by lazyLocateDb()

    @ImplicitReflectionSerializer
    private val updateCompleteMsg by updateComplete

    // TODO: Sticky intent since there should only be one pending update
    @ImplicitReflectionSerializer
    override fun onHandleIntent(intent: Intent?) {
        info { "Handling update intent service intent" }
        val showToastOnCompletion = intent?.getBooleanExtra("showToastOnCompletion", false) ?: false
        val preloadChangedImages = intent?.getBooleanExtra("preloadChangedImages", false) ?: false

        // Initialize the response, the following code is net and IO oriented, it could fail
        val (response, responseObject) = {
            info { "Updating remote preferences" }
            RemotePreferences.update()
            info { "Retrieving sync since $date" }

            // Get sync from server
            val sync = apiService.sync.apiSyncGet(date)

            info { sync }

            if (sync.conventionIdentifier != BuildConfig.CONVENTION_IDENTIFIER) {
                throw Exception("Convention Identifier mismatch\n\nExpected: ${BuildConfig.CONVENTION_IDENTIFIER}\nReceived: ${sync.conventionIdentifier}")
            }

            /*
                All images that have been deleted or changed can get removed from cache to free
                space. We need to base the query on existing (local) records so the imageService
                can access the cache by the "old" urls.
              */
            var invalidatedImages = images.items.filter {
                sync.images.deletedEntities.contains(it.id)
                || sync.images.changedEntities.any { rec -> rec.id == it.id } }

            for (image in invalidatedImages)
                imageService.removeFromCache(image)

            /*
                Preload images if they changed. Only StartActivity doesn't want this, because it's
                doing it manually with UI presentation.
              */
            if (preloadChangedImages) {
                for (image in sync.images.changedEntities)
                    imageService.preload(image)
            }

            // Apply sync
            announcements.apply(sync.announcements.convert())
            dealers.apply(sync.dealers.convert())
            days.apply(sync.eventConferenceDays.convert())
            rooms.apply(sync.eventConferenceRooms.convert())
            tracks.apply(sync.eventConferenceTracks.convert())
            events.apply(sync.events.convert())
            images.apply(sync.images.convert())
            knowledgeEntries.apply(sync.knowledgeEntries.convert())
            knowledgeGroups.apply(sync.knowledgeGroups.convert())
            maps.apply(sync.maps.convert())

            // Reconcile events with favorites
            faves = events.items.map { it.id }
                    .toSet()
                    .let { eventsIds ->
                        faves.filter { it in eventsIds }
                    }

            // Update notifications for favorites
            EventFavoriteBroadcast().updateNotifications(applicationContext, faves)

            // Store new time
            date = sync.currentDateTimeUtc

            info { "Synced, current sync time is $date" }

            // Make the success response message
            info { "Completed update successfully" }

            if (showToastOnCompletion) {
                runOnUiThread {
                    longToast("Successfully updated all content.")
                }
            }

            UPDATE_COMPLETE.toIntent {
                booleans["success"] = true
                objects["time"] = date
            } to UpdateComplete(true, date, null)
        } catchAlternative { ex: Throwable ->
            // Make the fail response message, transfer exception
            if (showToastOnCompletion) {
                runOnUiThread {
                    longToast("Failed to update: ${ex.message}")
                }
            }

            error("Completed update with error", ex)
            UPDATE_COMPLETE.toIntent {
                booleans["success"] = false
                objects["time"] = date
                objects["reason"] = ex
            } to UpdateComplete(false, date, ex)
        }

        // Send a broadcast notifying completion of this action
        LocalBroadcastManager.getInstance(this).sendBroadcast(response)

        updateCompleteMsg.send(responseObject)
    }
}