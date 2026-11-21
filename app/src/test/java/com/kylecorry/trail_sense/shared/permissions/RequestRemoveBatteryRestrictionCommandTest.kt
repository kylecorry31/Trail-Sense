package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.luna.specifications.Specification
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.preferences.Flag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class RequestRemoveBatteryRestrictionCommandTest {

    private lateinit var command: RequestRemoveBatteryRestrictionCommand
    private lateinit var flag: Flag
    private lateinit var alerter: IAlerter
    private lateinit var isRequired: Specification<Context>

    @BeforeEach
    fun setup() {
        val fragment = mock<AndromedaFragment>()
        whenever(fragment.requireContext()).thenReturn(mock())
        flag = mock()
        alerter = mock()
        isRequired = mock()
        command = RequestRemoveBatteryRestrictionCommand(fragment, flag, alerter, isRequired)
    }


    @Test
    fun doesNotAlertWhenNotRequired() {
        whenever(isRequired.isSatisfiedBy(any())).thenReturn(false)

        command.execute()

        verify(alerter, never()).alert()
        verify(flag).set(false)
    }

    @Test
    fun alertsWhenRequired() {
        whenever(isRequired.isSatisfiedBy(any())).thenReturn(true)

        command.execute()

        verify(alerter).alert()
        verify(flag).set(true)
    }

    @Test
    fun doesNotAlertWhenFlagIsTrue() {
        whenever(isRequired.isSatisfiedBy(any())).thenReturn(true)

        whenever(flag.get()).thenReturn(true)

        command.execute()

        verify(alerter, never()).alert()
    }
}
