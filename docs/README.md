# Equinox Documentation Local deve

This is the documentation of the Eclipse Equinox Framework. This README shows you how to build and run the documentation locally, which is useful if you want to extend the site. 

It is based on a static page generated called Jekyll which is used by Github Pages.

## Local docs development

```
# Start the local Jekyll Dev-Server
./run.sh
```


## Setup local docs development environment (MacOS)

If you get an error like `activesupport-7.0.7.2 requires ruby version >= 2.7.0, which is incompatible with the current version, ruby 2.6.10p210` when executing `./run.sh` then consider doing the following:

- Install rbenv Ruby Version manager https://github.com/rbenv/rbenv e.g. via brew
- and then use it to install and use e.g. ruby 3.1.2

```
brew install rbenv
rbenv init
rbenv install 3.1.2
rbenv global 3.1.2
./run.sh
```

After a successfull start of `./run.sh` you see this:

```
Server address: http://127.0.0.1:4000
Server running... press ctrl-c to stop.
```


Open http://127.0.0.1:4000 in your browser to see the result while developing. 
The server does support hot-reload so you should see changes to `.md` files immediately without restart (there are a few exceptions). Checkout the [jekyll-docs](https://jekyllrb.com/docs/pages/) to get more into the details and features.

