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

package industries.vocht.viki.rules_engine.action;

/**
 * Created by peter on 15/05/16.
 *
 */
public class ActionChangeDocumentSecurity implements IAction {

    private String acl_csv;

    public ActionChangeDocumentSecurity() {
    }

    public ActionChangeDocumentSecurity(String acl_csv) {
        this.acl_csv = acl_csv;
    }


    public String getAcl_csv() {
        return acl_csv;
    }

    public void setAcl_csv(String acl_csv) {
        this.acl_csv = acl_csv;
    }
}

