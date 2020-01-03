package com.vivasoft.vaka_device_manager.controllers;

import com.vivasoft.vaka_device_manager.models.ResponseTemplate;
import com.vivasoft.vaka_device_manager.readers.FileReader;
import com.vivasoft.vaka_device_manager.vaka_services.VakaAccessControl;
import com.vivasoft.vaka_device_manager.vaka_services.VakaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Md. Shamim
 * Date: ১৯/১১/১৯
 * Time: ১২:১৩ PM
 * Email: mdshamim723@gmail.com
 **/

@RestController
@CrossOrigin
@Slf4j
public class LogController {
    @Autowired
    private  FileReader fileReader;

    @Autowired
    private ApplicationContext applicationContext;

    private ConcurrentHashMap<String, VakaService> vakaServices = new ConcurrentHashMap<String, VakaService>();


    @GetMapping(value = "/logs")
    public ResponseEntity<?> getLogs() {

        String err;

        try {
            List<String> logs = fileReader.readLogFile();
            return ResponseEntity.ok(logs);
        } catch (IOException e) {
            err = "Error: " + e.getMessage();
        }

        return ResponseEntity.badRequest().body(new ResponseTemplate<>(
                false,
                null,
                err
        ));
    }


    @GetMapping(value = "/events")
    public  ResponseEntity<?> getEventData(@RequestParam("uid") String uid,
                                           @RequestParam("pwd") String pwd,
                                           @RequestParam(value = "sequenceId", required = false) Long sequenceId,
                                           @RequestParam("url") String url,
                                           @RequestParam("date") String date,
                                           @RequestParam("isProduction") String isProduction) throws Exception {

        VakaAccessControl vakaAccessControl = null;


        String[] segments = getAllSegmentsFromURL(url);

        log.info("connecting to vaka");
        String port = segments[1];
        String ip = segments[0];
        String errMessage = "";

//        if(vakaServices.containsKey(ip)) {
//            errMessage = String.format("There is already connection existing for ip %s", ip);
//            return ResponseEntity.badRequest().body(new ResponseTemplate<>(
//                    false,
//                    new ArrayList(),
//                    errMessage
//            ));
//        }

        VakaService vakaService = applicationContext.getBean(VakaService.class);
//        vakaServices.put(ip, vakaService);
        if(sequenceId == null ) {
            sequenceId = 0L;
        }
        vakaService.fetchVakaEvents(uid, pwd, port, ip, sequenceId,date,isProduction);
//        vakaServices.clear();

        return ResponseEntity.badRequest().body(new ResponseTemplate<>(
                false,
                new ArrayList(),
                errMessage
        ));

    }

    private Long getSequenceIdForIp(String ip) {
        return Long.valueOf(39595);
    }

    /* Segment Exception Handle */
    private String[] getAllSegmentsFromURL(String url) throws Exception {
        String[] segments = url.split(":");
        if (segments.length < 2) {
            log.info("Invalid Url Segment");
            throw new Exception("Invalid Url Segment. URL should contain the port number as well.");
        }

        return segments;
    }

    @PreDestroy
    private void closeAllVakaConnections() {
        for (VakaService service :
                vakaServices.values()) {
            service.destroyConnection();
        }
    }

}
