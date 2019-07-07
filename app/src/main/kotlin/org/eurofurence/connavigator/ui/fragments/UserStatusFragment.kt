package org.eurofurence.connavigator.ui.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.reactivex.android.schedulers.AndroidSchedulers
import org.eurofurence.connavigator.R
import org.eurofurence.connavigator.notifications.cancelFromRelated
import org.eurofurence.connavigator.preferences.AuthPreferences
import org.eurofurence.connavigator.services.PMService
import org.eurofurence.connavigator.util.extensions.fontAwesomeView
import org.eurofurence.connavigator.util.v2.compatAppearance
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.UI

class UserStatusFragment : AutoDisposingFragment(), AnkoLogger {
    val ui = UserStatusUi()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            UI { ui.createView(this) }.view

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Subscribe to changes in authentication, i.e., login status and user name.
        subscribeToAuthentication()

        // Subscribe to changes in PM availability.
        subscribeToPMs()
    }

    /**
     * Subscribes to authentication changes to display proper information on the UI and to activate a periodic refresh
     * of PMs.
     */
    private fun subscribeToAuthentication() {
        AuthPreferences
                .updated
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.isLoggedIn to it.username }
                .distinct()
                .subscribe { (isLoggedIn, username) ->
                    // React to user login changes.
                    if (isLoggedIn) {
                        // Log status change.
                        info { "User is logged in" }

                        // Display logged in, name and set UI action.
                        ui.apply {
                            title.text = getString(R.string.misc_welcome_user, username.capitalize())
                            layout.setOnClickListener {
                                val action = HomeFragmentDirections.actionFragmentViewHomeToFragmentViewMessageList()
                                findNavController().navigate(action)
                            }
                        }
                    } else {
                        // Log status change.
                        info { "User is not logged in" }

                        // Display not logged in, reset UI action.
                        ui.apply {
                            title.textResource = R.string.login_not_logged_in
                            subtitle.textResource = R.string.login_tap_to_login
                            layout.setOnClickListener {
                                findNavController().navigate(R.id.action_fragmentViewHome_to_loginActivity)
                            }
                        }
                    }
                }
                .collectOnDestroyView()
    }

    /**
     * Subscribes to PM availability by displaying and cancelling notifications, as well as indicating the number
     * of unread messages.
     */
    private fun subscribeToPMs() {
        PMService
                .updated
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe { messages ->
                    context?.apply {
                        for ((_, m) in messages)
                            notificationManager.cancelFromRelated(m.id)

                        info { "Fetched messages, ${messages.count()} messages found" }

                        val unreadMessages = messages.values.filter { it.readDateTimeUtc == null }

                        if (unreadMessages.isNotEmpty()) {
                            info { "Unread messages are present! Giving attention." }
                            ui.subtitle.text = getString(R.string.message_you_have_unread_messages, unreadMessages.size)
                            ui.subtitle.textColor = ContextCompat.getColor(this, R.color.primaryDark)
                            ui.userIcon.textColor = ContextCompat.getColor(this, R.color.primaryDark)
                        } else {
                            info { "No unread messages found, displaying total message counts" }
                            ui.subtitle.text = getString(R.string.message_you_have_messages, messages.count())
                            ui.subtitle.textColor = ContextCompat.getColor(this, android.R.color.tertiary_text_dark)
                            ui.userIcon.textColor = ContextCompat.getColor(this, android.R.color.tertiary_text_dark)
                        }
                    }
                }
                .collectOnDestroyView()

    }
}

class UserStatusUi : AnkoComponent<Fragment> {
    lateinit var title: TextView
    lateinit var subtitle: TextView
    lateinit var userIcon: TextView
    lateinit var layout: ViewGroup

    override fun createView(ui: AnkoContext<Fragment>) = with(ui) {
        linearLayout {
            this@UserStatusUi.layout = this
            isClickable = true

            lparams(matchParent, wrapContent) {
                setPadding(0, dip(20), 0, dip(20))
            }
            weightSum = 100F
            backgroundResource = R.color.cardview_light_background

            userIcon = fontAwesomeView {
                text = "{fa-user 30sp}"
                lparams(dip(0), matchParent, 15F)
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }

            verticalLayout {
                title = textView {
                    textResource = R.string.misc_title
                    compatAppearance = android.R.style.TextAppearance_Medium
                }

                subtitle = textView {
                    textResource = R.string.misc_fetching
                    compatAppearance = android.R.style.TextAppearance_Small
                }
                lparams(dip(0), matchParent, 75F)
            }

            fontAwesomeView {
                text = "{fa-chevron-right 24sp}"
                gravity = Gravity.CENTER
                lparams(dip(0), matchParent, 10F)
            }
        }
    }

}