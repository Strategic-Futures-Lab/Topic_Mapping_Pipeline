import json
from collections import OrderedDict
import csv
import copy
import time


def write_json(url, data):
	open(url, 'w').write(json.dumps(data))

def load_json(url):
	with open(url) as json_file:
   		return json.loads(json_file.read(), object_pairs_hook=OrderedDict)

def load_csv(url):
	with open(url) as csv_file:
		data = []
		for row in csv.reader(csv_file, delimiter=','):
			data.append(row)
		return data

def parse_row(row, sub_topics):
	r = None
	if row['[REQ]IncludedInModel']:
		distrib = [row['[REQ]TopicDistribution'][x] for x in sub_topics]
		if sum(distrib) > 0.0:
			r = copy.deepcopy(row)
			r['[REQ]TopicDistribution'] = distrib
	return [r]

def parse_similarities(sim, sub_topics):
	return [sim[x] for x in sub_topics]

start = time.time()

supTMUrl = './data/SE_sup_model.json'
subTMUrl = './data/SE_sub_model.json'
supToSubUrl = './data/SE_sup_to_sub.csv'

supToSub = load_csv(supToSubUrl)[1:]

subTM = load_json(subTMUrl)

subTM_metadata = subTM['metadata']
subTM_failedRetrievals = subTM['failedRetrievals']
subTM_rowData = subTM['rowData']
subTM_timeSlices = subTM['timeSlices']
subTM_topicDetails = subTM['topicDetails']
subTM_topicSimilarities = subTM['topicSimilarities']

t = time.time()
print('files loaded', t-start)
c = 0
for sup_topic in supToSub:
	TMUrl = './data/SE_sub_model_'+str(sup_topic[0])+'.json'
	sub_topics = list(map(lambda x: int(x), filter(lambda x: x!='', sup_topic[2:])))
	subTM_metadata['numTopics'] = str(len(sub_topics))
	subRowData = [x for r in subTM_rowData for x in parse_row(r, sub_topics) if x != None]
	subTopicDetails = [subTM_topicDetails[x] for x in sub_topics]
	subTopicSimilarities = [parse_similarities(subTM_topicSimilarities[x], sub_topics) for x in sub_topics]
	write_json(TMUrl, {'metadata': subTM_metadata,
					   'failedRetrievals': subTM_failedRetrievals,
					   'timeSlices': subTM_timeSlices,
					   'rowData': subRowData,
					   'topicDetails': subTopicDetails,
					   'topicSimilarities': subTopicSimilarities})
	t = time.time()
	print('file written', c, t-start)
	c += 1

t = time.time()
print('Completed', t-start)
# print(len(supToSub))