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

package industries.vocht.viki.file_agent;

import jcifs.smb.NtlmAuthenticator;
import jcifs.smb.NtlmPasswordAuthentication;

/**
 * Created by peter on 16/06/16.
 *
 * ntlm authenticator for samba
 *
 */
public class SMBAuthenticator extends NtlmAuthenticator {

    private String username;
    private String password;
    private String domain;

    public SMBAuthenticator( String username, String password, String domain ) {
        this.username = username;
        this.password = password;
        this.domain = domain;
        NtlmAuthenticator.setDefault(this);
    }

    public NtlmPasswordAuthentication getNtlmPasswordAuthentication() {
        return new NtlmPasswordAuthentication(domain, username, password);
    }

}

