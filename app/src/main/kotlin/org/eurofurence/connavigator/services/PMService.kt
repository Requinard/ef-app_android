package org.eurofurence.connavigator.services

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.swagger.client.model.PrivateMessageRecord
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.eurofurence.connavigator.preferences.AuthPreferences
import org.jetbrains.anko.AnkoLogger
import java.util.*

/**
 * Private message service. Calls are not indirected to background tasks, so subscription and
 * resolution must handle running in a proper context themselves.
 */
object pmService : AnkoLogger {
    /**
     * All messages from the last update, if it should be up to date, [fetch] must be called.
     */
    var all = emptyMap<UUID, PrivateMessageRecord>()
        private set

    /**
     * This subject will be notified on completed fetches, will initially be empty.
     */
    val updated: BehaviorSubject<Map<UUID, PrivateMessageRecord>> =
            BehaviorSubject.createDefault(all)

    /**
     * Observes a single key in the message items.
     */
    fun observeFind(messageId: UUID) =
            updated.flatMap { items ->
                // Get item at message ID, if present, return just it, otherwise empty.
                items[messageId]?.let {
                    Observable.just(it)
                } ?: Observable.empty()
            }

    /**
     * Updates the data, notifies subjects.
     */
    fun fetch() {
        // Call API method to get all private message records and store them.
        apiService.communications
                .addHeader("Authorization", AuthPreferences.asBearer())
        val next = apiService.communications
                .apiCommunicationPrivateMessagesGet()
                .associateBy(PrivateMessageRecord::getId)

        // Notify subject of successful messages, if they are actually different.
        if (next != all) {
            all = next
            updated.onNext(all)
        }
    }

    /**
     * Stores currently executing promises to prevent unnecessary calls.
     */
    private var lastFetch: Promise<Unit, Exception>? = null

    /**
     * Performs a fetch in the background as a task.
     */
    fun fetchInBackground(): Promise<Unit, Exception> {
        // Return same promise if running.
        return lastFetch ?: task { fetch().also { lastFetch = null } }.also { lastFetch = it }
    }

    /**
     * Finds the message for the given ID. If not in cache, a call to [fetch] will be dispatched.
     */
    fun find(messageId: UUID): PrivateMessageRecord? {
        // Get existing message and return
        val existing = all[messageId]
        if (existing != null)
            return existing

        // Fetch and return value, return null if failed.
        return try {
            fetch()
            all[messageId]
        } catch (t: Throwable) {
            null
        }
    }
}