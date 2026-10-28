package ir.mmxkarimi.shockwave.data.db.query

/** Builds the argument for `LIKE ? ESCAPE '\'` so user input cannot act as a wildcard. */
class LikePattern private constructor() {
    companion object {
        /** SQL fragment to append after a column name. */
        const val CLAUSE = " LIKE ? ESCAPE '\\'"

        /** "%query%" with `\`, `%`, `_` escaped; null for a blank query. */
        @JvmStatic
        fun contains(query: String): String? {
            val q = query.trim()
            if (q.isEmpty()) return null
            return "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        }
    }
}
