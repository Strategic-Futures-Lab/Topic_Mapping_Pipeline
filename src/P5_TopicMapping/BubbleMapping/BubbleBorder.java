package P5_TopicMapping.BubbleMapping;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Class providing a method to generate a border path for a set of bubbles after positioning ({@link BubblePack}).
 *
 * @author P. Le Bras
 * @version 1
 */
public class BubbleBorder {

    /** Constant value for PI. */
    private static final double PI = Math.PI;
    /** Constant value for TAU (2*PI). */
    private static final double TAU = 2 * PI;
    /** Constant value to approximate equality between coordinates or angle. */
    private static final double FLOATING_POINT = 0.00001;

    /**
     * Class implementing a 2-dimensional Vector.
     */
    static class Vec2{
        /** Vector's coordinates. */
        public double x, y;
        /**
         * Construstor.
         * @param x Vector's X coordinate.
         * @param y Vector's Y coordinate.
         */
        public Vec2(double x, double y){ this.x = x; this.y = y; }
        /**
         * Method calculating the distance between this and a given vector.
         * @param v Other vector to calculate distance between.
         * @return The distance between the two vectors.
         */
        public double distance(Vec2 v){ double dx = x-v.x, dy = y-v.y; return Math.sqrt(dx*dx+dy*dy); }
        /**
         * Method calculating the difference from this to another given vector.
         * @param v Other vector to calculate the difference towards.
         * @return The difference vector.
         */
        public Vec2 sub(Vec2 v){ return new Vec2(x - v.x, y - v.y); }
        /**
         * Method calculating the sum between this and another given vector.
         * @param v Other vector to calculate the sum with.
         * @return The sum vector.
         */
        public Vec2 add(Vec2 v){ return new Vec2(x + v.x, y + v.y); }
        /**
         * Method scaling this vector by a given factor.
         * @param s Scaling factor.
         * @return The scaled vector.
         */
        public Vec2 scale(double s){ return new Vec2(x * s, y * s); }
        /**
         * Method calculating the angle between this and another vector.
         * @param v Other vector to get the angle from.
         * @return The angle between vectors.
         */
        public double angle(Vec2 v){ double r = Math.atan2(v.y, v.x) - Math.atan2(y, x); if(r<0){r += TAU;} return r; }
        /**
         * Method returning this vector's magnitude.
         * @return The vector's magnitude.
         */
        public double magnitude(){ return Math.sqrt(x*x+y*y); }
        /**
         * Method tranforming this vector to a unit vector (magnitude = 1).
         * @return The transformed vector.
         */
        public Vec2 toUnitVec(){ return scale(1.0/magnitude()); }
    }

    /**
     * Class implementing an Arc.
     */
    static class Arc{
        /** The arc's center. */
        public Vec2 center;
        /** The arc's start angle. */
        public double startAngle;
        /** The arc's end angle. */
        public double endAngle;
        /** The arc's radius. */
        public double radius;
        /**
         * Constructor.
         * @param x Center X coordinate.
         * @param y Center Y coordinate.
         * @param sA Start angle.
         * @param eA End angle.
         * @param r Radius.
         */
        public Arc(double x, double y, double sA, double eA, double r){
            center = new Vec2(x, y); startAngle = sA; endAngle = eA; radius = r;
        }
    }

    /**
     * Class implementing a Circle, i.e. simplified bubble.
     */
    static class Circle{
        /** The circle's center. */
        public Vec2 center;
        /** The circle's radius. */
        public double radius;
        /**
         * Constructor.
         * @param x Center's X coordinate.
         * @param y Center's Y coordinate.
         * @param r Radius.
         */
        public Circle(double x, double y, double r){
            center = new Vec2(x, y); radius = r;
        }
        /**
         * Method checking if this circle intersects with another.
         * @param c Other circle to check with.
         * @return Boolean indicating an intersection.
         */
        public boolean intersects(Circle c){
            double dist = center.distance(c.center);
            if(dist > radius+c.radius) return false;
            return !(dist < Math.abs(radius - c.radius));
        }
        /**
         * Method calculating the intersection points between this and another circle, if intersecting.
         * @param c Other circle to get intersection points with.
         * @return Pair of intersection points.
         */
        public Vec2[] intersectionPoints(Circle c){
            Vec2 P0 = center, P1 = c.center;
            double d = P0.distance(P1),
                    a = (radius*radius - c.radius*c.radius + d*d) / (2*d),
                    h = Math.sqrt(radius*radius - a*a);
            Vec2 P2 = P1.sub(P0).scale(a/d).add(P0);
            double x3 = P2.x + h * (P1.y-P0.y) / d,
                    y3 = P2.y - h * (P1.x - P0.x) / d,
                    x4 = P2.x - h * (P1.y - P0.y) / d,
                    y4 = P2.y + h * (P1.x - P0.x) / d;
            return new Vec2[]{new Vec2(x3,y3), new Vec2(x4, y4)};
        }
        /**
         * Method instantiating a clone of this circle.
         * @return The clone circle.
         */
        public Circle clone(){
            return new Circle(center.x, center.y, radius);
        }
    }

    /**
     * Class implementing a circle part of an outermost subset of circles. Used to calculate the arcs making the border.
     */
    static class OuterCircle{
        /** The circle part of the chain. */
        public Circle circle;
        /** The point where the circle intersects with the chain. */
        public Vec2 intersectionPoint;
        /** Index of the circle in the chain. */
        public int index;
        /**
         * Constructor.
         * @param c The circle.
         * @param p The intersection point.
         * @param i The circle index.
         */
        public OuterCircle(Circle c, Vec2 p, int i){
            circle = c; intersectionPoint = p; index = i;
        }
    }

    /**
     * Class implementing an intersection point between circles.
     */
    static class Intersection{
        /** Intersection point. */
        public Vec2 point;
        /** Index of the circle, in the chain, this intersection is from. */
        public int circleIndex;
        /**
         * Constructor.
         * @param p Intersection point.
         * @param i Circle index.
         */
        public Intersection(Vec2 p, int i){
            point = p; circleIndex = i;
        }
    }

    /**
     * Class implementing a subpart of the bubble map border.
     */
    public static class Border {
        /** SVG Path instruction to draw the border. */
        public String path;
        /** SVG Transform instruction to position the border. */
        public String transform;
        /**
         * Constructor.
         * @param p Path string.
         * @param t Transform string.
         */
        public Border(String p, String t){
            path = p; transform = t;
        }
        /**
         * Method returning the border in JSON format, for writing on file.
         * @return The JSON formatted border.
         */
        public JSONObject toJSON(){
            JSONObject obj = new JSONObject();
            obj.put("d", path);
            obj.put("transform", transform);
            return obj;
        }
    }

    /**
     * Method cloning a list of circles.
     * @param in List of circles to clone.
     * @return List of circle clones.
     */
    private static ArrayList<Circle> cloneCircles(ArrayList<Circle> in){
        ArrayList<Circle> out = new ArrayList<>(in.size());
        for(Circle c: in){out.add(c.clone());}
        return out;
    }

    /**
     * Method finding the next intersection point in a list of circles.
     * @param circleIndex Index of the circle where to get the intersection from.
     * @param circles Set of circles to check intersection with.
     * @param direction Vector to find the closest intersection point (with smallest angle).
     * @return The intersection point found.
     */
    private static Intersection getNextClockwiseIntersection(int circleIndex, ArrayList<Circle> circles, Vec2 direction){
        Circle currentCircle = circles.get(circleIndex);
        ArrayList<Intersection> allIntersections = new ArrayList<>();
        for(int i = 0; i < circles.size(); i++){
            if(i != circleIndex){
                if(circles.get(i).intersects(currentCircle)){
                    Vec2[] intersectionPoints = circles.get(i).intersectionPoints(currentCircle);
                    allIntersections.add(new Intersection(intersectionPoints[0], i));
                    allIntersections.add(new Intersection(intersectionPoints[1], i));
                }
            }
        }
        double smallestAngle = 7.0; // Init with max angle (> 2*PI)
        Intersection intersectionWithSmallestAngle = null;
        for(Intersection intersection: allIntersections){
            double angle = direction.angle(intersection.point.sub(currentCircle.center));
            if(angle > FLOATING_POINT && angle < smallestAngle){
                smallestAngle = angle;
                intersectionWithSmallestAngle = intersection;
            }
        }
        return intersectionWithSmallestAngle;
    }

    /**
     * Method extracting the outermost subset of circles.
     * @param circles The complete set of circles.
     * @param curvature Value enlarging the circles and creating intersections (for the border).
     * @return The subset of outermost circles, overlapping and creating a border.
     */
    private static ArrayList<OuterCircle> getOuterCircleRing(ArrayList<Circle> circles, int curvature){
        ArrayList<Circle> circlesEnlarged = cloneCircles(circles);
        for(Circle c: circlesEnlarged){
            c.radius += curvature;
        }
        int leftmostCircleIndex = 0;
        for(int i = 1; i < circlesEnlarged.size(); i++){
            Circle c1 = circlesEnlarged.get(i);
            Circle c2 = circlesEnlarged.get(leftmostCircleIndex);
            if(c1.center.x - c1.radius < c2.center.x - c2.radius){
                leftmostCircleIndex = i;
            }
        }
        ArrayList<OuterCircle> ring = new ArrayList<>();
        int index = leftmostCircleIndex;
        Vec2 referenceDirection = new Vec2(-1.0, 0.0);
        while(true){
            Intersection intersection = getNextClockwiseIntersection(index, circlesEnlarged, referenceDirection);
            if(intersection == null) break;
            index = intersection.circleIndex;
            Circle circle = circles.get(index);
            referenceDirection = intersection.point.sub(circle.center);
            if(ring.size() > 0
                    && index == ring.get(0).index
                    && intersection.point.distance(ring.get(0).intersectionPoint) < FLOATING_POINT){
                break;
            }
            ring.add(new OuterCircle(circle, intersection.point, index));
        }
        return ring;
    }

    /**
     * Method generating a list of convex arcs given a set of outermost overlapping circles.
     * The arcs are extracted of the circles.
     * @param ring Set of outermost circles.
     * @return List of circle/convex arcs.
     */
    private static ArrayList<Arc> generateCircleArcs(ArrayList<OuterCircle> ring){
        ArrayList<Arc> arcs = new ArrayList<>();
        for(int i = 0; i < ring.size(); i++){
            Circle circle = ring.get(i).circle;
            Vec2 firstIntersection = ring.get(i).intersectionPoint;
            Vec2 secondIntersection = ring.get((i+1)%ring.size()).intersectionPoint;
            Vec2 centerToFirst = firstIntersection.sub(circle.center);
            Vec2 centerToSecond = secondIntersection.sub(circle.center);
            double arcStartAngle = new Vec2(0, -1).angle(centerToFirst);
            double arcEndAngle = new Vec2(0, -1).angle(centerToSecond);
            arcs.add(new Arc(circle.center.x, circle.center.y, arcStartAngle, arcEndAngle, circle.radius));
        }
        return arcs;
    }

    /**
     * Method generating a list of concave arcs given a set of outermost overlapping circles.
     * The arcs are calculated as tangent to the circles.
     * @param ring Set of outermost circles.
     * @param curvature Value of the concave radius.
     * @return List of tangent/concave arcs.
     */
    private static ArrayList<Arc> generateTangentArcs(ArrayList<OuterCircle> ring, double curvature){
        ArrayList<Arc> arcs = new ArrayList<>();
        for(int i = 0; i < ring.size(); i++){
            Vec2 intersection = ring.get(i).intersectionPoint;
            Circle firstCircle = ring.get((i>0)?i-1:ring.size()-1).circle;
            Circle secondCircle = ring.get(i).circle;
            Vec2 intersectionToFirst = firstCircle.center.sub(intersection);
            Vec2 intersectionToSecond = secondCircle.center.sub(intersection);
            double arcEndAngle = new Vec2(0, -1).angle(intersectionToFirst);
            double arcStartAngle = new Vec2(0, -1).angle(intersectionToSecond);
            arcs.add(new Arc(intersection.x, intersection.y, arcStartAngle, arcEndAngle, curvature));
        }
        return arcs;
    }

    /**
     * Method transforming a list of Arcs into Border sub-parts (SVG path + transform).
     * @param arcs Arcs to transform.
     * @return The list of Border sub-parts.
     */
    private static ArrayList<Border> arcsToPath(ArrayList<Arc> arcs){
        ArrayList<Border> paths = new ArrayList<>();
        for(Arc arc: arcs){
            double tmpStartAngle = arc.startAngle;
            if(tmpStartAngle > arc.endAngle){
                tmpStartAngle -= TAU;
            }
            DecimalFormat df = new DecimalFormat("#.###");
            df.setRoundingMode(RoundingMode.UP);
            String path = SVGArc.generateArc(arc.radius, tmpStartAngle, arc.endAngle);
            String transform = "translate("+
                    Double.parseDouble(df.format(arc.center.x))+","+
                    Double.parseDouble(df.format(arc.center.y))+")";
            paths.add(new Border(path, transform));
        }
        return paths;
    }

    /**
     * Method calculating the border for a cluster of bubbles.
     * @param nodes Bubble leaf nodes to calculate the border for.
     * @param curvature Value to enlarge bubble sizes (on top of there border padding) and define the curvature of
     *                  tangent/convex arcs between bubbles.
     * @return The set of Border sub-parts constituting the cluster's border.
     */
    private static ArrayList<Border> generateBorder(ArrayList<BubbleNode> nodes, int curvature){
        ArrayList<Circle> circles = new ArrayList<>();
        for (BubbleNode node: nodes){
            circles.add(new Circle(node.getX(), node.getY(), node.getR()+node.getBorderPadding()));
        }
        ArrayList<OuterCircle> ring = getOuterCircleRing(circles, curvature);
        ArrayList<Arc> arcs = new ArrayList<>();
        arcs.addAll(generateCircleArcs(ring));
        arcs.addAll(generateTangentArcs(ring, curvature));
        return arcsToPath(arcs);
    }

    /**
     * Main method: explores the bubble clusters and calls generateBorder on them.
     * @param root Root node of the hierarchy to explore.
     * @param padding Base padding value between the bubbles and the border.
     * @param curvature Curvature value for the tangent/convex arcs connecting bubbles' arcs.
     * @return The JSON formatted array of border sub-parts.
     */
    public static JSONArray borderHierarchy(BubbleNode root, int padding, int curvature){
        ArrayList<Border> borders = new ArrayList<>();
        for(int layerDepth = root.getHeight()-1; layerDepth >= 0; layerDepth--){
            ArrayList<BubbleCluster> clusters = BubbleCluster.getLayerClusters(root, layerDepth, padding);
            for(BubbleCluster cluster: clusters){
                if(cluster.parent.isClusterRoot()){
                    borders.addAll(generateBorder(cluster.nodes, curvature));
                }
            }
        }
        JSONArray arr = new JSONArray();
        for(Border b: borders){
            arr.add(b.toJSON());
        }
        return arr;
    }
}
