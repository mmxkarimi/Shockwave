package ir.mmxkarimi.shockwave.data.settings

/** Whether the cheaper "battery saver" visuals are forced: AUTO follows the system, ON / OFF override it. */
enum class BatterySaverMode {
    AUTO, ON, OFF;

    /** Whether cheaper visuals / no background prefetch apply, given the system's battery-saver state. */
    fun active(systemPowerSave: Boolean): Boolean = if (this == AUTO) systemPowerSave else this == ON
}
