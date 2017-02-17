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

package industries.vocht.viki.agent_common.client_interface;

import java.util.UUID;

/**
 * Created by peter on 6/03/16.
 *
 * a user object with password and sessionID
 *
 */
public class AgentUser {

    private UUID id;
    private String email;
    private String first_name;
    private String surname;
    private UUID organisation_id;
    private UUID salt;
    private String password_sha256;
    private boolean confirmed; // is this account a confirmed email address?
    private boolean system_user; // is this a system account?

    private UUID sessionID;
    private String password;

    public AgentUser() {
    }

    public UUID getSessionID() {
        return sessionID;
    }

    public void setSessionID(UUID sessionID) {
        this.sessionID = sessionID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public UUID getSalt() {
        return salt;
    }

    public void setSalt(UUID salt) {
        this.salt = salt;
    }

    public String getPassword_sha256() {
        return password_sha256;
    }

    public void setPassword_sha256(String password_sha256) {
        this.password_sha256 = password_sha256;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isSystem_user() {
        return system_user;
    }

    public void setSystem_user(boolean system_user) {
        this.system_user = system_user;
    }
}

