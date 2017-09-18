package org.kantega.niagara.example.eventsources;

import java.util.UUID;

public class Update {
    public final UUID    txId;
    public final UUID    id;
    public final String  entity;
    public final String  property;
    public final String  value;
    public    final   boolean delete;

    public Update(UUID txId, UUID id, String entity, String property, String value, boolean delete) {
        this.txId = txId;
        this.id = id;
        this.entity = entity;
        this.property = property;
        this.value = value;
        this.delete = delete;
    }
}
