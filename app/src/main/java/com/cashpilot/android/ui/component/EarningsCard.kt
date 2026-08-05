package com.cashpilot.android.ui.component

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashpilot.android.R
import com.cashpilot.android.model.Earnings
import com.cashpilot.android.ui.EarningsPresentation
import com.cashpilot.android.util.FormatUtils

/**
 * What the server says these apps earned.
 *
 * This card is the reason the earnings work existed. Every piece of it shipped
 * — the model, the parsing, the "keep the last value across an offline blip"
 * rule, the timestamp that makes staleness expressible — and no screen read any
 * of it, so the app the user opened still showed no earnings at all.
 *
 * The three rules below are the ones a display can quietly throw away, and each
 * of them is a lie if it goes:
 *
 *  1. **A missing figure is not zero.** `null` renders as "nothing read yet",
 *     never `$0.00`. A confident zero beside a service the user is running says
 *     the provider paid nothing, which is a claim nobody measured.
 *  2. **A kept figure is not a current one.** The last value survives an offline
 *     phone on purpose, so it must be labelled when it is old, or the user is
 *     told a stale number is live.
 *  3. **An account balance is not a device's earnings.** Providers report one
 *     balance per account, so a platform running on more than one machine
 *     cannot be attributed here — the card says so rather than implying it.
 */
@Composable
fun EarningsCard(
    earnings: Earnings?,
    asOfMillis: Long,
    nowMillis: Long,
    serverConfigured: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.earnings_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                earnings?.let {
                    Text(
                        stringResource(R.string.earnings_window, it.windowDays),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when (EarningsPresentation.mode(earnings, serverConfigured)) {
                // Standalone is a supported mode, so this is a normal state and
                // not a failure: there is no server to have asked.
                EarningsPresentation.Mode.NEEDS_SERVER -> Text(
                    stringResource(R.string.earnings_needs_server),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )

                // No heartbeat has ever carried figures, or every platform is
                // unread. Both are "not measured", which is not "earned zero".
                EarningsPresentation.Mode.NOTHING_READ -> {
                    Text(
                        stringResource(R.string.earnings_none_yet),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.earnings_none_yet_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }

                EarningsPresentation.Mode.FIGURE -> {
                    Text(
                        FormatUtils.formatPlatformEarnings(earnings?.totalUsd),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!earnings?.platformsWithoutReadings.isNullOrEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            pluralStringResource(
                                R.plurals.earnings_platforms_without_readings,
                                earnings?.platformsWithoutReadings?.size ?: 0,
                                earnings?.platformsWithoutReadings?.size ?: 0,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            // Staleness is reported whenever there is a figure on screen, which
            // includes a kept one from before the phone went offline.
            if (EarningsPresentation.isStale(earnings, serverConfigured, asOfMillis, nowMillis)) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.earnings_stale_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (asOfMillis > 0L) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(
                                R.string.earnings_stale,
                                DateUtils.getRelativeTimeSpanString(
                                    asOfMillis,
                                    nowMillis,
                                    DateUtils.MINUTE_IN_MILLIS,
                                ).toString(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}
