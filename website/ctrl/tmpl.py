
import base64
import jinja2
import logging
import os
import re

import html2text


jinja = jinja2.Environment(loader=jinja2.FileSystemLoader(os.path.dirname(__file__)+'/../tmpl'))


def _filter_post_id(post):
  return post.key().id()
jinja.filters['post_id'] = _filter_post_id


def _filter_post_tags(post):
  return ', '.join(post.tags)
jinja.filters['post_tags'] = _filter_post_tags


def _filter_post_url(post):
  return '%04d/%02d/%s' % (post.posted.year, post.posted.month, post.slug)
jinja.filters['post_url'] = _filter_post_url


def _filter_post_full_url(post):
  return 'http://'+os.environ.get('HTTP_HOST')+'/blog/'+_filter_post_url(post)
jinja.filters['post_full_url'] = _filter_post_full_url


def _filter_post_date(post):
  return post.posted.strftime('%d %b %Y')
jinja.filters['post_date'] = _filter_post_date


def _filter_post_date_time(post):
  return post.posted.strftime('%d %b %Y %H:%M')
jinja.filters['post_date_time'] = _filter_post_date_time


def _filter_post_date_rss(post):
  return post.posted.strftime('%a, %d %b %Y %H:%M:%S GMT')
jinja.filters['post_date_rss'] = _filter_post_date_rss


def _filter_post_date_std(post):
  return post.posted.strftime('%Y-%m-%d %H:%M:%S')
jinja.filters['post_date_std'] = _filter_post_date_std


def _filter_date_std(date):
  return date.strftime("%Y-%m-%d %H:%M:%S")
jinja.filters['date_std'] = _filter_date_std


def _filter_post_date_editable(post):
  return post.posted.strftime('%y-%m-%d %H:%M')
jinja.filters['post_date_editable'] = _filter_post_date_editable


def _filter_post_extract(post):
  return post.html[0:500]+'...'
jinja.filters['post_extract'] = _filter_post_extract


def _filter_profile_shield(profile):
  if not profile:
    return "/img/blank.png"
  if profile.user and profile.user.email() == "dean@codeka.com.au":
    return "/img/hal-64.png"
  if not profile.empire_id:
    return "/img/blank.png"
  return "https://game.war-worlds.com/realms/beta/empires/"+str(profile.empire_id)+"/shield?final=1&size=64"
jinja.filters['profile_shield'] = _filter_profile_shield


def _filter_dump_json(obj):
  return json.dumps(obj)
jinja.filters['dump_json'] = _filter_dump_json


def _filter_forum_post_author(post):
  return post.user.email()
jinja.filters['forum_post_author'] = _filter_forum_post_author


def _filter_html_to_plain(html):
  return html2text.html2text(html)
jinja.filters['html_to_plain'] = _filter_html_to_plain


def _filter_html_tidy(html):
  html = re.sub(r"<p>[\s"+chr(0xa0)+r"]*</p>", " ", html, flags=re.IGNORECASE)
  return html
jinja.filters["html_tidy"] = _filter_html_tidy


def _filter_base64(str):
  return base64.b64encode(str)
jinja.filters["base64"] = _filter_base64


def _filter_number(n):
  return "{:,}".format(n)
jinja.filters["number"] = _filter_number


def getTemplate(tmpl_name):
  return jinja.get_template(tmpl_name)


def render(tmpl, data):
  return tmpl.render(data)
