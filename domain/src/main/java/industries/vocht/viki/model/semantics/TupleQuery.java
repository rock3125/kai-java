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

package industries.vocht.viki.model.semantics;

import industries.vocht.viki.model.super_search.ISSearchItem;

/**
 * Created by peter on 9/06/16.
 *
 * A case tuple query; combination of a super search query
 * and an optional target metadata item of a tuple
 *
 */
public class TupleQuery {

    private String targetMetadata;
    private ISSearchItem searchItem;

    public TupleQuery() {
    }

    public TupleQuery(String targetMetadata, ISSearchItem searchItem) {
        this.targetMetadata = targetMetadata;
        this.searchItem = searchItem;
    }

    public String getTargetMetadata() {
        return targetMetadata;
    }

    public void setTargetMetadata(String targetMetadata) {
        this.targetMetadata = targetMetadata;
    }

    public ISSearchItem getSearchItem() {
        return searchItem;
    }

    public void setSearchItem(ISSearchItem searchItem) {
        this.searchItem = searchItem;
    }
}

