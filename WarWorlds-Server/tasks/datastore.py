'''
Created on 11/02/2012

@author: dean@codeka.com.au
'''

from mapreduce import operation as op


def bulkdelete(entity):
  yield op.db.Delete(entity)


