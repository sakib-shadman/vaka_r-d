package com.vivasoft.vaka_device_manager.vaka_services;

import com.axema.vaka.client.api.connection.Connection;
import com.axema.vaka.client.api.connection.CouldNotConnectException;
import com.axema.vaka.client.api.connection.VakaConnectionCreator;
import com.axema.vaka.client.api.connection.session.Session;
import com.axema.vaka.client.api.services.AccessControlService;
import com.axema.vaka.client.api.services.EventService;
import com.axema.vaka.client.api.services.Service;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Md. Shamim
 * Date: ৭/১১/১৯
 * Time: ৪:৪৬ PM
 * Email: mdshamim723@gmail.com
 **/

@Slf4j
@Data
public class VakaAccessControl {

    private Connection connection;
    private Session session;
    private String uid;
    private String pwd;
    private String ip;



    public VakaAccessControl(String ip, String port, String uid, String pwd) throws CouldNotConnectException {
        this.uid = uid;
        this.pwd = pwd;
        this.ip= ip;
        connection = VakaConnectionCreator.createVakaConnection(ip, Integer.parseInt(port));

        log.info("Opening connection to vaka...");
        connection.openTry(1);
        log.info("Connection to Vaka Opened.");
    }

    private void createVakaSession() throws IOException {
        log.info("Getting connection session...");
        session = connection.getSession(uid, pwd);
        log.info("Session retrieved.");
    }

    public Session getVakaSession() {
        return session;
    }

    public AccessControlService getAccessControlService() throws IOException {
        createVakaSession();
        return (AccessControlService) session.getService(Service.AvailableServices.ACCESS_CONTROL);
    }

    public EventService getEventService() throws IOException {
        createVakaSession();
        return (EventService) session.getService(Service.AvailableServices.EVENT);
    }

    private void sessionLogout() {
        log.info("Logging out...");
        if (session != null) session.logout();
    }

    public void closeConnection() {
        log.info("Closing connection...");
        if (connection != null) connection.close();
        log.info("Connection closed.");
    }

}
