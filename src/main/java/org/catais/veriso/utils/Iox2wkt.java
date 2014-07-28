package org.catais.veriso.utils;

import org.apache.log4j.Logger;

import ch.interlis.iom.IomConstants;
import ch.interlis.iom.IomObject;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.precision.SimpleGeometryPrecisionReducer;

/**
 * Methoden zur Umwandlung von Iom-Objekten nach JTS-Geometrien.
 *
 * @author Stefan Ziegler
 * @version 0.1
 */

public class Iox2wkt {
    private static Logger logger = Logger.getLogger(Iox2wkt.class);

    private static final CoordinateList coords = new CoordinateList();

    private static PrecisionModel pm = new PrecisionModel(1000);
    private static SimpleGeometryPrecisionReducer pr = new SimpleGeometryPrecisionReducer(
            pm);

    public Iox2wkt() {

    }

    /**
     * Wandelt ein IOM Polylineobjekt in eine JTS-Geometrie um. Kreisbögen
     * werden segmentiert. Durch die Angabe des korrekten maxOverlap wird
     * vermieden, dass Zentroidpunkte ausserhalb des Polygons zu liegen kommt.
     * Dabei wird aus dem maxOverlap resp. der maximalen Pfeilhöhe der
     * Segmentierungswinkel berechnet.
     *
     * @param polylineObj
     *            IOM Polylineobjekt
     * @param maxOverlaps
     *            maximal erlaubter Overlap
     * @return JTS Linestring
     */

    public static Geometry polyline2jts(IomObject polylineObj,
            double maxOverlaps) {

        coords.clear();

        boolean clipped = polylineObj.getobjectconsistency() == IomConstants.IOM_INCOMPLETE;
        for (int sequencei = 0; sequencei < polylineObj
                .getattrvaluecount("sequence"); sequencei++) {
            if (!clipped && sequencei > 0) {
                throw new IllegalArgumentException();
            }

            IomObject sequence = polylineObj.getattrobj("sequence", sequencei);
            for (int segmenti = 0; segmenti < sequence
                    .getattrvaluecount("segment"); segmenti++) {
                IomObject segment = sequence.getattrobj("segment", segmenti);
                if (segment.getobjecttag().equals("COORD")) {

                    String c1 = segment.getattrvalue("C1");
                    String c2 = segment.getattrvalue("C2");
                    // String c3=segment.getattrvalue("C3");

                    Coordinate coord = new Coordinate(Double.valueOf(c1),
                            Double.valueOf(c2));
                    coords.add(coord, false);

                } else if (segment.getobjecttag().equals("ARC")) {
                    String a1 = segment.getattrvalue("A1");
                    String a2 = segment.getattrvalue("A2");
                    String c1 = segment.getattrvalue("C1");
                    String c2 = segment.getattrvalue("C2");
                    // String c3=segment.getattrvalue("C3");

                    Coordinate ptStart = coords
                            .getCoordinate(coords.size() - 1);
                    Coordinate ptArc = new Coordinate(Double.valueOf(a1),
                            Double.valueOf(a2));
                    Coordinate ptEnd = new Coordinate(Double.valueOf(c1),
                            Double.valueOf(c2));

                    // logger.debug(ptStart.toString() + " " + ptArc.toString()
                    // + " " + ptEnd.toString());
                    interpolateArc(ptStart, ptArc, ptEnd, maxOverlaps);

                } else {
                    // custom line form is not supported
                }
            }
        }

        Geometry line = null;
        // Bei 3D-Koordinaten ergäbe das eben schon noch Sinn....
        try {
            line = new GeometryFactory().createLineString(coords
                    .toCoordinateArray());
        } catch (IllegalArgumentException e) {
            // e.printStackTrace();
            System.err.println("not valid linestring");
        }

        return line;
    }

    /**
     * Segmentiert ein Kreisbogenelement. Durch die Angabe des korrekten
     * maxOverlap wird vermieden, dass Zentroidpunkte ausserhalb des Polygons zu
     * liegen kommt. Dabei wird aus dem maxOverlap resp. der maximalen Pfeilhöhe
     * der Segmentierungswinkel berechnet.
     *
     * @param ptStart
     *            Bogenanfang
     * @param ptArc
     *            Bogenpunkt
     * @param ptEnd
     *            Bogenende
     * @param maxOverlaps
     *            maximal erlaubter Overlap
     */
    private static void interpolateArc(Coordinate ptStart, Coordinate ptArc,
            Coordinate ptEnd, double maxOverlaps) {

        double arcIncr = 1;
        if (maxOverlaps < 0.00001) {
            maxOverlaps = 0.002;
        }

        // TEMPORARY:
        maxOverlaps = 0.002;
        // maxOverlaps = 0.0001;

        LineSegment segment = new LineSegment(ptStart, ptEnd);
        double dist = segment.distancePerpendicular(ptArc);
        // logger.debug( "perpendicular distance: " + dist);

        // Abbruchkriterium Handgelenkt mal Pi...
        // (für ganz flache Kreisbogen).
        if (dist < maxOverlaps) {
            coords.add(ptEnd, false);
            return;
        }

        Coordinate center = getArcCenter(ptStart, ptArc, ptEnd);
        // logger.debug("arc center: " + center.toString());

        double cx = center.x;
        double cy = center.y;
        double px = ptArc.x;
        double py = ptArc.y;
        double r = Math.sqrt((cx - px) * (cx - px) + (cy - py) * (cy - py));

        double myAlpha = 2.0 * Math.acos(1.0 - maxOverlaps / r);

        if (myAlpha < arcIncr) {
            arcIncr = myAlpha;
        }

        double a1 = Math.atan2(ptStart.y - center.y, ptStart.x - center.x);
        double a2 = Math.atan2(ptArc.y - center.y, ptArc.x - center.x);
        double a3 = Math.atan2(ptEnd.y - center.y, ptEnd.x - center.x);

        double sweep;

        // Clockwise
        if (a1 > a2 && a2 > a3) {
            sweep = a3 - a1;
        }
        // Counter-clockwise
        else if (a1 < a2 && a2 < a3) {
            sweep = a3 - a1;
        }
        // Clockwise, wrap
        else if ((a1 < a2 && a1 > a3) || (a2 < a3 && a1 > a3)) {
            sweep = a3 - a1 + 2 * Math.PI;
        }
        // Counter-clockwise, wrap
        else if ((a1 > a2 && a1 < a3) || (a2 > a3 && a1 < a3)) {
            sweep = a3 - a1 - 2 * Math.PI;
        } else {
            sweep = 0.0;
        }

        double ptcount = Math.ceil(Math.abs(sweep / arcIncr));

        if (sweep < 0)
            arcIncr *= -1.0;

        double angle = a1;

        for (int i = 0; i < ptcount - 1; i++) {
            angle += arcIncr;

            if (arcIncr > 0.0 && angle > Math.PI)
                angle -= 2 * Math.PI;
            if (arcIncr < 0.0 && angle < -1 * Math.PI)
                angle -= 2 * Math.PI;

            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);

            Coordinate coord = new Coordinate(x, y);
            coords.add(coord, false);

            if ((angle < a2) && ((angle + arcIncr) > a2)) {
                coords.add(ptArc, false);
            }
            if ((angle > a2) && ((angle + arcIncr) < a2)) {
                coords.add(ptArc, false);
            }
        }
        coords.add(ptEnd, false);
    }

    /**
     * Berechnet Kreisbogenzentrum.
     *
     * @param ptStart
     *            Bogenanfang
     * @param ptArc
     *            Bogenpunkt
     * @param ptEnd
     *            Bogenende
     * @return Kreisbogenzentrum
     */
    private static Coordinate getArcCenter(Coordinate ptStart,
            Coordinate ptArc, Coordinate ptEnd) {
        double bx = ptStart.x;
        double by = ptStart.y;
        double cx = ptArc.x;
        double cy = ptArc.y;
        double dx = ptEnd.x;
        double dy = ptEnd.y;
        double temp, bc, cd, det, x, y;

        temp = cx * cx + cy * cy;
        bc = (bx * bx + by * by - temp) / 2.0;
        cd = (temp - dx * dx - dy * dy) / 2.0;
        det = (bx - cx) * (cy - dy) - (cx - dx) * (by - cy);

        det = 1 / det;

        x = (bc * (cy - dy) - cd * (by - cy)) * det;
        y = ((bx - cx) * cd - (cx - dx) * bc) * det;

        return new Coordinate(x, y);
    }

    /**
     * Wandelt ein IOM-Punktelement in ein WKT-String um.
     * 
     * @param pointObj
     *            IOM-Punktelement
     * @return WKT-Punktgeometrie
     */
    public static String point2wkt(IomObject pointObj) {
        String point_wkt = null;

        String c1 = pointObj.getattrvalue("C1");
        String c2 = pointObj.getattrvalue("C2");

        point_wkt = "POINT(" + c1 + " " + c2 + ")";

        return point_wkt;
    }

    /**
     * Wandelt ein IOM-Polylineobjekt in ein WKT-String um. Kreisbogen werden
     * unterstützt.
     *
     * @param polylineObj
     *            IOM-Polylineobjekt
     * @return WKT-Linestring oder WKT-Compoundcurve
     */
    public static String polyline2wkt(IomObject polylineObj) {

        String polyline_wkt = null;

        boolean clipped = polylineObj.getobjectconsistency() == IomConstants.IOM_INCOMPLETE;
        for (int sequencei = 0; sequencei < polylineObj
                .getattrvaluecount("sequence"); sequencei++) {
            if (!clipped && sequencei > 0) {
                throw new IllegalArgumentException();
            }
            Boolean curved = false;
            Boolean curves = false;
            StringBuffer line = new StringBuffer("(");
            String c1_tmp = null;
            String c2_tmp = null;

            IomObject sequence = polylineObj.getattrobj("sequence", sequencei);
            for (int segmenti = 0; segmenti < sequence
                    .getattrvaluecount("segment"); segmenti++) {
                IomObject segment = sequence.getattrobj("segment", segmenti);
                if (segment.getobjecttag().equals("COORD")) {

                    if (curved) {
                        line.append(",(");
                        line.append(c1_tmp + " " + c2_tmp);
                        line.append(",");
                        curved = false;
                    }

                    String c1 = segment.getattrvalue("C1");
                    String c2 = segment.getattrvalue("C2");
                    String c3 = segment.getattrvalue("C3");

                    line.append(c1 + " " + c2);
                    line.append(",");

                    c1_tmp = c1;
                    c2_tmp = c2;

                } else if (segment.getobjecttag().equals("ARC")) {

                    // Falls ein Circuarstring vorangegangen ist,
                    // muss noch ein Komma gesetzt werden.
                    if (curved) {
                        line.append(",");
                    }

                    // Liniensegment abschliessen, aber nur
                    // falls kein Circularstring vorangegangen
                    // ist.
                    if (segmenti != 0 && curved == false) {
                        line.deleteCharAt(line.length() - 1);
                        line.append("),");
                    }

                    // Flags für Kreisbogen setzen
                    curved = true;
                    curves = true;

                    String c1 = segment.getattrvalue("C1");
                    String c2 = segment.getattrvalue("C2");
                    String c3 = segment.getattrvalue("C3");
                    String a1 = segment.getattrvalue("A1");
                    String a2 = segment.getattrvalue("A2");

                    String curve = "CIRCULARSTRING(" + c1_tmp + " " + c2_tmp
                            + "," + a1 + " " + a2 + "," + c1 + " " + c2 + ")";

                    line.append(curve);

                    c1_tmp = c1;
                    c2_tmp = c2;

                } else {
                    // custom line form
                }

                // Falls keine Kreisbögen muss das
                // Liniensegment hier geschlossen werden.
                if (segmenti == (sequence.getattrvaluecount("segment") - 1)
                        && segmenti != 0) {
                    line.deleteCharAt(line.length() - 1);
                    line.append(")");
                }
            }
            // Falls keine Kreisbögen vorhanden sind,
            // gehts als Linestring durch, sonst
            // als Compoundcurve.
            if (curves) {
                line.insert(0, "COMPOUNDCURVE(");
                line.append(")");
            } else {
                line.insert(0, "LINESTRING");
                line.append("");
            }

            polyline_wkt = line.toString();

        }
        return polyline_wkt;
    }

}
