package au.com.codeka.warworlds.server.html

import au.com.codeka.warworlds.server.handlers.FileHandler

/**
 * Implementation of [FileHandler] that serves files out of html/static.
 */
class StaticFileHandler : FileHandler("data/html/static/")