package au.com.codeka.warworlds.server.handlers.pages;

import java.io.IOException;
import java.util.TreeMap;

import net.asfun.jangod.template.TemplateEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.OpenIdAuth;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;

public class HtmlPageHandler extends RequestHandler {
    private final Logger log = LoggerFactory.getLogger(HtmlPageHandler.class);

    private static TemplateEngine sTemplateEngine;
    static {
        sTemplateEngine = new TemplateEngine();

        String path = System.getProperty("au.com.codeka.warworlds.server.basePath");
        if (path == null) {
            path = HtmlPageHandler.class.getClassLoader().getResource("").getPath();
        }
        sTemplateEngine.getConfiguration().setWorkspace(path+"../data/tmpl");
    }

    @Override
    protected void get() throws RequestException {
        if (getCurrentUser(false) == null) {
            // if they're not authenticated yet, we'll have to redirect them to the authentication
            // page first.
            authenticate();
            return;
        }
        String path = getExtraOption() + getUrlParameter("path") + ".html";
        if (path.equals(getExtraOption()+".html")) {
            path = getExtraOption()+"index.html";
        }

        getResponse().setContentType("text/html");
        getResponse().setHeader("Content-Type", "text/html");
        try {
            TreeMap<String, Object> data = new TreeMap<String, Object>();
            getResponse().getWriter().write(sTemplateEngine.process(path, data));
        } catch (IOException e) {
            log.error("Error rendering template!", e);
        }
    }

    private void authenticate() {
        String url = OpenIdAuth.getAuthenticateUrl(getRequest());
        getResponse().setStatus(302);
        getResponse().addHeader("Location", url);
    }
}
