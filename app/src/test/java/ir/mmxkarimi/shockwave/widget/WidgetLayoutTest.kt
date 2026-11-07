package ir.mmxkarimi.shockwave.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutTest {
    @Test
    fun tallFourByOneStaysCompact() {
        // Samsung 4x1: about 360 x 116 dp, Glance reports the 250 x 100 bucket.
        assertEquals(WidgetLayout.COMPACT, widgetLayoutFor(250f, 100f))
        assertEquals(WidgetLayout.COMPACT, widgetLayoutFor(360f, 116f))
    }

    @Test
    fun shortFourByOneIsCompact() {
        assertEquals(WidgetLayout.COMPACT, widgetLayoutFor(180f, 48f))
    }

    @Test
    fun fourByTwoIsWide() {
        assertEquals(WidgetLayout.WIDE, widgetLayoutFor(250f, 140f))
        assertEquals(WidgetLayout.WIDE, widgetLayoutFor(360f, 230f))
    }

    @Test
    fun twoByTwoIsSquare() {
        assertEquals(WidgetLayout.SQUARE, widgetLayoutFor(110f, 100f))
    }
}
