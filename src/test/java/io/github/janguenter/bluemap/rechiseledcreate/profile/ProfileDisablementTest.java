/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileDisablementTest {

    @Test
    void mergesNormalizedPropertyAndEnvironmentLists() {
        assertEquals("bluemap.rechiseledcreate.disabledProfiles",
                ProfileDisablement.SYSTEM_PROPERTY);
        assertEquals("BLUEMAP_RECHISELEDCREATE_DISABLED_PROFILES",
                ProfileDisablement.ENVIRONMENT_VARIABLE);
        ProfileDisablement disabled = ProfileDisablement.from(
                "rechiseledcreate-1.1.1-atm-1.2.0, invalid value",
                "RECHISELEDCREATE-1.1.1-ATM-1.2.0,other"
        );
        assertTrue(disabled.isDisabled(RechiseledCreate111Fusion1312Profile.PROFILE_ID));
        assertEquals(2, disabled.disabledProfiles().size());
    }
}
