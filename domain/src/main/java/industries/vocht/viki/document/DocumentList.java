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

package industries.vocht.viki.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 17/10/15.
 *
 * list of documents, transport item, not a storage item
 *
 */
public class DocumentList {

    private UUID organisation_id;
    private List<Document> document_list;
    private String prevUrl;
    private int items_per_page;
    private int total_document_count;

    public DocumentList() {
        document_list = new ArrayList<>();
    }

    public DocumentList(List<Document> document_list) {
        this.document_list = document_list;
    }

    public List<Document> getDocument_list() {
        return document_list;
    }

    public void setDocument_list(List<Document> document_list) {
        this.document_list = document_list;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public String getPrevUrl() {
        return prevUrl;
    }

    public void setPrevUrl(String prevUrl) {
        this.prevUrl = prevUrl;
    }

    public int getItems_per_page() {
        return items_per_page;
    }

    public void setItems_per_page(int items_per_page) {
        this.items_per_page = items_per_page;
    }

    public int getTotal_document_count() {
        return total_document_count;
    }

    public void setTotal_document_count(int total_document_count) {
        this.total_document_count = total_document_count;
    }

    // sort by url
    public void sort() {
        Collections.sort(document_list);
    }
}

