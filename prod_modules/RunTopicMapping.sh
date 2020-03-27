#!/bin/bash

# filename variables
fileBaseName=myTopicMapping
corpusName=corpus
lemmaName=lemma
modelName=model_raw
dlcsvName=download_data
distibutionName=distribution
reduceName=model_reduced
frontName=model
mapName=map
separator=-

# directories
moduleDirectory=modules
inputDirectory=input
outputDirectory=output

# --------------- P1 ---------------
# LOADING DOCUMENTS
# to be updated with other input modules
PDFInput=$inputDirectory/pdfs/
corpusJSON=$outputDirectory/$fileBaseName$separator$corpusName.json
subDocSizeLimit=200
# ----------------------------------

# --------------- P2 ---------------
# STOP PHRASES
stopPhraseInput=$inputDirectory/StopPhrases.txt
lemmaColumns0=RawText

# LEMMATISATION
lemmaJSON=$outputDirectory/$fileBaseName$separator$lemmaName.json
removeOriginalText=true
minLemmas=10

# STOP WORDS
stopWordInput=$inputDirectory/StopWords.txt
# ----------------------------------

# --------------- P3 ---------------
# TOPIC MODELLING
numTopics=20
numIter=2000
numWords=100
numDocs=100
modelJSON=$outputDirectory/$fileBaseName$separator$modelName$separator$numTopics.json
# ----------------------------------

# --------------- P4 ---------------
# CSV DOWNLOADABLE DATA
DLCSVOutput=$outputDirectory/$fileBaseName$separator$dlcsvName$separator$numTopics.csv
topicWordsInDLCSV=3
orderDLCSVAlphabetically=true
DLCSVColumn0=ID
DLCSVColumn1=OriginalDocument
DLCSVColumn2=RawText

# DISTRIBUTIONS
distributionJSON=$outputDirectory/$fileBaseName$separator$distibutionName$separator$numTopics.json
keyColumn=Dataset

# SUB DOCUMENT REDUCE
subDocReduceJSON=$outputDirectory/$fileBaseName$separator$reduceName$separator$numTopics.json
padTimeSlice=true
roundPadding=10
columnsToKeep0=Dataset
# ----------------------------------

# --------------- P5 ---------------
# REDUCE JSON
frontJSON=$outputDirectory/$fileBaseName$separator$frontName$separator$numTopics.json
skippedSortColumn=ID
reducedColumn0='Document=OriginalDocument'
reducedColumn1='Dataset=Dataset'
# reducedColumn2='Document=ID'

# MAP LAYOUT
hexLayoutClusters=6
hexLayoutMethod=MAX
hexLayoutSurround=1
linkageTable=true
hexLayoutJSON=$outputDirectory/$fileBaseName$separator$mapName$separator$numTopics$separator$hexLayoutClusters.json
# genericTopic0=14
# genericTopic1=12
# genericTopic2=14
# ----------------------------------

# ------------- STEPS --------------
runA1PDFLoad=false
runB1RemoveStopPhrase=false
runB2Lemmatise=false
runB3RemoveStopWord=false
runC1TopicModel=false
runD1CreateCSV=false
runD2MakeDistribution=false
runD3ReduceSubDocuments=false
runE1ReduceJSON=false
runE2GenerateLayout=false

if [ $runA1PDFLoad = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/A1_PDFParser.jar $PDFInput $corpusJSON $subDocSizeLimit
fi

if [ $runB1RemoveStopPhrase = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/B1_RemoveStopPhrases.jar $corpusJSON $corpusJSON $stopPhraseInput $lemmaColumns0
fi

if [ $runB2Lemmatise = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/B2_LemmatiseJSONFile.jar $corpusJSON $lemmaJSON $removeOriginalText $minLemmas $lemmaColumns0
fi

if [ $runB3RemoveStopWord = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/B3_RemoveStopWords.jar $lemmaJSON $lemmaJSON $stopWordInput
fi

if [ $runC1TopicModel = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/C1_TopicModelFromJSON.jar $lemmaJSON $modelJSON $numTopics $numIter $numWords $numDocs
fi

if [ $runD1CreateCSV = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/D1_CreateDownloadableCSV.jar $modelJSON $DLCSVOutput $topicWordsInDLCSV $orderDLCSVAlphabetically $DLCSVColumn0 $DLCSVColumn1 $DLCSVColumn2
fi

if [ $runD2MakeDistribution = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/D2_DistributionByColumn.jar $modelJSON $distributionJSON $keyColumn #distributionColumn0
fi

if [ $runD3ReduceSubDocuments = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/D3_SubDocumentReduce.jar $modelJSON $subDocReduceJSON $padTimeSlice $roundPadding $columnsToKeep0 #columnsToKeep1
fi

if [ $runE1ReduceJSON = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/E1_CreateReducedJSONForD3.jar $subDocReduceJSON $frontJSON $skippedSortColumn $reducedColumn0 $reducedColumn1 # $reducedColumn2
fi

if [ $runE2GenerateLayout = "true" ] ; then
  java -Xms1G -Xmx6G -jar $moduleDirectory/E2_CreateHexMap.jar $subDocReduceJSON $hexLayoutJSON $hexLayoutClusters $hexLayoutMethod $hexLayoutSurround $linkageTable #$genericTopic0 # $genericTopic1 $genericTopic2
fi
