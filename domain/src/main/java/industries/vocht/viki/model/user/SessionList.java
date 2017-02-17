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

import industries.vocht.viki.model.Session;

import java.util.List;

/**
 * Created by peter on 19/06/16.
 *
 * a list of session objects
 *
 */
public class SessionList {

    private List<Session> sessionList;

    public SessionList() {
    }

    public SessionList(List<Session> sessionList) {
        this.sessionList = sessionList;
    }

    public List<Session> getSessionList() {
        return sessionList;
    }

    public void setSessionList(List<Session> sessionList) {
        this.sessionList = sessionList;
    }

}
