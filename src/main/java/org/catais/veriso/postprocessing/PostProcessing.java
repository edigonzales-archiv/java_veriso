package org.catais.veriso.postprocessing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class PostProcessing {
    private static Logger logger = Logger.getLogger(PostProcessing.class);

    HashMap params = null;

    private String postprocessingDatabase = null;
    private String dbhost = null;
    private String dbport = null;
    private String dbname = null;
    private String dbschema = null;
    private String dbuser = null;
    private String dbpwd = null;
    private String dbadmin = null;
    private String dbadminpwd = null;
    private String epsg = null;

    Connection pgconn = null;
    Connection sqconn = null;

    public PostProcessing(HashMap params) throws ClassNotFoundException,
            SQLException {
        logger.setLevel(Level.INFO);

        this.params = params;
        readParams();

        Class.forName("org.postgresql.Driver");
        pgconn = DriverManager.getConnection("jdbc:postgresql://" + dbhost
                + "/" + dbname, dbadmin, dbadminpwd);

        Class.forName("org.sqlite.JDBC");
        sqconn = DriverManager.getConnection("jdbc:sqlite:"
                + postprocessingDatabase);
    }

    public void run() throws FileNotFoundException, IOException, SQLException {
        if (pgconn != null && sqconn != null) {
            logger.info("Start postprocessing...");

            Statement v = null;
            Statement s = null;

            v = sqconn.createStatement();
            s = pgconn.createStatement();

            s.executeUpdate("SET work_mem TO '1GB';");
            s.executeUpdate("SET maintenance_work_mem TO '512MB';");

            ResultSet rv = null;
            rv = v.executeQuery("SELECT * FROM tables;");
            while (rv.next()) {
                String sql_tmp = rv.getString(rv.findColumn("sql_query"));
                String sql = sql_tmp.replace("$$DBSCHEMA", dbschema)
                        .replace("$$USER", dbuser).replace("$$ADMIN", dbadmin)
                        .replace("$$EPSG", epsg);
                logger.debug(sql);
                int m = 0;
                m = s.executeUpdate(sql);
            }
            rv.close();

            rv = v.executeQuery("SELECT * FROM views;");
            while (rv.next()) {
                String sql_tmp = rv.getString(rv.findColumn("sql_query"));
                String sql = sql_tmp.replace("$$DBSCHEMA", dbschema)
                        .replace("$$USER", dbuser).replace("$$ADMIN", dbadmin)
                        .replace("$$EPSG", epsg);
                logger.debug(sql);
                int m = 0;
                m = s.executeUpdate(sql);
            }
            rv.close();

            rv = v.executeQuery("SELECT * FROM inserts;");
            while (rv.next()) {
                String sql_tmp = rv.getString(rv.findColumn("sql_query"));
                String sql = sql_tmp.replace("$$DBSCHEMA", dbschema)
                        .replace("$$USER", dbuser).replace("$$ADMIN", dbadmin)
                        .replace("$$EPSG", epsg);
                logger.debug(sql);
                int m = 0;
                m = s.executeUpdate(sql);
            }
            rv.close();

            rv = v.executeQuery("SELECT * FROM updates;");
            while (rv.next()) {
                String sql_tmp = rv.getString(rv.findColumn("sql_query"));
                String sql = sql_tmp.replace("$$DBSCHEMA", dbschema)
                        .replace("$$USER", dbuser).replace("$$ADMIN", dbadmin)
                        .replace("$$EPSG", epsg);
                logger.debug(sql);
                int m = 0;
                m = s.executeUpdate(sql);
            }
            rv.close();

            s.executeUpdate("SET work_mem TO '1MB';");
            s.executeUpdate("SET maintenance_work_mem TO '16MB';");

            s.close();
            v.close();

            pgconn.close();
            sqconn.close();

            logger.info("End postprocessing.");
        }
    }

    private void readParams() {
        postprocessingDatabase = (String) params.get("postprocessingDatabase");
        if (postprocessingDatabase == null) {
            throw new IllegalArgumentException(
                    "postprocessingDatabase not set.");
        }

        dbhost = (String) params.get("dbhost");
        if (dbhost == null) {
            throw new IllegalArgumentException("'dbhost' not set.");
        }

        dbport = (String) params.get("dbport");
        if (dbport == null) {
            throw new IllegalArgumentException("'dbport' not set.");
        }

        dbname = (String) params.get("dbname");
        if (dbname == null) {
            throw new IllegalArgumentException("'dbname' not set.");
        }

        dbschema = (String) params.get("dbschema");
        if (dbschema == null) {
            throw new IllegalArgumentException("'dbschema' not set.");
        }

        dbuser = (String) params.get("dbuser");
        if (dbuser == null) {
            throw new IllegalArgumentException("'dbuser' not set.");
        }

        dbpwd = (String) params.get("dbpwd");
        if (dbpwd == null) {
            throw new IllegalArgumentException("'dbpwd' not set.");
        }

        dbadmin = (String) params.get("dbadmin");
        if (dbadmin == null) {
            throw new IllegalArgumentException("'dbadmin' not set.");
        }

        dbadminpwd = (String) params.get("dbadminpwd");
        if (dbadminpwd == null) {
            throw new IllegalArgumentException("'dbadminpwd' not set.");
        }

        epsg = (String) params.get("epsg");
        if (epsg == null) {
            epsg = "21781";
        }
    }
}