/**
 * P5_TopicMapping is the module containing all functionalities regarding the computation, from topics which have been
 * distributed and clustered, of mappable data (i.e. position and size) to make topics ready for visualisation.
 * <br>
 * It contains:<br>
 *  - {@link P5_TopicMapping.Hierarchy} with helper classes to navigate through topic similarity hierarchies;<br>
 *  - {@link P5_TopicMapping.BubbleMapping}, with its main class {@link P5_TopicMapping.BubbleMapping.BubbleMap}, to create bubble map data;<br>
 *  - {@link P5_TopicMapping.OverwriteMap} to facilitate the update of topic information without changing the map data.<br>
 */
package P5_TopicMapping;