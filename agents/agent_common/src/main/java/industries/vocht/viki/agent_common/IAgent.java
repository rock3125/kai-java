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

package industries.vocht.viki.agent_common;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by peter on 16/06/16.
 *
 * common interface for all agents
 *
 */
public interface IAgent {

    // star the agent
    void start() throws SQLException, InterruptedException, IOException, AgentException;

}

