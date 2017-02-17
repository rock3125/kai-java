package industries.vocht.viki.aiml;

import industries.vocht.viki.model.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 9/01/17.
 *
 * a forced binding of a * to a template
 *
 */
public class AimlBinding {

    private int stackIndex;         // how deep the result list was at the time of binding
    private List<Token> tokenList;  // the actual binding

    public AimlBinding() {
        this.tokenList = new ArrayList<>();
    }

    // empty binding
    public AimlBinding(int stackIndex) {
        this.tokenList = null;
        this.stackIndex = stackIndex;
    }

    public AimlBinding(int stackIndex, List<Token> tokenList) {
        this.tokenList = tokenList;
        this.stackIndex = stackIndex;
    }

    public int getStackIndex() {
        return stackIndex;
    }

    public void setStackIndex(int stackIndex) {
        this.stackIndex = stackIndex;
    }

    public List<Token> getTokenList() {
        return tokenList;
    }

    public void setTokenList(List<Token> tokenList) {
        this.tokenList = tokenList;
    }

}

