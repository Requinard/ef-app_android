package org.eurofurence.connavigator.ui

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.View
import org.eurofurence.connavigator.R
import org.eurofurence.connavigator.util.extensions.appBarLayout
import org.eurofurence.connavigator.util.extensions.coordinatorLayout
import org.eurofurence.connavigator.util.extensions.navigationView
import org.eurofurence.connavigator.util.extensions.tabLayout
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
class RouterActivity : AppCompatActivity(), AnkoLogger {
    val routes = mapOf(
            Regregexex("/") to FragmentViewHome::class,
            Regregexex("/info") to FragmentViewInfoGroups::class,
            Regregexex("/event") to FragmentViewEvents::class,
            Regregexex("/event/{uid}") to FragmentViewEvent::class
    )

    val history = Stack<Pair<Regregexex, Fragment>>()
    val ui = RouterUi()
    var current: Pair<Regregexex, Fragment>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        info { "Creating router" }

        ui.setContentView(this)

        info { "Sending router to default activity" }

        push("/")
    }

    fun push(newRoute: String) {
        info { "Pushing route $newRoute" }

        // Finding
        val route = routes.keys.find { it.basicMatch(newRoute) }
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
            route.match(newRoute).forEach { instance.arguments.putString(it.key, it.value) }

            current = Pair(route, instance)

            // And now we do transaction replacement
            info { "Starting fragment transaction" }

            supportFragmentManager.beginTransaction()
                    .replace(1, instance)
                    .commitAllowingStateLoss()
        } else {
            warn { "Supplied route was not valid" }
        }
    }

    override fun onBackPressed() {
        info { "Navigating router back in history" }

        if (history.peek() != null) {
            val lastHistory = history.pop()

            info { "History now contains ${history.size} items" }

            supportFragmentManager.beginTransaction()
                    .remove(current!!.second)
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

    override fun createView(ui: AnkoContext<RouterActivity>) = with(ui) {
        drawerLayout {
            lparams(matchParent, matchParent)
            backgroundResource = R.color.backgroundGrey
            fitsSystemWindows = true

            coordinatorLayout {
                lparams(matchParent, matchParent)

                verticalLayout {
                    lparams(matchParent, matchParent)

                    appBarLayout {
                        val toggle = ActionBarDrawerToggle(
                                ui.owner, this@drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
                        )
                        addDrawerListener(toggle)
                        toggle.syncState()

                        lparams(matchParent, wrapContent)

                        toolbar = toolbar {
                            lparams(matchParent, wrapContent)
                            title = "Eurofurence"
                        }

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

                setNavigationItemSelectedListener {
                    info { it.itemId }
                    true
                }
            }
        }
    }

}