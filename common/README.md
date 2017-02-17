# KAI neural network builder setup

## How do I get set up on DEV?

```
gradle clean build createJarDependencies -x test
scp common/build/jars/* user@remote:/path
scp common/build/libs/* user@remote:/path
scp common/src/rpm/*.sh user@remote:/path
ssh user@remote
cd path
chmod u+x *.sh
train_wsd_nnet.sh path_to_parsed.txt /output/path
```

* this system uses a pre-parsed training file (usually large-text.txt.parsed / 180GB RAW text) for WSD investigations.
