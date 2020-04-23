package au.com.codeka.warworlds.server.html

import au.com.codeka.warworlds.server.handlers.HandlerServlet
import au.com.codeka.warworlds.server.handlers.Route
import au.com.codeka.warworlds.server.html.account.*
import au.com.codeka.warworlds.server.html.render.EmpireRendererHandler
import au.com.codeka.warworlds.server.html.render.PlanetRendererHandler
import au.com.codeka.warworlds.server.html.render.StarRendererHandler
import com.google.common.collect.Lists

/**
 * Servlet for working with top-level [RequestHandler]s.
 */
class HtmlServlet : HandlerServlet(Lists.newArrayList(
    Route("/accounts", AccountsHandler::class.java),
    Route("/accounts/associate", AccountAssociateHandler::class.java),
    Route("/accounts/verify", AccountVerifyHandler::class.java),
    Route("/accounts/patreon-begin", PatreonBeginHandler::class.java),
    Route("/accounts/connect-to-patreon", ConnectToPatreonHandler::class.java),
    Route("/login", LoginHandler::class.java),
    Route(
        "/render/star/(?<star>[0-9]+)/(?<width>[0-9]+)x(?<height>[0-9]+)/(?<bucket>[a-z]+dpi)\\.png$",
        StarRendererHandler::class.java),
    Route(
        "/render/planet/(?<star>[0-9]+)/(?<planet>[0-9]+)/(?<width>[0-9]+)x(?<height>[0-9]+)/(?<bucket>[a-z]+dpi)\\.png$",
        PlanetRendererHandler::class.java),
    Route(
        "/render/empire/(?<empire>[0-9]+)/(?<width>[0-9]+)x(?<height>[0-9]+)/(?<bucket>[a-z]+dpi)\\.png$",
        EmpireRendererHandler::class.java),
    Route("/(?<path>.*)", StaticFileHandler::class.java))) {
}