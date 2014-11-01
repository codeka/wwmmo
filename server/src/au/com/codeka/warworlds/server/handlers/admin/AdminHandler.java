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

import org.joda.time.DateTime;

import au.com.codeka.carrot.base.CarrotException;
import au.com.codeka.carrot.base.FileResourceLocater;
import au.com.codeka.carrot.interpret.CarrotInterpreter;
import au.com.codeka.carrot.interpret.InterpretException;
import au.com.codeka.carrot.lib.Filter;
import au.com.codeka.carrot.template.TemplateEngine;
import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.Session;

import com.google.common.base.Throwables;
import com.google.gson.JsonElement;

public class AdminHandler extends RequestHandler {
    private final Log log = new Log("AdminHandler");

    private static TemplateEngine sTemplateEngine;
    static {
        sTemplateEngine = new TemplateEngine();

        sTemplateEngine.getConfiguration().setResourceLocater(
            new FileResourceLocater(sTemplateEngine.getConfiguration(),
            new File(Configuration.i.getDataDirectory(), "tmpl").getAbsolutePath()));
        sTemplateEngine.getConfiguration().setEncoding("utf-8");

        sTemplateEngine.getConfiguration().getFilterLibrary().register(new NumberFilter());
        sTemplateEngine.getConfiguration().getFilterLibrary().register(new AttrEscapeFilter());
        sTemplateEngine.getConfiguration().getFilterLibrary().register(new LocalDateFilter());
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
            data.put("stack_trace", Throwables.getStackTraceAsString(e));
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
        } catch (CarrotException | IOException e) {
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

    protected void writeJson(JsonElement json) {
        getResponse().setContentType("text/json");
        getResponse().setHeader("Content-Type", "text/json; charset=utf-8");
        try {
            getResponse().getWriter().write(json.toString());
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
        public Object filter(Object object, CarrotInterpreter interpreter,
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
        public Object filter(Object object, CarrotInterpreter interpreter,
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
        public Object filter(Object object, CarrotInterpreter interpreter,
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
