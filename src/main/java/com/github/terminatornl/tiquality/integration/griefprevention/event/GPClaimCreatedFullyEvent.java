package com.github.terminatornl.tiquality.integration.griefprevention.event;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Event that is fired AFTER a claim has been successfully created.
 * now we have access to its full data.
 */
public class GPClaimCreatedFullyEvent extends Event {

    private final Claim claim;

    public GPClaimCreatedFullyEvent(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return claim;
    }
}
