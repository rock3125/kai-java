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

import java.util.Map;

/**
 * Created by peter on 6/06/16.
 *
 * list of indexes
 *
 */
public class IndexList {

    private Map<String, DocumentIndexSet> indexSet;

    public IndexList() {
    }

    public IndexList( Map<String, DocumentIndexSet> indexSet) {
        this.indexSet = indexSet;
    }

    public Map<String, DocumentIndexSet> getIndexSet() {
        return indexSet;
    }

    public void setIndexSet(Map<String, DocumentIndexSet> indexSet) {
        this.indexSet = indexSet;
    }

}


