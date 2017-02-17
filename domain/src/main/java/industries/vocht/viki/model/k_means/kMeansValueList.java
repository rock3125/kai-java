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

package industries.vocht.viki.model.k_means;

import java.util.List;

/**
 * Created by peter on 13/06/16.
 *
 * for document comparisons
 *
 */
public class kMeansValueList {

    private List<kMeansValue> kMeansValueList;

    public kMeansValueList() {
    }

    public kMeansValueList( List<kMeansValue> kMeansValueList ) {
        this.kMeansValueList = kMeansValueList;
    }

    public List<kMeansValue> getkMeansValueList() {
        return kMeansValueList;
    }

    public void setkMeansValueList(List<kMeansValue> kMeansValueList) {
        this.kMeansValueList = kMeansValueList;
    }


}
