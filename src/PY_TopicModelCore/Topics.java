/*
 * Created by Stefano Padilla.
 * Last update 22 / 2 / 2015
 * Copyright Heriot-Watt University
 * Agreed for use inside EPSRC
 */

package PY_TopicModelCore;

public class Topics implements java.io.Serializable{

    private static final long serialVersionUID = -1048734038308190794L;
    public int id;
    public int labels = 0;
    public String[] topicLabels;
    public int [] topicLabelsIDs;
    public double[] topicWeights;

    public Topics(){
        topicLabels = new String[0];
        topicWeights = new double[0];
        topicLabelsIDs = new int[0];
    }

    public Topics(int labels){
        this.labels = labels;
        topicLabels = new String[100];      // Store only 100 topic labels
        topicWeights = new double[100];
        topicLabelsIDs = new int[100];
    }

    public Topics(int id, int labels, String[] topicLabels, int [] topicLabelsIDs, double[] topicWeights){
        this.id = id;
        this.labels = labels;
        this.topicLabels = topicLabels;
        this.topicLabelsIDs = topicLabelsIDs;
        this.topicWeights = topicWeights;
    }


}
