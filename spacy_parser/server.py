#!/usr/bin/env python

import uuid
import os
import logging

from datetime import datetime

from flask import Flask
from flask import request
from flask_cors import CORS

# setup logging to file
logger = logging.getLogger("kai-parser")
handler = logging.FileHandler('/var/log/kai/parser.log')
handler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s %(message)s'))
logger.addHandler(handler)

import kai
from kai.nnet_wsd.wsd_nnet import NNetWSD
from kai.parser.summarizer import SumySummarizer
from kai.parser.parser import Parser, JsonSystem


# gunicorn --bind 0.0.0.0:9000 --timeout 120 --threads 1 server:app

# parser:   curl -H "Content-Type: plain/text" -X POST --data "@test.txt" http://localhost:9000/parse
#           curl -H "Content-Type: plain/text" -X POST --data "@test-packet.txt" http://localhost:9000/parse-package
#           tagger, sentence splitter, and wsd using TF

# summarizer:   curl -H "Content-Type: plain/text" -X POST --data "@test.txt" http://localhost:9000/summarize/5
#               LSA based find most important sentence(s) as requested


# setup a text summarizer
summarizer = SumySummarizer()

# setup a spaCy parser
parser = Parser()

# nnet setup for WSD
wsd_model_filename = kai.resource_filename('combined-nnet.bin')
wsd_label_filename = kai.resource_filename('combined-ts.labels.txt')
window_size = 25
if os.path.isfile(wsd_model_filename):
    logger.info("loading wsd model " + wsd_model_filename)
    wsd_nnet = NNetWSD(wsd_model_filename, wsd_label_filename)
else:
    logger.info("no wsd models found, disabling wsd")
    wsd_nnet = None

# temp directory for the system
temp_dir = "/tmp"

# file types allowed for upload
ALLOWED_EXTENSIONS = {'txt'}

# 10MB maximum upload size for text files
max_upload_size_in_bytes = 10 * 1024 * 1024

app = Flask(__name__)
CORS(app, resources={r"/parse/*": {"origins": "*"}})


# is a file allowed for upload?
def allowed_file(filename):
    return filename[-3:].lower() in ALLOWED_EXTENSIONS


# create a temp file with a given .suffix (suffix must have dot in its string
def get_temp_file(suffix):
    return os.path.join(temp_dir, str(uuid.uuid4()) + suffix)


@app.route('/')
def index():
    return "parser service layer"


# curl -H "Content-Type: plain/text" -X POST --data "@test.txt" http://localhost:9000/parse
@app.route('/parse', methods=['POST'])
def parse():
    t1 = datetime.now()
    text = parser.cleanup_text(request.data)
    sentence_list, token_list, num_tokens = parser.parse_document(text)

    # perform wsd on all the tokens/words of a sentence
    if wsd_nnet is not None:
        wsd_nnet.wsd(token_list, window_size)

    delta = datetime.now() - t1
    return JsonSystem().encode({"processing_time": int(delta.total_seconds() * 1000),
                                "sentence_list": sentence_list,
                                "num_tokens": num_tokens,
                                "num_sentences": len(sentence_list)
                                })


# curl -H "Content-Type: plain/text" -X POST --data "@test.txt" http://localhost:9000/summarize/5
@app.route('/summarize/<int:num_sentences>', methods=['POST'])
def summarize(num_sentences):
    t1 = datetime.now()
    text = parser.cleanup_text(request.data)
    sentence_list = summarizer.summarize(text, num_sentences)
    delta = datetime.now() - t1
    return JsonSystem().encode({"processing_time": int(delta.total_seconds() * 1000),
                                "sentence_list": sentence_list
                                })


# curl -H "Content-Type: plain/text" -X POST --data "@test-packet.txt" http://localhost:9000/parse-package
@app.route('/parse-package', methods=['POST'])
def parse_package():
    # split the system into its tags and text
    text = parser.cleanup_text(request.data)

    # parse the package
    parts = dict()
    index = 0
    i1 = str.find(text, "<<<!", index)
    i2 = str.find(text, "!>>>", index)
    while i1 >= 0 and i2 > i1:
        key = text[i1+4:i2]
        i1 = str.find(text, "<<<!", i2+4)  # there is a next one?
        if i1 == -1:
            end = len(text)
            sub_text = text[i2+4:end]
        else:
            end = i1
            sub_text = text[i2+4:end]
            i2 = str.find(text, "!>>>", i1)
        parts[key] = sub_text

    total_num_tokens = 0
    total_num_sentences = 0
    packet_list = []
    for key in parts.keys():
        t1 = datetime.now()
        sentence_list, token_list, num_tokens = parser.parse_document(parts[key])

        # perform wsd on all the tokens/words of a sentence
        if wsd_nnet is not None:
            wsd_nnet.wsd(token_list, window_size)

        delta = datetime.now() - t1
        packet_list.append({'metadata': key, 'spacyTokenList': {"processing_time": int(delta.total_seconds() * 1000),
                                             "sentence_list": sentence_list,
                                             "num_tokens": num_tokens,
                                             "num_sentences": len(sentence_list)}})
        total_num_tokens += num_tokens
        total_num_sentences += len(sentence_list)

    # packetList
    return JsonSystem().encode({"packetList": packet_list,
                                "num_tokens": total_num_tokens,
                                "num_sentences": total_num_sentences})


# curl -X POST -F "file=@p376_292.wav" http://localhost:9000/stt
@app.route('/parse-file', methods=['GET','POST'])
def stt():
    if request.method == 'POST':
        if len(request.files) == 0:
            raise ValueError('POST is missing the file upload')
        file = request.files['file']
        if file and allowed_file(file.filename):
            if request.content_length == 0 or request.content_length > max_upload_size_in_bytes:
                raise ValueError("message content zero or too large (MAX SIZE:" + str(max_upload_size_in_bytes) + ")")

            # create a temp file
            in_file = get_temp_file(file.filename[-4:].lower())
            file.save(in_file)
            with open(in_file) as reader:
                text_content = '\n'.join(reader.readlines())

            try:
                t1 = datetime.now()
                sentence_list, token_list, num_tokens = parser.parse_document(text_content)

                # perform wsd on all the tokens/words of a sentence
                if wsd_nnet is not None:
                    wsd_nnet.wsd(token_list, window_size)

                delta = datetime.now() - t1
                return JsonSystem().encode({"processing_time": int(delta.total_seconds() * 1000),
                                            "sentence_list": sentence_list,
                                            "num_tokens": num_tokens,
                                            "num_sentences": len(sentence_list)
                                            })

            finally:
                os.remove(in_file)
        else:
            raise ValueError("invalid file in file-upload")

    return '''
                <!doctype html>
                <title>upload text-file for parsing</title>
                <h2>upload text-file</h2>
                <form action="/parse-file" method=post enctype=multipart/form-data>
                  <p><input type=file name=file>
                     <input type=submit value=upload>
                </form>
           '''

# non gunicorn use - debug
if __name__ == "__main__":
    logger.info("running in dev test mode (not containered)")
    app.run(host="0.0.0.0",
            port=9000,
            debug=True,
            use_reloader=False)
