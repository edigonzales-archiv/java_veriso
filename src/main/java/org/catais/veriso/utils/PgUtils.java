package org.catais.veriso.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.interlis.ili2c.metamodel.AbstractClassDef;
import ch.interlis.ili2c.metamodel.AreaType;
import ch.interlis.ili2c.metamodel.AssociationDef;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.CoordType;
import ch.interlis.ili2c.metamodel.Domain;
import ch.interlis.ili2c.metamodel.EnumerationType;
import ch.interlis.ili2c.metamodel.Model;
import ch.interlis.ili2c.metamodel.NumericType;
import ch.interlis.ili2c.metamodel.PolylineType;
import ch.interlis.ili2c.metamodel.PredefinedModel;
import ch.interlis.ili2c.metamodel.RoleDef;
import ch.interlis.ili2c.metamodel.SurfaceType;
import ch.interlis.ili2c.metamodel.Topic;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.ili2c.metamodel.TypeModel;
import ch.interlis.ili2c.metamodel.Viewable;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PgUtils {

    private static Logger logger = Logger.getLogger(PgUtils.class);

    private PgUtils() {
    };

    public static String getSqlCreateStatements(
            ch.interlis.ili2c.metamodel.TransferDescription td, String schema,
            String dbadmin, String dbuser, String epsg) throws IOException {
        logger.setLevel(Level.DEBUG);

        String grantUser = dbadmin;
        String selectUser = dbuser;

        StringBuffer enums = new StringBuffer("");
        StringBuffer tables = new StringBuffer("");

        Map CLASS_MAPPINGS = new HashMap();

        CLASS_MAPPINGS.put(String.class, "VARCHAR");
        CLASS_MAPPINGS.put(Boolean.class, "BOOLEAN");
        CLASS_MAPPINGS.put(Integer.class, "INTEGER");
        CLASS_MAPPINGS.put(Long.class, "BIGINT");
        CLASS_MAPPINGS.put(Float.class, "REAL");
        CLASS_MAPPINGS.put(Double.class, "DOUBLE PRECISION");
        CLASS_MAPPINGS.put(BigDecimal.class, "DECIMAL");
        CLASS_MAPPINGS.put(java.sql.Date.class, "DATE");
        CLASS_MAPPINGS.put(java.util.Date.class, "DATE");
        CLASS_MAPPINGS.put(java.sql.Time.class, "TIME");
        CLASS_MAPPINGS.put(java.sql.Timestamp.class, "TIMESTAMP");

        Map GEOM_TYPE_MAP = new HashMap();

        GEOM_TYPE_MAP.put("GEOMETRY", Geometry.class);
        GEOM_TYPE_MAP.put("POINT", Point.class);
        GEOM_TYPE_MAP.put("LINESTRING", LineString.class);
        GEOM_TYPE_MAP.put("POLYGON", Polygon.class);
        GEOM_TYPE_MAP.put("MULTIPOINT", MultiPoint.class);
        GEOM_TYPE_MAP.put("MULTILINESTRING", MultiLineString.class);
        GEOM_TYPE_MAP.put("MULTIPOLYGON", MultiPolygon.class);
        GEOM_TYPE_MAP.put("GEOMETRYCOLLECTION", GeometryCollection.class);

        Map GEOM_CLASS_MAPPINGS = new HashMap();

        Set keys = GEOM_TYPE_MAP.keySet();

        for (Iterator it = keys.iterator(); it.hasNext();) {
            String name = (String) it.next();
            Class geomClass = (Class) GEOM_TYPE_MAP.get(name);
            GEOM_CLASS_MAPPINGS.put(geomClass, name);
        }

        tables.append("-------------- Create schema --------------\n\n");
        tables.append("CREATE SCHEMA " + schema + " AUTHORIZATION " + grantUser
                + ";\n");
        tables.append("GRANT ALL ON SCHEMA " + schema + " TO " + grantUser
                + ";\n");
        tables.append("GRANT USAGE ON SCHEMA " + schema + " TO " + selectUser
                + ";\n\n");

        Iterator modeli = td.iterator();
        while (modeli.hasNext()) {
            Object mObj = modeli.next();
            if (mObj instanceof Model) {
                Model model = (Model) mObj;
                if (model instanceof TypeModel) {
                    continue;
                }
                if (model instanceof PredefinedModel) {
                    Iterator topici = model.iterator();
                    while (topici.hasNext()) {
                        Object tObj = topici.next();
                        if (tObj instanceof Domain) {
                            Type domainType = ((Domain) tObj).getType();
                            if (domainType instanceof EnumerationType) {
                                EnumerationType enumerationType = (EnumerationType) domainType;
                                ch.interlis.ili2c.metamodel.Enumeration enumerations = (ch.interlis.ili2c.metamodel.Enumeration) enumerationType
                                        .getConsolidatedEnumeration();

                                String enumName = "enum__"
                                        + model.getName().toLowerCase()
                                        + "_"
                                        + ((Domain) tObj).getName()
                                                .toLowerCase();

                                enums.append("\n");
                                enums.append("CREATE TABLE " + schema + "."
                                        + enumName);
                                enums.append("\n");
                                enums.append("(");
                                enums.append("\n ");
                                enums.append("ogc_fid SERIAL PRIMARY KEY, \n ");
                                enums.append("code INTEGER, \n ");
                                enums.append("code_txt VARCHAR \n ");
                                enums.append(")\n");
                                enums.append("WITH (OIDS=FALSE);\n");
                                enums.append("ALTER TABLE " + schema + "."
                                        + enumName + " OWNER TO " + grantUser
                                        + ";\n");
                                enums.append("GRANT ALL ON " + schema + "."
                                        + enumName + " TO " + grantUser + ";\n");
                                enums.append("GRANT SELECT ON " + schema + "."
                                        + enumName + " TO " + selectUser
                                        + ";\n\n");

                                ArrayList ev = new ArrayList();
                                ch.interlis.iom_j.itf.ModelUtilities
                                        .buildEnumList(
                                                ev,
                                                "",
                                                ((EnumerationType) domainType)
                                                        .getConsolidatedEnumeration());

                                for (int i = 0; i < ev.size(); i++) {
                                    String sql = "INSERT INTO " + schema + "."
                                            + enumName
                                            + "(code, code_txt) VALUES (" + i
                                            + ", '" + ev.get(i) + "');\n";
                                    enums.append(sql);
                                }
                            }
                        }
                    }
                    continue;
                }
                Iterator topici = model.iterator();
                while (topici.hasNext()) {
                    Object tObj = topici.next();
                    if (tObj instanceof Domain) {
                        Type domainType = ((Domain) tObj).getType();
                        if (domainType instanceof EnumerationType) {
                            EnumerationType enumerationType = (EnumerationType) domainType;
                            ch.interlis.ili2c.metamodel.Enumeration enumerations = (ch.interlis.ili2c.metamodel.Enumeration) enumerationType
                                    .getConsolidatedEnumeration();

                            String enumName = "enum__"
                                    + model.getName().toLowerCase() + "_"
                                    + ((Domain) tObj).getName().toLowerCase();

                            enums.append("\n");
                            enums.append("CREATE TABLE " + schema + "."
                                    + enumName);
                            enums.append("\n");
                            enums.append("(");
                            enums.append("\n ");
                            enums.append("ogc_fid SERIAL PRIMARY KEY, \n ");
                            enums.append("code INTEGER, \n ");
                            enums.append("code_txt VARCHAR \n ");
                            enums.append(")\n");
                            enums.append("WITH (OIDS=FALSE);\n");
                            enums.append("ALTER TABLE " + schema + "."
                                    + enumName + " OWNER TO " + grantUser
                                    + ";\n");
                            enums.append("GRANT ALL ON " + schema + "."
                                    + enumName + " TO " + grantUser + ";\n");
                            enums.append("GRANT SELECT ON " + schema + "."
                                    + enumName + " TO " + selectUser + ";\n\n");

                            ArrayList ev = new ArrayList();
                            ch.interlis.iom_j.itf.ModelUtilities.buildEnumList(
                                    ev, "", ((EnumerationType) domainType)
                                            .getConsolidatedEnumeration());

                            for (int i = 0; i < ev.size(); i++) {
                                String sql = "INSERT INTO " + schema + "."
                                        + enumName
                                        + "(code, code_txt) VALUES (" + i
                                        + ", '" + ev.get(i) + "');\n";
                                enums.append(sql);
                            }
                        }
                    }

                    if (tObj instanceof Topic) {
                        Topic topic = (Topic) tObj;

                        Iterator iter = topic.iterator();

                        while (iter.hasNext()) {
                            Object obj = iter.next();

                            if (obj instanceof Domain) {
                                Type domainType = ((Domain) obj).getType();
                                if (domainType instanceof EnumerationType) {
                                    EnumerationType enumerationType = (EnumerationType) domainType;
                                    ch.interlis.ili2c.metamodel.Enumeration enumerations = (ch.interlis.ili2c.metamodel.Enumeration) enumerationType
                                            .getConsolidatedEnumeration();

                                    String enumName = "enum__"
                                            + topic.getName().toLowerCase()
                                            + "_"
                                            + ((Domain) obj).getName()
                                                    .toLowerCase();

                                    enums.append("\n");
                                    enums.append("CREATE TABLE " + schema + "."
                                            + enumName);
                                    enums.append("\n");
                                    enums.append("(");
                                    enums.append("\n ");
                                    enums.append("ogc_fid SERIAL PRIMARY KEY, \n ");
                                    enums.append("code INTEGER, \n ");
                                    enums.append("code_txt VARCHAR \n ");
                                    enums.append(")\n");
                                    enums.append("WITH (OIDS=FALSE);\n");
                                    enums.append("ALTER TABLE " + schema + "."
                                            + enumName + " OWNER TO "
                                            + grantUser + ";\n");
                                    enums.append("GRANT ALL ON " + schema + "."
                                            + enumName + " TO " + grantUser
                                            + ";\n");
                                    enums.append("GRANT SELECT ON " + schema
                                            + "." + enumName + " TO "
                                            + selectUser + ";\n\n");

                                    ArrayList ev = new ArrayList();
                                    ch.interlis.iom_j.itf.ModelUtilities
                                            .buildEnumList(
                                                    ev,
                                                    "",
                                                    ((EnumerationType) domainType)
                                                            .getConsolidatedEnumeration());

                                    for (int i = 0; i < ev.size(); i++) {
                                        String sql = "INSERT INTO " + schema
                                                + "." + enumName
                                                + "(code, code_txt) VALUES ("
                                                + i + ", '" + ev.get(i)
                                                + "');\n";
                                        enums.append(sql);
                                    }
                                }
                            }

                            if (obj instanceof Viewable) {
                                Viewable v = (Viewable) obj;

                                if (isPureRefAssoc(v)) {
                                    continue;
                                }
                                String className = v.getScopedName(null);

                                // Array für btree Index
                                ArrayList btree_idx = new ArrayList();

                                String tableName = (className
                                        .substring(className.indexOf(".") + 1))
                                        .replace(".", "_").toLowerCase();

                                tables.append("-------------- New Table --------------\n\n");

                                tables.append("CREATE TABLE " + schema + "."
                                        + tableName);
                                tables.append("\n");
                                tables.append("(");
                                tables.append("\n ");
                                tables.append("ogc_fid SERIAL PRIMARY KEY, \n ");
                                tables.append("tid VARCHAR, \n");

                                btree_idx.add("ogc_fid");
                                btree_idx.add("tid");

                                // Arrays werden gebraucht für Index
                                // und geometry columns inserts.
                                ArrayList geomName = new ArrayList();
                                ArrayList geomType = new ArrayList();
                                String srid = epsg;

                                java.util.List attrv = ch.interlis.iom_j.itf.ModelUtilities
                                        .getIli1AttrList((AbstractClassDef) v);
                                Iterator attri = attrv.iterator();

                                while (attri.hasNext()) {
                                    ch.interlis.ili2c.metamodel.ViewableTransferElement attrObj = (ch.interlis.ili2c.metamodel.ViewableTransferElement) attri
                                            .next();

                                    if (attrObj.obj instanceof AttributeDef) {
                                        AttributeDef attrdefObj = (AttributeDef) attrObj.obj;
                                        Type type = attrdefObj
                                                .getDomainResolvingAliases();

                                        String attrName = attrdefObj.getName()
                                                .toLowerCase();

                                        if (type instanceof PolylineType) {
                                            tables.append(" ");
                                            tables.append(attrName + " ");
                                            tables.append("geometry(LINESTRING, "
                                                    + epsg + "),\n");

                                            geomType.add("LINESTRING");
                                            geomName.add(attrName);
                                        } else if (type instanceof SurfaceType) {
                                            tables.append(" ");
                                            tables.append(attrName + " ");
                                            tables.append("geometry(POLYGON, "
                                                    + epsg + "),\n");

                                            geomType.add("POLYGON");
                                            geomName.add(attrName);
                                        } else if (type instanceof AreaType) {
                                            tables.append(" ");
                                            tables.append(attrName + " ");
                                            tables.append("geometry(POLYGON, "
                                                    + epsg + "),\n");

                                            geomType.add("POLYGON");
                                            geomName.add(attrName);
                                        } else if (type instanceof CoordType) {
                                            tables.append(" ");
                                            tables.append(attrName + " ");
                                            tables.append("geometry(POINT, "
                                                    + epsg + "),\n");

                                            geomType.add("POINT");
                                            geomName.add(attrName);
                                        } else if (type instanceof NumericType) {
                                            tables.append(" ");
                                            tables.append(attrName + " ");
                                            tables.append("DOUBLE PRECISION,\n");
                                        } else if (type instanceof EnumerationType) {
                                            tables.append(" ");
                                            tables.append(attrName + " ");
                                            tables.append("INTEGER,\n");

                                            // append also the text
                                            // representation of the enumeration
                                            if (true) {
                                                tables.append(" ");
                                                tables.append(attrName
                                                        + "_txt ");
                                                tables.append("VARCHAR,\n");
                                            }

                                            Type attrType = (Type) attrdefObj
                                                    .getDomain();
                                            if (attrType instanceof EnumerationType) {
                                                EnumerationType enumerationType = (EnumerationType) attrType;
                                                ch.interlis.ili2c.metamodel.Enumeration enumerations = (ch.interlis.ili2c.metamodel.Enumeration) enumerationType
                                                        .getConsolidatedEnumeration();

                                                String enumName = "enum__"
                                                        + tableName + "_"
                                                        + attrName;

                                                enums.append("\n");
                                                enums.append("CREATE TABLE "
                                                        + schema + "."
                                                        + enumName);
                                                enums.append("\n");
                                                enums.append("(");
                                                enums.append("\n ");
                                                enums.append("ogc_fid SERIAL PRIMARY KEY, \n ");
                                                enums.append("code INTEGER, \n ");
                                                enums.append("code_txt VARCHAR \n ");
                                                enums.append(")\n");
                                                enums.append("WITH (OIDS=FALSE);\n");
                                                enums.append("ALTER TABLE "
                                                        + schema + "."
                                                        + enumName
                                                        + " OWNER TO "
                                                        + grantUser + ";\n");
                                                enums.append("GRANT ALL ON "
                                                        + schema + "."
                                                        + enumName + " TO "
                                                        + grantUser + ";\n");
                                                enums.append("GRANT SELECT ON "
                                                        + schema + "."
                                                        + enumName + " TO "
                                                        + selectUser + ";\n\n");

                                                ArrayList ev = new ArrayList();
                                                ch.interlis.iom_j.itf.ModelUtilities
                                                        .buildEnumList(
                                                                ev,
                                                                "",
                                                                ((EnumerationType) attrType)
                                                                        .getConsolidatedEnumeration());

                                                for (int i = 0; i < ev.size(); i++) {
                                                    String sql = "INSERT INTO "
                                                            + schema
                                                            + "."
                                                            + enumName
                                                            + "(code, code_txt) VALUES ("
                                                            + i + ", '"
                                                            + ev.get(i)
                                                            + "');\n";
                                                    enums.append(sql);
                                                }
                                            }

                                        } else {
                                            tables.append(" ");
                                            tables.append(attrName + " ");
                                            tables.append("VARCHAR,\n");
                                        }
                                    }

                                    if (attrObj.obj instanceof RoleDef) {
                                        RoleDef roledefObj = (RoleDef) attrObj.obj;
                                        String roleDefName = roledefObj
                                                .getName().toLowerCase();
                                        tables.append(" " + roleDefName + " ");
                                        tables.append("VARCHAR,\n");

                                        btree_idx.add(roleDefName);
                                    }
                                }

                                if (false) {
                                    tables.append(" gem_bfs INTEGER,\n");
                                    tables.append(" los INTEGER,\n");
                                    tables.append(" lieferdatum DATE,\n");
                                }

                                tables.deleteCharAt(tables.length() - 2);
                                tables.append(")\n");
                                tables.append("WITH (OIDS=FALSE);\n");
                                tables.append("ALTER TABLE " + schema + "."
                                        + tableName + " OWNER TO " + grantUser
                                        + ";\n");
                                tables.append("GRANT ALL ON " + schema + "."
                                        + tableName + " TO " + grantUser
                                        + ";\n");
                                tables.append("GRANT SELECT ON " + schema + "."
                                        + tableName + " TO " + selectUser
                                        + ";\n\n");

                                // create non-geometry index
                                for (int i = 0; i < btree_idx.size(); i++) {
                                    tables.append("CREATE INDEX idx_"
                                            + tableName + "_"
                                            + btree_idx.get(i) + "\n");
                                    tables.append("  ON " + schema + "."
                                            + tableName + "\n");
                                    tables.append("  USING btree\n");
                                    tables.append("  (" + btree_idx.get(i)
                                            + ");\n\n");
                                }

                                // additional index (gem_bfs...)
                                if (false) {
                                    tables.append("CREATE INDEX idx_"
                                            + tableName + "_gem_bfs\n");
                                    tables.append("  ON " + schema + "."
                                            + tableName + "\n");
                                    tables.append("  USING btree\n");
                                    tables.append("  (gem_bfs);\n\n");

                                    tables.append("CREATE INDEX idx_"
                                            + tableName + "_los\n");
                                    tables.append("  ON " + schema + "."
                                            + tableName + "\n");
                                    tables.append("  USING btree\n");
                                    tables.append("  (los);\n\n");
                                }

                                // create geometry index
                                for (int i = 0; i < geomName.size(); i++) {
                                    tables.append("CREATE INDEX idx_"
                                            + tableName + "_" + geomName.get(i)
                                            + "\n");
                                    tables.append("  ON " + schema + "."
                                            + tableName + "\n");
                                    tables.append("  USING gist\n");
                                    tables.append("  (" + geomName.get(i)
                                            + ");\n\n");
                                }
                            }
                        }
                    }
                }
            }
        }
        tables.append(enums);

        return tables.toString();
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