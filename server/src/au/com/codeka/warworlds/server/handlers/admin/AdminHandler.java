package au.com.codeka.warworlds.server.handlers.admin;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.asfun.jangod.interpret.InterpretException;
import net.asfun.jangod.interpret.JangodInterpreter;
import net.asfun.jangod.lib.Filter;
import net.asfun.jangod.lib.FilterLibrary;
import net.asfun.jangod.template.TemplateEngine;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.Session;

public class AdminHandler extends RequestHandler {
    private final Logger log = LoggerFactory.getLogger(AdminHandler.class);

    private static TemplateEngine sTemplateEngine;
    static {
        sTemplateEngine = new TemplateEngine();

        sTemplateEngine.getConfiguration().setWorkspace(new File(getBasePath(), "data/tmpl").getAbsolutePath());
        sTemplateEngine.getConfiguration().setEncoding("utf-8");

        FilterLibrary.addFilter(new NumberFilter());
        FilterLibrary.addFilter(new AttrEscapeFilter());
        FilterLibrary.addFilter(new LocalDateFilter());
    }

    @Override
    public void onBeforeHandle() {
        if (!(this instanceof AdminLoginHandler)) {
            // if we're not the Login handler and we're not yet authed, auth now
            if (getSessionNoError() == null || !getSessionNoError().isAdmin()) {
                // if they're not authenticated yet, we'll have to redirect them to the authentication
                // page first.
                authenticate();
                return;
            }
        }
    }

    @Override
    protected void handleException(RequestException e) {
        try {
            TreeMap<String, Object> data = new TreeMap<String, Object>();
            data.put("exception", e);
            data.put("stack_trace", ExceptionUtils.getStackTrace(e));
            render("exception.html", data);
        } catch(Exception e2) {
            setResponseBody(e.getGenericError());
        }
    }

    protected void render(String path, Map<String, Object> data) {
        data.put("realm", getRealm());
        Session session = getSessionNoError();
        if (session != null) {
            data.put("logged_in_user", session.getActualEmail());
        }

        getResponse().setContentType("text/html");
        getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
        try {
            getResponse().getWriter().write(sTemplateEngine.process(path, data));
        } catch (IOException e) {
            log.error("Error rendering template!", e);
        }
    }

    protected void write(String text) {
        getResponse().setContentType("text/plain");
        getResponse().setHeader("Content-Type", "text/plain; charset=utf-8");
        try {
            getResponse().getWriter().write(text);;
        } catch (IOException e) {
            log.error("Error writing output!", e);
        }
    }

    protected void authenticate() {
        URI requestUrl = null;
        try {
            requestUrl = new URI(getRequestUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String finalUrl = requestUrl.getPath();
        String redirectUrl = requestUrl.resolve("/realms/"+getRealm()+"/admin/login").toString();
        try {
            redirectUrl += "?continue="+URLEncoder.encode(finalUrl, "utf-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen
        }

        redirect(redirectUrl);
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

    private static class AttrEscapeFilter implements Filter {
        @Override
        public String getName() {
            return "attr-escape";
        }

        @Override
        public Object filter(Object object, JangodInterpreter interpreter,
                             String... args) throws InterpretException {
            return object.toString().replace("\"", "&quot;")
                    .replace("'", "&squot;");
        }
    }

    private static class LocalDateFilter implements Filter {

        @Override
        public String getName() {
            return "local-date";
        }

        @Override
        public Object filter(Object object, JangodInterpreter interpreter,
                String... args) throws InterpretException {
            if (object instanceof DateTime) {
                DateTime dt = (DateTime) object;
                return String.format(Locale.ENGLISH,
                        "<script>(function() {" +
                          " var dt = new Date(\"%s\");" +
                          " +document.write(dt.toLocaleString());" +
                        "})();</script>", dt);
            }

            throw new InterpretException("Expected a DateTime.");
        }
    }
}
