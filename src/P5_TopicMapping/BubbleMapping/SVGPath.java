package P5_TopicMapping.BubbleMapping;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Class facilitating the creation of SVG paths.
 * Adapted from D3: https://github.com/d3/d3-path/blob/main/src/path.js
 *
 * @author P. Le Bras
 * @version 1.0
 */
public class SVGPath {

    /** Constant value for PI */
    private static final double PI = Math.PI;
    /** Constant value for TAU (2*PI) */
    private static final double TAU = 2 * PI;
    /** Constant value to approximate equality between coordinates */
    private static final double EPSILON = 1e-6;
    /** Constant value to approximate equality between angles */
    private static final double tauEpsilon = TAU - EPSILON;

    /** Coordinates saving the current sub-path */
    private Double x0, y0, x1, y1 = null;
    /** Double formatter for rounding coordinates */
    private final DecimalFormat df;
    /** String saving the built path */
    private String path;

    /**
     * Constructor: initialising the path
     */
    public SVGPath(){
        path = "";
        df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.UP);
    }

    /**
     * Method used to internally round coordinates in the path
     * @param val value to round
     * @return rounded value
     */
    private double round(double val){
        return Double.parseDouble(df.format(val));
    }

    /**
     * Lets you specify the rounding precision
     * @param decimals number of decimal digits
     */
    public void setRoundPrecision(int decimals){
        if(decimals > 0){
            String d = "#".repeat(decimals);
            df.applyPattern("#."+d);
        }
    }

    /**
     * Appends a "move to" instruction to the path (M)
     * @param x X coordinate of the point to move to
     * @param y Y coordinate of the point to move to
     */
    public void moveTo(double x, double y){
        x0 = x1 = x; y0 = y1 = y;
        path += "M" + round(x1) + "," + round(y1);
    }

    /**
     * Closes the path (Z)
     */
    public void closePath(){
        if (x1 != null) {
            x1 = x0; y1 = y0;
            path += "Z";
        }
    }

    /**
     * Appends a line to the path (L)
     * @param x X coordinate of the line destination
     * @param y Y coordinate of the line destination
     */
    public void lineTo(double x, double y) {
        x1 = x; y1 = y;
        path += "L" +  round(x1) + "," + round(y1);
    }

    /**
     * Appends a quadratic bezier curve to the path (Q)
     * @param xc X coordinate of the slope control point
     * @param yc Y coordinate of the slope control point
     * @param x X coordinate of the curve destination
     * @param y Y coordinate of the curve destination
     */
    public void quadraticCurveTo(double xc, double yc, double x, double y) {
        x1 = x; y1 = y;
        path += "Q" + round(xc) + "," + round(yc) + "," + round(x1) + "," + round(y1);
    }

    /**
     * Appends a bezier curve to the path (C)
     * @param xc1 X coordinate of the first slope control point
     * @param yc1 Y coordinate of the first slope control point
     * @param xc2 X coordinate of the second slope control point
     * @param yc2 Y coordinate of the second slope control point
     * @param x X coordinate of the curve destination
     * @param y Y coordinate of the curve destination
     */
    public void bezierCurveTo(double xc1, double yc1, double xc2, double yc2, double x, double y) {
        x1 = x; y1 = y;
        path += "C" + round(xc1) + "," + round(yc1) + "," + round(xc2) + "," + round(yc2) + "," + round(x1) + "," + round(y1);
    }

    /**
     * Shortcut to create a rectangle. Width and height must be positive
     * @param x X coordinate of the rectangle
     * @param y Y coordinate of the rectangle
     * @param w width of the rectangle
     * @param h height of the rectangle
     */
    public void rect(double x, double y, double w, double h) {
        if(w >= 0 && h >= 0){
            x0 = x1 = x; y0 = y1 = y;
            path += "M" + round(x1) + "," + round(y1) + "h" + round(w) + "v" + round(h) + "h" + round(-w) + "Z";
        }
    }

    /**
     * Appends a circular arc to the path tangent to the line between the current point and the specified point ⟨xA, yA⟩
     * and ends tangent to the line between the specified points ⟨xA, yA⟩ and ⟨xB, yB⟩.
     * If the first tangent point is not equal to the current point, a straight line is drawn between the current point
     * and the first tangent point.
     * @param xA X coordinate of the first tangent point
     * @param yA Y coordinate of the first tangent point
     * @param xB X coordinate of the second tangent point
     * @param yB Y coordinate of the second tangent point
     * @param r arc radius
     */
    public void arcTo(double xA, double yA, double xB, double yB, double r) {
        Double xZ = x1, yZ = y1;
        Double xBA = xB - xA, yBA = yB - yA;
        Double xZA = xZ - xA, yZA = yZ - yA;
        double lZA_2 = xZA * xZA + yZA * yZA;
        if (r >= 0){
            if(x1 == null){ // if current subpath is empty move to xA,yA
                x1 = xA; y1 = yA;
                path += "M" + round(x1) + "," + round(y1);
            } else if(lZA_2 > EPSILON){ // continue only if A doesn't coincide with Z
                if(Math.abs(yZA * xBA - yBA * xZA) <= EPSILON || r == 0){
                    // if Z, A and B collinear, i.e. A coincident with B
                    // or if r is 0 : line to A
                    x1 = xA; y1 = yA;
                    path += "L" + round(x1) + "," + round(y1);
                } else { // draw an arc
                    Double xBZ = xB - xZ, yBZ = yB - yZ;
                    double lBA_2 = xBA * xBA + yBA * yBA;
                    double lBZ_2 = xBZ * xBZ + yBZ * yBZ;
                    Double lBA = Math.sqrt(lBA_2);
                    Double lZA = Math.sqrt(lZA_2);
                    Double l = r * Math.tan((PI - Math.acos((lBA_2 + lZA_2 - lBZ_2) / (2 * lBA * lZA))) / 2);
                    Double tZA = l / lZA, tBA = l / lBA;
                    if (Math.abs(tZA - 1) > EPSILON) {
                        // If the start tangent is not coincident with Z, line to
                        path += "L" + round(x1 + tZA * xZA) + "," + round(y1 + tZA * yZA);
                    }
                    int sweep = (yZA * xBZ > xZA * yBZ) ? 1 : 0;
                    x1 = xA + tBA * xBA;
                    y1 = yA + tBA * yBA;
                    path += "A" + round(r) + "," + round(r) + ",0,0," + sweep + "," + round(x1) + "," + round(y1);
                }

            }
        }
    }

    /**
     * Appends a circular arc segment with the specified center ⟨x, y⟩, radius, startAngle and endAngle.
     * If anticlockwise is true, the arc is drawn in the anticlockwise direction; otherwise, it is drawn in the
     * clockwise direction. If the current point is not equal to the starting point of the arc, a straight line is drawn
     * from the current point to the start of the arc.
     * @param x X coordinate of the arc center
     * @param y Y coordinate of the acr center
     * @param r arc radius
     * @param startAngle start angle
     * @param endAngle end angle
     * @param acw direction flag
     */
    public void arc(double x, double y, double r, double startAngle, double endAngle, boolean acw) {
        double dx = r * Math.cos(startAngle), dy = r * Math.sin(startAngle);
        double xA = x + dx, yA = y + dy;
        int cw = acw ? 0 : 1;
        double da = acw ? startAngle - endAngle : endAngle - startAngle;
        if(r >= 0){
            if(x1 == null){
                // Is this path empty? Move to A.
                path += "M" + round(xA) + "," + round(yA);
            } else if(Math.abs(x1 - xA) > EPSILON || Math.abs(y1 - yA) > EPSILON){
                // Or, is A not coincident with the previous point? Line to A.
                path += "L" + round(xA) + "," + round(yA);
            }
            if(r > 0){
                if(da < 0) {
                    // Does the angle go the wrong way? Flip the direction.
                    da = da % TAU + TAU;
                }
                if(da > tauEpsilon){
                    // Is this a complete circle? Draw two arcs to complete the circle.
                    x1 = xA; y1 = yA;
                    path += "A" + round(r) + "," + round(r) + ",0,1," + cw + "," + round(x - dx) + "," +
                            round(y - dy) + "A" + round(r) + "," + round(r) + ",0,1," + cw + "," + round(x1) +
                            "," + round(y1);
                } else if(da > EPSILON){
                    // Is this arc non-empty? Draw an arc!
                    int largeArc = (da >= PI) ? 1 : 0;
                    x1 = x + r * Math.cos(endAngle);
                    y1 = y + r * Math.sin(endAngle);
                    path += "A" + round(r) + "," + round(r) + ",0," + largeArc + "," +
                            cw + "," + round(x1) + "," + round(y1);
                }
            }
        }
    }

    /**
     * Returns the path string
     * @return SVG path
     */
    public String toString(){
        return path;
    }
}
