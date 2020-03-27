package PX_Helper;

import PY_TopicModelCore.WordData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Tom on 18/04/2018.
 */
public class JSONIOWrapper
{
    private JSONObject root;
    private JSONArray rowData;
    private JSONObject metadata;
    private JSONArray topicSimilarities;
    private JSONArray topicDetails;
    private JSONArray failedRetrievals;
    private JSONObject timeSlices;

    public void CreateBlankJSONStructure()
    {
        root = new JSONObject();
        rowData = new JSONArray();
        metadata = new JSONObject();
        topicSimilarities = new JSONArray();
        topicDetails = new JSONArray();
    }

    @SuppressWarnings("unchecked")
    public void LoadJSON(String filename)
    {
        System.out.println("\n**********\nLoading JSON File!\n***********\n");

        CreateBlankJSONStructure();
        JSONParser parser = new JSONParser();

        try (FileReader file = new FileReader(filename))
        {
            root = (JSONObject) parser.parse(file);

            if(root.get("rowData") != null)
                rowData = (JSONArray) root.get("rowData");

            if(root.get("metadata") != null)
                metadata = (JSONObject) root.get("metadata");

            if(root.get("failedRetrievals") != null)
                failedRetrievals = (JSONArray) root.get("failedRetrievals");

            if(root.get("topicSimilarities") != null)
                topicSimilarities = (JSONArray) root.get("topicSimilarities");

            if(root.get("topicDetails") != null)
                topicDetails = (JSONArray) root.get("topicDetails");

            if(root.get("timeSlices") != null)
                timeSlices = (JSONObject) root.get("timeSlices");
        }
        catch (IOException | ParseException e)
        {
            e.printStackTrace();
        }
        finally
        {
            System.out.println("\n**********\nJSON File Loaded!\n***********\n");
        }
    }



    @SuppressWarnings("unchecked")
    public void SaveJSON(String filename)
    {
        System.out.println("\n**********\nSaving JSON File!\n***********\n");

        try (FileWriter file = new FileWriter(filename))
        {
            root.put("metadata", metadata);
            root.put("failedRetrievals", failedRetrievals);
            root.put("rowData", rowData);
            root.put("topicSimilarities", topicSimilarities);
            root.put("topicDetails", topicDetails);
            root.put("timeSlices", timeSlices);

            String string = root.toJSONString();
            file.write(root.toJSONString());
            file.flush();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            System.out.println("\n**********\nJSON File Saved!\n***********\n");
        }
    }

    @SuppressWarnings("unchecked")
    public void SetRowData(ConcurrentHashMap<String, DocumentRow> JSONRows)
    {
        /*//Remove previous part...
        root.remove(rowData);
        rowData = new JSONArray();
        root.put("rowData", rowData);*/

        rowData = new JSONArray();

        /**
         * Make the ArrayList the full size we need, so we can add items wherever we like within it.
         */
        for(int i = 0; i < JSONRows.size(); i++)
        {
            rowData.add(null);
        }

        for (Map.Entry<String, DocumentRow> entry : JSONRows.entrySet())
        {
            JSONObject dataObj = new JSONObject();
            DocumentRow tempRow = entry.getValue();

            if(tempRow.getID() != null)
                dataObj.put("[REQ]ID", tempRow.getID());

            dataObj.put("[REQ]IncludedInModel", tempRow.isIncludedInModel());
            dataObj.put("[REQ]RemovalReason", tempRow.getRemovalReason());

            dataObj.put("[REQ]JSONRow", tempRow.getJSONRow());

            if(tempRow.getLemmaStringData() != null)
                dataObj.put("[REQ]LemmaStringData", tempRow.getLemmaStringData());

            if(tempRow.getNumLemmas() != -1)
                dataObj.put("[REQ]NumLemmas", tempRow.getNumLemmas());

            if(tempRow.getTopicDistribution() != null)
            {
                JSONArray distArray = new JSONArray();

                for(double value : tempRow.getTopicDistribution())
                    distArray.add(value);

                dataObj.put("[REQ]TopicDistribution", distArray);
            }

            for(Map.Entry<String, String> value : entry.getValue().getValues().entrySet())
            {
                dataObj.put(value.getKey(), value.getValue());
            }

            rowData.set(tempRow.getJSONRow(), dataObj);
        }
    }

    @SuppressWarnings("unchecked")
    public ConcurrentHashMap<String, DocumentRow> GetRowData()
    {
        ConcurrentHashMap<String, DocumentRow> JSONRows = new ConcurrentHashMap<>();

        for (JSONObject rowDetail : (Iterable<JSONObject>) rowData)
        {
            DocumentRow row = new DocumentRow();
            for (String value : (Iterable<String>) rowDetail.keySet())
            {
                switch (value)
                {
                    case "[REQ]ID":
                        row.setID((String) rowDetail.get(value));
                        break;
                    case "[REQ]IncludedInModel":
                        row.setIncludedInModel((boolean)rowDetail.get(value));
                        break;
                    case "[REQ]RemovalReason":
                        row.setRemovalReason((String)rowDetail.get(value));
                        break;
                    case "[REQ]JSONRow":
                        row.setJSONRow(Math.toIntExact((long)rowDetail.get(value)));
                        break;
                    case "[REQ]LemmaStringData":
                        row.setLemmaStringData((String) rowDetail.get(value));
                        break;
                    case "[REQ]NumLemmas":
                        row.setNumLemmas(Math.toIntExact((long)rowDetail.get(value)));
                        break;
                    case "[REQ]TopicDistribution":
                        JSONArray topicDist = (JSONArray) rowDetail.get(value);
                        double[] distArray = new double[topicDist.size()];
                        for(int i = 0; i < topicDist.size(); i++)
                            distArray[i] = (double)topicDist.get(i);
                        row.setTopicDistribution(distArray);
                        break;
                    default:
                        row.setValue(value, (String) rowDetail.get(value));
                        break;
                }
            }

            JSONRows.put(row.getID(), row);
        }

        return JSONRows;
    }

    @SuppressWarnings("unchecked")
    public ConcurrentHashMap<String, String> GetMetadata()
    {
        ConcurrentHashMap<String, String> metadataRows = new ConcurrentHashMap<>();

        for (String value : (Iterable<String>) metadata.keySet())
        {
            metadataRows.put(value, (String) metadata.get(value));
        }

        return metadataRows;
    }

    @SuppressWarnings("unchecked")
    public void SetMetadata(ConcurrentHashMap<String, String> JSONRows)
    {
        metadata = new JSONObject();

        for (Map.Entry<String, String> entry : JSONRows.entrySet())
        {
            metadata.put(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public void SetTopicSimilarities(double[][] simMatrix)
    {
        topicSimilarities = new JSONArray();
        for(int y = 0; y < simMatrix.length; y++)
        {
            JSONArray simRow = new JSONArray();
            for(int x = 0; x < simMatrix.length; x++)
            {
                simRow.add(simMatrix[x][y]);
            }
            topicSimilarities.add(simRow);
        }
    }

    @SuppressWarnings("unchecked")
    public double[][] GetTopicSimilarities()
    {
        int numTopics = 0;
        try
        {
            numTopics = Integer.parseInt((String)metadata.get("numTopics"));
        }
        catch (NumberFormatException e)
        {
            System.out.println("\n**********\nFAILED TO RECOVER THE NUMBER OF TOPICS! HAVE YOU RUN THE TOPIC MODELLING?\n**********\n");
        }

        double[][] simMatrix = new double[numTopics][numTopics];

        for(int y = 0; y < simMatrix.length; y++)
        {
            JSONArray simRow = (JSONArray) topicSimilarities.get(y);
            for (int x = 0; x < simMatrix.length; x++)
            {
                simMatrix[x][y] = (double)simRow.get(x);
            }
        }

        return simMatrix;
    }

    @SuppressWarnings("unchecked")
    public void SetTopicDetails(List<List<WordData>> wordsAndWeights, List<List<WordData>> documents)
    {
        topicDetails = new JSONArray();

        for(int topic = 0; topic < wordsAndWeights.size(); topic++)
        {
            JSONArray topicArray = new JSONArray();

            for(int word = 0; word < wordsAndWeights.get(topic).size(); word++)
            {
                JSONObject wordObj = new JSONObject();
                wordObj.put("label", wordsAndWeights.get(topic).get(word).label);
                wordObj.put("weight", wordsAndWeights.get(topic).get(word).weight);
                wordObj.put("wordID", wordsAndWeights.get(topic).get(word).id);
                topicArray.add(wordObj);
            }

            JSONArray documentArray = new JSONArray();
            for(int doc = 0; doc < documents.get(topic).size(); doc++)
            {
                JSONObject docObj = new JSONObject();
                docObj.put("docID", documents.get(topic).get(doc).label);
                docObj.put("weight", documents.get(topic).get(doc).weight);
                documentArray.add(docObj);
            }

            JSONObject detailObj = new JSONObject();
            detailObj.put("topWords", topicArray);
            detailObj.put("topDocs", documentArray);
            topicDetails.add(detailObj);
        }
    }

    @SuppressWarnings("unchecked")
    public List<List<WordData>> GetTopicWords()
    {
        List<List<WordData>> wordsAndWeights = new ArrayList<>();
        for(int topic = 0; topic < topicDetails.size(); topic++)
        {
            List<WordData> wordDataList = new ArrayList<>();
            JSONObject topicData = (JSONObject) topicDetails.get(topic);
            JSONArray topicWords = (JSONArray) topicData.get("topWords");

            for(int word = 0; word < topicWords.size(); word++)
            {
                WordData wordData = new WordData();
                JSONObject singleWord = (JSONObject) topicWords.get(word);

                wordData.label = (String) singleWord.get("label");
                wordData.weight = (double) singleWord.get("weight");

                wordDataList.add(wordData);
            }
            wordsAndWeights.add(wordDataList);
        }

        return wordsAndWeights;
    }

    @SuppressWarnings("unchecked")
    public List<List<WordData>> GetTopicDocuments()
    {
        List<List<WordData>> documents = new ArrayList<>();
        for(int topic = 0; topic < topicDetails.size(); topic++)
        {
            List<WordData> wordDataList = new ArrayList<>();
            JSONObject topicData = (JSONObject) topicDetails.get(topic);
            JSONArray topicWords = (JSONArray) topicData.get("topDocs");

            for(int word = 0; word < topicWords.size(); word++)
            {
                WordData wordData = new WordData();
                JSONObject singleWord = (JSONObject) topicWords.get(word);

                wordData.label = (String) singleWord.get("docID");
                wordData.weight = (double) singleWord.get("weight");

                wordDataList.add(wordData);
            }
            documents.add(wordDataList);
        }

        return documents;
    }

    @SuppressWarnings("unchecked")
    public void SetFailedRetrievals(ConcurrentHashMap<String, String> failedRows)
    {
        failedRetrievals = new JSONArray();

        for(Map.Entry<String, String> row : failedRows.entrySet())
        {
            JSONObject failReason = new JSONObject();
            failReason.put("ID", row.getKey());
            failReason.put("Reason", row.getValue());
            failedRetrievals.add(failReason);
        }
    }

    @SuppressWarnings("unchecked")
    public ConcurrentHashMap<String, String> GetFailedRetrievals()
    {
        ConcurrentHashMap<String, String> failRows = new ConcurrentHashMap<>();

        for(JSONObject failRow : (Iterable<JSONObject>)failedRetrievals)
        {
            failRows.put((String)failRow.get("ID"), (String)failRow.get("Reason"));
        }

        return failRows;
    }

    public void SetTimeSlices(ConcurrentHashMap<String, List<List<Double>>> timeSliceData, ConcurrentHashMap<String, Integer> numTimeSlices)
    {
        timeSlices = new JSONObject();

        for(Map.Entry<String, List<List<Double>>> entry : timeSliceData.entrySet())
        {
            JSONObject wrapObject = new JSONObject();
            JSONArray topicArray = new JSONArray();

            List<List<Double>> slices = entry.getValue();

            for(List<Double> topics : slices)
            {
                JSONArray sliceArray = new JSONArray();
                for(Double value : topics)
                {
                    sliceArray.add(value);
                }
                topicArray.add(sliceArray);
            }

            wrapObject.put("timeSlices", topicArray);
            wrapObject.put("numTimeSlices", numTimeSlices.get(entry.getKey()));
            timeSlices.put(entry.getKey(), wrapObject);
        }
    }

    public JSONObject GetTimeSlicesInJSON()
    {
        return timeSlices;
    }
}
