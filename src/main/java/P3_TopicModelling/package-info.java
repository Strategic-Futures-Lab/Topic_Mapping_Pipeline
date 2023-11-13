/**
 * P3_TopicModelling contains all modules related to the topic modelling process.<br>
 * - {@link P3_TopicModelling.LemmaReader} provides a common reader for lemma JSON file;<br>
 * - {@link P3_TopicModelling.TopicModelling} provides methods for running a topic model and saving topic and document files;<br>
 * - {@link P3_TopicModelling.HierarchicalTopicModelling} provides methods for running two topic models (main and sub),
 * assigning sub topics to main topics, and saving topic and document JSON file;<br>
 * - {@link P3_TopicModelling.ExportTopicModel} provides methods for exporting document and topic JSON file into "minified"
 * topic JSON files (list of topics with top words and top documents with data) and document CSV file(s) (with topic
 * distributions);<br>
 * - {@link P3_TopicModelling.InferDocuments} provides method for inferring distributions of topics from an existing model
 * onto new lemmatised documents and saving the new data on file(s).
 */
package P3_TopicModelling;