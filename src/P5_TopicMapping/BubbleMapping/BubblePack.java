package P5_TopicMapping.BubbleMapping;

import P5_TopicMapping.Hierarchy.NodeCallBack;
import PY_Helper.LogPrint;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.DistanceJointDef;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Class providing a method that creates an initial bubble pack layout from a hierarchy of nodes.
 * It then adjusts the bubbles and pulls them together to their final position for the bubble map using a force layout.
 *
 * @author P. Le Bras
 * @version 1.0
 */
public class BubblePack {

    /* ===========================================================================
      Methods packing bubbles using a pack layout.
    =========================================================================== */

    /**
     * Class implementing a link within a chain of bubbles.
     */
    private static class Link{
        /** Bubble attached to this link. */
        public BubbleNode node;
        /** Next link in the chain. */
        public Link next;
        /** Previous link in the chain. */
        public Link previous;
        /**
         * Constructor
         * @param n Bubble to attach to the link
         */
        public Link(BubbleNode n){
            node = n;
        }
    }

    /**
     * Class implementing a simplified version of a hierarchy node to manipulate it internally.
     * It only implements the coordinates and radius.
     */
    private static class SimpleNode{
        /** X coordinate of the bubble */
        public double x;
        /** Y coordinate of the bubble */
        public double y;
        /** Radius of the bubble */
        public double r;
        /**
         * Constructor
         * @param X X coordinate
         * @param Y Y coordinate
         * @param R Radius
         */
        public SimpleNode(double X, double Y, double R){
            x = X; y = Y; r = R;
        }
    }

    /**
     * Method placing a bubble given to previously placed bubbles
     * @param b First placed bubble
     * @param a Second placed bubble
     * @param c Bubble to place
     */
    private static void placeNodes(BubbleNode b, BubbleNode a, BubbleNode c){
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double d2 = dx * dx + dy * dy;
        if(d2 > 0){
            double a2 = Math.pow(a.getR()+c.getR(),2);
            double b2 = Math.pow(b.getR()+c.getR(),2);
            if(a2>b2){
                double x = (d2 + b2 - a2) / (2 * d2);
                double y = Math.sqrt(Math.max(0.0, b2 / d2 - x * x));
                c.setX(b.getX() - x * dx - y * dy);
                c.setY(b.getY() - x * dy + y * dx);
            } else {
                double x = (d2 + a2 - b2) / (2 * d2);
                double y = Math.sqrt(Math.max(0.0, a2 / d2 - x * x));
                c.setX(a.getX() + x * dx - y * dy);
                c.setY(a.getY() + x * dy + y * dx);
            }
        } else {
            c.setX(a.getX()+c.getR());
            c.setY(a.getY());
        }
    }

    /**
     * Method checking if two bubbles intersect.
     * @param a First bubble.
     * @param b Second bubble.
     * @return Boolean telling if the bubbles intersect.
     */
    private static boolean intersects(BubbleNode a, BubbleNode b){
        double dr = a.getR() + b.getR() - 1e-6;
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        return dr > 0.0 && dr * dr > dx * dx + dy * dy;
    }

    /** Method to find the squared distance between the origin (0,0) and the centroid between two consecutive circles
     * in a chain. The centroid between the two circles is calculated using geometric decomposition.
     * @param l Chain link to get the first bubble, the second bubble will be l.next.
     * @return The squared distance between (0,0) and the two bubbles' centroid.
     */
    private static double score(Link l){
        BubbleNode a = l.node;
        BubbleNode b = l.next.node;
        double ab = a.getR() + b.getR();
        double dx = (a.getX() * b.getR() + b.getX() * a.getR()) / ab;
        double dy = (a.getY() * b.getR() + b.getY() * a.getR()) / ab;
        return dx * dx + dy * dy;
    }

    /**
     * Method to append a bubble to an existing set of bubbles, shifting their position to create a compact layout.
     * @param B List of bubbles to append the node to.
     * @param p Bubble to append to the list.
     * @return The merged and updated list of bubbles.
     */
    private static ArrayList<SimpleNode> extendBasis(ArrayList<SimpleNode> B, SimpleNode p){
        ArrayList<SimpleNode> res = new ArrayList<>();
        if(enclosesWeakAll(p, B)) {
            res.add(p);
            return res;
        }
        // If we get here then B must have at least one element.
        for(int i = 0; i < B.size(); ++i){
            if(enclosesNot(p,B.get(i)) && enclosesWeakAll(encloseBasis2(B.get(i), p), B)){
                res.add(B.get(i));
                res.add(p);
                return res;
            }
        }
        // If we get here then B must have at least two elements.
        for(int i = 0; i < B.size()-1; ++i){
            for(int j = i+1; j < B.size(); ++j){
                SimpleNode ij = encloseBasis2(B.get(i), B.get(j));
                SimpleNode ip = encloseBasis2(B.get(i), p);
                SimpleNode jp = encloseBasis2(B.get(j), p);
                SimpleNode ijp = encloseBasis3(B.get(i), B.get(j), p);
                boolean enc_ij_p = enclosesNot(ij, p),
                        enc_ip_j = enclosesNot(ip, B.get(j)),
                        enc_jp_i = enclosesNot(jp, B.get(i)),
                        enc_ijp = enclosesWeakAll(ijp, B);
                if(enc_ij_p && enc_ip_j && enc_jp_i && enc_ijp) {
                    res.add(B.get(i));
                    res.add(B.get(j));
                    res.add(p);
                    return res;
                }
            }
        }
        // If we get here then something is very wrong.
        LogPrint.printNoteError("Error while packing topics");
        System.exit(1);
        return res;
    }

    /**
     * Method to check if a bubble doesn't enclose another.
     * @param a The possibly enclosing bubble.
     * @param b The possibly enclosed bubble.
     * @return Boolean telling if the first bubble doesn't enclose the other.
     */
    private static boolean enclosesNot(SimpleNode a, SimpleNode b) {
        double dr = a.r - b.r;
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        return dr < 0 || dr * dr < dx * dx + dy * dy;
    }

    /**
     * Method to check if a bubble encloses another.
     * @param a The possibly enclosing bubble.
     * @param b The possibly enclosed bubble.
     * @return Boolean telling if the first bubble encloses the other.
     */
    private static boolean enclosesWeak(SimpleNode a, SimpleNode b){
        double dr = a.r - b.r + Math.max(Math.max(a.r, b.r), 1) * 1e-9;
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        return dr > 0 && dr * dr > dx * dx + dy * dy;
    }

    /**
     * Method to check if a bubble encloses a set of other bubbles.
     * @param a The possibly enclosing bubble.
     * @param B The set of possibly enclosed bubbles.
     * @return Boolean telling if the first bubble encloses the set.
     */
    private static boolean enclosesWeakAll(SimpleNode a, ArrayList<SimpleNode> B){
        for (SimpleNode simpleNode : B) {
            if (!enclosesWeak(a, simpleNode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to compute and return a new bubble enclosing the given set of bubbles.
     * The set of bubbles must have between 1 and 3 bubbles.
     * @param B Set of bubbles to enclose.
     * @return The enclosing bubble.
     */
    private static SimpleNode encloseBasis(ArrayList<SimpleNode> B) {
        switch (B.size()) {
            case 1: return encloseBasis1(B.get(0));
            case 2: return encloseBasis2(B.get(0), B.get(1));
            case 3: return encloseBasis3(B.get(0), B.get(1), B.get(2));
            default: return null;
        }
    }

    /**
     * Method returning a new bubble enclosing the given bubble.
     * @param a Bubble to enclose.
     * @return The enclosing bubble.
     */
    private static SimpleNode encloseBasis1(SimpleNode a) {
        return new SimpleNode(a.x, a.y, a.r);
    }

    /**
     * Method returning a new bubble enclosing the two given bubble.
     * @param a First bubble to enclose.
     * @param b Second bubble to enclose.
     * @return The enclosing bubble.
     */
    private static SimpleNode encloseBasis2(SimpleNode a,
                                            SimpleNode b) {
        double x1 = a.x, y1 = a.y, r1 = a.r;
        double x2 = b.x, y2 = b.y, r2 = b.r;
        double x21 = x2 - x1, y21 = y2 - y1, r21 = r2 - r1;
        double l = Math.sqrt(x21 * x21 + y21 * y21);
        return new SimpleNode((x1 + x2 + x21 / l * r21) / 2,
                (y1 + y2 + y21 / l * r21) / 2,
                (l + r1 + r2) / 2);
    }

    /**
     * Method returning a new bubble enclosing the three given bubble.
     * @param a First bubble to enclose.
     * @param b Second bubble to enclose.
     * @param c Third bubble to enclose.
     * @return The enclosing bubble.
     */
    private static SimpleNode encloseBasis3(SimpleNode a,
                                            SimpleNode b,
                                            SimpleNode c) {
        double x1 = a.x, y1 = a.y, r1 = a.r,
                x2 = b.x, y2 = b.y, r2 = b.r,
                x3 = c.x, y3 = c.y, r3 = c.r;
        double a2 = x1 - x2, a3 = x1 - x3, b2 = y1 - y2,
                b3 = y1 - y3, c2 = r2 - r1, c3 = r3 - r1;
        double d1 = x1 * x1 + y1 * y1 - r1 * r1,
                d2 = d1 - x2 * x2 - y2 * y2 + r2 * r2,
                d3 = d1 - x3 * x3 - y3 * y3 + r3 * r3;
        double ab = a3 * b2 - a2 * b3,
                xa = (b2 * d3 - b3 * d2) / (ab * 2) - x1,
                xb = (b3 * c2 - b2 * c3) / ab,
                ya = (a3 * d2 - a2 * d3) / (ab * 2) - y1,
                yb = (a2 * c3 - a3 * c2) / ab;
        double A = xb * xb + yb * yb - 1,
                B = 2 * (r1 + xa * xb + ya * yb),
                C = xa * xa + ya * ya - r1 * r1,
                r = -(A!=0 ? (B + Math.sqrt(B * B - 4 * A * C)) / (2 * A) : C / B);
        return new SimpleNode( x1 + xa + xb * r, y1 + ya + yb * r, r);
    }

    /**
     * Method computing and returning a new bubble enclosing a given chain of bubbles.
     * @param chain The chain fo bubbles to enclose.
     * @return The enclosing bubble.
     */
    private static SimpleNode enclose(ArrayList<SimpleNode> chain){
        int i = 0;
        int n = chain.size();
        // Collections.shuffle(chain);
        ArrayList<SimpleNode> B = new ArrayList<>();
        SimpleNode p;
        SimpleNode e = null;
        while(i < n){
            p = chain.get(i);
            if(e != null && enclosesWeak(e, p)) ++i;
            else {
                B = extendBasis(B, p);
                e = encloseBasis(B);
                i = 0;
            }
        }
        return e;
    }

    /**
     * Method positioning a set of leaf nodes compactly and returning the radius for their parent node.
     * @param nodes Set of nodes to pack.
     * @return The radius for the parent node.
     */
    private static double packNodes(ArrayList<BubbleNode> nodes){
        int n = nodes.size();
        // no node to place
        if(n == 0){ return 0.0; }
        // place first node
        BubbleNode a = nodes.get(0);
        a.setX(0.0);
        a.setY(0.0);
        // only one node, returns its radius
        if(n <= 1){ return a.getR(); }
        // place second node
        BubbleNode b = nodes.get(1);
        a.setX(-b.getR());
        b.setX(a.getR());
        b.setY(0.0);
        // only two nodes, returns their cumulated radii
        if(n <= 2){ return a.getR()+b.getR(); }
        // place third node
        BubbleNode c = nodes.get(2);
        placeNodes(b, a, c);
        // initialise chain
        Link A = new Link(a);
        Link B = new Link(b);
        Link C = new Link(c);
        A.next = B;
        C.previous = B;
        B.next = C;
        A.previous = C;
        C.next = A;
        B.previous = A;
        // place remaining nodes
        pack: for(int i = 3; i < n; i++){
            c = nodes.get(i);
            placeNodes(A.node, B.node, c);
            C = new Link(c);
            // Find the closest intersecting circle on the front-chain, if any.
            Link J = B.next;
            Link K = A.previous;
            double sj = B.node.getR();
            double sk = B.node.getR();
            do {
                if(sj <= sk){
                    if(intersects(J.node, C.node)){
                        B = J;
                        A.next = B;
                        B.previous = A;
                        --i;
                        continue pack;
                    }
                    sj += J.node.getR();
                    J = J.next;
                } else {
                    if(intersects(K.node, C.node)){
                        A = K;
                        A.next = B;
                        B.previous = A;
                        --i;
                        continue pack;
                    }
                    sk += K.node.getR();
                    K = K.previous;
                }
            } while (J != K.next);
            // Insert the new circle c between a and b.
            C.previous = A;
            C.next = B;
            A.next = C;
            B.previous = C;
            B = C;
            // Compute new centroid pair
            double aa = score(A);
            while(C.next != B){
                C = C.next;
                double ca = score(C);
                if(ca < aa){
                    A = C;
                    aa = ca;
                }
            }
            B = A.next;
        }
        // Compute the enclosing circle of the front chain.
        ArrayList<SimpleNode> chain = new ArrayList<>();
        chain.add(new SimpleNode(B.node.getX(),B.node.getY(),B.node.getR()));
        C = B;
        while (C.next != B){
            C = C.next;
            chain.add(new SimpleNode(C.node.getX(),C.node.getY(),C.node.getR()));
        }
        SimpleNode enclosing = enclose(chain);
        for (BubbleNode node : nodes) {
            node.setX(node.getX() - enclosing.x);
            node.setY(node.getY() - enclosing.y);
        }
        return enclosing.r;
    }

    /**
     * Callback class for initialising the radius of leaf nodes.
     */
    private static class RadiusLeaf implements NodeCallBack<BubbleNode> {
        public void method(BubbleNode n){
            if(n.isLeaf()){
                n.setR(n.getSize());
            }
        }
    }

    /**
     * Callback class for packing a node's children and setting the node's radius accordingly.
     */
    private static class PackChildren implements NodeCallBack<BubbleNode>{
        public void method(BubbleNode n) {
            if(!n.isLeaf()){
                n.setR(packNodes((ArrayList<BubbleNode>)(ArrayList<?>) n.getChildren()));
            }
        }
    }

    /**
     * Callback class for moving a node's children with it.
     */
    private static class TranslateChildren implements NodeCallBack<BubbleNode>{
        public void method(BubbleNode n){
            if(!n.isRoot()){
                BubbleNode p = (BubbleNode) n.getParent();
                n.setX(p.getX() + n.getX());
                n.setY(p.getY() + n.getY());
            }

        }
    }

    /**
     * Method launching the hierarchy packing.
     * @param root Root node of the hierarchy to pack.
     * @param width Target width of the layout.
     * @param height Target height of the layout.
     */
    private static void packHierarchy(BubbleNode root, int width, int height){
        root.setX((double) width/2);
        root.setY((double) height/2);
        root.eachBefore(new RadiusLeaf());
        root.eachAfter(new PackChildren());
        root.eachBefore(new TranslateChildren());
    }

    /* ===========================================================================
      Methods adjusting bubbles using a force layout.
    =========================================================================== */

    /**
     * Method to computes the centroid of a list of bubbles.
     * @param nodes The list of bubbles.
     * @return The bubbles centroid.
     */
    private static Vec2 getCircleCentroid(ArrayList<BubbleNode> nodes){
        float circleMassSum = 0.0F;
        Vec2 centroid = new Vec2();
        centroid.setZero();
        for(BubbleNode n: nodes){
            float circleMass = (float) (n.getR() * n.getR() * Math.PI);
            circleMassSum += circleMass;
            centroid.x += (float) n.getX() * circleMass;
            centroid.y += (float) n.getY() * circleMass;
        }
        centroid = centroid.mul(1.0F/circleMassSum);
        return centroid;
    }

    /**
     * Method creating a physics body for a cluster of bubbles.
     * @param cluster Bubble cluster.
     * @param world Physics world in which to create the body.
     * @return The created physics body.
     */
    private static Body createClusterBody(BubbleCluster cluster, World world){
        Vec2 bodyCentroid  = getCircleCentroid(cluster.nodes);
        BodyDef BDef = new BodyDef(); BDef.position = bodyCentroid; BDef.type = BodyType.DYNAMIC;
        Body body = world.createBody(BDef);
        for(BubbleNode n: cluster.nodes){
            Vec2 centerGlobal = new Vec2((float) n.getX(), (float) n.getY());
            Vec2 centerLocal = centerGlobal.sub(bodyCentroid);
            CircleShape circle = new CircleShape();
            circle.m_p.set(centerLocal);
            circle.setRadius((float) (n.getR() + n.getBubblePadding()));
            FixtureDef FDef = new FixtureDef();
            FDef.density = 1.0F; FDef.friction = 0.00001F; FDef.shape = circle;
            Fixture fixture = body.createFixture(FDef);
            fixture.setUserData(n);
        }
        return body;
    }

    /**
     * Method applying a force layout on a cluster of bubbles.
     * @param clusters The cluster of bubbles to apply the layout on.
     * @param centroid The cluster centroid.
     */
    private static void layoutCluster(ArrayList<BubbleCluster> clusters, Vec2 centroid){
        World world = new World(new Vec2(0.0F, 0.0F));
        ArrayList<Body> clusterBodies = new ArrayList<>();
        // LogPrint.printNote(clusters.size()+" clusters, centroid: "+centroid.toString(), 2);
        for(BubbleCluster cluster: clusters){
            // LogPrint.printNote(cluster.nodes.size()+" leaves", 3);
            clusterBodies.add(createClusterBody(cluster, world));
        }
        BodyDef BDef = new BodyDef(); BDef.position = centroid;
        Body attractorBody = world.createBody(BDef);
        for(Body body: clusterBodies){
            DistanceJointDef JDef = new DistanceJointDef();
            JDef.frequencyHz = 0.9F;
            JDef.dampingRatio = 0.001F;
            JDef.initialize(attractorBody, body, attractorBody.getPosition(), body.getPosition());
            JDef.length = 0.0F;
            world.createJoint(JDef);
        }
        float timeStep = 1.0F/60.0F;
        int velocityIterations = 6,
                positionIterations = 2;
        for(int i = 0; i < 1000; i++ ){
            world.step(timeStep, velocityIterations, positionIterations);
        }
        for(Body b = world.getBodyList(); b != null; b = b.getNext()){
            for(Fixture f = b.getFixtureList(); f != null; f = f.getNext()){
                if(f.getShape().getType() == ShapeType.CIRCLE){
                    Vec2 center = b.getWorldPoint(((CircleShape) f.getShape()).m_p);
                    BubbleNode raw = (BubbleNode) f.getUserData();
                    // String init = "topic "+raw.getTopicId()+"; initial position: "+raw.getX()+", "+raw.getY();
                    raw.setX(center.x);
                    raw.setY(center.y);
                    // init += "; new position: "+raw.getX()+", "+raw.getY();
                    // LogPrint.printNote(init, 2);
                }
            }
        }
    }

    /**
     * Method launching the force layout on a bubble hierarchy.
     * @param root Root node of the bubble hierarchy.
     * @param padding Base value for the padding around bubbles.
     */
    private static void forceHierarchy(BubbleNode root, int padding){
        for(int layerDepth = root.getHeight()-1; layerDepth >= 0; layerDepth--){
            // for this layer depth computer the leave clusters
            ArrayList<BubbleCluster> bubbleClusters = BubbleCluster.getLayerClusters(root, layerDepth, padding);
            // get the unique parents of each cluster
            HashSet<BubbleNode> parents = new HashSet<>();
            for(BubbleCluster c: bubbleClusters){
                parents.add((BubbleNode) c.parent.getParent());
            }
            // for each unique parent
            for(BubbleNode parent: parents){
                // get its layer clusters
                ArrayList<BubbleCluster> currentClusters = new ArrayList<>(bubbleClusters);
                currentClusters.removeIf(c -> c.parent.getParent() != parent);
                // get its leaf nodes
                ArrayList<BubbleNode> leaves = new ArrayList<>();
                for(BubbleCluster c: currentClusters){
                    leaves.addAll(c.nodes);
                }
                // compute centroid of leaves
                Vec2 centroid = getCircleCentroid(leaves);
                // perform the layout
                layoutCluster(currentClusters, centroid);
            }
        }
    }

    /* ===========================================================================
      Main method.
    =========================================================================== */

    /**
     * Main method: calls packHierarchy first and then adjustHierarchy.
     * @param root Root node of the hierarchy to pack
     * @param padding Base padding value between node bubbles
     * @param width Target width of the final layout
     * @param height Target height of the final layout
     */
    public static void layoutHierarchy(BubbleNode root, int padding, int width, int height){
        packHierarchy(root, width, height);
        forceHierarchy(root, padding);
    }
}
