/*
 * Created by Stefano Padilla.
 * Last update 22 / 2 / 2015
 * Copyright Heriot-Watt University
 * Agreed for use inside EPSRC
 */

package PY_TopicModelCore;

public class WordData implements java.io.Serializable {

    private static final long serialVersionUID = 8325577336916113274L;
    public int id;
    public int topic;
    public String label;
    public double weight;


    public WordData(){
    }

    public WordData(int id, int topic, String label, double weight){
        this.id = id;
        this.topic = topic;
        this.label = label;
        this.weight = weight;
    }

}
