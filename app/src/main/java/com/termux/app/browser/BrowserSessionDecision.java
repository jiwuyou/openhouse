package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserSessionDecision {

    @NonNull public final String requestedSessionMode;
    @NonNull public final String resolvedSessionMode;
    @Nullable public final String requestedSessionProfile;
    @Nullable public final String resolvedSessionProfile;
    @Nullable public final String requestedShareFrom;
    @Nullable public final String resolvedShareFrom;

    public BrowserSessionDecision(@NonNull String requestedSessionMode,
                                  @NonNull String resolvedSessionMode,
                                  @Nullable String requestedSessionProfile,
                                  @Nullable String resolvedSessionProfile,
                                  @Nullable String requestedShareFrom,
                                  @Nullable String resolvedShareFrom) {
        this.requestedSessionMode = requestedSessionMode;
        this.resolvedSessionMode = resolvedSessionMode;
        this.requestedSessionProfile = requestedSessionProfile;
        this.resolvedSessionProfile = resolvedSessionProfile;
        this.requestedShareFrom = requestedShareFrom;
        this.resolvedShareFrom = resolvedShareFrom;
    }

}
