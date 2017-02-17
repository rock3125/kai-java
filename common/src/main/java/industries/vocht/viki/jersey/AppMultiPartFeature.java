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

package industries.vocht.viki.jersey;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by peter on 20/03/16.
 *
 * setup the multi-part upload class
 *
 */
public class AppMultiPartFeature extends ResourceConfig {  // Application

    public AppMultiPartFeature() {
        packages("industries.vocht.viki.services");
        register(MultiPartFeature.class);
    }

}

