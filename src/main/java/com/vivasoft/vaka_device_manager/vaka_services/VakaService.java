package com.vivasoft.vaka_device_manager.vaka_services;

import com.axema.vaka.client.api.connection.CouldNotConnectException;
import com.axema.vaka.client.api.data.InsufficientPermissionsException;
import com.axema.vaka.client.api.exceptions.LoginFailedException;
import com.axema.vaka.client.api.global.LoggingConstants;
import com.axema.vaka.client.api.services.EventService;
import com.axema.vaka.client.backend.logger.VakaLogger;
import com.vivasoft.vaka_device_manager.eventlistener.SampleEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;
import pico.db.DBException;
import pico.io.DateTypeConverter;
import pico.types.DateTime;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class VakaService {

    private VakaAccessControl vakaAccessControl;
    private String ip;
    private long connectionCloseDelay;
    private boolean showVakaVerboseLog;

    public VakaService(long connectionCloseDelay, boolean showVakaVerboseLog) {
        this.connectionCloseDelay = connectionCloseDelay;
        this.showVakaVerboseLog= showVakaVerboseLog;
    }

    public void fetchVakaEvents(@RequestParam("uid") String uid, @RequestParam("pwd") String pwd, String port, String ip, long sequenceId, String date, String isProduction) {
        String errMessage = "";
        try {
            this.ip = ip;

            log.info("Create event action: segments found ip: {}, port: {}", ip, port);

            vakaAccessControl = new VakaAccessControl(ip, port, uid, pwd);
            if(showVakaVerboseLog) {
                LoggingConstants.Logging.EventService =  LoggingConstants.LogLevel.ALL;
            }
            VakaLogger.setOutputStream(System.out);
            listenForEvents(sequenceId,date,isProduction);

        } catch (CouldNotConnectException e) {
            errMessage = "Unable to Connect: " + e.getMessage();
            log.error(errMessage);
        }   catch (InsufficientPermissionsException e) {
            errMessage = "Permission Denied: " + e.getMessage();
            log.error(errMessage);
        } catch (Exception e) {
            errMessage = "Unknown Error: " + e.getMessage();
            log.error(errMessage);
        }

    }

    private void listenForEvents(long sequenceId, String dateFromApi, String isProduction) {
        if(vakaAccessControl == null) {
            log.error("there is no connection for ip {}", ip);
            return;
        }
        try {
            EventService eventService = vakaAccessControl.getEventService();
            SampleEventListener listener = new SampleEventListener(ip,dateFromApi,isProduction);
            // - Enable event stream
            eventService.enableEventStream();

            log.info("Started listening for ip {}", vakaAccessControl.getIp());

            Thread.sleep(3000);

            if(sequenceId == 0){
                String[] dateSegments = getDateSegmentsFromDate(dateFromApi);
                int year = Integer.parseInt(dateSegments[0]);
                int month = Integer.parseInt(dateSegments[1]);
                int day = Integer.parseInt(dateSegments[2]);
                DateTime date = new DateTime(year, month, day);
                eventService.requestEventsBlocking(listener, EventService.SEQUENCE_ID_NULL, DateTypeConverter.DateTimeToSystemTime(date));
            } else {
                DateTime date = new DateTime();
                eventService.requestEventsBlocking(listener,sequenceId, DateTypeConverter.DateTimeToSystemTime(date));
            }

            destroyConnection();
            //scheduleClose(connectionCloseDelay);
        } catch (IOException e) {
            log.error("IO exception", e);
        } catch (DBException e) {
            log.error("DB exception", e);
        } catch (Exception e) {
            log.error("Exception ", e);
        }
    }

    private void scheduleClose(long connectionCloseDelay) {
        log.info("scheduling close connection after {} ms for ip {}", connectionCloseDelay, ip);
        TimerTask task = new TimerTask() {
            public void run() {
                log.info("closing connection for ip {}", ip);
                destroyConnection();
            }
        };
        Timer timer = new Timer("Timer");

        timer.schedule(task, connectionCloseDelay);
    }

    @PreDestroy
    public void destroyConnection() {
        if(vakaAccessControl != null) {
            log.info("closing connection for ip {}", vakaAccessControl.getIp());
            vakaAccessControl.closeConnection();
            vakaAccessControl = null;
        }
    }

    private String[] getDateSegmentsFromDate(String date) throws Exception {
        String[] segments = date.split("-");
        return segments;
    }
}
