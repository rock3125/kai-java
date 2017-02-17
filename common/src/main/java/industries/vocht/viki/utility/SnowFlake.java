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

package industries.vocht.viki.utility;

/*
 * Created by peter on 4/02/15.
 *
 * use Twitter's Snowflake Algorithm to get a unique long id number
 *
 * 41 bits timestamp
 * 10 bits machine id (configured)
 * 12 bits sequence number (for same timestamps + 1)
 *
 */
public class SnowFlake
{
    // the twitter epoch to subtract from the timestamp
    private static final long twEpoch = 1288834974657L;

    // bits and masks
    private static final long workerIdBits = 5L;
    private static final long datacenterIdBits = 5L;
    private static final long maxWorkerId = (1L << workerIdBits) - 1;
    private static final long maxDatacenterId = (1L << datacenterIdBits) - 1;
    private static final long sequenceBits = 12L;

    private static final long workerIdShift = sequenceBits;
    private static final long datacenterIdShift = sequenceBits + workerIdBits;
    private static final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private static final long sequenceMask = (1L << sequenceBits) - 1;

    // initialise for a data-center / worker id
    public SnowFlake( long dataCenterID, long workerID )
    {
        // sanity check for workerId
        if (workerID > maxWorkerId || workerID < 0)
            throw new IllegalArgumentException("worker Id can't be greater than " + maxWorkerId + " or less than 0");

        if (dataCenterID > maxDatacenterId || dataCenterID < 0)
            throw new IllegalArgumentException("datacenter Id can't be greater than " + maxDatacenterId + " or less than 0");

        this.dataCenterID = dataCenterID;
        this.workerID = workerID;
        this.sequence = 0L; // always starts at zero - and reset to zero every ms change
        this.lastTimeStamp = 0L; // reset
    }

    // create the next id
    public long nextID()
    {
        synchronized (SnowFlake.class)
        {
            long timeStamp = System.currentTimeMillis(); // get current time in ms

            if ( lastTimeStamp == timeStamp ) // hasn't changed since last id?
            {
                sequence = (sequence + 1) & sequenceMask; // add 1 to sequence
                if ( sequence == 0 ) // overflow?
                    timeStamp = tilNextMillis( lastTimeStamp ); // wait 1 ms
            }
            else
                sequence = 0L; // reset sequence - time has changed

            lastTimeStamp = timeStamp; // record last time

            // calculate the id
            return ((timeStamp - twEpoch) << timestampLeftShift) |
                    (dataCenterID << datacenterIdShift) |
                    (workerID << workerIdShift) |
                    sequence;
        }
    }

    // wait until the timestamp ticks over
    private long tilNextMillis( long lastTimeStamp )
    {
        long timeStamp = System.currentTimeMillis();
        while ( timeStamp <= lastTimeStamp )
            timeStamp = System.currentTimeMillis();
        return timeStamp;
    }

    private long dataCenterID; // a unique id per machine
    private long workerID; // combined with dataCenterID makes a unique id

    private long sequence; // unique sequence id per time tick

    private long lastTimeStamp; // what the time was last time
}
