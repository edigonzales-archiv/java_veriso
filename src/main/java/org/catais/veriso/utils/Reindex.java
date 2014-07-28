package org.catais.veriso.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Reindex {
    private static Logger logger = Logger.getLogger(Reindex.class);

    private HashMap params = null;

    private String dbhost = null;
    private String dbport = null;
    private String dbname = null;
    private String dbschema = null;
    private String dbadmin = null;
    private String dbadminpwd = null;

    Connection conn = null;

    public Reindex(HashMap params) throws ClassNotFoundException, SQLException {
        logger.setLevel(Level.INFO);

        this.params = params;
        readParams();

        Class.forName("org.postgresql.Driver");
        this.conn = DriverManager.getConnection("jdbc:postgresql://" + dbhost
                + "/" + dbname, dbadmin, dbadminpwd);
    }

    public void run() throws SQLException {
        if (conn != null) {

            Statement s = null;
            Statement v = null;
            s = conn.createStatement();
            v = conn.createStatement();

            s.executeUpdate("SET work_mem TO '1GB';");
            s.executeUpdate("SET maintenance_work_mem TO '512MB';");

            String[] schemas = this.dbschema.split(",");
            for (int i = 0; i < schemas.length; i++) {

                ResultSet rs = null;
                rs = s.executeQuery("SELECT 'REINDEX TABLE "
                        + schemas[i]
                        + ".' || relname || ';' FROM pg_class JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace WHERE nspname = '"
                        + schemas[i] + "' AND relkind IN ('r');");

                logger.info("Reindexing tables from: " + schemas[i]);
                while (rs.next()) {
                    String sql = rs.getString(1);
                    logger.debug(sql);
                    int m = 0;
                    m = v.executeUpdate(sql);
                }
                rs.close();
                logger.info("Reindexing finished.");

            }

            s.executeUpdate("SET work_mem TO '1MB';");
            s.executeUpdate("SET maintenance_work_mem TO '16MB';");

            s.close();
            v.close();
            conn.close();
        }
    }

    private void readParams() {
        this.dbhost = (String) params.get("dbhost");
        logger.debug("dbhost: " + this.dbhost);
        if (this.dbhost == null) {
            throw new IllegalArgumentException("'dbhost' not set.");
        }

        this.dbport = (String) params.get("dbport");
        logger.debug("dbport: " + this.dbport);
        if (this.dbport == null) {
            throw new IllegalArgumentException("'dbport' not set.");
        }

        this.dbname = (String) params.get("dbname");
        logger.debug("dbport: " + this.dbname);
        if (this.dbname == null) {
            throw new IllegalArgumentException("'dbname' not set.");
        }

        this.dbschema = (String) params.get("dbschema");
        logger.debug("dbschema: " + this.dbschema);
        if (this.dbschema == null) {
            throw new IllegalArgumentException("'dbschema' not set.");
        }

        this.dbadmin = (String) params.get("dbadmin");
        logger.debug("dbadmin: " + this.dbadmin);
        if (this.dbadmin == null) {
            throw new IllegalArgumentException("'dbadmin' not set.");
        }

        this.dbadminpwd = (String) params.get("dbadminpwd");
        logger.debug("dbadminpwd: " + this.dbadminpwd);
        if (this.dbadminpwd == null) {
            throw new IllegalArgumentException("'dbadminpwd' not set.");
        }

    }
}
