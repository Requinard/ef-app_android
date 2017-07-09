package org.eurofurence.connavigator.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import org.jetbrains.anko.*
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
 * Created by requinard on 7/9/17.
 */
class RouterActivity : AppCompatActivity(), AnkoLogger {
    val routes = mapOf(
            "/" to FragmentViewHome::class,
            "/info" to FragmentViewInfoGroups::class,
            "/event" to FragmentViewEvents::class,
            "/event/{uid}" to FragmentViewEvent::class
    )

    val history = Stack<Pair<String, Fragment>>()
    val parameterBinder = Regex("""[{]([^}]+)[}]""")
    var current: Pair<String, Fragment>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        info { "Creating router" }

        verticalLayout {
            button("Events") {
                setOnClickListener { push("/event") }
            }
            button("Info") {
                setOnClickListener { push("/info") }
            }
            id = 1
        }

        info { "Sending router to default activity" }

        push("/")
    }

    fun push(route: String) {
        info { "Pushing route $route" }

        // Finding
        val fragment = routes[route]
        if (fragment != null) {
            info { "Route is valid. Creating instance" }

            // Since we are moving to a new element, we move current to the history
            history.push(current)

            info { "History now contains ${history.size} items" }

            // then we re-assign current
            val instance = fragment.createInstance()
            current = Pair(route, instance)

            // Now we check the URL for variables


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