package org.eurofurence.connavigator.util.extensions

import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.NavigationView
import android.support.design.widget.TabLayout
import android.support.v7.widget.RecyclerView
import android.view.ViewManager
import com.github.chrisbanes.photoview.PhotoView
import com.github.lzyzsd.circleprogress.ArcProgress
import com.joanzapata.iconify.widget.IconButton
import com.joanzapata.iconify.widget.IconTextView
import org.jetbrains.anko.custom.ankoView
import us.feras.mdv.MarkdownView

/**
 * Extensions to anko viewmanager
 */

inline fun ViewManager.arcProgress(init: ArcProgress.() -> Unit) = ankoView({ ArcProgress(it) }, 0, init)

inline fun ViewManager.recycler(init: RecyclerView.() -> Unit) = ankoView({ RecyclerView(it) }, 0, init)
inline fun ViewManager.photoView(init: PhotoView.() -> Unit) = ankoView({ PhotoView(it) }, 0, init)
inline fun ViewManager.markdownView(init: MarkdownView.() -> Unit) = ankoView({ MarkdownView(it) }, 0, init)
inline fun ViewManager.fontAwesomeView(init: IconTextView.() -> Unit) = ankoView({ IconTextView(it) }, 0, init)
inline fun ViewManager.fontAwesomeButton(init: IconButton.() -> Unit) = ankoView({ IconButton(it) }, 0, init)
inline fun ViewManager.coordinatorLayout(init: CoordinatorLayout.() -> Unit) = ankoView({ CoordinatorLayout(it) }, 0, init)
inline fun ViewManager.appBarLayout(init: AppBarLayout.() -> Unit) = ankoView({ AppBarLayout(it) }, 0, init)
inline fun ViewManager.tabLayout(init: TabLayout.() -> Unit) = ankoView({ TabLayout(it) }, 0, init)
inline fun ViewManager.navigationView(init: NavigationView.() ->  Unit) =  ankoView({NavigationView(it)}, 0, init)
