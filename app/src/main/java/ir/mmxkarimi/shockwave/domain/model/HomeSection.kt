package ir.mmxkarimi.shockwave.domain.model

import java.util.Collections

/** A titled row on the Home screen. Immutable. */
data class HomeSection(
    @get:JvmName("id") val id: String,
    @get:JvmName("title") val title: String,
    @get:JvmName("subtitle") val subtitle: String?,
    @get:JvmName("style") val style: HomeSectionStyle,
    @get:JvmName("items") var items: List<HomeItem>,
) {
    init {
        items = Collections.unmodifiableList(ArrayList(items))
    }
}
