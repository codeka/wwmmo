package au.com.codeka.warworlds.server.handlers.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import proguard.obfuscate.MappingProcessor;
import proguard.obfuscate.MappingReader;
import au.com.codeka.warworlds.server.RequestException;

/** Handler for de-obfuscating stack traces. */
public class AdminDebugRetraceHandler extends AdminHandler {
    private final Logger log = LoggerFactory.getLogger(AdminDebugRetraceHandler.class);

    @Override
    protected void post() throws RequestException {
        if (!isAdmin()) {
            return;
        }

        getResponse().setHeader("Content-Type", "text/plain");

        if (!getRequest().getHeader("Content-Type").equals("text/plain")) {
            throw new RequestException(400, "Expected text/plain request body.");
        }
        String version = getRequest().getParameter("version");
        version = version.substring(version.lastIndexOf('.') + 1);

        File mappingFile = new File(getBasePath(), "data/proguard/mapping-" + version + ".txt");
        if (!mappingFile.isFile()) {
            try {
                IOUtils.copy(getRequest().getInputStream(), getResponse().getOutputStream());
            } catch (Exception e) {
                throw new RequestException(e);
            }
            return;
        }

        StackTrace stackTrace = new StackTrace();
        try {
            stackTrace.read(new BufferedReader(new InputStreamReader(
                    getRequest().getInputStream())));

            MappingReader mappingReader = new MappingReader(mappingFile);
            mappingReader.pump(stackTrace);

            stackTrace.write(getResponse().getWriter());
        } catch(Exception e) {
            log.error("Uh oh!", e);
            throw new RequestException(e);
        }
    }

    private static class StackTrace implements MappingProcessor {
        private ArrayList<StackTraceLine> mLines = new ArrayList<StackTraceLine>();

        public void read(BufferedReader reader) throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                mLines.add(new StackTraceLine(line));
            }
        }

        public void write(PrintWriter writer) throws IOException {
            for (StackTraceLine line : mLines) {
                writer.println(line.toString());
            }
        }

        @Override
        public boolean processClassMapping(String unobfuscatedClassName,
                String obfuscatedClassName) {
            boolean processed = false;
            for (StackTraceLine line : mLines) {
                processed = processed || line.processClassMapping(unobfuscatedClassName,
                        obfuscatedClassName);
            }
            return processed;
        }

        @Override
        public void processFieldMapping(String unobfuscatedClassName, String fieldType,
                String unobfuscatedFieldName, String obfuscatedFieldName) {
            // ignore this, since stack traces don't contain fields.
        }

        @Override
        public void processMethodMapping(String unobfuscatedClassName, int firstLine, int lastLine,
                String returnType, String unobfuscatedMethodName, String unobfuscatedMethodArgs,
                String obfuscatedMethodName) {
            for (StackTraceLine line : mLines) {
                line.processMethodMapping(unobfuscatedClassName, firstLine, lastLine, returnType,
                        unobfuscatedMethodName, unobfuscatedMethodArgs, obfuscatedMethodName);
            }
        }
        
    }

    private static class StackTraceLine implements MappingProcessor {
        private String mLine;
        private String mObfuscatedClassName;
        private String mObfuscatedMethodName;
        private String mUnobfuscatedClassName;
        private ArrayList<String> mUnobfuscatedMethodNames = new ArrayList<String>();

        // matches any valid java identifier (see http://stackoverflow.com/questions/5205339)
        private static Pattern sPattern = Pattern.compile("([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+[\\p{L}_$][\\p{L}\\p{N}_$]*");

        public StackTraceLine(String line) {
            mLine = line;

            Matcher matcher = sPattern.matcher(line);
            while (matcher.matches()) {
                String fullMethodName = matcher.group();
                String className = fullMethodName.substring(0, fullMethodName.indexOf('.'));
                String methodName = fullMethodName.substring(fullMethodName.indexOf('.') + 1);
                if (mObfuscatedClassName == null
                        || className.length() > mObfuscatedClassName.length()) {
                    mObfuscatedClassName = className;
                    mObfuscatedMethodName = methodName;
                }
            }
        }

        @Override
        public String toString() {
            String line = mLine;
            if (mObfuscatedClassName != null) {
                String originalName = mObfuscatedClassName + "." + mObfuscatedMethodName;
                String realName = mUnobfuscatedClassName + ".";
                if (mUnobfuscatedMethodNames.size() == 1) {
                    realName += mUnobfuscatedMethodNames.get(0);
                } else {
                    realName += "(";
                    for (int i = 0; i < mUnobfuscatedMethodNames.size(); i++) {
                        if (i > 0) {
                            realName += "|";
                        }
                        realName += mUnobfuscatedMethodNames.get(i);
                    }
                    realName += ")";
                }
                line = line.replace(originalName, realName);
            }

            return line;
        }

        @Override
        public boolean processClassMapping(String unobfuscatedClassName,
                String obfuscatedClassName) {
            if (mObfuscatedClassName == null) {
                return false;
            }

            if (obfuscatedClassName.equals(mObfuscatedClassName)) {
                mUnobfuscatedClassName = unobfuscatedClassName;
                return true;
            }
            return false;
        }

        @Override
        public void processFieldMapping(String unobfuscatedClassName, String fieldType,
                String unobfuscatedFieldName, String obfuscatedFieldName) {
            // ignore this, since stack traces don't contain fields.
        }

        @Override
        public void processMethodMapping(String unobfuscatedClassName, int firstLine, int lastLine,
                String returnType, String unobfuscatedMethodName, String unobfuscatedMethodArgs,
                String obfuscatedMethodName) {
            if (mUnobfuscatedClassName == null) {
                return;
            }

            if (mUnobfuscatedClassName.equals(unobfuscatedClassName)
                    && mObfuscatedMethodName.equals(obfuscatedMethodName)) {
                mUnobfuscatedMethodNames.add(unobfuscatedMethodName);
            }
        }
    }
}
