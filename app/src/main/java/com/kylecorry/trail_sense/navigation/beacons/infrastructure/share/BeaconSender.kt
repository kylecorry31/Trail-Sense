package com.kylecorry.trail_sense.navigation.beacons.infrastructure.share

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.sharing.ShareAction
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder

class BeaconSender(private val fragment: Fragment) : IBeaconSender {
    override fun send(beacon: Beacon) {
        Share.share(
            fragment, beacon.name, listOf(
                ShareAction.Copy,
                ShareAction.QR,
                ShareAction.Maps,
                ShareAction.Send
            )
        ) {
            when (it) {
                ShareAction.Copy -> {
                    val sender = BeaconCopy(fragment.requireContext())
                    sender.send(beacon)
                }
                ShareAction.QR -> {
                    CustomUiUtils.showQR(
                        fragment,
                        beacon.name,
                        BeaconQREncoder().encode(beacon)
                    )
                }
                ShareAction.Maps -> {
                    val sender = BeaconGeoSender(fragment.requireContext())
                    sender.send(beacon)
                }
                ShareAction.Send -> {
                    val sender = BeaconSharesheet(fragment.requireContext())
                    sender.send(beacon)
                }
                else -> {
                    // Do nothing
                }
            }
        }
    }
}