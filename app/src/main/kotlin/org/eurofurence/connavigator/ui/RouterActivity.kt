package org.eurofurence.connavigator.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import org.eurofurence.connavigator.R
import org.eurofurence.connavigator.broadcast.Route
import org.eurofurence.connavigator.broadcast.pushRoute
import org.eurofurence.connavigator.database.Db
import org.eurofurence.connavigator.database.HasDb
import org.eurofurence.connavigator.database.UpdateIntentService
import org.eurofurence.connavigator.database.lazyLocateDb
import org.eurofurence.connavigator.net.imageService
import org.eurofurence.connavigator.pref.AuthPreferences
import org.eurofurence.connavigator.util.extensions.*
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.support.v4.drawerLayout
import java.util.*
import kotlin.reflect.full.createInstance

/**
 * Regular expression making regular expressions.
 */
data class Regregexex(val value: String) {
    companion object {
        /**
         * Higher order regex.
         */
        private val outer by lazy { Regex("""[{]([^}]+)[}]""") }
    }

    /**
     * The compiled values, computed on use.
     */
    private val compiled by lazy {
        val k = mutableListOf<String>()
        val m = outer.replace(value) { it ->
            k += it.groupValues[1]
            """\E(.+)\Q"""
        }
        Regex("""^\Q$m\E$""") to k.toList()

    }


    /**
     * All the keys defined in the value.
     */
    val keys: List<String>
        get() = compiled.second

    fun basicMatch(string: String) =
            compiled.first
                    .matches(string)

    /**
     * Matches a string and returns the assignments.
     */
    fun match(string: String) =
            compiled.first
                    .matchEntire(string)
                    ?.groupValues
                    ?.drop(1)
                    ?.mapIndexed { i, x -> keys[i] to x }
                    ?.associate { it }
                    ?: emptyMap()
}


/**
 * Router for fragments
 */
class RouterActivity : AppCompatActivity(), HasDb, AnkoLogger {
    override val db by lazyLocateDb()

    val routes = mapOf(
            Regregexex("/") to FragmentViewHome::class,
            Regregexex("/info") to FragmentViewInfoGroups::class,
            Regregexex("/info/{uid}") to FragmentViewInfo::class,
            Regregexex("/event") to FragmentViewEvents::class,
            Regregexex("/event/{uid}") to FragmentViewEvent::class,
            Regregexex("/dealer") to FragmentViewDealers::class,
            Regregexex("/dealer/{uid}") to FragmentViewDealer::class,
            Regregexex("/maps") to FragmentViewMaps::class,
            Regregexex("/about") to FragmentViewAbout::class,
            Regregexex("/messages") to FragmentViewMessages::class
    )

    val history = Stack<Pair<Regregexex, Fragment>>()
    val ui = RouterUi()
    val update by pushRoute

    var current: Pair<Regregexex, Fragment>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        info { "Creating router" }

        ui.setContentView(this)

        info { "Sending router to default activity" }

        // Start at home
        push(Route("/"))

        // Handle incoming intent
        if(intent.action == Intent.ACTION_VIEW){
            push(Route(intent.dataString))
        }

        // Listen for history updates
        update.listen { push(it) }

        // Set up the navigation menu
        configureNavigation()
    }

    private fun configureNavigation() {
        ui.navigation.setNavigationItemSelectedListener  {
            when (it.itemId) {
                R.id.navHome -> update.send(Route("/"))
                R.id.navInfo -> update.send(Route("/info"))
                R.id.navEvents -> update.send(Route("/event"))
                R.id.navDealersDen -> update.send(Route("/dealer"))
                R.id.navMaps -> update.send(Route("/maps"))
                R.id.navAbout -> update.send(Route("/about"))
                R.id.navLogin -> startActivity<LoginActivity>()
                R.id.navMessages -> {
                    if(AuthPreferences.isLoggedIn())
                        update.send(Route("/messages"))
                    else
                        longToast("You need to login before you can see private messages!")
                }
                R.id.navWebSite -> browse("https://www.eurofurence.org/")
                R.id.navWebTwitter -> browse("https://twitter.com/eurofurence")
                R.id.navDevReload -> UpdateIntentService.dispatchUpdate(this)
                R.id.navDevSettings -> startActivity<PreferenceActivity>()
                R.id.navDevClear -> {
                    AlertDialog.Builder(ContextThemeWrapper(this, R.style.appcompatDialog))
                            .setTitle("Clearing Database")
                            .setMessage("This will get rid of all cached items you have stored locally. You will need an internet connection to restart!")
                            .setPositiveButton("Clear", { dialogInterface, i -> db.clear(); imageService.clear(); System.exit(0) })
                            .setNegativeButton("Cancel", { dialogInterface, i -> })
                            .show()
                }
                else -> Unit
            }

            ui.drawer.closeDrawer(GravityCompat.START)

            true
        }
    }

    fun push(newRoute: Route) {
        info { "Pushing url $newRoute" }

        // Finding
        val route = routes.keys.find { it.basicMatch(newRoute.url) }
        if (route != null) {
            // Get the fragment
            val fragment = routes[route]!!

            info { "Route is valid. Creating instance" }

            // Since we are moving to a new element, we move current to the history
            history.push(current)

            info { "History now contains ${history.size} items" }

            // then we re-assign current
            val instance = fragment.createInstance()

            // Match the bound parameters
            instance.arguments = Bundle()
            route.match(newRoute.url).forEach { instance.arguments.putString(it.key, it.value) }

            current = Pair(route, instance)

            // And now we do transaction replacement
            info { "Starting fragment transaction" }

            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.in_slide_and_fade, R.anim.out_slide_and_fade, R.anim.in_slide_and_fade, R.anim.out_slide_and_fade)
                    .replace(1, instance)
                    .commitAllowingStateLoss()
        } else {
            warn { "Supplied url was not valid" }
        }
    }

    override fun onBackPressed() {
        info { "Navigating router back in history" }

        if (history.peek() != null) {
            val lastHistory = history.pop()

            info { "History now contains ${history.size} items" }

            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.in_slide_and_fade, R.anim.out_slide_and_fade, R.anim.in_slide_and_fade, R.anim.out_slide_and_fade)
                    .replace(1, lastHistory.second)
                    .commit()
        } else {
            super.onBackPressed()
        }
    }
}


class RouterUi : AnkoComponent<RouterActivity>, AnkoLogger {
    lateinit var toolbar: Toolbar
    lateinit var navigation: NavigationView
    lateinit var drawer: DrawerLayout

    override fun createView(ui: AnkoContext<RouterActivity>) = with(ui) {
        drawerLayout {
            drawer = this
            lparams(matchParent, matchParent)
            backgroundResource = R.color.backgroundGrey
            fitsSystemWindows = true

            coordinatorLayout {
                lparams(matchParent, matchParent)

                verticalLayout {
                    lparams(matchParent, matchParent)

                    appBarLayout {

                        lparams(matchParent, wrapContent)

                        toolbar = toolbar {
                            lparams(matchParent, wrapContent)
                            title = "Eurofurence"
                        }

                        val toggle = ActionBarDrawerToggle(
                                ui.owner, this@drawerLayout, toolbar, R.string.navigation_drawer_open,
                                R.string.navigation_drawer_close
                        )
                        addDrawerListener(toggle)
                        toggle.syncState()

                        tabLayout {
                            lparams(matchParent, wrapContent)
                            visibility = View.GONE
                        }
                    }

                    frameLayout {
                        lparams(matchParent, wrapContent)
                        id = 1
                    }
                }
            }


            navigation = navigationView {
                lparams(matchParent, wrapContent) {
                    gravity = Gravity.START
                }

                inflateHeaderView(R.layout.layout_nav_header)

                inflateMenu(R.menu.nav_drawer)
            }
        }
    }
}
