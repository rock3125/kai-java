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

package industries.vocht.viki.document;

/**
 * Created by peter on 6/06/16.
 *
 * bit patterns for the processing pipeline
 *
 */
public enum DocumentProcessingPipeline {

    Convert(1),
    Parse(2),
    Vectorize(4),
    Summarize(8),
    Index(16),
    Cluster(32),
    Entities(64),
    Emotions(128);


    DocumentProcessingPipeline( long bits ) {
        this.bits = bits;
    }

    /**
     * is the document ready for processing for this tag?
     * @param document the document to check
     * @return true if the document should go through this part of the pipe
     */
    public boolean isEnabled( Document document ) {
        return document != null && ((bits & document.getProcessingPipeline()) != 0L);
    }

    private long bits;

}

