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
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.utility.SentenceFromBinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 2/04/16.
 *
 * items common to the service layers
 *
 */
public class ServiceLayerCommon {

    static final Logger logger = LoggerFactory.getLogger(ServiceLayerCommon.class);

    @Autowired
    protected IDao dao;

    /**
     * get the parse-tree for a given document
     * @param organisation_id the document's organisation id
     * @param url the document's url
     * @return a parse-tree of the document if it exists
     * @throws IOException
     */
    protected List<Sentence> getSentenceList(UUID organisation_id, String url) throws IOException {
        Map<String, byte[]> map = dao.getDocumentDao().getDocumentParseTreeMap(organisation_id, url);
        if ( map != null && map.containsKey(Document.META_BODY) ) {
            byte[] data = map.get(Document.META_BODY);
            SentenceFromBinary parseTreeConverter = new SentenceFromBinary();
            return parseTreeConverter.convert(data);
        }
        return null;
    }

    /**
     * check the existing session and return the user associated with the session
     * @param userService the user service to user for the session check
     * @param sessionIDStr the session's ID
     * @param request the HTTP request object
     * @return the user object
     * @throws ApplicationException invalid sessions and other errors
     */
    public User checkSession(UserService userService, String sessionIDStr, HttpServletRequest request ) throws ApplicationException {
        if ( sessionIDStr == null ) {
            throw new ApplicationException("invalid parameters");
        }
        if ( userService == null ) {
            throw new ApplicationException("invalid system state (user-service null)");
        }
        User existingUser = userService.getUser(UUID.fromString(sessionIDStr), request.getRemoteAddr());
        if ( existingUser == null ) {
            throw new ApplicationException("invalid session");
        }
        return existingUser;
    }


}

