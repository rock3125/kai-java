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

/**
 * Created by peter on 4/03/16.
 * sortable by user/group
 *
 */
public class AgentAcl implements Comparable<AgentAcl> {

    private String user_group;
    private boolean has_access;

    public AgentAcl() {
    }

    public AgentAcl(String user_group, boolean has_access ) {
        this.user_group = user_group;
        this.has_access = has_access;
    }

    public String getUser_group() {
        return user_group;
    }

    public void setUser_group(String user_group) {
        this.user_group = user_group;
    }

    public boolean isHas_access() {
        return has_access;
    }

    public void setHas_access(boolean has_access) {
        this.has_access = has_access;
    }

    @Override
    public String toString() {
        return user_group + ":" + has_access;
    }

    public String toPrettyString() {
        return user_group + " " + (has_access ? "has access" : "is denied");
    }

    @Override
    public int compareTo(AgentAcl acl) {
        return user_group.compareTo(acl.user_group);
    }

}
