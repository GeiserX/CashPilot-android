package com.cashpilot.android.redesign

import org.junit.Assume

/** A phone-sized canvas: 411 x 891 dp, rendered at xhdpi (822 x 1782 px). */
const val PHONE = "w411dp-h891dp-xhdpi"

/** Images land in the repository's docs, next to the PR that shows them. */
const val OUT = "../docs/redesign"

/**
 * The design screenshots are documentation, not goldens: they are written on
 * request and never compared. Skipped unless CASHPILOT_RENDER_REDESIGN is set.
 */
fun renderGate() = Assume.assumeTrue(
    "design screenshots render only with CASHPILOT_RENDER_REDESIGN=1",
    System.getenv("CASHPILOT_RENDER_REDESIGN") != null,
)
