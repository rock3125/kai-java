/*
 * Copyright (c) 2016 by Peter de Vocht
 *
 * All rights reserved. No part of this publication may be reproduced, distributed, or
 * transmitted in any form or by any means, including photocopying, recording, or other
 * electronic or mechanical methods, without the prior written permission of the publisher,
 * except in the case of brief quotations embodied in critical reviews and certain other
 * noncommercial uses permitted by copyright law.
 *
 */

package industries.vocht.viki.services;

import com.hazelcast.core.IMap;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.group.Group;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.model.user.UserEmailList;
import industries.vocht.viki.model.user.UserList;
import industries.vocht.viki.model.user.UserWithExtras;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by peter on 4/03/16.
 *
 */
@Component
public class UserService {

    private final static Logger logger = LoggerFactory.getLogger(UserService.class);

    // the system user's email is "organisation_id" + SYSTEM_USER_EMAIL_POSTFIX
    public static final String SYSTEM_USER_EMAIL_POSTFIX = "_system@vocht.industries";
    public static final String SYSTEM_USER_FIRST_NAME = "Viki";
    public static final String SYSTEM_USER_SURNAME = "de Vocht";
    public static final String SYSTEM_IP_ADDRESS = "::1";

    @Value("${system.min.password.length:8}")
    private int MIN_PASSWORD_LENGTH; // password minimum length restriction

    @Autowired
    private IDao dao; // dao access

    @Autowired
    private IHazelcast hazelcast;

    public UserService() {
    }

    /**
     * login an existing user
     * @param email the user's email
     * @param password the user's password
     * @param ipAddress the ip address of the request
     * @return a sessionID for the user
     */
    public UUID login(String email, String password, String ipAddress) throws ApplicationException {
        if ( email == null || password == null || password.length() < MIN_PASSWORD_LENGTH || ipAddress == null ) {
            throw new ApplicationException("login: invalid parameter");
        }
        UUID session_id = dao.getUserDao().login(email, password, ipAddress);
        setupUserAccess(session_id, ipAddress);
        return session_id;
    }

    /**
     * setup security access for the user and its session
     * go through all the existing ACLs for the system and see for each if the user
     * has access - store this access in hazelcast for security checking
     * @param session_id the session's id
     * @param ipAddress the ip address for security purposes
     * @throws ApplicationException
     */
    private void setupUserAccess( UUID session_id, String ipAddress ) throws ApplicationException {
        if ( session_id != null ) {

            // setup user security session
            User user = dao.getUserDao().getUserForSession(session_id, ipAddress);

            // setup groups for this user
            List<String> userGroupAccessList = new ArrayList<>();
            List<Group> groupList = dao.getGroupDao().readAllGroups(user.getOrganisation_id());
            if ( groupList != null ) {
                userGroupAccessList.addAll(groupList.stream().filter(group -> group.getUser_list() != null && group.getUser_list().contains(user.getEmail())).map(group -> group.getName() + ":true").collect(Collectors.toList()));
            }

            // intersect the two sets to come up with the user's access set
            IMap<Integer, List<String>> aclSet = hazelcast.getHashAclMap(user.getOrganisation_id());
            if ( aclSet != null ) {
                HashSet<Integer> userAccessSet = new HashSet<>();
                String accessKey = user.getEmail() + ":true";
                for ( Integer key : aclSet.keySet() ) {
                    // does the user have access to this set?
                    List<String> accessList = aclSet.get(key);
                    if ( accessList.contains(accessKey) ) {
                        userAccessSet.add(key);
                    }
                    userAccessSet.addAll(userGroupAccessList.stream().filter(accessList::contains).map(userKey -> key).collect(Collectors.toList()));
                }

                // even if empty - we need an accees set for this user
                hazelcast.getUserAclMap(user.getOrganisation_id()).put(user.getEmail(), userAccessSet);
            }
        }
    }

    /**
     * get all active sessions and upate the security objects for each of these sessions
     * @throws ApplicationException
     */
    public void updateActiveSessionSecurity( UUID organisation_id ) throws ApplicationException {
        if ( organisation_id != null ) {
            List<Session> sessionList = dao.getUserDao().getActiveSessions(organisation_id);
            if (sessionList != null) {
                for (Session session : sessionList) {
                    setupUserAccess( session.getId(), null );
                }
            }
        }
   }

    /**
     * login a special system user
     * @param email the email address of the system user
     * @param ipAddress a fake ip address only possible for the system user
     * @return the sessionID of the system user
     * @throws ApplicationException
     */
    public UUID loginSystemUser(String email, String ipAddress) throws ApplicationException {
        if ( email == null || ipAddress == null ) {
            throw new ApplicationException("loginSystemUser: invalid parameter");
        }
        return dao.getUserDao().loginSystemUser(email, ipAddress);
    }

    /**
     * logout an existing user
     * @param sessionID the user's session
     */
    public void logout(UUID organisation_id, UUID sessionID) throws ApplicationException {
        if ( sessionID == null ) {
            throw new ApplicationException("logout: invalid session");
        }
        dao.getUserDao().logout(organisation_id, sessionID);
    }

    /**
     * get an existing user object for a given session
     * @param sessionID the session id
     * @param ipAddress the ip address of the request
     * @return the user object if session is valid
     * @throws ApplicationException
     */
    public User getUser(UUID sessionID, String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null ) {
            throw new ApplicationException("getUser: invalid parameter");
        }
        User user = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( user != null ) {
            user.setSalt(null); // never return to ui
            user.setPassword_sha256(null);
        }
        return user;
    }

    /**
     * get an existing user object for a given session
     * @param email the email address of the user
     * @param ipAddress the ip address of the request
     * @return the user object if session is valid
     * @throws ApplicationException
     */
    public User getUserByEmail(String email, String ipAddress) throws ApplicationException {
        if ( email == null || ipAddress == null ) {
            throw new ApplicationException("getUserByEmail: invalid parameter");
        }
        User user = dao.getUserDao().getUserByEmail(email, ipAddress);
        if ( user != null ) {
            user.setSalt(null); // never return to ui
            user.setPassword_sha256(null);
        }
        return user;
    }

    /**
     * create a new user
     * @param sessionID the session ID
     * @param user the user object to create
     * @param userPassword the password to use for this new user
     * @param ipAddress the security ip address
     * @return the new user object
     * @throws ApplicationException
     */
    public User createUser(UUID sessionID, User user, String userPassword, String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || user == null || user.getEmail() == null || user.getOrganisation_id() == null ) {
            throw new ApplicationException("createUser: invalid parameter");
        }
        if ( userPassword == null || userPassword.length() < MIN_PASSWORD_LENGTH ) {
            throw new ApplicationException("createUser: invalid password");
        }
        User sessionUser = getUser(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("createUser: invalid session");
        }
        User existingUser = dao.getUserDao().read(user.getEmail());
        if ( existingUser != null ) {
            throw new ApplicationException("createUser: user already exists");
        }
        // never allow the create system to change the "activated" flag
        user.setConfirmed(false);
        try {
            User newUser = dao.getUserDao().create(sessionUser.getOrganisation_id(), user, userPassword);
            if ( newUser != null ) {
                newUser.setSalt(null);
                newUser.setPassword_sha256(null);
            }
            return newUser;
        } catch ( NoSuchAlgorithmException | UnsupportedEncodingException ex ){
            throw new ApplicationException("createUser:" + ex.getMessage());
        }
    }

    /**
     * create a special user account for the system itself, this is a user that is used
     * by the system exclusively to perform tasks and a user that cannot login
     * @param user the user object to create
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws ApplicationException
     */
    public void createSystemUser(UserWithExtras user)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, ApplicationException {
        if ( user != null && user.getOrganisation_id() != null && user.getPassword() != null ) {
            user.setSystem_user(true);
            user.setConfirmed(true);
            dao.getUserDao().create(user.getOrganisation_id(), user, user.getPassword());
        }
    }

    /**
     * update an existing user.  A user can update themselves, or the primary user can update any other user
     * @param sessionID the session ID
     * @param user the user object to update
     * @param userPassword an optional password to update - null is no change
     * @param ipAddress the ip-address for security purposes
     * @throws ApplicationException
     */
    public void updateUser(UUID sessionID, User user, String userPassword, String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || user == null || user.getEmail() == null || user.getOrganisation_id() == null ) {
            throw new ApplicationException("updateUser: invalid parameter");
        }
        if ( userPassword != null && userPassword.length() < MIN_PASSWORD_LENGTH ) {
            throw new ApplicationException("updateUser: password must be a minimum of " + MIN_PASSWORD_LENGTH + " characters.");
        }
        User sessionUser = getUser(sessionID, ipAddress);
        if ( sessionUser == null || !sessionUser.getOrganisation_id().equals(user.getOrganisation_id()) ) {
            throw new ApplicationException("updateUser: user not in your organisation");
        }
        Organisation organisation = dao.getOrganisationDao().getOrganisationById(sessionUser.getOrganisation_id());
        // the user is allowed to update their own record, or the primary user is allowed to update other users
        if ( !sessionUser.getEmail().equals(user.getEmail()) && !organisation.getPrimary_user().equals(sessionUser.getId()) ) {
            throw new ApplicationException("updateUser: you're only allowed to update your own record, or the su is allowed to update everyone");
        }
        // never allow the update system to change the "activated" flag
        user.setConfirmed(false);
        try {
            dao.getUserDao().update(user, userPassword);
        } catch ( NoSuchAlgorithmException | UnsupportedEncodingException ex ){
            throw new ApplicationException("createOrganisation:" + ex.getMessage());
        }
    }

    /**
     * delete a user
     * @param sessionID the session logged in security context
     * @param origanisation_id the organisation of the user
     * @param email the email address of the user
     * @param ipAddress security context
     * @throws ApplicationException
     */
    public void deleteUser(UUID sessionID, UUID origanisation_id, String email, String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || email == null || origanisation_id == null ) {
            throw new ApplicationException("deleteUser: invalid parameter");
        }
        User sessionUser = getUser(sessionID, ipAddress);
        if ( sessionUser == null || !sessionUser.getOrganisation_id().equals(origanisation_id) ) {
            throw new ApplicationException("deleteUser: user not in your organisation");
        }
        Organisation organisation = dao.getOrganisationDao().getOrganisationById(sessionUser.getOrganisation_id());
        // the user is allowed to update their own record, or the primary user is allowed to update other users
        if ( !sessionUser.getEmail().equals(email) && !organisation.getPrimary_user().equals(sessionUser.getId()) ) {
            throw new ApplicationException("deleteUser: you're only allowed to update your own record, or the su is allowed to update everyone");
        }
        dao.getUserDao().delete(sessionUser.getOrganisation_id(), email);
    }

    /**
     * convert a set of emails to a user list
     * @param list a list of emails
     * @param page page offset into the list
     * @param numItemsPerPage num of items per page
     * @return a list of users (or empty object)
     */
    public UserList getListFromEmails(List<String> list, int page, int numItemsPerPage) {
        if ( list != null && list.size() > 0 ) {

            UserList userList = new UserList();
            userList.setPage(page);
            userList.setItems_per_page(numItemsPerPage);
            userList.setTotal_user_count(list.size());

            int offset = page * numItemsPerPage;
            int endOffset = offset + numItemsPerPage;
            for (int i = offset; i < endOffset; i++) {
                if (i < list.size()) {
                    User user = dao.getUserDao().read(list.get(i));
                    if (user != null) {
                        userList.getUser_list().add(user);
                    }
                }
            }
            return userList;
        } else {
            return new UserList();
        }
    }

    /**
     * get a paginated user list
     * @param sessionID the session of the user
     * @param page the page (in multiples of numItemsPerPage)
     * @param numItemsPerPage number of items per each page
     * @param ipAddress ip address for security
     * @return user list object
     * @throws ApplicationException
     */
    public UserList getPaginatedUserList(UUID sessionID, int page, int numItemsPerPage,
                                         String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null ) {
            throw new ApplicationException("getUserList: invalid parameter");
        }
        User sessionUser = getUser(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("getUserList: invalid session");
        }
        UserEmailList userEmailList = dao.getUserDao().getUserList(sessionUser.getOrganisation_id());
        List<User> userList = new ArrayList<>();
        List<String> emailList = userEmailList.getUser_list();
        int offset = page * numItemsPerPage;
        int endOffset = offset + numItemsPerPage;
        for ( int i = offset; i < endOffset; i++ ) {
            if ( i < emailList.size() ) {
                User user = dao.getUserDao().read(emailList.get(i));
                if ( user != null ) {
                    user.setPassword_sha256(null); // never return to ui
                    user.setSalt(null);
                    userList.add(user);
                }
            }
        }
        UserList list = new UserList();
        list.setUser_list(userList);
        list.setOrganisation_id(sessionUser.getOrganisation_id());
        list.setPage(page);
        list.setItems_per_page(numItemsPerPage);
        list.setTotal_user_count(emailList.size());
        return list;
    }


    /**
     * create an account activation event
     * @param email the email of the user
     * @return the uuid of the activation request
     */
    public UUID createAccountActivation( String email ) throws ApplicationException {
        return dao.getUserDao().createAccountActivation(email);
    }

    /**
     * user confirms an account (or tries to)
     * @param email the email of the user
     * @param activationID the activation id they've received to activate this account
     * @throws ApplicationException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public void confirmAccount( String email, UUID activationID )
            throws ApplicationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        if (email != null && activationID != null) {
            // confirm this reset link is valid
            UUID storedResetID = dao.getUserDao().getAccountActivation(email);
            if ( storedResetID == null ) {
                logger.debug("confirmAccount: unknown activation request " + activationID.toString());
                throw new ApplicationException("Sorry, we have no record of an account activation request.");
            }
            if ( storedResetID.compareTo(activationID) != 0 ) {
                logger.debug("confirmAccount: mismatching activation request " + activationID.toString());
                throw new ApplicationException("Sorry, the activation code is incorrect.  You can regenerate a request by re-creating your account.");
            }
            // activate the account
            dao.getUserDao().confirmAccount(email);
            logger.debug("confirmAccount: account verified " + activationID.toString());
        } else {
            throw new ApplicationException("confirmAccount: invalid parameters");
        }
    }

    /**
     * create a new passord reset request
     * @param email the email of the user
     * @return the uuid of the password reset
     */
    public UUID resetPasswordRequest( String email ) throws ApplicationException {
        if ( email == null ) {
            throw new ApplicationException("resetPasswordRequest: invalid parameter");
        }
        return dao.getUserDao().createPasswordResetRequest(email);
    }

    /**
     * reset a user's password
     * @param email the email of the user
     * @param resetID the id for the reset request
     * @param newPassword the password to set
     * @throws ApplicationException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public void resetPassword( String email, UUID resetID, String newPassword )
            throws ApplicationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        if (email != null && resetID != null && newPassword != null && newPassword.length() >= MIN_PASSWORD_LENGTH ) {
            // check it is a valid request
            UUID storedRequest = dao.getUserDao().getPasswordResetRequest(email);
            if ( storedRequest == null ) {
                throw new ApplicationException("no such password-reset request");
            } else if ( !storedRequest.equals(resetID) ) {
                throw new ApplicationException("invalid password-reset request (ids do not match)");
            }
            dao.getUserDao().resetPassword(email, newPassword); // perform the reset
        } else {
            if ( newPassword != null && newPassword.length() < MIN_PASSWORD_LENGTH ) {
                throw new ApplicationException("resetPassword: password too short");
            } else {
                throw new ApplicationException("resetPassword: invalid parameters");
            }
        }
    }



}

