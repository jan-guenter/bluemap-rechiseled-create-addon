/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.activation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteActivationTest {

    @Test
    void failureIsStickyAndRouteStartsInactive() {
        RouteActivation route = new RouteActivation("test-route");
        assertFalse(route.isActive());
        route.activate();
        assertTrue(route.isActive());
        route.fail("schema-mismatch");
        route.activate();
        route.inactive("later-reload");
        assertEquals(RouteActivation.State.FAILED, route.snapshot().state());
        assertEquals("schema-mismatch", route.snapshot().detail());
    }
}
