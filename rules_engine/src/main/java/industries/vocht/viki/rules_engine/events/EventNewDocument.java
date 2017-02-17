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

package industries.vocht.viki.rules_engine.events;

/**
 * Created by peter on 15/05/16.
 *
 */
public class EventNewDocument implements IEvent {

    private String origin_filter;
    private String document_type_filter;

    public EventNewDocument() {
    }

    public EventNewDocument( String origin_filter, String document_type_filter ) {
        this.origin_filter = origin_filter;
        this.document_type_filter = document_type_filter;
    }

    public String getOrigin_filter() {
        return origin_filter;
    }

    public void setOrigin_filter(String origin_filter) {
        this.origin_filter = origin_filter;
    }

    public String getDocument_type_filter() {
        return document_type_filter;
    }

    public void setDocument_type_filter(String document_type_filter) {
        this.document_type_filter = document_type_filter;
    }
}
