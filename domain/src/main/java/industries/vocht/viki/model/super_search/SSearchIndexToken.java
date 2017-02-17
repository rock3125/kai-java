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

package industries.vocht.viki.model.super_search;

/**
 * Created by peter on 25/04/16.
 *
 * index with token
 *
 */
public class SSearchIndexToken {

    private int index;
    private ISSearchItem item;

    public SSearchIndexToken() {
    }

    public SSearchIndexToken( ISSearchItem item, int index ) {
        this.item = item;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ISSearchItem getItem() {
        return item;
    }

    public void setItem(ISSearchItem item) {
        this.item = item;
    }


}


