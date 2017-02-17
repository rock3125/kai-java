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

package industries.vocht.viki.dao;

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDatabase;
import industries.vocht.viki.model.Session;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.model.user.UserEmailList;
import industries.vocht.viki.utility.Sha256;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class UserDao {

    private IDatabase db;
    private int minPasswordLength;

    public UserDao(IDatabase db, int minPasswordLength) {
        this.db = db;
        this.minPasswordLength = minPasswordLength;
    }

    /**
     * login a user from an ip-address
     * @param email the email of the user to login
     * @param password the password
     * @param ipAddress the ip-address for the session
     * @return a new session id (multiple sessions allowed per user)
     */
    public UUID login( String email, String password, String ipAddress ) throws ApplicationException {
        if ( ipAddress == null ) {
            throw new ApplicationException("login: invalid ip-address");
        }
        User user = read(email);
        if ( user == null ) {
            throw new ApplicationException("login: no such user");
        }
        if ( !user.isConfirmed() ) {
            throw new ApplicationException("login: user account not activated.");
        }
        if ( user.isSystem_user() ) {
            throw new ApplicationException("login: cannot login with system account.");
        }
        String passwordSha256;
        try {
            passwordSha256 = new Sha256().generateSha256Password(user.getSalt(), password);
        } catch ( NoSuchAlgorithmException | UnsupportedEncodingException ex ){
            throw new ApplicationException("login: password algorithm failed");
        }
        if ( user.getPassword_sha256() == null || user.getPassword_sha256().compareToIgnoreCase(passwordSha256) != 0 ) {
            throw new ApplicationException("login: invalid password/user combination");
        }
        // do NOT re-use sessions - the ip address checker makes it impossible
        // to login from two different addresses
        Session session = db.createSession(email, ipAddress);
        // mark this session as active
        db.createActiveSession( user.getOrganisation_id(), session );
        return session.getId();
    }

    /**
     * login the system user - strict checking of system user-ness
     * @param email the email address of the system user
     * @param ipAddress a fake ip address for the system user
     * @return the session id for the system user
     */
    public UUID loginSystemUser( String email, String ipAddress ) throws ApplicationException {
        User user = read(email);
        if ( user == null ) {
            throw new ApplicationException("loginSystemUser: no such user");
        }
        if ( !user.isSystem_user() ) {
            throw new ApplicationException("loginSystemUser: not a system account.");
        }
        return db.createSession(email, ipAddress).getId();
    }

    /**
     * get a user by email address
     * @param email the email of the user to login
     * @param ipAddress the ip-address for the session
     * @return a new session id (multiple sessions allowed per user)
     */
    public User getUserByEmail( String email, String ipAddress ) throws ApplicationException {
        if ( ipAddress == null ) {
            throw new ApplicationException("getUserByEmail: invalid ip-address");
        }
        User user = read(email);
        if ( user == null ) {
            throw new ApplicationException("getUserByEmail: no such user");
        }
        return user;
    }

    /**
     * logout
     * @param sessionID the user's session to logout
     */
    public void logout( UUID organisation_id, UUID sessionID ) {
        db.removeActiveSession(organisation_id, sessionID);
        db.clearSession(sessionID);
    }

    /**
     * get a user for the given session / ip-address
     * @param sessionID the session object for security
     * @param ipAddress an ip-address for security purposes
     * @return a valid user object or null if session invalid
     */
    public User getUserForSession( UUID sessionID, String ipAddress ) throws ApplicationException {
        Session session = db.getSession(sessionID); // refreshes the session automatically
        if ( session != null ) {
            User user = read(session.getEmail());

            // check the ip address matches the session's for non system users
            if ( !user.isSystem_user() && ipAddress != null ) {
                if (session.getIp_address() == null || session.getIp_address().compareToIgnoreCase(ipAddress) != 0) {
                    throw new ApplicationException("getUserForSession: invalid session, ip-address changed");
                }
            }

            user.setPassword_sha256(null); // never ever return security info back
            user.setSalt(null);

            return user;
        }
        return null;
    }

    /**
     * create a new user object
     * @param organisation_id the organisation this user belongs to
     * @param user the user object with data (must have email)
     * @param userPassword the user's password (must be valid)
     * @return the new user object
     */
    public User create(UUID organisation_id, User user, String userPassword)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, ApplicationException {
        if ( organisation_id != null && user != null && user.getEmail() != null &&
                userPassword != null && userPassword.length() >= minPasswordLength ) {

            // update the user list if this is not a system user
            if ( !user.isSystem_user() ) {
                UserEmailList userEmailList = db.readUserList(organisation_id);
                userEmailList.getUser_list().add(user.getEmail());
                db.updateUserList(organisation_id, userEmailList);
            }

            User updatedUser = db.createUser(organisation_id, user, userPassword);
            updatedUser.setSalt(null); // never return to ui
            updatedUser.setPassword_sha256(null);
            return updatedUser;
        } else {
            throw new ApplicationException("create: invalid parameter");
        }
    }

    /**
     * get a user object from an email
     * @param email the email of the user
     * @return a valid user object or null if dne
     */
    public User read(String email) {
        if ( email != null ) {
            return db.readUser(email);
        }
        return null;
    }

    /**
     * return a list of all active sessions for an organisation
     * @param organisation_id the organisation in question
     * @return a list of active session objects
     */
    public List<Session> getActiveSessions(UUID organisation_id) {
        return db.getActiveSessions(organisation_id);
    }

    /**
     * update an existing user
     * @param user the user object with the updates
     * @param userPassword an optional password change (set to null if n/a)
     */
    public void update(User user, String userPassword)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, ApplicationException {
        // check parameters are valid, userPassword can be null
        if ( user == null || user.getEmail() == null ) {
            throw new ApplicationException("update: invalid parameter");
        }
        User existingUser = read(user.getEmail()); // check the user exists before update
        if ( existingUser == null ) {
            throw new ApplicationException("update: no such user");
        }
        db.updateUser(user, userPassword);
        user.setPassword_sha256(null); // never return to ui
        user.setSalt(null);
    }

    /**
     * delete an existing user (user must exist)
     * @param email the email of the user object to delete
     */
    public void delete(UUID organisation_id, String email) throws ApplicationException {
        if ( email == null ) {
            throw new ApplicationException("delete: invalid parameter");
        }
        User existingUser = read(email); // check the user exists before delete
        if ( existingUser == null ) {
            throw new ApplicationException("delete: no such user");
        }
        db.deleteUser(organisation_id, email);
    }

    /**
     * get an account activation id for a given email
     * @param email the email
     * @return an account activation id or null if dne
     */
    public UUID getAccountActivation( String email ) {
        return db.getAccountActivation(email);
    }

    /**
     * create a new account activation id for the given user - make sure the user isn't already active
     * and exists
     * @param email the user's id
     * @return a new activation id
     */
    public UUID createAccountActivation( String email ) throws ApplicationException {
        User user = db.readUser(email);
        if ( user == null ) {
            throw new ApplicationException("no such user");
        }
        if ( user.isConfirmed() ) {
            throw new ApplicationException("user already activated");
        }
        return db.createAccountActivation(email);
    }

    /**
     * get a password reset id for a given email
     * @param email the email
     * @return an password reset id or null if dne
     */
    public UUID getPasswordResetRequest( String email ) {
        return db.getPasswordResetRequest(email);
    }

    /**
     * create a new password reset id for the given user
     * @param email the user's id
     * @return a new password reset id
     */
    public UUID createPasswordResetRequest( String email ) {
        return db.createPasswordResetRequest(email);
    }

    /**
     * perform an actual password reset
     * @param email the account id
     * @param newPassword the new password
     */
    public void resetPassword(String email, String newPassword)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException {
        if ( newPassword == null || newPassword.length() < minPasswordLength ) {
            throw new ApplicationException("invalid password");
        }
        db.resetPassword(email, newPassword);
    }

    /**
     * confirm an account and activate it
     * @param email the account's id
     */
    public void confirmAccount(String email)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException {
        db.confirmAccount(email);
    }

    /**
     * access the user list of an organisation
     * @param organisation_id the id of the organisation to get all users for
     * @return user-list
     */
    public UserEmailList getUserList(UUID organisation_id) {
        return db.readUserList(organisation_id);
    }

}

