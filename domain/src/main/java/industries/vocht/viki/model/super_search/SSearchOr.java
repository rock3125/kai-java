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

import java.util.List;

/**
 * Created by peter on 25/04/16.
 *
 * disjunction - OR between two ssearch items
 *
 */
public class SSearchOr implements ISSearchItem {

    private ISSearchItem left;
    private ISSearchItem right;

    public SSearchOr() {
    }

    public SSearchOr(ISSearchItem left, ISSearchItem right) {
        this.left = left;
        this.right = right;
    }

    public void getSearchTerms(List<SSearchWord> inList) {
        if ( left != null ) {
            left.getSearchTerms(inList);
        }
        if ( right != null ) {
            right.getSearchTerms(inList);
        }
    }

    public ISSearchItem getLeft() {
        return left;
    }

    public void setLeft(ISSearchItem left) {
        this.left = left;
    }

    public ISSearchItem getRight() {
        return right;
    }

    public void setRight(ISSearchItem right) {
        this.right = right;
    }


}


