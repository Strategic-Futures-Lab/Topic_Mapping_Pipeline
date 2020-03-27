@echo off
title Run SFL Topic Mapping

rem filename variables
set fileBaseName=myTopicMapping
set corpusName=corpus
set lemmaName=lemma
set modelName=model_raw
set dlcsvName=download_data
set distibutionName=distribution
set reduceName=model_reduced
set frontName=model
set mapName=map
set separator=-

rem directories
set moduleDirectory=modules
set inputDirectory=input
set outputDirectory=output

rem --------------- P1 ---------------
rem LOADING DOCUMENTS
rem to be updated with other input modules
set PDFInput=%inputDirectory%/pdfs/
set corpusJSON=%outputDirectory%/%fileBaseName%%separator%%corpusName%.json
set subDocSizeLimit=200
rem ----------------------------------

rem --------------- P2 ---------------
rem STOP PHRASES
set stopPhraseInput=%inputDirectory%/StopPhrases.txt
set lemmaColumns0=RawText

rem LEMMATISATION
set lemmaJSON=%outputDirectory%/%fileBaseName%%separator%%lemmaName%.json
set removeOriginalText=true
set minLemmas=10

rem STOP WORDS
set stopWordInput=%inputDirectory%/StopWords.txt
rem ----------------------------------

rem --------------- P3 ---------------
rem TOPIC MODELLING
set numTopics=20
set numIter=2000
set numWords=100
set numDocs=100
set modelJSON=%outputDirectory%/%fileBaseName%%separator%%modelName%%separator%%numTopics%.json
rem ----------------------------------

rem --------------- P4 ---------------
rem CSV DOWNLOADABLE DATA
set DLCSVOutput=%outputDirectory%/%fileBaseName%%separator%%dlcsvName%%separator%%numTopics%.csv
set topicWordsInDLCSV=3
set orderDLCSVAlphabetically=true
set DLCSVColumn0=ID
set DLCSVColumn1=OriginalDocument
set DLCSVColumn2=RawText

rem DISTRIBUTIONS
set distributionJSON=%outputDirectory%/%fileBaseName%%separator%%distibutionName%%separator%%numTopics%.json
set keyColumn=Dataset

rem SUB DOCUMENT REDUCE
set subDocReduceJSON=%outputDirectory%/%fileBaseName%%separator%%reduceName%%separator%%numTopics%.json
set padTimeSlice=true
set roundPadding=10
set columnsToKeep0=Dataset
rem ----------------------------------

rem --------------- P5 ---------------
rem REDUCE JSON
set frontJSON=%outputDirectory%/%fileBaseName%%separator%%frontName%%separator%%numTopics%.json
set skippedSortColumn=ID
set reducedColumn0="Document=OriginalDocument"
set reducedColumn1="Dataset=Dataset"
rem reducedColumn2='Document=ID'

rem MAP LAYOUT
set hexLayoutClusters=6
set hexLayoutMethod=MAX
set hexLayoutSurround=1
set linkageTable=true
set hexLayoutJSON=%outputDirectory%/%fileBaseName%%separator%%mapName%%separator%%numTopics%%separator%%hexLayoutClusters%.json
rem genericTopic0=14
rem genericTopic1=12
rem genericTopic2=14
rem ----------------------------------

rem ------------- STEPS --------------
rem Use the following values to set whether you want a step to run or not! Leave them COMPLETELY BLANK (no white space either) to skip a step.
rem PLEASE NOTE: In general, you should re-run every step from the earliest one you run to then end. i.e. if you set step 4 to run, you should also re-run step 5 and 6.
set runA1PDFLoad=
set runB1RemoveStopPhrase=
set runB2Lemmatise=
set runB3RemoveStopWord=
set runC1TopicModel=
set runD1CreateCSV=
set runD2MakeDistribution=
set runD3ReduceSubDocuments=
set runE1ReduceJSON=
set runE2GenerateLayout=

if defined runA1PDFLoad (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/A1_PDFParser.jar %PDFInput% %corpusJSON% %subDocSizeLimit%
)

if defined runB1RemoveStopPhrase (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/B1_RemoveStopPhrases.jar %corpusJSON% %corpusJSON% %stopPhraseInput% %lemmaColumns0%
)

if defined runB2Lemmatise (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/B2_LemmatiseJSONFile.jar %corpusJSON% %lemmaJSON% %removeOriginalText% %minLemmas% %lemmaColumns0%
)

if defined runB3RemoveStopWord (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/B3_RemoveStopWords.jar %lemmaJSON% %lemmaJSON% %stopWordInput%
)

if defined runC1TopicModel (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/C1_TopicModelFromJSON.jar %lemmaJSON% %modelJSON% %numTopics% %numIter% %numWords% %numDocs%
)

if defined runD1CreateCSV (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/D1_CreateDownloadableCSV.jar %modelJSON% %DLCSVOutput% %topicWordsInDLCSV% %orderDLCSVAlphabetically% %DLCSVColumn0% %DLCSVColumn1% %DLCSVColumn2%
)

if defined runD2MakeDistribution (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/D2_DistributionByColumn.jar %modelJSON% %distributionJSON% %keyColumn%
)

if defined runD3ReduceSubDocuments (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/D3_SubDocumentReduce.jar %modelJSON% %subDocReduceJSON% %padTimeSlice% %roundPadding% %columnsToKeep0%
)

if defined runE1ReduceJSON (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/E1_CreateReducedJSONForD3.jar %subDocReduceJSON% %frontJSON% %skippedSortColumn% %reducedColumn0% %reducedColumn1%
)

if defined runE2GenerateLayout (
  java -Xms1G -Xmx6G -jar %moduleDirectory%/E2_CreateHexMap.jar %subDocReduceJSON% %hexLayoutJSON% %hexLayoutClusters% %hexLayoutMethod% %hexLayoutSurround% %linkageTable%
)
