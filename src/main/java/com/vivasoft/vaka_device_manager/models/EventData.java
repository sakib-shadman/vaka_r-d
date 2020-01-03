package com.vivasoft.vaka_device_manager.models;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class EventData {

    private long timeStamp;
    private String eventType;
    private String message;
    private long sequenceId;
    private String sourceName;
    private Integer sourceAddress;
    private Integer operation;
    private String url;
    private String user;
    private String person;
}
