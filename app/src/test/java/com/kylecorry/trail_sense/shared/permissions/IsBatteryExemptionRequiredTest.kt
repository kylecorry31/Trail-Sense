package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class IsBatteryExemptionRequiredTest {

    private lateinit var exemptionRequired: IsBatteryExemptionRequired
    private lateinit var isRestricted: Specification<Context>
    private lateinit var isRequired: Specification<Context>

    @BeforeEach
    fun setUp() {
        isRestricted = mock()
        isRequired = mock()
        exemptionRequired = IsBatteryExemptionRequired(isRestricted, isRequired)
    }

    @Test
    fun exemptionIsNotRequiredWhenNoRestrictions() {
        // Not restricted
        whenever(isRestricted.isSatisfiedBy(any())).thenReturn(false)

        // Required
        whenever(isRequired.isSatisfiedBy(any())).thenReturn(true)

        // Verify exemption is not required
        assert(!exemptionRequired.isSatisfiedBy(mock()))
    }

    @Test
    fun exemptionIsRequiredWhenRestrictions(){
        // Restricted
        whenever(isRestricted.isSatisfiedBy(any())).thenReturn(true)

        // Required
        whenever(isRequired.isSatisfiedBy(any())).thenReturn(true)

        // Verify exemption is required
        assert(exemptionRequired.isSatisfiedBy(mock()))
    }

    @Test
    fun exemptionIsNotRequiredWhenRestrictionsButNotRequired(){
        // Restricted
        whenever(isRestricted.isSatisfiedBy(any())).thenReturn(true)

        // Not required
        whenever(isRequired.isSatisfiedBy(any())).thenReturn(false)

        // Verify exemption is not required
        assert(!exemptionRequired.isSatisfiedBy(mock()))
    }
}