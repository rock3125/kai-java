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

package industries.vocht.viki.hazelcast_messages;

import java.io.IOException;

/**
 * Created by peter on 2/06/16.
 *
 * the thing that implements a message received / send processor
 *
 */
public interface IHazelcastMessageProcessor {

    // incoming
    void receive( IHazelcastMessage message );

    // outgoing
    void publish( IHazelcastMessage message ) throws IOException;

}
