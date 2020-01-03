package com.vivasoft.vaka_device_manager.controllers;

import com.axema.vaka.client.api.connection.CouldNotConnectException;
import com.axema.vaka.client.api.data.*;
import com.axema.vaka.client.api.exceptions.LoginFailedException;
import com.axema.vaka.client.api.services.AccessControlService;
import com.vivasoft.vaka_device_manager.models.PersonResponse;
import com.vivasoft.vaka_device_manager.models.ResponseTemplate;
import com.vivasoft.vaka_device_manager.vaka_services.VakaAccessControl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pico.db.DBException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/account"})
@Slf4j
public class AccountController {

    @CrossOrigin
    @GetMapping(value = {"/check"}, produces = {"application/json"})
    public ResponseEntity<?> checkConnectivity(@RequestParam("uid") String uid,
                                         @RequestParam("pwd") String pwd,
                                         @RequestParam("url") String url) {

        VakaAccessControl vakaAccessControl = null;
        String errMessage;

        try {
            log.info("Called all Person fetch method with params: " + "|" + url + "|" + uid + "|" + pwd);

            String[] segments = getAllSegmentsFromURL(url);

            log.info("connecting to vaka");
            log.info("Check Connection action: segments found ip: {}, port: {}", segments[0], segments[1]);
            vakaAccessControl = new VakaAccessControl(segments[0], segments[1], uid, pwd);

            return ResponseEntity.ok(new ResponseTemplate<>(
                    true,
                    null,
                    "Successfully connected."
            ));

        } catch (CouldNotConnectException e) {
            errMessage = "Unable to Connect: " + e.getMessage();
        } catch (Exception e) {
            errMessage = "Error: " + e.getMessage();
        } finally {
            if (vakaAccessControl != null) vakaAccessControl.closeConnection();
        }

        log.error(errMessage);
        return ResponseEntity.badRequest().body(new ResponseTemplate<>(
                false,
                null,
                errMessage
        ));
    }

    @CrossOrigin
    @GetMapping(value = {"/people"}, produces = {"application/json"})
    public ResponseEntity<?> getUserList(@RequestParam("uid") String uid,
                                         @RequestParam("pwd") String pwd,
                                         @RequestParam("url") String url) {

        VakaAccessControl vakaAccessControl = null;
        String errMessage;

        try {
            log.info("Called all Person fetch method with params: " + "|" + url + "|" + uid + "|" + pwd);

            String[] segments = getAllSegmentsFromURL(url);

            log.info("connecting to vaka");
            log.info("Create people action: segments found ip: {}, port: {}", segments[0], segments[1]);
            vakaAccessControl = new VakaAccessControl(segments[0], segments[1], uid, pwd);

            log.info("Getting Access Control Object...");
            AccessControlService accessControlService = vakaAccessControl.getAccessControlService();
            log.info("Access Control Retrieved.");

            log.info("Fetching All People.");
            List<Person> people = accessControlService.getPersonList();

            List<PersonResponse> _people = people.stream()
                    .map(p -> new PersonResponse(
                            p.getName(), p.getMainSecurityToken().getKey(), p.getValidityStartTime(), p.getValidityStopTime()
                    )).collect(Collectors.toList());

            log.info("Successfully Fetched All People.");

            return ResponseEntity.ok(_people);

        } catch (CouldNotConnectException e) {
            errMessage = "Unable to Connect: " + e.getMessage();
        } catch (LoginFailedException e) {
            errMessage = "Login Failed: " + e.getMessage();
        } catch (UnsupportedEncodingException e) {
            errMessage = "Unsupported Encoding: " + e.getMessage();
        } catch (IOException e) {
            errMessage = "Connection Error: " + e.getMessage();
        } catch (DBException e) {
            errMessage = "Vaka Database Error: " + e.getMessage();
        } catch (InsufficientPermissionsException e) {
            errMessage = "Permission Denied: " + e.getMessage();
        } catch (Exception e) {
            errMessage = "Unknown Error: " + e.getMessage();
        } finally {
            if (vakaAccessControl != null) vakaAccessControl.closeConnection();
        }

        log.error(errMessage);
        return ResponseEntity.badRequest().body(new ResponseTemplate<>(
                false,
                new ArrayList(),
                errMessage
        ));
    }

    @CrossOrigin
    @PostMapping(value = {"/create"}, produces = {"application/json"})
    public ResponseEntity<?> setAccountAccess(@RequestParam("accountName") String accountName,
                                              @RequestParam("accessCode") String accessCode,
                                              @RequestParam("url") String url,
                                              @RequestParam("uid") String uid,
                                              @RequestParam("pwd") String pwd,
                                              @RequestParam(value = "startDate", defaultValue = "") String startDate,
                                              @RequestParam(value = "accessGroupName", defaultValue = "Kund") String accessGroupName) {

        VakaAccessControl vakaAccessControl = null;
        String errMessage;
        Person person;

        try {
            log.info("Called create method with params: " + accountName + "|" + accessCode + "|" + url + "|" + uid + "|" + pwd);

            String[] segments = getAllSegmentsFromURL(url);

            log.info("connecting to vaka");
            log.info("Create people action: segments found ip: {}, port: {}", segments[0], segments[1]);
            vakaAccessControl = new VakaAccessControl(segments[0], segments[1], uid, pwd);

            log.info("Getting Access Control Object...");
            AccessControlService accessControlService = vakaAccessControl.getAccessControlService();
            log.info("Access Control Retrieved.");

            log.info("Trying to create Person in Vaka...");
            person = accessControlService.createPerson();
            log.info("Person Created.");

            person.setName(accountName);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            if (!startDate.isEmpty()) {
                Date parsedDate = dateFormat.parse(startDate);
                Timestamp timestamp = new Timestamp(parsedDate.getTime());
                person.setValidityStartTime(timestamp.getTime());
            }

            List<OrganisationGroup> organisationGroups = accessControlService.getOrganisationGroupList();

            for (OrganisationGroup organisationGroup : organisationGroups) {
                if (organisationGroup.getName().equalsIgnoreCase(accessGroupName)) {
                    person.setOrganisationGroup(organisationGroup.getId());
                    break;
                }
            }

            AccessGroup group = accessControlService.getAccessGroup("Full behörighet");
            if ((group != null) && (!person.getAccessEntries().contains(group))) {
                person.addAccessEntry(group);
            }

            log.info("Creating access token Tag: " + accessCode);
            SecurityToken personTag = accessControlService.createSecurityToken(SecurityToken.TokenType.PersonalCode, accessCode);
            log.info("Tag created.");

            log.info("Attaching tag to person");
            person.setMainSecurityToken(personTag);

            log.info("Adding person to Access Control...");
            accessControlService.addPerson(person);
            log.info("Person added.");

            log.info("Operation Completed.");

            return ResponseEntity.ok(new ResponseTemplate<>(
                    true,
                    new PersonResponse(person.getName(), accessCode, person.getValidityStartTime(), person.getValidityStopTime()),
                    "Successfully created person"
            ));

        } catch (CouldNotConnectException e) {
            errMessage = "Unable to Connect: " + e.getMessage();
        } catch (LoginFailedException e) {
            errMessage = "Login Failed: " + e.getMessage();
        } catch (UnsupportedEncodingException e) {
            errMessage = "Unsupported Encoding: " + e.getMessage();
        } catch (IOException e) {
            errMessage = "Connection Error: " + e.getMessage();
        } catch (DBException e) {
            errMessage = "Vaka Database Error: " + e.getMessage();
        } catch (InsufficientPermissionsException e) {
            errMessage = "Permission Denied: " + e.getMessage();
        } catch (Exception e) {
            errMessage = "Unknown Error: " + e.getMessage();
        } finally {
            if (vakaAccessControl != null) vakaAccessControl.closeConnection();
        }

        log.error(errMessage);
        return ResponseEntity.badRequest().body(new ResponseTemplate<>(
                false,
                null,
                errMessage
        ));
    }

    @CrossOrigin
    @PostMapping(value = "/update", produces = {"application/json"})
    public ResponseEntity<?> updateAccountAccess(@RequestParam("accountName") String accountName,
                                                 @RequestParam("accessCode") String accessCode,
                                                 @RequestParam("url") String url,
                                                 @RequestParam("uid") String uid, @RequestParam("pwd") String pwd,
                                                 @RequestParam(value = "startDate", defaultValue = "") String startDate,
                                                 @RequestParam(value = "endDate", defaultValue = "") String endDate,
                                                 @RequestParam(value = "accessGroupName", defaultValue = "Kund") String accessGroupName) {

        VakaAccessControl vakaAccessControl = null;
        Person person;
        String errMessage;

        try {
            log.info("Called updated method with params: " + accountName + "|" + accessCode + "|" + url + "|" + uid + "|" + pwd);

            String[] segments = getAllSegmentsFromURL(url);

            log.info("connecting to vaka");
            log.info("Create people action: segments found ip: {}, port: {}", segments[0], segments[1]);
            vakaAccessControl = new VakaAccessControl(segments[0], segments[1], uid, pwd);

            log.info("Getting Access Control Object...");
            AccessControlService accessControlService = vakaAccessControl.getAccessControlService();
            log.info("Access Control Retrieved.");

            log.info("Trying to fetch person.");
            person = accessControlService.getPerson(accountName);

            if (person == null) {
                log.info("Trying to find person.");
                List<Person> peoples = accessControlService.getPersonList();
                for (Person people : peoples) {
                    if (people.getName().equals(accountName)) {
                        person = people;
                        break;
                    }
                }
            }

            if (person == null) {
                log.info("Person not found by name " + accountName);
                throw new Exception("Sorry, but there is no one that goes by the name " + accountName + " here.");
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            if (!startDate.isEmpty()) {
                log.info("Starting Date Found!");
                Date parsedDate = dateFormat.parse(startDate);
                Timestamp timestamp = new Timestamp(parsedDate.getTime());
                person.setValidityStartTime(timestamp.getTime());
            }
            if (!endDate.isEmpty()) {
                log.info("End Date Found!");
                Date parsedDate = dateFormat.parse(endDate);
                Timestamp timestamp = new Timestamp(parsedDate.getTime());
                person.setValidityStopTime(timestamp.getTime());
            }
            if (!accessCode.isEmpty()) {
                log.info("Access Code Found!");
                SecurityToken personTag = accessControlService.createSecurityToken(SecurityToken.TokenType.PersonalCode,
                        accessCode);
                person.setMainSecurityToken(personTag);

                List<OrganisationGroup> organisationGroups = accessControlService.getOrganisationGroupList();
                for (OrganisationGroup organisationGroup : organisationGroups) {
                    if (organisationGroup.getName().equalsIgnoreCase(accessGroupName)) {
                        person.setOrganisationGroup(organisationGroup.getId());
                        break;
                    }
                }
                AccessGroup group = accessControlService.getAccessGroup("Full behörighet");
                if ((group != null) && (!person.getAccessEntries().contains(group))) {
                    log.info("Group Found!");
                    person.addAccessEntry(group);
                }
            }
            accessControlService.updatePerson(person);
            log.info("Operation Completed.");

            return ResponseEntity.ok(new ResponseTemplate<>(
                    true,
                    new PersonResponse(person.getName(), accessCode, person.getValidityStartTime(), person.getValidityStopTime()),
                    "Successfully update person"
            ));

        } catch (CouldNotConnectException e) {
            errMessage = "Unable to Connect: " + e.getMessage();
        } catch (LoginFailedException e) {
            errMessage = "Login Failed: " + e.getMessage();
        } catch (UnsupportedEncodingException e) {
            errMessage = "Unsupported Encoding: " + e.getMessage();
        } catch (IOException e) {
            errMessage = "Connection Error: " + e.getMessage();
        } catch (DBException e) {
            errMessage = "Vaka Database Error: " + e.getMessage();
        } catch (InsufficientPermissionsException e) {
            errMessage = "Permission Denied: " + e.getMessage();
        } catch (Exception e) {
            errMessage = "Unknown Error: " + e.getMessage();
        } finally {
            if (vakaAccessControl != null) vakaAccessControl.closeConnection();
        }

        log.error(errMessage);
        return ResponseEntity.badRequest().body(new ResponseTemplate<>(
                false,
                null,
                errMessage
        ));
    }

    @CrossOrigin
    @DeleteMapping(value = "/delete/{accountName}", produces = {"application/json"})
    public ResponseEntity<?> deletePeople(@PathVariable("accountName") String accountName,
                                          @RequestParam("url") String url,
                                          @RequestParam("uid") String uid,
                                          @RequestParam("pwd") String pwd) {

        VakaAccessControl vakaAccessControl = null;
        Person person;
        String errMessage;

        try {
            log.info("Called delete method with params: " + accountName + "|" + "|" + url + "|" + uid + "|" + pwd);

            String[] segments = getAllSegmentsFromURL(url);

            log.info("connecting to vaka");
            log.info("Create people action: segments found ip: {}, port: {}", segments[0], segments[1]);
            vakaAccessControl = new VakaAccessControl(segments[0], segments[1], uid, pwd);

            log.info("Getting Access Control Object...");
            AccessControlService accessControlService = vakaAccessControl.getAccessControlService();
            log.info("Access Control Retrieved.");

            log.info("Trying to fetch person.");
            person = accessControlService.getPerson(accountName);

            if (person == null) {
                log.info("Trying to find person.");
                List<Person> peoples = accessControlService.getPersonList();

                for (Person people : peoples) {
                    if (people.getName().equals(accountName)) {
                        person = people;
                        break;
                    }
                }
            }

            if (person == null) {
                log.info("Person not found by name " + accountName);
                throw new Exception("Sorry, but there is no one that goes by the name " + accountName + " here.");
            }

            log.info("Trying to remove person");
            accessControlService.removePerson(person);

            log.info("Operation Completed.");

            return ResponseEntity.ok(new ResponseTemplate<Map>(
                    true,
                    new HashMap() {{
                        put("name", accountName);
                    }},
                    "Successfully remove person"
            ));

        } catch (CouldNotConnectException e) {
            errMessage = "Unable to Connect: " + e.getMessage();
        } catch (LoginFailedException e) {
            errMessage = "Login Failed: " + e.getMessage();
        } catch (UnsupportedEncodingException e) {
            errMessage = "Unsupported Encoding: " + e.getMessage();
        } catch (IOException e) {
            errMessage = "Connection Error: " + e.getMessage();
        } catch (DBException e) {
            errMessage = "Vaka Database Error: " + e.getMessage();
        } catch (InsufficientPermissionsException e) {
            errMessage = "Permission Denied: " + e.getMessage();
        } catch (Exception e) {
            errMessage = "Unknown Error: " + e.getMessage();
        } finally {
            if (vakaAccessControl != null) vakaAccessControl.closeConnection();
        }

        log.error(errMessage);
        return ResponseEntity.badRequest().body(new ResponseTemplate<>(
                false,
                null,
                errMessage
        ));
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


}
