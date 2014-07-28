package org.catais.veriso.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Utils {
    private static Logger logger = Logger.getLogger(Utils.class);

    public Utils() {

    }

    public static HashMap readProperties(String fileName)
            throws FileNotFoundException, IOException {
        logger.setLevel(Level.INFO);

        Properties properties = new Properties();
        BufferedInputStream stream = new BufferedInputStream(
                new FileInputStream(fileName));
        properties.load(stream);
        stream.close();

        HashMap params = new HashMap();

        // Postprocessing database
        String postprocessFilename = properties
                .getProperty("postprocessingDatabase");
        if (postprocessFilename != null) {
            params.put("postprocessingDatabase", postprocessFilename);
        } else {
            throw new IllegalArgumentException(
                    "postprocessingDatabase parameter not set.");
        }
        logger.debug("postprocessingDatabase: " + postprocessFilename);

        // Import parameters
        String epsg = properties.getProperty("epsg");
        if (epsg != null) {
            params.put("epsg", epsg.trim());
        } else {
            throw new IllegalArgumentException("epsg parameter not set.");
        }
        logger.debug("epsg: " + epsg);

        String importModelName = properties.getProperty("importModelName");
        if (importModelName != null) {
            params.put("importModelName", importModelName.trim());
        } else {
            throw new IllegalArgumentException(
                    "importModelName parameter not set.");
        }
        logger.debug("importModelName: " + importModelName);

        String itfFile = properties.getProperty("importItfFile");
        if (itfFile != null) {
            params.put("importItfFile", itfFile.trim());
        } else {
            throw new IllegalArgumentException(
                    "importItfFile parameter not set.");
        }
        logger.debug("Import Interlis File: " + itfFile);

        String enumText = properties.getProperty("enumerationText");
        boolean enumerationText = false;
        if (enumText != null) {
            enumerationText = Boolean.parseBoolean(enumText.trim());
        } else {
            throw new IllegalArgumentException(
                    "enumerationText parameter not set.");
        }
        params.put("enumerationText", enumerationText);
        logger.debug("Enumeration Text: " + enumerationText);

        String renumber = properties.getProperty("renumberTid");
        boolean renumberTid = false;
        if (renumber != null) {
            renumberTid = Boolean.parseBoolean(renumber.trim());
        } else {
            throw new IllegalArgumentException("renumberTid parameter not set.");
        }
        params.put("renumberTid", renumberTid);
        logger.debug("Renumber TID: " + renumberTid);

        String schemaOnlyProcess = properties.getProperty("schemaOnly");
        boolean schemaOnly = false;
        if (schemaOnlyProcess != null) {
            schemaOnly = Boolean.parseBoolean(schemaOnlyProcess.trim());
        } else {
            throw new IllegalArgumentException("schemaOnly parameter not set.");
        }
        params.put("schemaOnly", schemaOnly);
        logger.debug("schemaOnly: " + schemaOnly);

        String files4Qgis = properties.getProperty("qgisFiles");
        boolean qgisFiles = false;
        if (files4Qgis != null) {
            qgisFiles = Boolean.parseBoolean(files4Qgis.trim());
        } else {
            // throw new
            // IllegalArgumentException("renumberTid parameter not set.");
            qgisFiles = Boolean.FALSE;
        }
        params.put("qgisFiles", qgisFiles);
        logger.debug("qgisFiles: " + qgisFiles);

        // Database parameters
        String dbhost = properties.getProperty("dbhost");
        if (dbhost != null) {
            params.put("dbhost", dbhost.trim());
        } else {
            throw new IllegalArgumentException("dbhost parameter not set.");
        }
        logger.debug("dbhost: " + dbhost);

        String dbport = properties.getProperty("dbport");
        if (dbport != null) {
            params.put("dbport", dbport.trim());
        } else {
            throw new IllegalArgumentException("dbport parameter not set.");
        }
        logger.debug("dbport: " + dbport);

        String dbname = properties.getProperty("dbname");
        if (dbname != null) {
            params.put("dbname", dbname.trim());
        } else {
            throw new IllegalArgumentException("dbname parameter not set.");
        }
        logger.debug("dbname: " + dbname);

        String dbschema = properties.getProperty("dbschema");
        if (dbschema != null) {
            params.put("dbschema", dbschema.trim());
        } else {
            throw new IllegalArgumentException("dbschema parameter not set.");
        }
        logger.debug("dbschema: " + dbschema);

        String dbuser = properties.getProperty("dbuser");
        if (dbuser != null) {
            params.put("dbuser", dbuser.trim());
        } else {
            throw new IllegalArgumentException("dbuser parameter not set.");
        }
        logger.debug("dbuser: " + dbuser);

        String dbpwd = properties.getProperty("dbpwd");
        if (dbpwd != null) {
            params.put("dbpwd", dbpwd.trim());
        } else {
            throw new IllegalArgumentException("dbpwd parameter not set.");
        }
        logger.debug("dbpwd: " + dbpwd);

        String dbadmin = properties.getProperty("dbadmin");
        if (dbadmin != null) {
            params.put("dbadmin", dbadmin.trim());
        } else {
            throw new IllegalArgumentException("dbadmin parameter not set.");
        }
        logger.debug("dbadmin: " + dbadmin);

        String dbadminpwd = properties.getProperty("dbadminpwd");
        if (dbadminpwd != null) {
            params.put("dbadminpwd", dbadminpwd.trim());
        } else {
            throw new IllegalArgumentException("dbadminpwd parameter not set.");
        }
        logger.debug("dbadminpwd: " + dbadminpwd);

        // Maintenance
        String doVacuumProcess = properties.getProperty("vacuum");
        boolean doVacuum = false;
        if (doVacuumProcess != null) {
            doVacuum = Boolean.parseBoolean(doVacuumProcess.trim());
        } else {
            throw new IllegalArgumentException("vacuum parameter not set.");
        }
        params.put("vacuum", doVacuum);
        logger.debug("vaccum: " + doVacuum);

        String doReindexProcess = properties.getProperty("reindex");
        boolean doReindex = false;
        if (doReindexProcess != null) {
            doReindex = Boolean.parseBoolean(doReindexProcess.trim());
        } else {
            throw new IllegalArgumentException("reindex parameter not set.");
        }
        params.put("reindex", doReindex);
        logger.debug("reindex: " + doReindex);

        return params;
    }
}
