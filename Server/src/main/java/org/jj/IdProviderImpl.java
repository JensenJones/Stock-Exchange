package org.jj;

import java.util.UUID;

public class IdProviderImpl implements IdProvider {

    @Override
    public UUID getUUID() {
        return UUID.randomUUID();
    }
}
