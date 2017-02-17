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

import industries.vocht.viki.model.cluster.ClusterAddress;

import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 6/03/16.
 *
 * a user object with password and sessionID
 *
 */
public class UserWithExtras extends User {

    private UUID sessionID;
    private String password;

    // cluster configuration
    private List<ClusterAddress> cluster_address_list;

    // any other tabs that need to show up
    private List<UserTab> user_tab_list;


    public UserWithExtras() {
    }

    public UserWithExtras(User user) {
        copy(user);
    }

    // copy user into this object
    private void copy(User user) {
        id = user.getId();
        email = user.getEmail();
        first_name = user.getFirst_name();
        surname = user.getSurname();
        organisation_id = user.getOrganisation_id();
        salt = user.getSalt();
        password_sha256 = user.getPassword_sha256();
        confirmed = user.isConfirmed();
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

    public List<ClusterAddress> getCluster_address_list() {
        return cluster_address_list;
    }

    public void setCluster_address_list(List<ClusterAddress> cluster_address_list) {
        this.cluster_address_list = cluster_address_list;
    }

    public List<UserTab> getUser_tab_list() {
        return user_tab_list;
    }

    public void setUser_tab_list(List<UserTab> user_tab_list) {
        this.user_tab_list = user_tab_list;
    }

}
