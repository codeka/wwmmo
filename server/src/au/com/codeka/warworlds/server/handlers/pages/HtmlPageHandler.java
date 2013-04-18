package au.com.codeka.warworlds.server.handlers.pages;

import java.io.IOException;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.asfun.jangod.template.TemplateEngine;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;

public class HtmlPageHandler extends RequestHandler {
    private final Logger log = LoggerFactory.getLogger(HtmlPageHandler.class);

    private static TemplateEngine sTemplateEngine;
    static {
        sTemplateEngine = new TemplateEngine();

        String path = HtmlPageHandler.class.getClassLoader().getResource("").getPath();
        sTemplateEngine.getConfiguration().setWorkspace(path+"../data/tmpl");
    }

    @Override
    protected void get() throws RequestException {
        String path = getExtraOption() + getUrlParameter("path") + ".html";
        if (path.equals("admin/.html")) {
            path = "admin/dashboard.html";
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
}
