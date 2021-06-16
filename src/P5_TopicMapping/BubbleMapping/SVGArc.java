package P5_TopicMapping.BubbleMapping;

/**
 * Class facilitating the creation of SVG arc paths.
 * Adapted from D3: https://github.com/d3/d3-shape/blob/main/src/arc.js
 *
 * @author P. Le Bras
 * @version 1.0
 */
public class SVGArc {

    /** Constant value for PI */
    private static final double PI = Math.PI;
    /** Constant value for TAU (2*PI) */
    private static final double TAU = 2 * PI;
    /** Constant value to approximate equality between coordinates */
    private static final double EPSILON = 1e-12;

    /**
     * Generates an arc path with the given radii and angles
     * @param innerRadius inner radius of the arc
     * @param outerRadius outer radius of the arc
     * @param startAngle start angle of the arc
     * @param endAngle end angle of the arc
     * @return the generated arc path
     */
    public static String generateArc(double innerRadius, double outerRadius, double startAngle, double endAngle){
        double iR = innerRadius, oR = outerRadius;
        double sA = startAngle - PI / 2;
        double eA = endAngle - PI / 2;
        double dA = Math.abs(eA - sA);
        boolean cw = eA > sA;

        SVGPath context = new SVGPath();

        // Ensure that the outer radius is always larger than the inner radius.
        if (oR < iR) {
            double r = oR; oR = iR; iR = r;
        };

        if(!(oR > EPSILON)) {
            // is it a point ?
            context.moveTo(0.0, 0.0);
        } else if(dA > TAU - EPSILON){
            // Or is it a circle or annulus?
            context.moveTo(oR * Math.cos(sA), oR * Math.sin(sA));
            context.arc(0.0, 0.0, oR, sA, eA, !cw);
            if(iR > EPSILON){
                context.moveTo(iR * Math.cos(eA), iR * Math.sin(eA));
                context.arc(0.0, 0.0, iR, eA, sA, cw);
            }
        } else {
            // Or is it a circular or annular sector?
            double x01 = oR * Math.cos(sA),
                y01 = oR * Math.sin(sA),
                x10 = iR * Math.cos(eA),
                y10 = iR * Math.sin(eA);
            if(!(dA > EPSILON)){
                // Is the sector collapsed to a line?
                context.moveTo(x01, y01);
            } else {
                // Or is the outer ring just a circular arc?
                context.moveTo(x01, y01);
                context.arc(0, 0, oR, sA, eA, !cw);
            }
            if(!(iR > EPSILON) || !(dA > EPSILON)){
                // Is there no inner ring, and it’s a circular sector?
                // Or perhaps it’s an annular sector collapsed due to padding?
                context.lineTo(x10, y10);
            } else {
                // Or is the inner ring just a circular arc?
                context.arc(0, 0, iR, eA, sA, cw);
            }
        }
        context.closePath();
        return context.toString();
    }

    /**
     * Generates an arc path with the given radius and angles
     * @param radius radius of the arc
     * @param startAngle start angle of the arc
     * @param endAngle end angle of the arc
     * @return the generated arc path
     */
    public static String generateArc(double radius, double startAngle, double endAngle){
        return generateArc(radius, radius, startAngle, endAngle);
    }
}
