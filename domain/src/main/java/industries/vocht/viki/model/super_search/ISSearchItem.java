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
 * grouping item for all super search terms
 *
 */
public interface ISSearchItem {

    // return the search-terms used in this query with exactness flag set
    void getSearchTerms(List<SSearchWord> inList);

}

