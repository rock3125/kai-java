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

package industries.vocht.viki.main;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by peter on 27/07/16.
 *
 * spring boot jersey sample
 *
 */
@EnableAutoConfiguration
@ComponentScan
public class SpringApplication {

    public static void main(String[] args) throws Exception {
        new SpringApplicationBuilder(SpringApplication.class)
                .bannerMode(Banner.Mode.CONSOLE)
                .run(args);
    }


}


