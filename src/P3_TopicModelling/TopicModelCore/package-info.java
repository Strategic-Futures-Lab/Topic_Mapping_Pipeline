/**
 * P3_TopicModelling.TopicModelCore essentially contains all classes needed to run topic modelling (and inference) with MALLET.
 * Using a list of input documents ({@link P3_TopicModelling.TopicModelCore.InputDocument}), the
 * {@link P3_TopicModelling.TopicModelCore.TopicModel} class lets you run the modelling process and stores the result in
 * lists of modelled documents ({@link P3_TopicModelling.TopicModelCore.ModelledDocument}) and topics
 * ({@link P3_TopicModelling.TopicModelCore.ModelledTopic}). <br>
 * It also capture logs from MALLET, using {@link P3_TopicModelling.TopicModelCore.MalletLogHandler} and stores
 * log-likelihood ({@link P3_TopicModelling.TopicModelCore.LogLikelihoodRecord}) and topics ({@link P3_TopicModelling.TopicModelCore.TopicRecord}) logs.
 */
package P3_TopicModelling.TopicModelCore;