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

package industries.vocht.viki.client;

/**
 * Created by peter on 27/07/16.
 *
 * document client interface
 *
 */
public class DocumentComparisonClientInterface extends ClientInterfaceCommon {

    // document system is default port 14080
    public DocumentComparisonClientInterface(String host, int port ) {
        super(host, port);
    }

}
