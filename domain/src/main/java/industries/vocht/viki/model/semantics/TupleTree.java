package industries.vocht.viki.model.semantics;

import industries.vocht.viki.model.Token;
import industries.vocht.viki.utility.BinarySerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 18/12/16.
 *
 * Tuple tree item
 *
 */
public class TupleTree {

    private List<Token> tokenList;  // the tokens of this part of the tree
    private String srl;             // srl label
    private int offset;              // start offset of item in the SRL tree
    private TupleTree left;
    private TupleTree right;

    public TupleTree() {
        this.tokenList = new ArrayList<>();
    }

    public String toString() {
        if ( tokenList != null ) {
            StringBuilder sb = new StringBuilder();
            for (Token token : tokenList) {
                sb.append(token).append(" ");
            }
            return sb.toString().trim();
        }
        return "";
    }

    /**
     * add a new srl item relative to this one
     * @param srl the srl label of the item
     * @param offset the offset of this item
     * @param tokenList the list of tokens for this SRL item
     */
    public void add(String srl, int offset, List<Token> tokenList) {
        if ( srl != null && tokenList != null && tokenList.size() > 0 ) {
            if (offset < this.offset) {
                if (left == null) {
                    left = new TupleTree(srl, offset, tokenList);
                } else {
                    left.add(srl, offset, tokenList);
                }
            } else if (offset > this.offset) {
                if (right == null) {
                    right = new TupleTree(srl, offset, tokenList);
                } else {
                    right.add(srl, offset, tokenList);
                }
            }
        }
    }

    /**
     * return the number of adjectives and nouns in the tree
     * @return the count
     */
    public int nounAndAdjectiveCount() {
        int count = 0;
        if ( tokenList != null ) {
            for (Token part : tokenList) {
                if (part.getPennType() != null &&
                        (part.getPennType().toString().startsWith("NN") || part.getPennType().toString().startsWith("JJ"))) {
                    count += 1;
                }
            }
        }
        if ( left != null ) {
            count += left.nounAndAdjectiveCount();
        }
        if ( right != null ) {
            count += right.nounAndAdjectiveCount();
        }
        return count;
    }

    /**
     * retrieve an item by it SRL label
     * @param srl the label to look for
     * @return null or the SRL item list
     */
    public List<Token> findSrlParameter(String srl) {
        if ( srl != null ) {
            if (srl.equals(this.srl)) {
                return tokenList;
            } else {
                if ( left != null ) {
                    List<Token> tokenList = left.findSrlParameter(srl);
                    if ( tokenList != null ) {
                        return tokenList;
                    }
                }
                if ( right != null ) {
                    List<Token> tokenList = right.findSrlParameter(srl);
                    if ( tokenList != null ) {
                        return tokenList;
                    }
                }
            }
        }
        return null; // not found here
    }

    /**
     * return all the tokens in this entire tree recursively, ordered
     * @return the list of tokens
     */
    public List<Token> retrieveAllTokens() {
        List<Token> finalTokenList = new ArrayList<>();
        if ( left != null ) {
            finalTokenList.addAll(left.retrieveAllTokens());
        }
        if ( tokenList != null ) {
            finalTokenList.addAll(tokenList);
        }
        if ( right != null ) {
            finalTokenList.addAll(right.retrieveAllTokens());
        }
        return finalTokenList;
    }

    /**
     * return all the verb tokens in this entire tree recursively, ordered
     * @return the list of tokens
     */
    public List<Token> retrieveAllVerbs() {
        List<Token> finalTokenList = new ArrayList<>();
        if ( left != null ) {
            finalTokenList.addAll(left.retrieveAllVerbs());
        }
        if ( tokenList != null ) {
            for ( Token token : tokenList ) {
                if (token.getPennType() != null && token.getPennType().toString().startsWith("VB") ) {
                    finalTokenList.add(token);
                }
            }
        }
        if ( right != null ) {
            finalTokenList.addAll(right.retrieveAllVerbs());
        }
        return finalTokenList;
    }

    /**
     * return the number of verbs in the tree
     * @return the count
     */
    public int verbCount() {
        int count = 0;
        if ( tokenList != null ) {
            for (Token part : tokenList) {
                if (part.getPennType() != null &&
                        (part.getPennType().toString().startsWith("VB") ) ) {
                    count += 1;
                }
            }
        }
        if ( left != null ) {
            count += left.verbCount();
        }
        if ( right != null ) {
            count += right.verbCount();
        }
        return count;
    }

    /**
     * returse the sub-tree of this item and return a string
     * @param separator the separator to use for items of this set
     * @return a string that is the ordered parts of this object
     */
    public String subTreeToString(String separator) {
        String str = "";
        if (separator != null ) {
            if ( left != null ) {
                if ( str.length() > 0 ) {
                    str = str + separator;
                }
                str = str + left.subTreeToString(separator);
            }
            if ( str.length() > 0 ) {
                str = str + separator;
            }
            str += toString();
            if ( right != null ) {
                if ( str.length() > 0 ) {
                    str = str + separator;
                }
                str = str + right.subTreeToString(separator);
            }
        }
        return str;
    }

    public TupleTree(String srl, int offset, List<Token> tokenList) {
        this.srl = srl;
        this.offset = offset;
        this.tokenList = tokenList;
    }

    /**
     * copy the entire tree
     * @param tree the tree to copy
     * @return null or a copy of the tree item
     */
    public static TupleTree copy(TupleTree tree) {
        if ( tree != null ) {
            TupleTree copy = new TupleTree();
            copy.setSrl(tree.srl);
            copy.setOffset(tree.offset);
            List<Token> newTokenList = new ArrayList<>();
            newTokenList.addAll(tree.getTokenList());
            copy.setTokenList(newTokenList);
            copy.setLeft(copy(tree.left));
            copy.setRight(copy(tree.right));
            return copy;
        }
        return null;
    }

    // serialise into a byte array @ offset
    public void write( BinarySerializer serializer ) {
        serializer.writeByte(0x2a); // write magic marker

        serializer.writeString(srl);
        serializer.writeInt(offset);

        if ( tokenList == null ) {
            serializer.writeInt(0);
        } else {
            serializer.writeInt(tokenList.size());
            for ( Token token : tokenList ) {
                token.write(serializer);
            }
        }

        if ( left == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            left.write(serializer);
        }

        if ( right == null ) {
            serializer.writeByte(0);
        } else {
            serializer.writeByte(1);
            right.write(serializer);
        }
    }


    // read from byte array @ offset
    public void read( BinarySerializer serializer ) throws IOException {
        int magicMarker = serializer.readByte();
        if ( (magicMarker & 0xff) != 0x2a ) {
            throw new IOException("invalid TupleTree magic marker, invalid data");
        }

        srl = serializer.readString();
        offset = serializer.readInt();

        int numTokens = serializer.readInt();
        tokenList = new ArrayList<>();
        for (int i = 0; i < numTokens; i++ ) {
            Token token = new Token();
            token.read(serializer);
            tokenList.add(token);
        }

        int exists = serializer.readByte();
        if ( exists == 0 ) {
            left = null;
        } else {
            left = new TupleTree();
            left.read(serializer);
        }

        exists = serializer.readByte();
        if ( exists == 0 ) {
            right = null;
        } else {
            right= new TupleTree();
            right.read(serializer);
        }

    }

    public List<Token> getTokenList() {
        return tokenList;
    }

    public void setTokenList(List<Token> tokenList) {
        this.tokenList= tokenList;
    }

    public TupleTree getLeft() {
        return left;
    }

    public void setLeft(TupleTree left) {
        this.left = left;
    }

    public TupleTree getRight() {
        return right;
    }

    public void setRight(TupleTree right) {
        this.right = right;
    }

    public String getSrl() {
        return srl;
    }

    public void setSrl(String srl) {
        this.srl = srl;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

}
