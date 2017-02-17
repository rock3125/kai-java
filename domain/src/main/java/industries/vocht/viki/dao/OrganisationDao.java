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
import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.model.user.UserEmailList;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class OrganisationDao {

    private IDatabase db;
    private boolean isUnitTest = false;
    private int minPasswordLength;

    public OrganisationDao(IDatabase db, int minPasswordLength) {
        this.db = db;
        this.minPasswordLength = minPasswordLength;
    }

    /**
     * create an initial organisation with its first user
     * @param organisation the organisation object with its details
     * @param user the user for this organisation
     * @param userPassword the user's password
     */
    public void create(Organisation organisation, User user, String userPassword)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, ApplicationException {

        if ( organisation == null || organisation.getName() == null ) {
            throw new ApplicationException("invalid organisation object");
        }
        if ( user == null || user.getEmail() == null || user.getFirst_name() == null || user.getSurname() == null ) {
            throw new ApplicationException("invalid user object");
        }
        if ( userPassword == null || userPassword.length() < minPasswordLength) {
            throw new ApplicationException("invalid user password");
        }

        if ( !isUnitTest ) {
            Organisation existingOrganisation = db.readOrganisation(organisation.getName());
            if (existingOrganisation != null) {
                throw new ApplicationException("an organisation with that name already exists: " + existingOrganisation.getName());
            }

            // check the user doesn't exist yet
            User existingUser = db.readUser(user.getEmail());
            if (existingUser != null) {
                throw new ApplicationException("a user with that email already exists: " + user.getEmail());
            }
        }

        // create the organisation
        Organisation organisationCreated = db.createOrganisation(organisation);
        user.setOrganisation_id(organisationCreated.getId());

        // never allow the create system to change the "activated" flag
        user.setConfirmed(false);

        // create the initial user
        User userCreated = db.createUser(organisationCreated.getId(), user, userPassword);
        user.setId(userCreated.getId()); // get new id of this user

        // update the user list
        UserEmailList userEmailList = db.readUserList(organisation.getId());
        userEmailList.getUser_list().add(user.getEmail());
        db.updateUserList(organisation.getId(), userEmailList);

        // update the organisation with the new primary user
        organisation.setPrimary_user(userCreated.getId());
        db.updateOrganisation(organisation);
    }

    /**
     * get an organisation by UUID
     * @param id the UUID
     * @return the organisation if it exists
     */
    public Organisation getOrganisationById(UUID id) throws ApplicationException {
        String name = db.getOrganisationName(id);
        if ( name == null ) {
            throw new ApplicationException("getOrganisationById: unknown organisation");
        }
        return read(name);
    }

    public Organisation read(String name) {
        return db.readOrganisation(name);
    }

    public void update(Organisation organisation) {
        db.updateOrganisation(organisation);
    }

    public List<Organisation> getOrganisationList() {
        return db.getOrganisationList();
    }

    public void setUnitTest() {
        this.isUnitTest = true;
    }

}

