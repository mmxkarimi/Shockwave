package ir.mmxkarimi.shockwave.domain.repository;

import androidx.lifecycle.LiveData;
import ir.mmxkarimi.shockwave.domain.model.HomeSection;
import java.util.List;

/** Supplies the Home screen sections (New releases, Throwbacks, Quick picks ...). Empty sections are omitted. */
public interface HomeFeedSource {

    LiveData<List<HomeSection>> getSections();
}
