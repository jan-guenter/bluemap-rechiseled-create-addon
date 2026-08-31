/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.activation;

import io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523.FusionProgramCatalog;

/** Process-scoped state for the exact Rechiseled: Create bridge route. */
public final class RechiseledCreateRuntime {

    public static final String ROUTE_ID = "rechiseledcreate-1.1.1-atm-1.2.0";
    public static final RechiseledCreateRuntime INSTANCE = new RechiseledCreateRuntime();

    private final RouteActivation route = new RouteActivation(ROUTE_ID);
    private volatile FusionProgramCatalog catalog;

    private RechiseledCreateRuntime() {
    }

    public RouteActivation route() {
        return route;
    }

    public FusionProgramCatalog catalog() {
        return catalog;
    }

    public synchronized void activate(FusionProgramCatalog installedCatalog) {
        catalog = java.util.Objects.requireNonNull(installedCatalog, "installedCatalog");
        route.activate();
    }

    public synchronized void inactive(String detail) {
        catalog = null;
        route.inactive(detail);
    }

    public void disable(String detail) {
        catalog = null;
        route.fail(detail);
    }
}
