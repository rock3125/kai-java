### installation
Python 3.5+, setup your virtualenv
```
virtualenv -p /usr/bin/python3.6 .env
. .env/bin/activate
```
then install requirements and spaCy data
```
pip install -r requirements.txt
python -m spacy download en_core_web_sm
```

* production run without gevent (sync) can be run multi-threaded with w
  but takes x times the amount of RAM, better to use --threads

* CAREFUL: this app runs Tensorflow, --threads > 1 ruins the graph and the program will fail.

```
gunicorn --bind 0.0.0.0:8000 --timeout 120 --threads 1 server:app
```

* gevent is stricly single threaded only, --threads has no effects in gevent

```
gunicorn -k gevent --bind 0.0.0.0:8000 --timeout 120 server:app
```
