package au.com.codeka.warworlds.server.handlers.pages;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.OpenIdAuth;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;

import net.asfun.jangod.template.TemplateEngine;
import net.asfun.jangod.interpret.InterpretException;
import net.asfun.jangod.interpret.JangodInterpreter;
import net.asfun.jangod.lib.Filter;
import net.asfun.jangod.lib.FilterLibrary;

public class BasePageHandler extends RequestHandler {
    private final Logger log = LoggerFactory.getLogger(BasePageHandler.class);

    private static TemplateEngine sTemplateEngine;
    static {
        sTemplateEngine = new TemplateEngine();

        String path = System.getProperty("au.com.codeka.warworlds.server.basePath");
        if (path == null) {
            path = HtmlPageHandler.class.getClassLoader().getResource("").getPath();
        }
        sTemplateEngine.getConfiguration().setWorkspace(path+"../data/tmpl");

        FilterLibrary.addFilter(new NumberFilter());
    }

    protected void render(String path, Map<String, Object> data) {
        getResponse().setContentType("text/html");
        getResponse().setHeader("Content-Type", "text/html");
        try {
            getResponse().getWriter().write(sTemplateEngine.process(path, data));
        } catch (IOException e) {
            log.error("Error rendering template!", e);
        }
    }

    protected boolean isAdmin() throws RequestException {
        if (getSession(false) == null || !getSession(false).isAdmin()) {
            // if they're not authenticated yet, we'll have to redirect them to the authentication
            // page first.
            authenticate();
            return false;
        }

        return true;
    }

    protected void authenticate() throws RequestException {
        String url = OpenIdAuth.getAuthenticateUrl(getRequest());
        getResponse().setStatus(302);
        getResponse().addHeader("Location", url);
    }

    private static class NumberFilter implements Filter {
        private static DecimalFormat sFormat = new DecimalFormat("#,##0");

        @Override
        public String getName() {
            return "number";
        }

        @Override
        public Object filter(Object object, JangodInterpreter interpreter,
                             String... args) throws InterpretException {
            if (object == null) {
                return object;
            }

            if (object instanceof Integer) {
                int n = (int) object;
                return sFormat.format(n);
            }
            if (object instanceof Long) {
                long n = (long) object;
                return sFormat.format(n);
            }
            if (object instanceof Float) {
                float n = (float) object;
                return sFormat.format(n);
            }
            if (object instanceof Double) {
                double n = (double) object;
                return sFormat.format(n);
            }

            throw new InterpretException("Expected a number.");
        }
        
    }
}
