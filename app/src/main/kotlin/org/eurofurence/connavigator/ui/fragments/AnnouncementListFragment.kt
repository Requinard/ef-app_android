package org.eurofurence.connavigator.ui.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.pawegio.kandroid.i
import io.reactivex.disposables.Disposables
import io.swagger.client.model.AnnouncementRecord
import org.eurofurence.connavigator.R
import org.eurofurence.connavigator.database.HasDb
import org.eurofurence.connavigator.database.lazyLocateDb
import org.eurofurence.connavigator.pref.AppPreferences
import org.eurofurence.connavigator.ui.FragmentViewHomeDirections
import org.eurofurence.connavigator.ui.communication.ContentAPI
import org.eurofurence.connavigator.ui.views.NonScrollingLinearLayout
import org.eurofurence.connavigator.util.delegators.view
import org.eurofurence.connavigator.util.extensions.*
import org.eurofurence.connavigator.util.v2.compatAppearance
import org.eurofurence.connavigator.util.v2.plus
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.UI

class AnnouncementListFragment : Fragment(), HasDb, AnkoLogger {
    inner class AnnouncementDataholder(itemView: View?) : RecyclerView.ViewHolder(itemView), ContentAPI {
        val layout: LinearLayout by view()
        val announcementTitle: TextView by view()
        val announcementContent: TextView by view()
    }

    inner class AnnoucementRecyclerDataAdapter :
            RecyclerView.Adapter<AnnouncementDataholder>(), AnkoLogger {

        var announcements = emptyList<AnnouncementRecord>()

        override fun onBindViewHolder(holder: AnnouncementDataholder, position: Int) {
            val announcement = announcements[position]

            info { "updating announcement $position" }

            holder.announcementTitle.text = announcement.title
            holder.announcementContent.text = announcement.area
            holder.layout.setOnClickListener {
                val action = FragmentViewHomeDirections.actionFragmentViewHomeToFragmentViewAnnouncement(announcement.id)
                findNavController().navigate(action)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementDataholder {
            return AnnouncementDataholder(AnnouncementUi().createView(
                    AnkoContext.create(parent.context.applicationContext, parent)))
        }

        override fun getItemCount(): Int {
            return announcements.count()
        }
    }

    override val db by lazyLocateDb()

    var subscriptions = Disposables.empty()

    val ui = AnnouncementsUi()
    private val announcementAdapter by lazy { AnnoucementRecyclerDataAdapter() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            UI { ui.createView(this) }.view

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        info { "Creating announcements list" }

        ui.announcements.adapter = announcementAdapter

        subscriptions += db.subscribe {
            info { "Updating items in announcement recycler" }
            ui.layout.visibility = if (getAnnouncements().count() == 0) View.GONE else View.VISIBLE
            announcementAdapter.announcements = getAnnouncements()
            announcementAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        subscriptions.dispose()
        subscriptions = Disposables.empty()
    }

    private fun getAnnouncements() = db.announcements.items
            .filterIf(!AppPreferences.showOldAnnouncements) { it.validFromDateTimeUtc.time <= now().millis && it.validUntilDateTimeUtc.time > now().millis }
            .sortedBy { it.validFromDateTimeUtc }
            .toList()
}

class AnnouncementsUi : AnkoComponent<Fragment> {
    lateinit var announcements: RecyclerView
    lateinit var title: TextView
    lateinit var layout: ViewGroup

    override fun createView(ui: AnkoContext<Fragment>) = with(ui) {
        verticalLayout {
            layout = this

            backgroundResource = R.color.cardview_light_background
            lparams(matchParent, wrapContent)

            title = textView("Announcements") {
                compatAppearance = android.R.style.TextAppearance_DeviceDefault_Small
                padding = dip(15)
            }.lparams(matchParent, wrapContent) {
                setMargins(0, 0, 0, 0)
            }

            announcements = recycler {
                itemAnimator = DefaultItemAnimator()
            }.lparams(matchParent, wrapContent)
        }
    }
}

class AnnouncementUi : AnkoComponent<ViewGroup> {
    override fun createView(ui: AnkoContext<ViewGroup>) = with(ui) {
        linearLayout {
            lparams(matchParent, wrapContent)
            id = R.id.layout
            weightSum = 100F

            fontAwesomeView {
                text = "{fa-file-text 24sp}"
                lparams(dip(0), matchParent, 15F) {
                    setMargins(0, dip(5), 0, 0)
                }
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }

            verticalLayout {
                textView("Announcement Title") {
                    id = R.id.announcementTitle
                    compatAppearance = android.R.style.TextAppearance_Medium
                }.lparams(matchParent, wrapContent) {
                    setMargins(0, 0, 0, 0)
                }

                textView("Announcement Content") {
                    id = R.id.announcementContent
                    compatAppearance = android.R.style.TextAppearance_DeviceDefault_Small
                }.lparams(matchParent, wrapContent) {
                    setMargins(0, 0, 0, dip(10))
                }

            }.lparams(dip(0), wrapContent, 80F) {
                setMargins(0, 0, 0, dip(5))
            }
        }
    }
}