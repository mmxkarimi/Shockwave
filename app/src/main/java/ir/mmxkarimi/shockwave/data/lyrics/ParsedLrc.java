package ir.mmxkarimi.shockwave.data.lyrics;

import ir.mmxkarimi.shockwave.domain.model.LyricLine;
import ir.mmxkarimi.shockwave.domain.model.Lyrics;
import java.util.List;
import java.util.Map;

/**
 * Result of {@link LrcParser#parse}.
 *
 * @param metadata LRC header tags with lower-cased keys ({@code ti}, {@code ar}, {@code al}, {@code by}, ...)
 * @param offsetMs the {@code [offset:]} tag; already applied to {@code lines}
 */
public record ParsedLrc(List<LyricLine> lines, boolean synced, Map<String, String> metadata, long offsetMs) {

    public Lyrics toLyrics(String trackId, String source) {
        return new Lyrics(trackId, lines, synced, source);
    }
}
