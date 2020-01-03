package com.vivasoft.vaka_device_manager.eventlistener;

import com.axema.vaka.client.api.connection.Connection;
import com.axema.vaka.client.api.events.VakaEvent;
import com.axema.vaka.client.api.events.VakaEventListener;
import com.axema.vaka.client.api.events.typeinterfaces.EventUserIsPerson;
import com.axema.vaka.client.api.events.typeinterfaces.EventUserIsUser;
import com.vivasoft.vaka_device_manager.models.EventData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class SampleEventListener extends VakaEventListener {

    //@Value("${url.production}")
    String STAGING_URL = "http://staging.instorage.se:8080/api/vaka/logs/store";
    // @Value("${url.staging}")
    String PRODUCTION_URL = "https://api.instorage.se/api/vaka/logs/store";
    String ip;
    String currentDateString;
    String isProduction;

    public SampleEventListener(String ip, String currentDateString, String isProduction) {

        // - Call base class constructor
        super();
        // - Register a “access granted” category filter
        this.ip = ip;
        this.currentDateString = currentDateString;
        this.isProduction = isProduction;
        this.getFilter().addCategory(VakaEvent.VakaEventTypeCategory.ACCESS_GRANTED);
        this.getFilter().addCategory(VakaEvent.VakaEventTypeCategory.ACCESS_DENIED);
        this.getFilter().addCategory(VakaEvent.VakaEventTypeCategory.ALARM);
        this.getFilter().addCategory(VakaEvent.VakaEventTypeCategory.DOOR);
        this.getFilter().addCategory(VakaEvent.VakaEventTypeCategory.DOOR_CONTROL);
        this.getFilter().addCategory(VakaEvent.VakaEventTypeCategory.SYSTEM);
    }


    @Override
    public void onEvent(Connection connection, VakaEvent vakaEvent) {

        log.info("date {}, event type {}, message {}, sequence id {}, source name {}, source address {}, operation {}"
                , new Date(vakaEvent.getTimeStamp()), vakaEvent.getEventType(), vakaEvent.getText(), vakaEvent.getSequenceId()
                , vakaEvent.getSourceName(), vakaEvent.getSourceAddress(), vakaEvent.getOperation()
        );
        postEventInfo(vakaEvent);
    }

    private void postEventInfo(VakaEvent vakaEvent) {

        try {
            Date currentDate = new SimpleDateFormat("yyyy-MM-dd").parse(currentDateString);
            Date eventDate = new Date(vakaEvent.getTimeStamp());
            String formattedEventDate = new SimpleDateFormat("yyyy-MM-dd").format(eventDate);
            log.info("current date {}, event date {}", currentDateString, formattedEventDate);

            if (formattedEventDate.equalsIgnoreCase(currentDateString)) {
                log.info("date {}, event type {}, message {}, sequence id {}, source name {}, source address {}, operation {}"
                        , new Date(vakaEvent.getTimeStamp()), vakaEvent.getEventType(), vakaEvent.getText(), vakaEvent.getSequenceId()
                        , vakaEvent.getSourceName(), vakaEvent.getSourceAddress(), vakaEvent.getOperation()
                );


                Integer person = 0;
                if (vakaEvent instanceof EventUserIsPerson) {
                    person = ((EventUserIsPerson) vakaEvent).getPerson();
                    log.info("person name {}", person);

                }

                Integer user = 0;
                if (vakaEvent instanceof EventUserIsUser) {
                    user = ((EventUserIsUser) vakaEvent).getUser();
                    log.info("user name {}", user);
                }
                EventData eventData = EventData
                        .builder()
                        .eventType(vakaEvent.getEventType().toString())
                        .message(vakaEvent.getText())
                        .operation(vakaEvent.getOperation())
                        .sequenceId(vakaEvent.getSequenceId())
                        .sourceAddress(vakaEvent.getSourceAddress())
                        .sourceName(vakaEvent.getSourceName())
                        .timeStamp(vakaEvent.getTimeStamp())
                        .url(ip)
                        .build();

                if(person != 0){
                    eventData.setPerson(String.valueOf(person));
                }
                if(user !=0){
                    eventData.setUser(String.valueOf(user));
                }
                String uri = "";
                if (isProduction.equalsIgnoreCase("1")) {
                    uri = PRODUCTION_URL;
                    log.info("production url {}", uri);
                } else if (isProduction.equalsIgnoreCase("0")) {
                    uri = STAGING_URL;
                    log.info("staging url {}", uri);
                }
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.postForObject(uri, eventData, EventData.class);
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

}
