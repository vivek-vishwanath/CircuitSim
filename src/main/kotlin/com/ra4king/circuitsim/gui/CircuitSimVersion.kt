package com.ra4king.circuitsim.gui

import java.util.regex.Matcher
import java.util.regex.Pattern

class CircuitSimVersion(@JvmField val version: String) : Comparable<CircuitSimVersion?> {
    private val major: Int
    private val minor: Int
    private val bugfix: Int
    private val beta: Boolean

    init {

        val matcher: Matcher = VERSION_PATTERN.matcher(version)

        require(matcher.find()) { "Invalid version string: $version" }

        major = matcher.group(1).toInt()
        minor = matcher.group(2).toInt()
        bugfix = matcher.group(3).toInt()
        beta = !matcher.group(4).isEmpty()
    }

    override fun compareTo(other: CircuitSimVersion?) =
        other?.let {
            (major.compareTo(it.major) * 8 +
                minor.compareTo(it.minor) * 4 +
                bugfix.compareTo(it.bugfix) * 2 +
                it.beta.compareTo(beta)).compareTo(0)
        } ?: 0

    companion object {
        private val VERSION_PATTERN: Pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(b?)")

        @JvmField
        val VERSION = CircuitSimVersion("1.11.0-CE")
    }
}
