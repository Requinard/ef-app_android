package org.eurofurence.connavigator.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.eurofurence.connavigator.R
import org.eurofurence.connavigator.database.Database
import org.eurofurence.connavigator.tracking.Analytics
import org.eurofurence.connavigator.ui.communication.ContentAPI
import org.eurofurence.connavigator.ui.fragments.EventView
import org.eurofurence.connavigator.util.delegators.view
import org.eurofurence.connavigator.util.extensions.applyOnRoot
import org.eurofurence.connavigator.util.extensions.letRoot
import org.eurofurence.connavigator.util.extensions.size
import org.joda.time.DateTime

/**
 * Created by David on 5/3/2016.
 */
class FragmentEventsViewpager : Fragment(), ContentAPI {
    inner class EventFragmentPagerAdapter(val fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager) {
        override fun getPageTitle(position: Int): CharSequence? {
            return DateTime(database.eventConferenceDayDb.asc { it.date }[position].date).dayOfWeek().asShortText
        }

        override fun getItem(position: Int): Fragment? {
            return EventView(position, database.eventConferenceDayDb.asc { it.date }[position])
        }

        override fun getCount(): Int {
            return database.eventConferenceDayDb.size
        }
    }

    val database: Database get() = letRoot { it.database }!!

    val eventPager by view(ViewPager::class.java)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(R.layout.fview_events_viewpager, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        Analytics.changeScreenName("View Events Viewpager")

        eventPager.adapter = EventFragmentPagerAdapter(childFragmentManager)
        eventPager.offscreenPageLimit = 3

        applyOnRoot { tabs.setupWithViewPager(eventPager) }
    }

    override fun dataUpdated() {
        eventPager.adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        applyOnRoot { tabs.setupWithViewPager(null) }
    }
}