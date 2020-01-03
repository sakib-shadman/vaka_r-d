package com.vivasoft.vaka_device_manager.models;

import com.axema.vaka.client.api.events.VakaEvent;
import pico.types.DbEvent;

public class VakaEventSample  extends VakaEvent {
    public VakaEventSample(DbEvent event) {
        super(event);
    }




}
