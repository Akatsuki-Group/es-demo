package com.baiqi.elasticsearch.service.impl;

import com.baiqi.elasticsearch.entity.JobDetail;
import com.baiqi.elasticsearch.service.JobFullTextService;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.Min;
import org.elasticsearch.search.aggregations.metrics.MinAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*
 *  ????????????-????????????
 * */
public class JobFullTextServiceImpl implements JobFullTextService {

    private RestHighLevelClient restHighLevelClient;
    // ??????????????????
    private static final String JOB_IDX = "job_index";

    public JobFullTextServiceImpl() {
        // ?????????ES?????????
        // 1. ??????RestHighLevelClient????????????????????????
        // 2. ??????RestClient.builder???????????????RestClientBuilder
        // 3. ???HttpHost?????????ES?????????
        RestClientBuilder restClientBuilder = RestClient.builder(
                new HttpHost("192.168.21.130", 9200, "http")
                , new HttpHost("192.168.21.131", 9200, "http")
                , new HttpHost("192.168.21.132", 9200, "http"));
       /* RestClientBuilder restClientBuilder = RestClient.builder(
                new HttpHost("192.168.21.130", 9200, "http"));*/
        restHighLevelClient = new RestHighLevelClient(restClientBuilder);
    }

    @Override
    public void add(JobDetail jobDetail) throws IOException {
        //1.	??????IndexRequest?????????????????????ES????????????????????????
        IndexRequest indexRequest = new IndexRequest(JOB_IDX);

        //2.	????????????ID???
        indexRequest.id(jobDetail.getId() + "");

        //3.	??????FastJSON???????????????????????????JSON???
        String json = JSONObject.toJSONString(jobDetail);

        //4.	??????IndexRequest.source??????????????????????????????????????????????????????JSON?????????
        indexRequest.source(json, XContentType.JSON);

        //5.	??????ES High level client??????index?????????????????????????????????????????????????????????
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    @Override
    public JobDetail findById(long id) throws IOException {
        // 1.	??????GetRequest?????????
        GetRequest getRequest = new GetRequest(JOB_IDX, id + "");

        // 2.	??????RestHighLevelClient.get??????GetRequest?????????????????????ES?????????????????????
        GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);

        // 3.	???ES????????????????????????JSON?????????
        String json = getResponse.getSourceAsString();

        // 4.	?????????FastJSON???JSON??????????????????JobDetail?????????
        JobDetail jobDetail = JSONObject.parseObject(json, JobDetail.class);

        // 5.	?????????????????????ID
        jobDetail.setId(id);

        return jobDetail;
    }

    @Override
    public void update(JobDetail jobDetail) throws IOException {
        // 1.	????????????ID?????????????????????
        // a)	??????GetRequest
        GetRequest getRequest = new GetRequest(JOB_IDX, jobDetail.getId() + "");

        // b)	??????client???exists??????????????????????????????????????????
        boolean exists = restHighLevelClient.exists(getRequest, RequestOptions.DEFAULT);

        if(exists) {
            // 2.	??????UpdateRequest??????
            UpdateRequest updateRequest = new UpdateRequest(JOB_IDX, jobDetail.getId() + "");

            // 3.	??????UpdateRequest????????????????????????JSON??????
            updateRequest.doc(JSONObject.toJSONString(jobDetail), XContentType.JSON);

            // 4.	??????client??????update??????
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        }
    }

    @Override
    public void deleteById(long id) throws IOException {
        // 1.	??????delete??????
        DeleteRequest deleteRequest = new DeleteRequest(JOB_IDX, id + "");

        // 2.	??????RestHighLevelClient??????delete??????
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);

    }

    @Override
    public List<JobDetail> searchByKeywords(String keywords) throws IOException {
        // 1.??????SearchRequest????????????
        // ???????????????????????????????????????????????????API
        SearchRequest searchRequest = new SearchRequest(JOB_IDX);

        // 2.????????????SearchSourceBuilder??????????????????????????????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 3.??????QueryBuilders.multiMatchQuery?????????????????????????????????title???jd??????????????????SearchSourceBuilder
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(keywords, "title", "jd");

        // ????????????????????????????????????????????????
        searchSourceBuilder.query(multiMatchQueryBuilder);

        // 4.??????SearchRequest.source????????????????????????????????????
        searchRequest.source(searchSourceBuilder);

        // 5.??????RestHighLevelClient.search????????????
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hitArray = searchResponse.getHits().getHits();

        // 6.????????????
        ArrayList<JobDetail> jobDetailArrayList = new ArrayList<>();

        for (SearchHit documentFields : hitArray) {
            // 1)?????????????????????
            String json = documentFields.getSourceAsString();

            // 2)???JSON????????????????????????
            JobDetail jobDetail = JSONObject.parseObject(json, JobDetail.class);

            // 3)??????SearchHit.getId????????????ID
            jobDetail.setId(Long.parseLong(documentFields.getId()));

            jobDetailArrayList.add(jobDetail);
        }

        return jobDetailArrayList;
    }

    @Override
    public Map<String, Object> searchByPage(String keywords, int pageNum, int pageSize) throws IOException {
        // 1.??????SearchRequest????????????
        // ???????????????????????????????????????????????????API
        SearchRequest searchRequest = new SearchRequest(JOB_IDX);

        // 2.????????????SearchSourceBuilder??????????????????????????????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 3.??????QueryBuilders.multiMatchQuery?????????????????????????????????title???jd??????????????????SearchSourceBuilder
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(keywords, "title", "jd");

        // ????????????????????????????????????????????????
        searchSourceBuilder.query(multiMatchQueryBuilder);

        // ?????????????????????
        searchSourceBuilder.size(pageSize);
        // ??????????????????????????????
        searchSourceBuilder.from((pageNum - 1) * pageSize);

        // 4.??????SearchRequest.source????????????????????????????????????
        searchRequest.source(searchSourceBuilder);

        // 5.??????RestHighLevelClient.search????????????
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hitArray = searchResponse.getHits().getHits();

        // 6.????????????
        ArrayList<JobDetail> jobDetailArrayList = new ArrayList<>();

        for (SearchHit documentFields : hitArray) {
            // 1)?????????????????????
            String json = documentFields.getSourceAsString();

            // 2)???JSON????????????????????????
            JobDetail jobDetail = JSONObject.parseObject(json, JobDetail.class);

            // 3)??????SearchHit.getId????????????ID
            jobDetail.setId(Long.parseLong(documentFields.getId()));

            jobDetailArrayList.add(jobDetail);
        }

        // 8.	??????????????????Map?????????????????????????????????
        // a)	total -> ??????SearchHits.getTotalHits().value???????????????????????????
        // b)	content -> ????????????????????????
        long totalNum = searchResponse.getHits().getTotalHits().value;
        HashMap hashMap = new HashMap();
        hashMap.put("total", totalNum);
        hashMap.put("content", jobDetailArrayList);


        return hashMap;
    }

    @Override
    public Map<String, Object> searchByScrollPage(String keywords, String scrollId, int pageSize) throws IOException {
        SearchResponse searchResponse = null;

        if(scrollId == null) {
            // 1.??????SearchRequest????????????
            // ???????????????????????????????????????????????????API
            SearchRequest searchRequest = new SearchRequest(JOB_IDX);

            // 2.????????????SearchSourceBuilder??????????????????????????????
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            // 3.??????QueryBuilders.multiMatchQuery?????????????????????????????????title???jd??????????????????SearchSourceBuilder
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(keywords, "title", "jd");

            // ????????????????????????????????????????????????
            searchSourceBuilder.query(multiMatchQueryBuilder);

            // ????????????
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title");
            highlightBuilder.field("jd");
            highlightBuilder.preTags("<font color='red'>");
            highlightBuilder.postTags("</font>");

            // ?????????????????????
            searchSourceBuilder.highlighter(highlightBuilder);

            // ?????????????????????
            searchSourceBuilder.size(pageSize);

            // 4.??????SearchRequest.source????????????????????????????????????
            searchRequest.source(searchSourceBuilder);

            //--------------------------
            // ??????scroll??????
            //--------------------------
            searchRequest.scroll(TimeValue.timeValueMinutes(5));

            // 5.??????RestHighLevelClient.search????????????
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        }
        // ???????????????????????????????????????scroll id????????????
        else {
            SearchScrollRequest searchScrollRequest = new SearchScrollRequest(scrollId);
            searchScrollRequest.scroll(TimeValue.timeValueMinutes(5));

            // ??????RestHighLevelClient??????scroll??????
            searchResponse = restHighLevelClient.scroll(searchScrollRequest, RequestOptions.DEFAULT);
        }

        //--------------------------
        // ??????ES???????????????
        //--------------------------

        SearchHit[] hitArray = searchResponse.getHits().getHits();

        // 6.????????????
        ArrayList<JobDetail> jobDetailArrayList = new ArrayList<>();

        for (SearchHit documentFields : hitArray) {
            // 1)?????????????????????
            String json = documentFields.getSourceAsString();

            // 2)???JSON????????????????????????
            JobDetail jobDetail = JSONObject.parseObject(json, JobDetail.class);

            // 3)??????SearchHit.getId????????????ID
            jobDetail.setId(Long.parseLong(documentFields.getId()));

            jobDetailArrayList.add(jobDetail);

            // ??????????????????????????????????????????
            // ???????????????
            Map<String, HighlightField> highlightFieldMap = documentFields.getHighlightFields();
            HighlightField titleHL = highlightFieldMap.get("title");
            HighlightField jdHL = highlightFieldMap.get("jd");

            if(titleHL != null) {
                // ?????????????????????????????????
                Text[] fragments = titleHL.getFragments();
                // ?????????????????????????????????????????????????????????
                StringBuilder builder = new StringBuilder();
                for(Text text : fragments) {
                    builder.append(text);
                }
                // ?????????????????????
                jobDetail.setTitle(builder.toString());
            }

            if(jdHL != null) {
                // ?????????????????????????????????
                Text[] fragments = jdHL.getFragments();
                // ?????????????????????????????????????????????????????????
                StringBuilder builder = new StringBuilder();
                for(Text text : fragments) {
                    builder.append(text);
                }
                // ?????????????????????
                jobDetail.setJd(builder.toString());
            }
        }

        // 8.	??????????????????Map?????????????????????????????????
        // a)	total -> ??????SearchHits.getTotalHits().value???????????????????????????
        // b)	content -> ????????????????????????
        long totalNum = searchResponse.getHits().getTotalHits().value;
        HashMap hashMap = new HashMap();
        hashMap.put("scroll_id", searchResponse.getScrollId());
        hashMap.put("content", jobDetailArrayList);

        return hashMap;
    }



    @Override
    public void close() throws IOException {
        restHighLevelClient.close();
    }
}
