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

import java.util.List;

/**
 * Created by peter on 14/06/16.
 *
 * a list of case tuple result items for UI display
 *
 */
public class TupleResultList {

    private List<TupleResult> caseTupleList;

    public TupleResultList() {
    }

    public TupleResultList(List<TupleResult> caseTupleList ) {
        this.caseTupleList = caseTupleList;
    }

    public List<TupleResult> getCaseTupleList() {
        return caseTupleList;
    }

    public void setCaseTupleList(List<TupleResult> caseTupleList) {
        this.caseTupleList = caseTupleList;
    }


}

