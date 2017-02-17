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

package industries.vocht.viki.model.indexes;

/**
 * Created by peter on 21/04/16.
 *
 * a time based index
 *
 */
public class TimeIndex implements IIndex {

    private String url;
    private long date_time;
    private int offset;
    private int acl_hash;

    public TimeIndex() {
    }

    public TimeIndex( String url, int offset, long date_time, int acl_hash ) {
        this.url = url;
        this.date_time = date_time;
        this.acl_hash = acl_hash;
        this.offset = offset;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getDate_time() {
        return date_time;
    }

    public void setDate_time(long date_time) {
        this.date_time = date_time;
    }

    @Override
    public int getAcl_hash() {
        return acl_hash;
    }

    public void setAcl_hash(int acl_hash) {
        this.acl_hash = acl_hash;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

}


