# README

KAI knowledge management system.  A search engine/document management system written in Java, using Apache Cassandra (3.9) and Hazelcast and the spaCy parser.  Uses GRADLE for build.  Missing some data-files I still need to upload due to their size.

## How do I get set up on DEV?

* building the system
* see below for installing node and others
* cassandra authorization setup
* swagger use / see if services are up


## How do I get set up on a server?

* build the system
```$xslt
gradle clean build createJarDependencies -x test

cd web/admin
grunt build

cd web/search
grunt build

ansible-playbook ansible/setup-cluster-one.yml --extra-vars "web_port=8085 server_port=10080 rf=1 external_name=peter-vm" 
```

### installing nodejs, grunt, bower, yeoman, ruby and compass on MINT and Ubuntu
```bash
sudo apt-get install -y nodejs
sudo apt-get install -y npm
sudo apt-get install -y ruby-full
sudo gem install compass
sudo npm install -g n
sudo n stable
sudo npm install -g yo bower grunt-cli
sudo npm install -g gruntjs/grunt-contrib-imagemin jasmine-core grunt-karma karma karma-phantomjs-launcher phantomjs-prebuilt
cd web/admin
npm install
bower install
```

#### errors with NodeJS version > 5
Use NodeJS version 5, not 6 or later
```
node --version
```

to rollback to version 5:

```
curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.31.1/install.sh | bash
exit

nvm ls
nvm install 5
nvm use 5
node --version
```

#### errors with imagemin
```bash
cd node_modules/grunt-contrib-imagemin
npm install imagemin@4.0.0
sudo chown -R peter. /home/peter/.config
```

### Grunt commands for server
```bash
cd resources/web-app
```
to serve and debug, will open browser
```bash
grunt serve
```
to build an optimized source
```bash
grunt build
```

### cassandra auth setup (not required for dev)

edit cassandra.yaml and change the authenticator

#### authenticator: AllowAllAuthenticator
authenticator: PasswordAuthenticator
```bash
cqlsh -u cassandra -p cassandra
```

#### set new super user

```SQL
CREATE USER viki with PASSWORD 'password' SUPERUSER;
```

#### logout, log back in as new user and remove cassandra default
```SQL
DROP USER cassandra;
```

### using swagger

* to get the swagger.json from the api, use the url [http://localhost:8085/api-doc/swagger.json]
* to use the full web interface [http://localhost:8085/swagger/]


### Ansible

Ansible is a powerful tool for deploying and managing servers in clusters.  Ansible will ensure
that your machines are up-to-date as well as have all the required software and changes.
See: http://docs.ansible.com/

Each of the target machines is expected to be a Centos 7 machine (any version).  Ansible will
install all other necessary software needed for running this service, including the auto OS
updater (yum-cron).


* installing Ansible on Ubuntu
```
sudo apt-get install software-properties-common
sudo apt-add-repository ppa:ansible/ansible
sudo apt-get update
sudo apt-get install ansible
```

* setup Ansible on Ubuntu
    put into:
```
/etc/ansible/hosts
```
    and
```
~/ansible_hosts
```

    the values
```
[kai-single-node]
ip-address-or-hostname-1
ip-address-or-hostname-2
```

    and many more if you need them

* setup the ssh certificates; run as root:
```
ssh-keygen -t rsa -b 4096
```

    on each of these machines if not already done, then setup yourself on these hosts in /root/.ssh/authorized_keys
    to do this use: 
```
ssh-copy-id root@ip-address-or-hostname-1, etc.
```

* then test these servers using
```
ansible all -m ping -u root
```


* before running this script, FIRST BUILD the project
```
mvn clean install deploy
```

* run this playbook using
```
ansible-playbook ansible/setup.yml
```

* once the playbook has finished successfully you can test the server by browsing to
```
http://ip-address-or-hostname-1:8085/api/swagger/
```

