package org.catais.veriso.utils;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;

import ch.interlis.ili2c.metamodel.AreaType;
import ch.interlis.ili2c.metamodel.AssociationDef;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.CoordType;
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

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GtUtils {

    private static Logger logger = Logger.getLogger(GtUtils.class);

    private GtUtils() {
    };

    public static LinkedHashMap getFeatureTypesFromItfTransferViewables(
            ch.interlis.ili2c.metamodel.TransferDescription td, String epsg) {
        LinkedHashMap ret = new LinkedHashMap();
        Iterator modeli = td.iterator();
        while (modeli.hasNext()) {
            Object mObj = modeli.next();

            if (mObj instanceof Model) {
                Model model = (Model) mObj;
                if (model instanceof TypeModel) {
                    continue;
                }
                if (model instanceof PredefinedModel) {
                    continue;
                }
                Iterator topici = model.iterator();
                while (topici.hasNext()) {
                    Object tObj = topici.next();

                    if (tObj instanceof Topic) {
                        Topic topic = (Topic) tObj;
                        Iterator iter = topic.getViewables().iterator();

                        while (iter.hasNext()) {
                            Object obj = iter.next();

                            if (obj instanceof Viewable) {
                                Viewable v = (Viewable) obj;

                                if (isPureRefAssoc(v)) {
                                    continue;
                                }
                                String className = v.getScopedName(null);

                                SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                                typeBuilder.setName(className);
                                // typeBuilder.setNamespaceURI(
                                // "http://www.catais.org" );
                                typeBuilder.setSRS("EPSG:" + epsg);

                                typeBuilder.add("tid", String.class);

                                Iterator attri = v.getAttributesAndRoles2();

                                while (attri.hasNext()) {
                                    ch.interlis.ili2c.metamodel.ViewableTransferElement attrObj = (ch.interlis.ili2c.metamodel.ViewableTransferElement) attri
                                            .next();
                                    if (attrObj.obj instanceof AttributeDef) {
                                        AttributeDef attrdefObj = (AttributeDef) attrObj.obj;
                                        Type type = attrdefObj
                                                .getDomainResolvingAliases();

                                        if (type instanceof PolylineType) {
                                            typeBuilder.add(attrdefObj
                                                    .getName().toLowerCase(),
                                                    LineString.class);
                                        } else if (type instanceof SurfaceType) {
                                            String name = attrdefObj.getName();

                                            // Neuen SimpleFeatureTypeBuilder
                                            // für
                                            // Surface Helper-Table erstellen:
                                            // - tid
                                            // - foreign key, zB.
                                            // '_itf_ref_XXXXX (eigentlich egal)
                                            // - Key (Name) des Builders ist
                                            // Tabellenname + '_Attributname"
                                            // Dem 'originalen' (Main-Table)
                                            // Builder wird
                                            // eine Polygonklasse hinzugefügt.

                                            SimpleFeatureTypeBuilder typeBuilderRef = new SimpleFeatureTypeBuilder();
                                            typeBuilderRef.setName(className
                                                    + "_ " + name);
                                            // typeBuilderRef.setNamespaceURI(
                                            // "http://www.catais.org" );
                                            typeBuilderRef.setSRS("EPSG:"
                                                    + epsg);

                                            typeBuilderRef.add("tid",
                                                    String.class);
                                            typeBuilderRef.add("_itf_ref",
                                                    String.class);
                                            typeBuilderRef.add(
                                                    name.toLowerCase(),
                                                    LineString.class);

                                            if (true) {
                                                typeBuilderRef.add("gem_bfs",
                                                        Integer.class);
                                                typeBuilderRef.add("los",
                                                        Integer.class);
                                                typeBuilderRef.add(
                                                        "lieferdatum",
                                                        Date.class);
                                            }

                                            SimpleFeatureType featureTypeRef = typeBuilderRef
                                                    .buildFeatureType();
                                            ret.put(className + "_" + name,
                                                    featureTypeRef);

                                            typeBuilder.add(name.toLowerCase(),
                                                    Polygon.class);
                                        } else if (type instanceof AreaType) {
                                            String name = attrdefObj.getName();

                                            // Neuen SimpleFeatureTypeBuilder
                                            // für
                                            // Area Helper-Table erstellen.
                                            // Ähnlich
                                            // wie oben, nur dass im 'original'
                                            // Builder
                                            // zusätzliche eine Point-Geometrie
                                            // hinzugefügt werden muss.

                                            SimpleFeatureTypeBuilder typeBuilderRef = new SimpleFeatureTypeBuilder();
                                            typeBuilderRef.setName(className
                                                    + "_" + name);
                                            // typeBuilderRef.setNamespaceURI(
                                            // "http://www.catais.org" );
                                            typeBuilderRef.setSRS("EPSG:"
                                                    + epsg);

                                            typeBuilderRef.add("tid",
                                                    String.class);
                                            typeBuilderRef.add("_itf_ref",
                                                    String.class);
                                            typeBuilderRef.add(
                                                    name.toLowerCase(),
                                                    LineString.class);

                                            if (false) {
                                                typeBuilderRef.add("gem_bfs",
                                                        Integer.class);
                                                typeBuilderRef.add("los",
                                                        Integer.class);
                                                typeBuilderRef.add(
                                                        "lieferdatum",
                                                        Date.class);
                                            }

                                            SimpleFeatureType featureTypeRef = typeBuilderRef
                                                    .buildFeatureType();
                                            ret.put(className + "_" + name,
                                                    featureTypeRef);

                                            typeBuilder.add(name.toLowerCase()
                                                    + "_point", Point.class);
                                            typeBuilder.add(name.toLowerCase(),
                                                    Polygon.class);

                                        } else if (type instanceof CoordType) {
                                            typeBuilder.add(attrdefObj
                                                    .getName().toLowerCase(),
                                                    Point.class);
                                        } else if (type instanceof NumericType) {
                                            typeBuilder.add(attrdefObj
                                                    .getName().toLowerCase(),
                                                    Double.class);
                                        } else if (type instanceof EnumerationType) {
                                            typeBuilder.add(attrdefObj
                                                    .getName().toLowerCase(),
                                                    Integer.class);
                                            if (true) {
                                                typeBuilder.add(attrdefObj
                                                        .getName()
                                                        .toLowerCase()
                                                        + "_txt", String.class);
                                            }

                                        } else {
                                            typeBuilder.add(attrdefObj
                                                    .getName().toLowerCase(),
                                                    String.class);
                                        }
                                    }
                                    if (attrObj.obj instanceof RoleDef) {
                                        RoleDef roledefObj = (RoleDef) attrObj.obj;
                                        typeBuilder.add(roledefObj.getName()
                                                .toLowerCase(), String.class);
                                    }
                                }

                                if (false) {
                                    typeBuilder.add("gem_bfs", Integer.class);
                                    typeBuilder.add("los", Integer.class);
                                    typeBuilder.add("lieferdatum", Date.class);
                                }

                                SimpleFeatureType featureType = typeBuilder
                                        .buildFeatureType();

                                ret.put(className, featureType);
                            }
                        }
                    }
                }
            }
        }
        return ret;
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