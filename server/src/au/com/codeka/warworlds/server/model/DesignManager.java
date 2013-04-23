package au.com.codeka.warworlds.server.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import au.com.codeka.common.model.BaseDesignManager;
import au.com.codeka.common.model.DesignKind;

public class DesignManager extends BaseDesignManager {
    private String mBasePath;

    public static void setup(String basePath) {
        DesignManager.i = new DesignManager(basePath);
        DesignManager.i.setup();
    }

    private DesignManager(String basePath) {
        mBasePath = basePath;
    }

    @Override
    protected InputStream open(DesignKind designKind) throws IOException {
        String fileName = mBasePath;
        if (designKind == DesignKind.SHIP) {
            fileName += "../data/designs/ships.xml";
        } else {
            fileName += "../data/designs/buildings.xml";
        }
        return new FileInputStream(fileName);
    }
}
