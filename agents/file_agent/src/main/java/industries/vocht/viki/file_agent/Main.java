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

package industries.vocht.viki.file_agent;

import industries.vocht.viki.agent_common.IAgent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by peter on 16/06/16.
 *
 * start the file agent
 *
 */
public class Main {

    /**
     * generic agent start
     * @param args no arguments required
     * @throws Exception
     */
    public static void main( String[] args ) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("/file_agent/application-context.xml");
        IAgent agent = (IAgent)context.getBean("agent");
        agent.start();
    }

}

