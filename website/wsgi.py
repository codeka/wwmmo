from flask import Flask, redirect, request
from urllib.parse import urlparse, urlunparse

from ctrl import ctrl, tmpl
from handlers import handlers, blog, main

app = Flask(__name__)
app.config['CANONICAL_HOST'] = None
app.config['DATA_PATH'] = '/tmp'
app.config.from_envvar('CONFIG_FILE')

app.register_blueprint(ctrl)
app.register_blueprint(handlers)

def redirect_to_canonical():
  """Runs on every request and if you're on war-worlds.com, say, we'll redirect
     you to the www site."""
  if app.config['CANONICAL_HOST']:
    url = urlparse(request.url)
    if url.netloc != app.config['CANONICAL_HOST']:
      urlparts = list(url)
      urlparts[1] = app.config['CANONICAL_HOST']
      return redirect(urlunparse(urlparts), code=301)
app.before_request(redirect_to_canonical)
