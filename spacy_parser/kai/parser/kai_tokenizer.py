import string
from spacy.tokens import Doc


# whitespace tokenizer
class WhitespaceTokenizer(object):
    def __init__(self, nlp):
        self.vocab = nlp.vocab

    def __call__(self, text):
        words = text.split(' ')
        # All tokens 'own' a subsequent space character in this tokenizer
        spaces = [True] * len(words)
        return Doc(self.vocab, words=words, spaces=spaces)


# simple KAI compatible tokenizer
class KAITokenizer:
    def __init__(self):
        extraAZ_a = ["\u00c0", "\u00c1", "\u00c2", "\u00c3", "\u00c4", "\u00c5", "\u00c6",
                    "\u00e0", "\u00e1", "\u00e2", "\u00e3", "\u00e4", "\u00e5", "\u00e6", "\u0100", "\u0101"]
        extraAZ_c = ["\u00c7", "\u00e7", "\u0106", "\u0107", "\u0108", "\u0109", "\u010a", "\u010b", "\u010c", "\u010d"]
        extraAZ_e = ["\u00c8", "\u00c9", "\u00ca", "\u00cb", "\u00d8", "\u00d9", "\u00da", "\u00db", "\u00e8", "\u00e9",
                     "\u00ea", "\u00eb", "\u0112", "\u0113"]
        extraAZ_i = ["\u00cc", "\u00cd", "\u00ce", "\u00cf", "\u00ec", "\u00ed", "\u00ee", "\u00ef","\u012a", "\u012b"]
        extraAZ_o = ["\u00d2", "\u00d3", "\u00d4", "\u00d5", "\u00d6", "\u00d7", "\u00d8",
                    "\u00f2", "\u00f3", "\u00f4", "\u00f5", "\u00f6", "\u00f7", "\u00f8","\u014c", "\u014d"]
        extraAZ_u = ["\u00d9", "\u00da", "\u00db", "\u00dc", "\u00f9", "\u00fa", "\u00fb", "\u00fc","\u016a", "\u016b"]
        ws = [' ', '\t', '\r', '\n', '\u0008', '\ufeff', '\u303f', '\u3000', '\u2420', '\u2408', '\u202f',
              '\u205f', '\u2000', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2007', '\u2008',
              '\u2009', '\u200a', '\u200b']
        fs = ['\u002e', '\u06d4', '\u0701', '\u0702', '\ufe12', '\ufe52', '\uff0e', '\uff61']

        # setup a lookup for all the valid ASCII characters
        self.azLookup = {}
        self.wsLookup = {}
        self.fsLookup = {}
        for i in string.ascii_lowercase: self.azLookup[ord(i)] = True
        for i in string.ascii_uppercase: self.azLookup[ord(i)] = True
        for i in extraAZ_a: self.azLookup[ord(i)] = True
        for i in extraAZ_c: self.azLookup[ord(i)] = True
        for i in extraAZ_e: self.azLookup[ord(i)] = True
        for i in extraAZ_i: self.azLookup[ord(i)] = True
        for i in extraAZ_o: self.azLookup[ord(i)] = True
        for i in extraAZ_u: self.azLookup[ord(i)] = True
        for i in ws: self.wsLookup[ord(i)] = True
        for i in fs: self.fsLookup[ord(i)] = True

    # return true if ch is a white space character
    def is_white_space(self, ch):
        return ord(ch) in self.wsLookup

    def is_full_stop(self, ch):
        return ord(ch) in self.fsLookup

    def tokenize(self, text):
        words = []
        spaces = []
        index = 0
        while index < len(text):
            ch = text[index]

            # a..z A..Z together
            curr = ""
            active = False
            while index < len(text) and ord(ch) in self.azLookup:
                curr += ch
                index += 1
                active = True
                if index < len(text):
                    ch = text[index]

            if active:
                words.append(curr)
                spaces.append(False)
                continue

            # white spaces
            active = False
            while index < len(text) and self.is_white_space(ch):
                index += 1
                active = True
                if index < len(text):
                    ch = text[index]

            if active:
                words.append(' ')
                spaces.append(True)
                continue

            # full stops
            active = False
            while index < len(text) and self.is_full_stop(ch):
                index += 1
                words.append('.')
                spaces.append(False)
                active = True
                if index < len(text):
                    ch = text[index]

            if active:
                continue

            # numbers together
            curr = ""
            active = False
            while index < len(text) and '0' <= ch <= '9':
                curr += ch
                index += 1
                active = True
                if index < len(text):
                    ch = text[index]

            if active:
                words.append(curr)
                spaces.append(False)
                continue

            # all other items individually
            words.append(ch)
            spaces.append(False)
            index += 1

        return words, spaces


# spacy's use of tokenizer
class KAISpacyTokenizer(object):
    def __init__(self, nlp):
        self.vocab = nlp.vocab
        self.tokenizer = KAITokenizer()

    def __call__(self, text):
        words, spaces = self.tokenizer.tokenize(text)
        # All tokens 'own' a subsequent space character in this tokenizer
        return Doc(self.vocab, words=words, spaces=spaces)


