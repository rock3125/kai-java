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

import java.io.Serializable;

/**
 * Created by peter on 2/06/16.
 *
 * message to all nodes possessing neural networks
 * reset the failed loaders so networks will get another
 * "peek" when next encountered
 *
 */
public class HMsgNNetResetCaches extends IHazelcastMessage implements Serializable {

    public HMsgNNetResetCaches() {
    }


}

