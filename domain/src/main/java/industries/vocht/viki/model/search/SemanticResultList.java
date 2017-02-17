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

package industries.vocht.viki.model.search;

import industries.vocht.viki.model.semantics.Tuple;

import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 9/06/16.
 *
 * list of results for tuple sets
 *
 */
public class SemanticResultList {

    private UUID organisation_id;
    private String metadata;
    private List<Tuple> list;
    private int page;
    private int items_per_page;
    private int total_document_count;

    public SemanticResultList() {
    }

    public SemanticResultList( String metadata, List<Tuple> list ) {
        this.metadata = metadata;
        this.list = list;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public List<Tuple> getList() {
        return list;
    }

    public void setList(List<Tuple> list) {
        this.list = list;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
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


}
