import os

cmd = 'java -Xms1G -Xmx6G -jar'
moduleLoc = '../module_topic_mapping_pipeline/modules'

distribCmd = cmd+' '+moduleLoc+'/D2_DistributionByColumn.jar'
distribColumn = 'LeadROName'
distribParams = distribColumn

mapCmd = cmd+' '+moduleLoc+'/E2_CreateHexMap.jar'
mapLayoutClustersSub = '1'
mapLayoutClustersSup = '8'
mapLayoutMethod = 'MAX'
mapLayoutSurround= '1'
mapIncludeLinkage = 'true'
mapGenericSub = ''
mapGenericSup = '13 18'
mapParamsSub = mapLayoutClustersSub+' '+mapLayoutMethod+' '+mapLayoutSurround+' '+mapIncludeLinkage+' '+mapGenericSub
mapParamsSup = mapLayoutClustersSup+' '+mapLayoutMethod+' '+mapLayoutSurround+' '+mapIncludeLinkage+' '+mapGenericSup

for i in range(0,60):
    modelFile = 'data/SE_sub_model_'+str(i)+'.json'
    distribFile = 'data/SE_sub_distrib_'+str(i)+'.json'
    mapFile = 'data/SE_sub_map_'+str(i)+'.json'
    os.system(distribCmd+' '+modelFile+' '+distribFile+' '+distribParams)
    os.system(mapCmd+' '+modelFile+' '+mapFile+' '+mapParamsSub)

modelFile = 'data/SE_sup_model.json'
distribFile = 'data/SE_sup_distrib.json'
mapFile = 'data/SE_sup_map.json'
os.system(distribCmd+' '+modelFile+' '+distribFile+' '+distribParams)
os.system(mapCmd+' '+modelFile+' '+mapFile+' '+mapParamsSup)