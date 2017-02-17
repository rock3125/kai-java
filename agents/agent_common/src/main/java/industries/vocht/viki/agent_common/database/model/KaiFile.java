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

package industries.vocht.viki.agent_common.database.model;

import org.joda.time.DateTime;

/**
 * Created by peter on 16/06/16.
 *
 * kai file bean
 *
 */
public class KaiFile {

    private int id;
    private String filename;
    private int parent_id;
    private String hash;
    private String metadata_hash;
    private int agent_id;
    private DateTime last_checked;
    private DateTime last_uploaded;
    private String file_type;

    public KaiFile() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getParent_id() {
        return parent_id;
    }

    public void setParent_id(int parent_id) {
        this.parent_id = parent_id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getMetadata_hash() {
        return metadata_hash;
    }

    public void setMetadata_hash(String metadata_hash) {
        this.metadata_hash = metadata_hash;
    }

    public int getAgent_id() {
        return agent_id;
    }

    public void setAgent_id(int agent_id) {
        this.agent_id = agent_id;
    }

    public DateTime getLast_checked() {
        return last_checked;
    }

    public void setLast_checked(DateTime last_checked) {
        this.last_checked = last_checked;
    }

    public DateTime getLast_uploaded() {
        return last_uploaded;
    }

    public void setLast_uploaded(DateTime last_uploaded) {
        this.last_uploaded = last_uploaded;
    }

    public String getFile_type() {
        return file_type;
    }

    public void setFile_type(String file_type) {
        this.file_type = file_type;
    }
}

