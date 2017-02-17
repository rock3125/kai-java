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

package industries.vocht.viki.system_stats;

/**
 * Created by peter on 19/12/16.
 *
 * a list of authors and their "score" regarding a topic, all adding up to 1.0
 *
 */
public class AuthorFrequency {

    private String author;
    private float score;

    public AuthorFrequency() {
    }

    public AuthorFrequency(String author, float score) {
        this.setAuthor(author);
        this.setScore(score);
    }


    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

}
