# web

This project is generated with [yo angular generator](https://github.com/yeoman/generator-angular)
version 0.12.1.

## Build & development

Run `grunt` or `grunt build` for building and `grunt serve` for preview.

## Testing

Running `grunt test` will run the unit tests with karma.



## To use an existing yeoman project:

## For the first time:

install node.js http://nodejs.org/ (if windows add it to PATH)
install grunt bower yeoman npm install -g yo bower grunt-cli
if using compass 
-- (if windows or linux) install ruby http://rubyinstaller.org/ (if windows add it to PATH)
-- change the gem source to http gem source -r http://rubygems.org (if SSL error)
-- install compass  gem install compass
Then:

npm install

bower install

Finally

grunt serve to launch the webapp

### installing nodejs, grunt, bower, yeoman, ruby and compass
```bash
sudo apt-get install nodejs
sudo apt-get install npm
sudo npm install -g n
sudo n stable
sudo npm install -g yo bower grunt-cli
sudo npm install -g gruntjs/grunt-contrib-imagemin jasmine-core grunt-karma karma karma-phantomjs-launcher phantomjs-prebuilt
sudo apt-get install ruby-full
sudo gem install compass
cd web/admin
npm install
bower install
```

#### errors with node js
Use NodeJs version 5, not 6 or later
check: node --version

to rollback to version 5:

```
curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.31.1/install.sh | bash
exit

nvm ls
nvm use 5
nvm install 5
nvm use 5
node --version
```

#### errors with imagemin
```bash
cd node_modules/grunt-contrib-imagemin
npm install imagemin@4.0.0
sudo chown -R `whoamo`. /home/`whoami`/.config
```

### Grunt commands for server
to serve and debug, will open browser
```bash
grunt serve
```
to build an optimized source
```bash
grunt build
```
