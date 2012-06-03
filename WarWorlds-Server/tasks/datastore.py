"""datastore.py: Tasks that work directly with the datastore."""

from mapreduce import operation as op


def bulkdelete(entity):
  """This is a mapreduce function that deletes entities.

  For debugging purposes, we sometimes want to clear the data store of a certain kind of entity.
  This function can be used by the mapreduce framework to do just that. To be used carefully, since
  it can rack up quite a bit of quota!"""
  yield op.db.Delete(entity)

