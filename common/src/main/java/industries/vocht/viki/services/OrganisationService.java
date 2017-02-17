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

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
@Component
public class OrganisationService {

    private static Logger logger = LoggerFactory.getLogger(OrganisationService.class);

    @Autowired
    private IDao dao; // dao access

    @Value("${system.min.password.length:8}")
    private int minPasswordLength; // password minimum length restriction

    public OrganisationService() {}

    /**
     * create a new organisation with its first user
     * @param organisation the organisation to create
     * @param user the user to add as the first user of the organisation
     * @return success code
     */
    public Organisation createOrganisation(Organisation organisation, User user, String userPassword ) throws ApplicationException {
        if ( organisation == null || user == null || userPassword == null ) {
            throw new ApplicationException("createOrganisation: null values not allowed");
        }
        if ( userPassword.length() < minPasswordLength ) {
            throw new ApplicationException("createOrganisation: user password too short");
        }
        if ( user.getEmail() == null || !user.getEmail().contains("@") || user.getEmail().length() < 5) {
            throw new ApplicationException("createOrganisation: user primary key (email) missing or invalid");
        }
        if ( organisation.getName() == null || organisation.getName().length() < 2 ) {
            throw new ApplicationException("createOrganisation: organisation primary key(name) missing or invalid");
        }
        try {
            dao.getOrganisationDao().create(organisation, user, userPassword);
        } catch ( NoSuchAlgorithmException | UnsupportedEncodingException ex ){
            throw new ApplicationException("createOrganisation:" + ex.getMessage());
        }
        return organisation;
    }

    /**
     * update an existing organisation (must have same name and id as before)
     * @param organisation the organisation to update
     * @param ipAddress the ip address of the request
     * @return the updated organisation object
     */
    public Organisation updateOrganisation(UUID sessionID, Organisation organisation, String ipAddress) throws ApplicationException {
        if ( sessionID == null || organisation == null || organisation.getId() == null ) {
            throw new ApplicationException("updateOrganisation: null values not allowed");
        }
        if ( organisation.getName() == null || organisation.getName().length() < 2 ) {
            throw new ApplicationException("updateOrganisation: organisation primary key(name) missing or invalid");
        }
        // check we've got the same PK as we're expected to have
        Organisation existingOrg = dao.getOrganisationDao().read(organisation.getName());
        if ( existingOrg == null || existingOrg.getId() == null ) {
            throw new ApplicationException("updateOrganisation: existing organisation: " + organisation.getName() + " not found");
        }
        if ( !existingOrg.getId().equals(organisation.getId()) ) {
            throw new ApplicationException("updateOrganisation: invalid id");
        }
        User user = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( user == null || !user.getOrganisation_id().equals(organisation.getId())) {
            throw new ApplicationException("updateOrganisation: user session invalid or incorrect organisation id");
        }
        dao.getOrganisationDao().update(organisation);
        return organisation;
    }

    /**
     * retrieve an organisation by name
     * @param name the name of the organisation
     * @return null if not found otherwise the organisation
     */
    public Organisation getOrganisationByName(String name) throws ApplicationException {
        if ( name == null || name.length() < 2 ) {
            throw new ApplicationException("getOrganisationByName: organisation primary key(name) missing or invalid");
        }
        return dao.getOrganisationDao().read(name);
    }

    /**
     * retrieve an organisation by name
     * @param organisation_id the id of the organisation
     * @return null if not found otherwise the organisation
     */
    public Organisation getOrganisationById(UUID organisation_id) throws ApplicationException {
        if ( organisation_id == null ) {
            throw new ApplicationException("getOrganisationById: organisation primary key(id) missing or invalid");
        }
        return dao.getOrganisationDao().getOrganisationById(organisation_id);
    }
}

