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

package industries.vocht.viki.model;

import industries.vocht.viki.model.Token;

/**
 * Created by peter on 25/04/16.
 *
 * parser helper - a token with index
 *
 */
public class TokenWithIndex {

    private int index;
    private Token item;

    public TokenWithIndex() {
    }

    public TokenWithIndex( Token item, int index ) {
        this.item = item;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Token getItem() {
        return item;
    }

    public void setItem(Token item) {
        this.item = item;
    }


}


