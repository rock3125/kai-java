package industries.vocht.viki.grammar;

import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;

/**
 * Created by peter on 11/12/16.
 *
 * same tests as grammar test but using the full parser as a framework
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class GrammarParserTest {

    @Autowired
    private NLParser parser;

    @Before
    public void setup() {
        Assert.notNull(parser);
    }

    @Test
    public void testDateTime1() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "2016-04-18 15:59:07" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("2016-04-18 15:59:07") );
    }

    @Test
    public void testDateTime2() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "2016-04-18 15:59:07.123" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("2016-04-18 15:59:07.123") );
    }

    @Test
    public void testDateTime3() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "2011-01-31" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("2011-01-31") );
    }

    @Test
    public void testDateTime4() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "2016-04-12 11:22:00" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("2016-04-12 11:22:00") );
    }

    @Test
    public void testDateTime5() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "June 1, 2001" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("June 1,2001") );
    }

    @Test
    public void testUrl1() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "mailto://Blair-l/customer___oneok/22.txt" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("mailto://Blair-l/customer___oneok/22.txt") );
    }

    @Test
    public void testUrl2() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "www.peter.co.nz" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("www.peter.co.nz") );
    }

    @Test
    public void testUrl3() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "http://www.peter.co.nz" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("http://www.peter.co.nz") );
    }

    @Test
    public void testUrl4() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "mailto://Blair-l/customer___oneok/22.txt,mailto://Blair-l/customer___oneok/23.txt" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 3);
        Assert.isTrue( tokenList.get(0).getText().equals("mailto://Blair-l/customer___oneok/22.txt") );
        Assert.isTrue( tokenList.get(2).getText().equals("mailto://Blair-l/customer___oneok/23.txt") );
    }

    @Test
    public void testTime1() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "11:23:00" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("11:23:00") );
    }

    @Test
    public void testTime2() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "23:23" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("23:23") );
    }

    @Test
    public void testTime3() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "23:23:00.000" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("23:23:00.000") );
    }

    @Test
    public void testTime4() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "00:00:00" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("00:00:00") );
    }

    @Test
    public void testTime5() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "00:00:00.000" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("00:00:00.000") );
    }

    @Test
    public void testTime6() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "23:59:59.999" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("23:59:59.999") );
    }

    @Test
    public void testPhone1() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "713-853-5660" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("713-853-5660") );
    }

    @Test
    public void testPhone2() throws IOException {
        List<Sentence> sentenceList = parser.parseText( "(713) 853-5660" );
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        List<Token> tokenList = sentenceList.get(0).getTokenList();
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("(713) 853-5660") );
    }

}
