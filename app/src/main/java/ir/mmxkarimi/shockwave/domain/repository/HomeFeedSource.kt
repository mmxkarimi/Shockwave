package ir.mmxkarimi.shockwave.domain.repository

import androidx.lifecycle.LiveData
import ir.mmxkarimi.shockwave.domain.model.HomeSection

/** Supplies the Home screen sections (New releases, Throwbacks, Quick picks ...). Empty sections are omitted. */
interface HomeFeedSource {

    fun getSections(): LiveData<List<HomeSection>>
}
