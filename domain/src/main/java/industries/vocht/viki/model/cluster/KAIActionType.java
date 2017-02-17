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

package industries.vocht.viki.model.cluster;

import java.io.Serializable;

/**
 * Created by peter on 29/05/16.
 *
 * different types of infrastructure items
 * of the micro architecture
 *
 */
public enum KAIActionType implements Serializable {

    Analysis,
    Clustering,
    Converter,
    Document,
    Group,
    Index,
    Knowledge,
    WSD,
    Parser,
    Report,
    Rule,
    Statistics,
    Search,
    Security,
    Summary,
    Time,
    Vectorize,
    Speech,
    DocumentComparison,
    KBEntry,                // knowledge base entry system (entity, geography and user custom ones)

    LAST_ITEM

}

