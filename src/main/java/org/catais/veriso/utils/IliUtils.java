package org.catais.veriso.utils;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.metamodel.AssociationDef;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.ilirepository.IliManager;
import ch.interlis.iom_j.itf.ItfReader;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.StartBasketEvent;

public class IliUtils {
    private static Logger logger = Logger.getLogger(IliUtils.class);

    public IliUtils() {

    }

    public static ch.interlis.ili2c.metamodel.TransferDescription compileModel(
            String importModelName) throws Ili2cException,
            IllegalArgumentException {
        ch.interlis.ili2c.metamodel.TransferDescription iliTd = null;

        IliManager manager = new IliManager();
        String repositories[] = new String[] { "http://www.catais.org/models/",
                "http://models.geo.admin.ch/" };
        manager.setRepositories(repositories);

        ArrayList modelNames = new ArrayList();
        modelNames.add(importModelName);

        Configuration config = manager.getConfig(modelNames, 1.0);
        iliTd = Ili2c.runCompiler(config);

        if (iliTd == null) {
            throw new IllegalArgumentException("INTERLIS compiler failed");
        }

        return iliTd;
    }

    public static String getModelNameFromItf(String itf) throws IoxException {
        String modelName = null;
        ItfReader ioxReader = null;

        try {
            ioxReader = new ch.interlis.iom_j.itf.ItfReader(new java.io.File(
                    itf));
            IoxEvent event = ioxReader.read();
            StartBasketEvent be = null;

            do {
                event = ioxReader.read();
                if (event instanceof StartBasketEvent) {
                    be = (StartBasketEvent) event;
                    break;
                }
            }

            while (!(event instanceof EndTransferEvent));
            ioxReader.close();
            ioxReader = null;

            if (be == null) {
                logger.error("no baskets in transfer-file");
                throw new IllegalArgumentException(
                        "no baskets in transfer-file");
            } else {
                String namev[] = be.getType().split("\\.");
                modelName = namev[0];
            }
        } finally {

            if (ioxReader != null) {
                ioxReader.close();
                ioxReader = null;
            }
        }
        return modelName;
    }

    public static boolean isPureRefAssoc(Viewable v) {
        if (!(v instanceof AssociationDef)) {
            return false;
        }
        AssociationDef assoc = (AssociationDef) v;
        // embedded and no attributes/embedded links?
        if (assoc.isLightweight() && !assoc.getAttributes().hasNext()
                && !assoc.getLightweightAssociations().iterator().hasNext()) {
            return true;
        }
        return false;
    }

}