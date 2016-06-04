package org.eurofurence.connavigator.ui.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.CardView
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.swagger.client.model.EventConferenceDay
import io.swagger.client.model.EventEntry
import org.eurofurence.connavigator.R
import org.eurofurence.connavigator.database.Database
import org.eurofurence.connavigator.net.imageService
import org.eurofurence.connavigator.ui.FragmentViewEvent
import org.eurofurence.connavigator.ui.communication.ContentAPI
import org.eurofurence.connavigator.ui.filters.IEventFilter
import org.eurofurence.connavigator.util.EmbeddedLocalBroadcastReceiver
import org.eurofurence.connavigator.util.Formatter
import org.eurofurence.connavigator.util.delegators.view
import org.eurofurence.connavigator.util.extensions.applyOnRoot
import org.eurofurence.connavigator.util.extensions.get
import org.eurofurence.connavigator.util.extensions.letRoot
import org.eurofurence.connavigator.util.extensions.localReceiver
import java.util.*

/**
 * Event view recycler to hold the viewpager items
 */
class EventRecyclerFragment(val filterStrategy: IEventFilter, val filterVal: Any = Unit) : Fragment(), ContentAPI {

    // Event view holder finds and memorizes the views in an event card
    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventImage by view(ImageView::class.java)
        val eventTitle by view(TextView::class.java)
        val eventDate by view(TextView::class.java)
        val eventCard by view(CardView::class.java)
    }

    inner class DataAdapter : RecyclerView.Adapter<EventViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, pos: Int) =
                EventViewHolder(LayoutInflater
                        .from(parent.context)
                        .inflate(R.layout.fragment_event_card, parent, false))

        override fun getItemCount() =
                effectiveEvents.size

        override fun onBindViewHolder(holder: EventViewHolder, pos: Int) {
            // Get the event for the position
            val event = effectiveEvents[pos]

            // Assign the properties of the view
            holder.eventTitle.text = Formatter.eventTitle(event)
            holder.eventDate.text = Formatter.eventToTimes(event, database)

            // Load image
            imageService.load(database.imageDb[event.imageId], holder.eventImage)

            // Assign the on-click action
            holder.itemView.setOnClickListener {
                applyOnRoot { navigateToEvent(event) }
            }

            if (database.favoritedDb.items.filter { it.id.equals(event.id) }.count() > 0)
                holder.eventCard.setCardBackgroundColor(context.getColor(R.color.primaryLighter))
        }
    }

    val events by view(RecyclerView::class.java)

    var effectiveEvents = emptyList<EventEntry>()

    val database: Database get() = letRoot { it.database }!!

    lateinit var updateReceiver: EmbeddedLocalBroadcastReceiver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_events, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        effectiveEvents = filterStrategy.filter(database, filterVal).toList()

        // Configure the recycler
        events.adapter = DataAdapter()
        events.layoutManager = LinearLayoutManager(activity)
        events.itemAnimator = DefaultItemAnimator()

        updateReceiver = context.localReceiver(FragmentViewEvent.EVENT_STATUS_CHANGED) {
            events.adapter.notifyDataSetChanged()
        }

        updateReceiver.register()
    }

    override fun onPause() {
        super.onPause()
        updateReceiver.unregister()
    }

    override fun onResume() {
        super.onResume()
        updateReceiver.register()
    }

    override fun dataUpdated() {
        effectiveEvents = filterStrategy.filter(database, filterVal).toList()
        events.adapter.notifyDataSetChanged()
    }
}