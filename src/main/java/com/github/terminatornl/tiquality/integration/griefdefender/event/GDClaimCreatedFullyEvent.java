package com.github.terminatornl.tiquality.integration.griefdefender.event;

import com.griefdefender.api.claim.Claim;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Event that is fired AFTER a claim has been successfully created.
 * now we have access to its full data.
 */
public class GDClaimCreatedFullyEvent extends Event {

    private final Claim claim;

    public GDClaimCreatedFullyEvent(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return claim;
    }
}
