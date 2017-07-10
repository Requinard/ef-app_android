package org.eurofurence.connavigator.broadcast

import org.eurofurence.connavigator.util.v2.internalSpec
import kotlin.serialization.Serializable


@Serializable
data class Route(val url: String)

val pushRoute = internalSpec<Route>()
