package industries.vocht.viki.parser;

import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.SpacyToken;
import industries.vocht.viki.model.SpacyTokenList;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.semantics.Tuple;

import java.util.*;

/**
 * Created by peter on 11/12/16.
 *
 * convert a spacy parsed entity to an SRL tuple
 *
 */
public class SpacyToTuple {

    // tags that are sub-tags rather than top level SRL tags
    private HashSet<String> undesirable_srl_tags;

    // the lexicon
    private Lexicon lexicon;

    public SpacyToTuple(Lexicon lexicon) {
        this.lexicon = lexicon;
        undesirable_srl_tags = new HashSet<>();
        undesirable_srl_tags.add("prep");
        undesirable_srl_tags.add("punct");
        undesirable_srl_tags.add("conj");
        undesirable_srl_tags.add("ccomp");
        undesirable_srl_tags.add("cc");
        undesirable_srl_tags.add("det");
        undesirable_srl_tags.add("dative");
        undesirable_srl_tags.add("compound");
        undesirable_srl_tags.add("advmod");
        undesirable_srl_tags.add("amod");
        undesirable_srl_tags.add("attr");
    }

    /**
     * Convert a spacy document to a list of sentences
     * @param spacyDocument the document to convert
     * @return a list of sentences, or null if there is no valid spacy document
     */
    public List<Sentence> spaceyDocumentToSentenceList(SpacyTokenList spacyDocument) {
        if ( spacyDocument != null && spacyDocument.getSentence_list() != null ) {
            List<Sentence> sentenceList = new ArrayList<>();
            for ( int i = 0; i < spacyDocument.getSentence_list().length; i++ ) {
                SpacyToken[] s_sentence = spacyDocument.getSentence_list()[i];
                Sentence sentence = sentenceToToken(s_sentence);
                sentence.setTuple(sentenceToTuple(i, s_sentence));
                sentenceList.add(sentence);
            }
            if ( sentenceList.size() > 0 ) {
                return sentenceList;
            }
        }
        return null;
    }

    /**
     * convert an entire spacy document to a list of SRL tuples
     * @param spacyDocument the spacy document parse
     * @return null if document empty, or a list of Tuples representing the document
     */
    public List<Tuple> spacyDocumentToTupleList(SpacyTokenList spacyDocument) {
        if ( spacyDocument != null && spacyDocument.getSentence_list() != null ) {
            List<Tuple> tupleList = new ArrayList<>();
            for ( int i = 0; i < spacyDocument.getSentence_list().length; i++ ) {
                SpacyToken[] sentence = spacyDocument.getSentence_list()[i];
                Tuple tuple = sentenceToTuple(i, sentence);
                if ( tuple != null ) {
                    tupleList.add(tuple);
                }
            }
            if ( tupleList.size() > 0 ) {
                return tupleList;
            }
        }
        return null;
    }

    /**
     * convert a spacy sentence to a list of tokens
     * @param sentence the spacy sentence to tokenizer
     * @return a sentence
     */
    public Sentence sentenceToToken(SpacyToken[] sentence) {
        if ( sentence != null && sentence.length > 0 ) {
            List<Token> tokenList = new ArrayList<>();
            for (SpacyToken token : sentence ) {
                // apply any lexicon semantics to the token
                tokenList.add(setSemantic(token.convertToToken()));
            }
            return new Sentence(tokenList);
        }
        return null;
    }

    /**
     * Set the semantic on a token
     * @param token the token whose semantic to set
     * @return the original token
     */
    private Token setSemantic(Token token) {
        if ( token != null ) {
            List<Token> tokenList2 = lexicon == null ? null : lexicon.getByName(token.getText());
            if (tokenList2 != null) {
                for (Token lt : tokenList2) {
                    if (lt.getSemantic() != null) {
                        token.setSemantic(lt.getSemantic());
                        break;
                    }
                }
            }
        }
        return token;
    }

    /**
     * convert a single Spacy sentence to a tuple
     * @param sentence_id the id of the sentence (its index, zero based)
     * @param sentence the list of spacy tokens for this sentence
     * @return a Tuple, or null if the conversion failed
     */
    public Tuple sentenceToTuple(int sentence_id, SpacyToken[] sentence) {
        if ( sentence != null && sentence.length > 0 ) {
            // get the root
            SpacyToken root = null;
            for (SpacyToken token : sentence) {
                if (token.getDep().equals("ROOT")) {
                    root = token;
                    break;
                }
            }
            // root must be a non null verb type
            if (root != null && root.getTag().startsWith("VB")) {
                // setup a lookup table
                Map<Integer,List<SpacyToken>> lookup = new HashMap<>();
                for ( SpacyToken s_token : sentence ) {
                    if (s_token.getList() != null && s_token.getList().length > 0 ) {
                        int id = s_token.getList()[0];
                        if ( lookup.containsKey(id) ) {
                            lookup.get(id).add(s_token);
                        } else {
                            List<SpacyToken> tokenList = new ArrayList<>();
                            tokenList.add(s_token);
                            lookup.put(id, tokenList);
                        }
                    }
                }

                List<List<SpacyToken>> sentence_list = new ArrayList<>();
                List<List<SpacyToken>> remove_sentence_list = new ArrayList<>();
                for ( SpacyToken token : sentence ) {
                    List<SpacyToken> list = new ArrayList<>();
                    list.add(token);
                    sentence_list.add(list);
                    remove_sentence_list.add(new ArrayList<>());
                }
                boolean changing;
                do {
                    changing = false;
                    int j = 0;
                    for (List<SpacyToken> list : sentence_list) {
                        for (SpacyToken token : list ) {
                            if (token.getList() != null && token.getList().length > 0) {
                                int index = -1;
                                for (int v : token.getList()) {
                                    if ( v != root.getIndex() ) {
                                        index = v;
                                    } else {
                                        break;
                                    }
                                }
                                if (index != -1 && index < sentence_list.size()) {
                                    if (!sentence_list.get(index).contains(token)) {
                                        sentence_list.get(index).add(token);
                                        remove_sentence_list.get(j).add(token);
                                        changing = true;
                                    }
                                }
                            }
                        }
                        j += 1;
                    }
                } while (changing);

                int j = 0;
                for (List<SpacyToken> list : remove_sentence_list) {
                    List<SpacyToken> new_list = sentence_list.get(j);
                    for ( SpacyToken token : list ) {
                        new_list.remove(token);
                    }
                    j += 1;
                }

                // sort by offset
                Tuple tuple = new Tuple();
                tuple.add("root", root.getIndex(), setSemantic(root.convertToToken()));
                for ( List<SpacyToken> tokenList : sentence_list ) {
                    if (tokenList.size() > 0) {
                        String srlTag = tokenList.get(0).getDep();
                        // skip punctuation and root
                        if ((tokenList.size() == 1 && srlTag.equals("punct")) || tokenList.contains(root))
                            continue;
                        Collections.sort(tokenList);
                        int offset = tokenList.get(0).getIndex();
                        List<Token> convertedList = new ArrayList<>();
                        for (SpacyToken s_token : tokenList) {
                            Token token = setSemantic(s_token.convertToToken());
                            convertedList.add(token);
                            // improve the SRL tag?
                            if ( undesirable_srl_tags.contains(srlTag) &&
                                 !undesirable_srl_tags.contains(s_token.getDep()) ) {
                                srlTag = s_token.getDep();
                            }
                        }
                        tuple.add(srlTag, offset, convertedList);
                    }
                }
                tuple.setSentence_id(sentence_id);
                return tuple;
            } // if root != null
        } // if sentence != null && > 0
        return null;
    }


}
