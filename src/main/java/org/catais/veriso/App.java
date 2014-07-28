package org.catais.veriso;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.catais.veriso.interlis.IliReader;
import org.catais.veriso.postprocessing.PostProcessing;
import org.catais.veriso.utils.QgisUtils;
import org.catais.veriso.utils.Reindex;
import org.catais.veriso.utils.Utils;
import org.catais.veriso.utils.Vacuum;

import ch.interlis.ili2c.Ili2cException;
import ch.interlis.iox.IoxException;

/**
 * 1234
 *
 */
public class App {
    private static Logger logger = Logger.getLogger(App.class);

    public static void main(String[] args) {
        logger.setLevel(Level.DEBUG);

        String iniFileName = null;

        try {
            // configure log4j
            InputStream is = App.class.getResourceAsStream("log4j.properties");
            PropertyConfigurator.configure(is);

            logger.info("Start: " + new Date());

            // read properties file that contains all parameters
            iniFileName = (String) args[0];
            HashMap params = Utils.readProperties(iniFileName);

            logger.debug(params);

            boolean doVacuum = (Boolean) params.get("vacuum");
            boolean doReindex = (Boolean) params.get("reindex");
            boolean doQgisFiles = (Boolean) params.get("qgisFiles");

            logger.info("doVacuum: " + doVacuum);
            logger.info("doReindex: " + doReindex);
            logger.info("doQgisFiles: " + doQgisFiles);

            // Create json file for QGIS UI.
            // We DO NOT need to do this very often (once per model).
            // So it is not yet very comfortable.
            if (doQgisFiles) {
                String importModelName = (String) params.get("importModelName");
                String dbschema = (String) params.get("dbschema");

                QgisUtils.createTopicsTablesJson(importModelName, dbschema);
            }

            // do the import
            {
                IliReader iliReader = new IliReader(params);
                iliReader.startTransaction();
                iliReader.read();
                iliReader.commitTransaction();

                PostProcessing postProcessing = new PostProcessing(params);
                postProcessing.run();

                Vacuum vacuum = new Vacuum(params);
                vacuum.run();

                Reindex reindex = new Reindex(params);
                reindex.run();
            }

        } catch (IOException e) {
            // QgisUtils.createTopicsTablesJson
            // ...
            // IliReader
            e.printStackTrace();
            logger.error(e.getMessage());
        } catch (Ili2cException e) {
            // QgisUtils
            // IliReader
            e.printStackTrace();
            logger.error(e.getMessage());
        } catch (IoxException e) {
            // IliReader
            logger.error(e.getMessage());
        } catch (SQLException e) {
            // IliReader
            e.printStackTrace();
            logger.error(e.getMessage());
        } catch (ClassNotFoundException e) {
            // IliReader
            e.printStackTrace();
            logger.error(e.getMessage());
        } catch (IllegalArgumentException e) {
            // IliReader
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        logger.info("Import completed.");
    }
}
