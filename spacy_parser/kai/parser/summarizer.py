import logging
from sumy.parsers.plaintext import PlaintextParser
from sumy.nlp.tokenizers import Tokenizer
from sumy.summarizers.lsa import LsaSummarizer as Summarizer
from sumy.nlp.stemmers import Stemmer
from sumy.utils import get_stop_words

logger = logging.getLogger("kai-parser")

# setup sumy
language = 'english'
logger.info("setting up sumy the summarizer for " + language)
s_stemmer = Stemmer(language)
s_summarizer = Summarizer(s_stemmer)
s_summarizer.stop_words = get_stop_words(language)
logger.info("sumy setup done")


# little class wrapper around sumy
class SumySummarizer:
    # setup
    def __init__(self):
        self.language = language
        self.summarizer = s_summarizer

    # summary a text returning num_sentences, the top n-number of sentences for this document
    def summarize(self, text, num_sentences):
        parser = PlaintextParser.from_string(text, Tokenizer(self.language))
        sentence_list = []
        for sentence in self.summarizer(parser.document, num_sentences):
            sentence_list.append(sentence._text)
        return sentence_list
