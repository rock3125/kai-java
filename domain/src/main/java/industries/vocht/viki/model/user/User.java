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

package industries.vocht.viki.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class User {
    protected UUID id;
    protected String email;
    protected String first_name;
    protected String surname;
    protected UUID organisation_id;
    protected UUID salt;
    protected String password_sha256;
    protected boolean confirmed; // is this account a confirmed email address?
    private boolean system_user; // is this a system account?

    public User() {
    }

    @JsonIgnore
    public String getFullname() {
        String fullname = "";
        if ( first_name != null && surname != null ) {
            fullname = first_name + " " + surname;
        } else if ( first_name != null ) {
            fullname = first_name;
        } else if ( surname != null ) {
            fullname = surname;
        }
        return fullname.trim();
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
