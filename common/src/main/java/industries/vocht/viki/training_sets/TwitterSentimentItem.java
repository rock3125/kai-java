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

package industries.vocht.viki.training_sets;

import industries.vocht.viki.model.Token;

import java.util.List;

/**
 * Created by peter on 4/04/16.
 *
 * a single twitter sentiment item for judgement testing
 * from the twitter-sentiment-analysis-trainingset.csv file
 *
 */
public class TwitterSentimentItem {

    public enum Sentiment {
        Positive,
        Negative
    }

    private Sentiment sentiment;
    private List<Token> text;

    public TwitterSentimentItem() {
    }

    public TwitterSentimentItem(Sentiment sentiment, List<Token> text) {
        this.setSentiment(sentiment);
        this.setText(text);
    }

    public Sentiment getSentiment() {
        return sentiment;
    }

    public void setSentiment(Sentiment sentiment) {
        this.sentiment = sentiment;
    }

    public List<Token> getText() {
        return text;
    }

    public void setText(List<Token> text) {
        this.text = text;
    }


}

