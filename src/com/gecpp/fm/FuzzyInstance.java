package com.gecpp.fm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;

import org.json.*;

import com.gecpp.fm.Dao.IndexAdj;
import com.gecpp.fm.Dao.IndexRate;
import com.gecpp.fm.Dao.IndexResult;
import com.gecpp.fm.Dao.IndexShort;
import com.gecpp.fm.Dao.Keyword;
import com.gecpp.fm.Dao.MultiKeyword;
import com.gecpp.fm.Dao.Product;
import com.gecpp.fm.Dao.Keyword.KeywordKind;
import com.gecpp.fm.Dao.Keyword.NLP;
import com.gecpp.fm.Dao.MfsAlternate;
import com.gecpp.fm.Logic.FuzzySearchLogic;
import com.gecpp.fm.Logic.KeywordLogic;
import com.gecpp.fm.Logic.OmSearchLogic;
import com.gecpp.fm.Logic.PmSearchLogic;
import com.gecpp.fm.Logic.RedisSearchLogic;
import com.gecpp.fm.Util.CommonUtil;
import com.gecpp.fm.Util.DbHelper;
import com.gecpp.fm.Util.LogQueryHistory;
import com.gecpp.fm.Util.SortUtil;
import com.gecpp.fm.Util.StopWatch;
import com.gecpp.fm.model.FuzzyManagerModel;
import com.gecpp.fm.model.OrderManagerModel;
import com.gecpp.om.OrderManager;
import com.gecpp.p.product.domain.Mfs;
import com.luhuiguo.chinese.ChineseUtils;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import java.util.Date;

class ValueComparator implements Comparator<String> {

    Map<String, Float> base;
    public ValueComparator(Map<String, Float> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}

class IntComparator implements Comparator<String> {

    Map<String, Integer> base;
    public IntComparator(Map<String, Integer> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}

class OrdComparator implements Comparator<Integer> {

    Map<Integer, Integer> base;
    public OrdComparator(Map<Integer, Integer> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(Integer a, Integer b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}


public class FuzzyInstance {
	
	
	private String strSkipWord = ", . ; + - | / \\ ' \" : ? < > [ ] { } ! @ # $ % ^ & * ( ) ~ ` _ － ? ， （ ）";
	private String[] SkipWord = null;
	
	
	
	
	private List<String> getNotRepeatPns(List<String> pns, List<String> fuzzyPns) {
        List<String> notRepeatPns = new ArrayList<>();
        Set<String> pnSet = new HashSet<>();
        pnSet.addAll(pns);
        for (int i = 0; i < fuzzyPns.size() && pnSet.size() < 20; i++) {
            pnSet.add(fuzzyPns.get(i));
        }
        notRepeatPns.addAll(pnSet);

        return notRepeatPns;
    }
	
	
	
	/**
     * create by lhp 2015-07-20 copy from qegoo
     * 根据pn生成pn_key
     *
     * @param pn
     * @return
     */
    
    
    private String parsePnKeyForSearch(String pn) {
        String pnKey = pn;

        pnKey = pnKey.replaceAll("\"", "&quot;");
        pnKey = pnKey.replaceAll("\'", "&apos;");
        pnKey = pnKey.trim();

        pnKey = org.apache.commons.lang3.StringUtils.replaceEach(pnKey,
                new String[]{" ", "/", "+", "?", "%", "#", "&", "=", "-", "(", ")", "\'", ".", "quot;", "apos;", "\""},
                new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""});

        pnKey = pnKey.replace("|", "/"); // ?在?面里?modelname中/??成|,在此???回/
        pnKey = pnKey.replace("<", "");
        pnKey = pnKey.replace(">", "");

        // 去除??前后空格
        pnKey = pnKey.toUpperCase().trim();
        //pnKey = pnKey + "%";

        return pnKey;
    }
	
	
	
	


    
	public int InsertFuzzyRecord(int pid, String pn, String mfs,
			String catalog, String description, String param, Connection conn, CRFClassifier<CoreLabel> segmenter) {
		String sPid = Integer.toString(pid);
		
		// first delete old data
		String strSql = "delete from qeindex where page = " + pid;
		execUpdate(strSql, conn);
		
		ProcessData(sPid, pn, mfs, catalog, description, param, conn, segmenter);
		
		DbHelper.attemptClose(conn);
		
		return 1;
	}
	
	protected void ProcessData(String pid, String pn, String mfs,
			String catalog, String description, String param, Connection conn, CRFClassifier<CoreLabel> segmenter) {
		Map<String, String> scoreMap = null;

		// 清除雜訊
		if (description != null && !description.isEmpty())
			description.replaceAll("[\"\']", "");
		if (param != null && !param.isEmpty())
			param.replaceAll("[\"\']", "");
 
		// 料號
        //scoreMap = segmentData(pn);

        // 料號需有完整紀錄
        //if(!scoreMap.containsKey(pn))
        //{
        InsertPostgrel(pn.toUpperCase(),
                Integer.parseInt(pid),
                1,
                0, pn, mfs, catalog, pn, conn);
        //}

        //InsertAllWord(pid, 0, pn, mfs, catalog, scoreMap);

        // mfs
        scoreMap = segmentData(mfs, segmenter);
        InsertAllWord(pid, 1, pn, mfs, catalog, scoreMap, conn);

        // catalog
        scoreMap = segmentDataCatalog(catalog);
        InsertAllWord(pid, 2, pn, mfs, catalog, scoreMap, conn);

        // description
        scoreMap = segmentDataDDesc(description);
        InsertAllWord(pid, 3, pn, mfs, catalog, scoreMap, conn);

        // param
        scoreMap = segmentDataParam(param);
        InsertAllWord(pid, 4, pn, mfs, catalog, scoreMap, conn);
		
		
	}
	
	
	protected Map<String, String> segmentDataCatalog(String strData)
    {
        String [] strFullword = null;

        List<String> sList = new ArrayList<String>();
        List<String> sFullword = new ArrayList<String>();

        Map<String, String> scoreMap = new HashMap<String, String>();

        String val = null;

        val = strData;


        if (val != null) {

            val = val.toUpperCase();

            val = val.trim();

            val = val.replace("，", " ");

            val = val.replace(">", " ");

            strFullword = val.split(" ");

            if(strFullword != null) {
                for (String stoken : strFullword) {



                    stoken = stoken.replace(" ", "");

                    if (SkipWord(stoken) || stoken.length() == 0)
                        continue;


                    if (stoken.trim() == "")
                        continue;

                    //InsertPostgrel(stoken, Integer.parseInt(pid), 1, 4, pn, mfs, catalog, val);
                    sList.add(stoken);
                    sFullword.add(val);
                }
            }

        }


        for(int i=0; i<sList.size(); i++)
        {
            float weight = 0.0f;

            weight = (float)similarity(sList.get(i), sFullword.get(i));


            if(scoreMap.containsKey(sList.get(i).toUpperCase()))
            {

                String sValue = scoreMap.get(sList.get(i).toUpperCase());
                String [] token = sValue.split(",");

                double score = Double.parseDouble(token[0]);

                // 取最大值
                if(score < weight)
                    score = weight;

                String s = Double.toString(score);

                if(s.length() > 4)
                    s = s.substring(0, 4);

                s += "," + sFullword.get(i);

                scoreMap.put(sList.get(i).toUpperCase(), s);

            }
            else {
                String s = Float.toString(weight) + "," + sFullword.get(i);
                scoreMap.put(sList.get(i).toUpperCase(), s);
            }
        }

        return scoreMap;

    }

    protected Map<String, String> segmentDataDDesc(String strData)
    {
        String [] strFullword = null;

        List<String> sList = new ArrayList<String>();
        List<String> sFullword = new ArrayList<String>();

        Map<String, String> scoreMap = new HashMap<String, String>();

        String val = null;

        val = strData;


        if (val != null) {

            val = val.toUpperCase();

            val = val.trim();

            val = val.replace("，", " ");


            strFullword = val.split(" ");

            if(strFullword != null) {
                for (String stoken : strFullword) {


                    stoken = stoken.replace(" ", "");

                    if (SkipWord(stoken) || stoken.length() == 0)
                        continue;

                    if (stoken.trim() == "")
                        continue;

                    //InsertPostgrel(stoken, Integer.parseInt(pid), 1, 4, pn, mfs, catalog, val);
                    sList.add(stoken);
                    sFullword.add(val);
                }
            }

        }


        for(int i=0; i<sList.size(); i++)
        {
            float weight = 0.0f;

            weight = (float)similarity(sList.get(i), sFullword.get(i));


            if(scoreMap.containsKey(sList.get(i).toUpperCase()))
            {

                String sValue = scoreMap.get(sList.get(i).toUpperCase());
                String [] token = sValue.split(",");

                double score = Double.parseDouble(token[0]);

                // 取最大值
                if(score < weight)
                    score = weight;

                String s = Double.toString(score);

                if(s.length() > 4)
                    s = s.substring(0, 4);

                s += "," + sFullword.get(i);

                scoreMap.put(sList.get(i).toUpperCase(), s);

            }
            else {
                String s = Float.toString(weight) + "," + sFullword.get(i);
                scoreMap.put(sList.get(i).toUpperCase(), s);
            }
        }

        return scoreMap;

    }

    protected Map<String, String> segmentDataParam(String strData)
    {
        String [] strFullword = null;

        List<String> sList = new ArrayList<String>();
        List<String> sFullword = new ArrayList<String>();

        Map<String, String> scoreMap = new HashMap<String, String>();

        if(strData != null && !strData.isEmpty()) {

            try {
                JSONObject json = new JSONObject(strData);

                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String val = null;
                    try {
                        val = json.getString(key);
                    } catch (Exception e) {
                        System.out.println(e.toString());
                    }

                    if (val != null) {

                        val = val.toUpperCase();

                        val = val.trim();

                        val = val.replace("，", " ");

                        if (val.contains("HTTP"))
                            continue;

                        if (val.contains("PDF"))
                            continue;

                        strFullword = val.split(" ");

                        if (strFullword != null) {
                            for (String stoken : strFullword) {



                                stoken = stoken.replace(" ", "");

                                if (SkipWord(stoken) || stoken.length() == 0)
                                    continue;

                                if (stoken.trim() == "")
                                    continue;

                                //InsertPostgrel(stoken, Integer.parseInt(pid), 1, 4, pn, mfs, catalog, val);
                                sList.add(stoken);
                                sFullword.add(val);
                            }
                        }

                    }


                }
            } catch (JSONException e) {
                return scoreMap;
            }

            for (int i = 0; i < sList.size(); i++) {
                float weight = 0.0f;

                weight = (float) similarity(sList.get(i), sFullword.get(i));


                if (scoreMap.containsKey(sList.get(i).toUpperCase())) {

                    String sValue = scoreMap.get(sList.get(i).toUpperCase());
                    String[] token = sValue.split(",");

                    double score = 0.0;
                    try{
                        score = Double.parseDouble(token[0]);
                    }
                    catch (NumberFormatException e)
                    {}

                    // 取最大值
                    if (score < weight)
                        score = weight;

                    String s = Double.toString(score);

                    if (s.length() > 4)
                        s = s.substring(0, 4);

                    s += "," + sFullword.get(i);

                    scoreMap.put(sList.get(i).toUpperCase(), s);

                } else {
                    String s = Float.toString(weight) + "," + sFullword.get(i);
                    scoreMap.put(sList.get(i).toUpperCase(), s);
                }
            }
        }

        return scoreMap;

    }
    
    protected double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }

    protected int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }
	
    public OrderResult QueryFuzzyRecordByDeptSearch(String strData, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			int currentPage, 
			int pageSize)
	{
        // 回傳值
        OrderResult result = null;
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        // 分析輸入的查詢
        Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        // 加亮
        String strHighLight = "";
        for(String stoken:keyQuery.getKeyword())
        {
        	strHighLight += stoken + ",";
        }
        // Log 紀錄
        LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
        
        // 純料號的結果
        List<String> pnList = new ArrayList<String>();
        // Redis的結果
        List<IndexResult> redisResult = new ArrayList<IndexResult>();
        // Fuzzy的結果
        List<IndexResult> fuzzyResult = new ArrayList<IndexResult>();
        
        // 料號排序的結果
        List<IndexResult> sortedIndexResult = null;
        
        // 如果輸入的查詢有問題，回傳空的結果
        if(keyQuery.getCount()== 0)
        {
        	result = new OrderResult();
        	result.setTotalCount(0);
        	result.setPns(new String[0]);
        	return result;
        }
        
        // 純料號的方式
        if (keyQuery.getCount() == 1 && keyQuery.getKind().get(0).equals(KeywordKind.IsPn)) {
        	// 計時
        	StopWatch watch = new StopWatch("PmSearch");
        	
        	pnList = PmSearchLogic.PmSearch(keyQuery);
        	
        	// 為了修正從pm_pn中找不到的問題
        	if(!pnList.contains(strData.toUpperCase()))
        		pnList.add(strData.toUpperCase());
        	
        	// 20160223 料號預先排序應該只限於排序料號
        	HashMap<String, Integer> hashPnWeight = FuzzyManagerModel.OrderPn(pnList);
    		sortedIndexResult = SortUtil.SortIndexResultSimple(hashPnWeight, 0);
        	
        	watch.getElapseTime(keyQuery, pnList);
        	
        	nSearchType = 1;
        }
        
        if (keyQuery.getCount() > 1) // Redis的交集
        {
        	StopWatch watch = new StopWatch("RedisSearch");
        	
        	try
        	{
        		redisResult = RedisSearchLogic.getRedisSearchId(keyQuery);
        	}
        	catch(Exception e)
        	{
        		List<String> sErr = new ArrayList<String>();
        		sErr.add(e.getMessage());
        		watch.getElapseTime(keyQuery, sErr);
        	}
        	watch.getElapseTimeIndexResult(keyQuery, redisResult);
        	
        }
        
     // 20160223 for more percisely pn search
        if(pnList.size() == 0)
        {
	        // 不夠的再由FuzzySearch補充
	        if(pnList.size() + redisResult.size() < 50)
	        {
	        	StopWatch watch = new StopWatch("FuzzySearch");
	        	if(nSearchType == 1)	// 以純料號搜尋
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearch(keyQuery);
	        	else
	        	{
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearchId(keyQuery);
	        		// reorder 
	        		//redisResult = SortUtil.RegroupIndexResult(redisResult, fuzzyResult);
	        		// 直接加在下面
	        		redisResult.addAll(fuzzyResult);
	        	}
	        	
	        	watch.getElapseTimeIndexResult(keyQuery, fuzzyResult);
	        }
        }
        
        
        
        // 利用hash排除重複
        Map<String, Integer> uniqPn = new HashMap<String, Integer>();
        // 交給排序模組
        List<String> OmList = new ArrayList<String>();
      
        int nTotalCount = 0;
        
        if(nSearchType == 1) // search by pm and/or fuzzy
        {
        	// 最後整理出的唯一料號表
            List<String> sPnReturn = new ArrayList<String>();
            
            // 純料號的先
            for(IndexResult res : sortedIndexResult)
            {
            	uniqPn.put(res.getPn(), 1);
            	sPnReturn.add(res.getPn());
            }
            //sPnReturn.addAll(pnList);
            
            // FuzzySearch
            for(IndexResult tuple : fuzzyResult)
            {
            	if(!uniqPn.containsKey(tuple.getPn()))
            	{
            		uniqPn.put(tuple.getPn(), 1);
            		sPnReturn.add(tuple.getPn());
            	}
            }
          
            
            OmList.addAll(sPnReturn);
            
        }
        else
        {
        	List<String> pageList = new ArrayList<String> ();
        	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
        	
        	int nCount = 0;

        	for(IndexResult tuple : redisResult)
        	{
        		OmList.add(tuple.getPn());
        		
        		nCount++;
        		
        		// 先取500筆以上即可
            	if(nCount > 500)
            		break;
        	}
        	
        }
    
        StopWatch watch = new StopWatch("OrderManager");
        
        OrderManager om = new OrderManager();
        if(nSearchType == 1)	// 以純料號搜尋
        {
        	result = om.getProductByGroupInStoreDeep(inventory, lead, rohs, mfs, abbreviation, OmList, currentPage, pageSize);
        	if(result.getTotalCount() == 0)
        		return QueryFuzzyRecordByDeptSearchAgain(strData, inventory, lead, rohs, mfs, abbreviation,currentPage, pageSize);
        }
        else
        	result = om.getProductByGroupInStoreIdDeep(inventory, lead, rohs, mfs, abbreviation, OmList, currentPage, pageSize);
        
        watch.getElapseTimeOrderResult(keyQuery, OmList);

        // 去除逗號
        strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
        result.setHighLight(strHighLight);
        
        

        return result;
	}
    
    
    public OrderResult QueryFuzzyRecordByDeptSearchAgain(String strData, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			int currentPage, 
			int pageSize)
	{
        // 回傳值
        OrderResult result = null;
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        // 分析輸入的查詢
        Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        // 加亮
        String strHighLight = "";
        for(String stoken:keyQuery.getKeyword())
        {
        	strHighLight += stoken + ",";
        }
        // Log 紀錄
        LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
        
        // 純料號的結果
        List<String> pnList = new ArrayList<String>();
        // Redis的結果
        List<IndexResult> redisResult = new ArrayList<IndexResult>();
        // Fuzzy的結果
        List<IndexResult> fuzzyResult = new ArrayList<IndexResult>();
        
        // 料號排序的結果
        List<IndexResult> sortedIndexResult = null;
        
        // 如果輸入的查詢有問題，回傳空的結果
        if(keyQuery.getCount()== 0)
        {
        	result = new OrderResult();
        	result.setTotalCount(0);
        	result.setPns(new String[0]);
        	return result;
        }
        
        
        
        if (keyQuery.getCount() > 1) // Redis的交集
        {
        	StopWatch watch = new StopWatch("RedisSearch");
        	
        	try
        	{
        		redisResult = RedisSearchLogic.getRedisSearchId(keyQuery);
        	}
        	catch(Exception e)
        	{
        		List<String> sErr = new ArrayList<String>();
        		sErr.add(e.getMessage());
        		watch.getElapseTime(keyQuery, sErr);
        	}
        	watch.getElapseTimeIndexResult(keyQuery, redisResult);
        	
        }
        
     // 20160223 for more percisely pn search
        if(pnList.size() == 0)
        {
	        // 不夠的再由FuzzySearch補充
	        if(pnList.size() + redisResult.size() < 50)
	        {
	        	StopWatch watch = new StopWatch("FuzzySearch");
	        	if(nSearchType == 1)	// 以純料號搜尋
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearch(keyQuery);
	        	else
	        	{
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearchId(keyQuery);
	        		// reorder 
	        		//redisResult = SortUtil.RegroupIndexResult(redisResult, fuzzyResult);
	        		// 直接加在下面
	        		redisResult.addAll(fuzzyResult);
	        	}
	        	
	        	watch.getElapseTimeIndexResult(keyQuery, fuzzyResult);
	        }
        }
        
        
        
        // 利用hash排除重複
        Map<String, Integer> uniqPn = new HashMap<String, Integer>();
        // 交給排序模組
        List<String> OmList = new ArrayList<String>();
      
        int nTotalCount = 0;
        
        if(nSearchType == 1) // search by pm and/or fuzzy
        {
        	// 最後整理出的唯一料號表
            List<String> sPnReturn = new ArrayList<String>();
            
            // 純料號的先
            for(IndexResult res : sortedIndexResult)
            {
            	uniqPn.put(res.getPn(), 1);
            	sPnReturn.add(res.getPn());
            }
            //sPnReturn.addAll(pnList);
            
            // FuzzySearch
            for(IndexResult tuple : fuzzyResult)
            {
            	if(!uniqPn.containsKey(tuple.getPn()))
            	{
            		uniqPn.put(tuple.getPn(), 1);
            		sPnReturn.add(tuple.getPn());
            	}
            }
          
            
            OmList.addAll(sPnReturn);
            
        }
        else
        {
        	List<String> pageList = new ArrayList<String> ();
        	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
        	
        	int nCount = 0;

        	for(IndexResult tuple : redisResult)
        	{
        		OmList.add(tuple.getPn());
        		
        		nCount++;
        		
        		// 先取500筆以上即可
            	if(nCount > 500)
            		break;
        	}
        	
        }
    
        StopWatch watch = new StopWatch("OrderManager");
        
        OrderManager om = new OrderManager();
        if(nSearchType == 1)	// 以純料號搜尋
        	result = om.getProductByGroupInStoreDeep(inventory, lead, rohs, mfs, abbreviation, OmList, currentPage, pageSize);
        else
        	result = om.getProductByGroupInStoreIdDeep(inventory, lead, rohs, mfs, abbreviation, OmList, currentPage, pageSize);
        
        watch.getElapseTimeOrderResult(keyQuery, OmList);

        // 去除逗號
        strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
        result.setHighLight(strHighLight);
        
        

        return result;
	}
    
    
    /* 20170606 ------------------            新網頁 */
    public OrderResultDetail QueryNewPageV1(String strData, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> pkg,
			int hasStock,
			int noStock,
			int hasPrice,
			int hasInquery,
			int amount,					// 起訂量
			List<String> currencies,	// 幣別
			List<Integer> catalog_ids,	// 分類ID
			int currentPage, 
			int pageSize)
	{
        // 回傳值
        OrderResultDetail result = null;
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        String strUntaged = "";
        String strTaged = "";
        
     // if cached_mfs
        List<IndexRate> cachedPnsMfs = KeywordLogic.getCacheMfs(strData);
        if(cachedPnsMfs.size() > 0)
        {
        	Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        	
        	// 加亮
            String strHighLight = "";
            for(String stoken:keyQuery.getKeyword())
            {
            	strHighLight += stoken + ",";
            }
            // Log 紀錄
            LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
  
            List<String> recordPns = new ArrayList<String>();
            
            // for Pns Record
            int count = 0;
            for(IndexRate res : cachedPnsMfs)
            {
            	recordPns.add(res.getPn());
            	count++;
            	
            	if(count > 200)
            		break;
            }

            // 計時
            StopWatch watch = new StopWatch("PmSearch");

            	
            watch.getElapseTime(keyQuery, recordPns);

            int nTotalCount = 0;

        
            watch = new StopWatch("OrderManager");
            
            OrderManager om = new OrderManager();
           
            result = om.QueryNewPageMfsV1(inventory, lead, rohs, mfs, abbreviation, cachedPnsMfs, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids);
            	
            watch.getElapseTimeOrderResult(keyQuery, recordPns);

            // 去除逗號
            strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
            //result.setHighLight(strHighLight);
            

            return result;
        }
        
        // if cached
        List<IndexResult> cachedPns = KeywordLogic.getCache(strData);
        if(cachedPns.size() > 0)
        {
        	Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        	
        	// 加亮
            String strHighLight = "";
            for(String stoken:keyQuery.getKeyword())
            {
            	strHighLight += stoken + ",";
            }
            // Log 紀錄
            LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
  
            List<String> recordPns = new ArrayList<String>();
            
            // for Pns Record
            int count = 0;
            for(IndexResult res : cachedPns)
            {
            	recordPns.add(res.getPn());
            	count++;
            	
            	if(count > 200)
            		break;
            }

            // 計時
            StopWatch watch = new StopWatch("PmSearch");

            	
            watch.getElapseTime(keyQuery, recordPns);

            int nTotalCount = 0;

        
            watch = new StopWatch("OrderManager");
            
            OrderManager om = new OrderManager();
           
            result = om.QueryNewPageMfsV1(inventory, lead, rohs, mfs, abbreviation, cachedPnsMfs, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids);
            	
            watch.getElapseTimeOrderResult(keyQuery, recordPns);

            // 去除逗號
            strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
            //result.setHighLight(strHighLight);
            
            return result;
        }
        
        // 分類搜尋
        if("".equals(strData) && catalog_ids != null && catalog_ids.size() > 0)
        {
        
        	List<String> catalogPns = OmSearchLogic.Catalog(catalog_ids);
        	List<IndexRate> aRet = new ArrayList<IndexRate>();
        	
        	// for Pns Record
            int count = 0;
            for(String res : catalogPns)
            {
            	IndexRate ir = new IndexRate(res, 0, res, 0, 0,  1);
			
				aRet.add(ir);
				
            	count++;
            	
            	if(count > 200)
            		break;
            }
        	
        	OrderManager om = new OrderManager();

        	result = om.QueryNewPageCatalogV1(inventory, lead, rohs, mfs, abbreviation, aRet, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids);
            
      
            // 去除逗號
            //result.setHighLight(strHighLight);
            
            return result;
        }
	
	 // 取出製造商
        List<String> core_mfs = new ArrayList<String>();
	    MfsAlternate retAlter = KeywordLogic.ExtractMfsName(strData, core_mfs, abbreviation);
	
	    strUntaged = retAlter.getStrData().trim();
	
		strTaged = retAlter.getTagData().trim();
	    
	    
	    //abbreviation = retAlter.getSupplier();
	    
	    // 分析輸入的查詢
	    Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData, strUntaged, strTaged, retAlter.getMfs(), retAlter.getSupplier());
	    // 加亮
	    String strHighLight = "";
	    for(String stoken:keyQuery.getKeyword())
	    {
	    	strHighLight += stoken + ",";
	    }
	    // Log 紀錄
	    LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
	    
	    // 純料號的結果
	    List<String> pnList = new ArrayList<String>();
	    // Redis的結果
	    List<IndexResult> redisResult = new ArrayList<IndexResult>();
	    // Fuzzy的結果
	    List<IndexResult> fuzzyResult = new ArrayList<IndexResult>();
	    
	    // 料號排序的結果
	    List<IndexResult> sortedIndexResult = null;
	    
	    // 如果輸入的查詢有問題，回傳空的結果
	    if(keyQuery.getCount()== 0)
	    {
	    	result = new OrderResultDetail();
	    	result.setTotalCount(0);
	    	result.setPns(new String[0]);
	    	//result.setPkg(new String[0]);
	    	//result.setSupplier(new String[0]);
	    	return result;
	    }
	    
	    // 純料號的方式
	    boolean bSearchPn = false;
	    List<KeywordKind> kinds = keyQuery.getKind();
	    for(KeywordKind kind : kinds)
	    {
	    	if(kind.equals(KeywordKind.IsPn))
	    		bSearchPn = true;
	    }
	
	    if (bSearchPn == true) {
	    	// 計時
	    	StopWatch watch = new StopWatch("PmSearch");
	    	
	    	pnList = PmSearchLogic.PmSearch(keyQuery);
	    
	    	// 
	    	//core_mfs = retAlter.getMfs();
	    	
	    	// 為了修正從pm_pn中找不到的問題
	    	if(!pnList.contains(strUntaged.toUpperCase()))
	    		pnList.add(strUntaged.toUpperCase());
	    	
	    	// 20160223 料號預先排序應該只限於排序料號
	    	HashMap<String, Integer> hashPnWeight = FuzzyManagerModel.OrderPn(pnList);
			sortedIndexResult = SortUtil.SortIndexResultSimple(hashPnWeight, 0);
	    	
	    	watch.getElapseTime(keyQuery, pnList);
	    	
	    	nSearchType = 1;
	    }
	    
	    if (bSearchPn == false) // Redis的交集
	    {
	    	StopWatch watch = new StopWatch("RedisSearch");
	    	
	    	try
	    	{
	    		redisResult = RedisSearchLogic.getRedisSearchId(keyQuery);
	    	}
	    	catch(Exception e)
	    	{
	    		List<String> sErr = new ArrayList<String>();
	    		sErr.add(e.getMessage());
	    		watch.getElapseTime(keyQuery, sErr);
	    	}
	    	watch.getElapseTimeIndexResult(keyQuery, redisResult);
	    	
	    }
	    
	 // 20160223 for more percisely pn search
	    if(pnList.size() == 0)
	    {
	        // 不夠的再由FuzzySearch補充
	        if(pnList.size() + redisResult.size() < 50)
	        {
	        	StopWatch watch = new StopWatch("FuzzySearch");
	        	if(nSearchType == 1)	// 以純料號搜尋
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearch(keyQuery);
	        	else
	        	{
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearchId(keyQuery);
	        		// reorder 
	        		//redisResult = SortUtil.RegroupIndexResult(redisResult, fuzzyResult);
	        		// 直接加在下面
	        		redisResult.addAll(fuzzyResult);
	        	}
	        	
	        	watch.getElapseTimeIndexResult(keyQuery, fuzzyResult);
	        }
	    }
	    
	    
	    
	    // 利用hash排除重複
	    Map<String, Integer> uniqPn = new HashMap<String, Integer>();
	    // 交給排序模組
	    List<String> OmList = new ArrayList<String>();
	  
	    int nTotalCount = 0;
	    
	    if(nSearchType == 1) // search by pm and/or fuzzy
	    {
	    	// 最後整理出的唯一料號表
	        List<String> sPnReturn = new ArrayList<String>();
	        
	        // 純料號的先
	        for(IndexResult res : sortedIndexResult)
	        {
	        	uniqPn.put(res.getPn(), 1);
	        	sPnReturn.add(res.getPn());
	        }
	        //sPnReturn.addAll(pnList);
	        
	        // FuzzySearch
	        for(IndexResult tuple : fuzzyResult)
	        {
	        	if(!uniqPn.containsKey(tuple.getPn()))
	        	{
	        		uniqPn.put(tuple.getPn(), 1);
	        		sPnReturn.add(tuple.getPn());
	        	}
	        }
	      
	        
	        OmList.addAll(sPnReturn);
	        
	    }
	    else
	    {
	    	List<String> pageList = new ArrayList<String> ();
	    	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
	    	
	    	int nCount = 0;
	
	    	for(IndexResult tuple : redisResult)
	    	{
	    		OmList.add(tuple.getPn());
	    		
	    		nCount++;
	    		
	    		// 先取500筆以上即可
	        	if(nCount > 1000)
	        		break;
	    	}
	    	
	    }
	
	    StopWatch watch = new StopWatch("OrderManager");
	    
	    OrderManager om = new OrderManager();

	    if(nSearchType == 1)
	    	result = om.QueryNewPageV1(inventory, lead, rohs, mfs, abbreviation, OmList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, core_mfs);
	    else
	    	result = om.QueryNewPageIdV1(inventory, lead, rohs, mfs, abbreviation, OmList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids);
	  
	    watch.getElapseTimeOrderResult(keyQuery, OmList);
	
	    // 去除逗號
	    strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
	    //result.setHighLight(strHighLight);
	    
	    if(retAlter.getMfs().size() > 0)
	    {
	    	List<Mfs> returnMfs = new ArrayList<Mfs>();
	    	for(Integer id : retAlter.getMfs())
	    	{
	    		for (Mfs mfsinstance : result.getMfsStandard()) {      
	            	// mfs
	    			if(mfsinstance.getId().equals(id))
	                {
	                	returnMfs.add(mfsinstance);
	                }
	            }
	        }
	    
	    	if(returnMfs.size() > 0)
	    		result.setMfsStandard(returnMfs);
	    }
	
	    return result;
	}
    
    public String sendGet(String url) {

        StringBuffer response = new StringBuffer();
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header

            con.setRequestProperty("Authorization", "myAuthorizationProp");

            int responseCode = 0;

            try {
            	responseCode = con.getResponseCode();
            }
            catch(Exception e)
            {
            	return "";
            }
            
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();


            if(responseCode != 200)
            {
            	System.out.println("\n" + dateFormat.format(date) + " Sending 'GET' request to URL : " + url);
                System.out.println("Response Code : " + responseCode);
                
                return "";
            }
            	
            
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;


            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response.toString();
    }
    
private List<String> ElasticQuery(String query){
    	
    	String strJson = "";
		try {
			// 2018/08/02 add index for product, mfs, news
			strJson = sendGet("http://192.168.3.221:9200" + "/product/_search?q=" + "\"" + URLEncoder.encode(query, "UTF-8") + "\"" + "&_source_include=pn&size=100&from=0");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
        List<String> idArray = new ArrayList<String>();

        JSONArray hitsArray = null;
        JSONObject hits = null;
        JSONObject source = null;
        JSONObject json = null;

        try
        {
        	if("".equalsIgnoreCase(strJson))
				return idArray;
	        json = new JSONObject(strJson);
	        hits = json.getJSONObject("hits");
	        hitsArray = hits.getJSONArray("hits");
	
	        for (int i=0; i<hitsArray.length(); i++) {
	            JSONObject h = hitsArray.getJSONObject(i);
	            source = h.getJSONObject("_source");
	            String object = (source.getString("pn"));
	            idArray.add(object);
	        }
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }

        /*
        if(idArray.size() == 0){
    		try {
    				strJson = sendGet("http://192.168.3.221:9200" + "/product/_search/?q=" + CommonUtil.getElasticQueryString(query).replaceAll("/", "//") + "&_source_include=pn&size=1000&from=0");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		
    		try
            {
    			if("".equalsIgnoreCase(strJson))
    				return idArray;
    	        json = new JSONObject(strJson);
    	        hits = json.getJSONObject("hits");
    	        hitsArray = hits.getJSONArray("hits");
    	
    	        for (int i=0; i<hitsArray.length(); i++) {
    	            JSONObject h = hitsArray.getJSONObject(i);
    	            source = h.getJSONObject("_source");
    	            String object = (source.getString("pn"));
    	            idArray.add(object);
    	        }
            }
            catch(Exception e)
            {
            	e.printStackTrace();
            }
		}
		*/
        
        return idArray;
    }
    
	private List<ProductDesign> ElasticQueryDesign(String pn, String org, int page){
		
		String strJson = "";
		try {
			// 2018/08/02 add index for product, mfs, news
			strJson = sendGet("http://192.168.3.221:9200" + "/mfs/_search?pretty&q=\"" + URLEncoder.encode(pn, "UTF-8") + "\"+\"" + URLEncoder.encode(org, "UTF-8") + "\"&size=5&from=" + page * 5);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	    List<ProductDesign> idArray = new ArrayList<ProductDesign>();
	
	    JSONArray hitsArray = null;
	    JSONObject hits = null;
	    JSONObject source = null;
	    JSONObject json = null;
	
	    try
	    {
	    	if("".equalsIgnoreCase(strJson))
				return idArray;
	        json = new JSONObject(strJson);
	        hits = json.getJSONObject("hits");
	        hitsArray = hits.getJSONArray("hits");
	
	        for (int i=0; i<hitsArray.length(); i++) {
	            JSONObject h = hitsArray.getJSONObject(i);
	            source = h.getJSONObject("_source");
	            
	            ProductDesign obj = new ProductDesign();
	            obj.setId(Integer.parseInt(source.getString("id")));
	            obj.setName(source.getString("name"));
	            obj.setMfs(source.getString("mfs"));
	            obj.setType(source.getString("total_count").equalsIgnoreCase("1") ? "app" : "design");
	            
	            idArray.add(obj);
	        }
	    }
	    catch(Exception e)
	    {
	    	e.printStackTrace();
	    }
	
	    return idArray;
	}

	private List<ProductNews> ElasticQueryNews(String pn, String org, int page){
	
		String strJson = "";
		try {
			// 2018/08/02 add index for product, mfs, news
			strJson = sendGet("http://192.168.3.221:9200" + "/news/_search?pretty&sort=create_time:desc&q=\"" + URLEncoder.encode(pn, "UTF-8") + "\"+\"" + URLEncoder.encode(org, "UTF-8") + "\"&size=5&from=" + page * 5);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	    List<ProductNews> idArray = new ArrayList<ProductNews>();
	
	    JSONArray hitsArray = null;
	    JSONObject hits = null;
	    JSONObject source = null;
	    JSONObject json = null;
	
	    try
	    {
	    	if("".equalsIgnoreCase(strJson))
				return idArray;
	        json = new JSONObject(strJson);
	        hits = json.getJSONObject("hits");
	        hitsArray = hits.getJSONArray("hits");
	
	        for (int i=0; i<hitsArray.length(); i++) {
	            JSONObject h = hitsArray.getJSONObject(i);
	            source = h.getJSONObject("_source");
	            
	            ProductNews obj = new ProductNews();
	            obj.setId(Integer.parseInt(source.getString("id")));
	            obj.setArticle(source.getString("main_title"));
	            obj.setCdate(source.getString("create_time").substring(0, 10));
	            
	            idArray.add(obj);
	        }
	    }
	    catch(Exception e)
	    {
	    	e.printStackTrace();
	    }
	
	    return idArray;
	}

    
    /* 20170726 ------------------            新網頁 */
    public OrderResultDetail QueryNewPageV2(String strData, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> pkg,
			int hasStock,
			int noStock,
			int hasPrice,
			int hasInquery,
			int amount,					// 起訂量
			List<String> currencies,	// 幣別
			List<Integer> catalog_ids,	// 分類ID
			int isLogin,				// 是否登入
			int isPaid,					// 是否付費
			int currentPage, 
			int pageSize)
	{
        // 回傳值
        OrderResultDetail result = null;
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        String strUntaged = "";
        String strTaged = "";
        
        // 20180506 transfer Big5 to Simplified chinese
        strData = ChineseUtils.toSimplified(strData);
        
        // 2018/03/23 for special TE
        if("TE".equalsIgnoreCase(strData.trim()))
        	strData = "TE CONNECTIVITY";
        
        // 2018/08/24 判斷是哪一種關鍵字
        
        
     // if cached_mfs
        List<IndexRate> cachedPnsMfs = KeywordLogic.getCacheMfs(strData);
        // 2018/05/19 for suppler search(digikey)
        if(cachedPnsMfs.size() > 0 && cachedPnsMfs.get(0).getPn().equalsIgnoreCase("supplier"))
        {
        	if(abbreviation == null)
        		abbreviation = new ArrayList<Integer>();
        	// get all supplier pn
            for(IndexRate res : cachedPnsMfs)
            	abbreviation.add(res.getOrder());
            
            Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        	
        	// 加亮
            String strHighLight = "";
            for(String stoken:keyQuery.getKeyword())
            {
            	strHighLight += stoken + ",";
            }
            
            List<String> dummyPns = new ArrayList<String>();
            List<String> dummyMfs = new ArrayList<String>();
            
            int nTotalCount = 0;

        
            OrderManager om = new OrderManager();
           
            result = om.QuerySupplierV2(inventory, lead, rohs, mfs, abbreviation, dummyPns, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, dummyMfs, isLogin, isPaid);	

            // 去除逗號
            strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
            //result.setHighLight(strHighLight);
            
            SetNewsAndDesign(result, strData);
            
            return result;
            
        }
        if(cachedPnsMfs.size() > 0)
        {
        	Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        	
        	// 加亮
            String strHighLight = "";
            for(String stoken:keyQuery.getKeyword())
            {
            	strHighLight += stoken + ",";
            }
            // Log 紀錄
            LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
  
            List<String> recordPns = new ArrayList<String>();
            
            // for Pns Record
            int count = 0;
            for(IndexRate res : cachedPnsMfs)
            {
            	recordPns.add(res.getPn());
            	count++;
            	
            	if(count > 500)
            		break;
            }

            // 計時
            StopWatch watch = new StopWatch("PmSearch");

            	
            watch.getElapseTime(keyQuery, recordPns);

            int nTotalCount = 0;

        
            watch = new StopWatch("OrderManager");
            
            OrderManager om = new OrderManager();
           
            result = om.QueryNewPageMfsV2(inventory, lead, rohs, mfs, abbreviation, cachedPnsMfs, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, isLogin, isPaid);
            	
            watch.getElapseTimeOrderResult(keyQuery, recordPns);

            // 去除逗號
            strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
            //result.setHighLight(strHighLight);
            
            SetNewsAndDesign(result, strData);
            
            return result;
        }
        
        
        // if cached
        List<IndexResult> cachedPns = KeywordLogic.getCache(strData);
        if(cachedPns.size() > 0)
        {
        	Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        	
        	// 加亮
            String strHighLight = "";
            for(String stoken:keyQuery.getKeyword())
            {
            	strHighLight += stoken + ",";
            }
            // Log 紀錄
            LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
  
            List<IndexRate> recordPns = new ArrayList<IndexRate>();
            
            // for Pns Record
            int count = 0;
            for(IndexResult res : cachedPns)
            {
            	IndexRate rate = new IndexRate(res.getPn(), 0, "", 0, 0, 0);
            	
            	recordPns.add(rate);
            	count++;
            	
            	if(count > 500)
            		break;
            }

            // 計時
            StopWatch watch = new StopWatch("PmSearch");

            	
            //watch.getElapseTime(keyQuery, recordPns);

            int nTotalCount = 0;

        
            watch = new StopWatch("OrderManager");
            
            OrderManager om = new OrderManager();
           
            result = om.QueryNewPageMfsV2(inventory, lead, rohs, mfs, abbreviation, recordPns, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, isLogin, isPaid);
            	
            //watch.getElapseTimeOrderResult(keyQuery, recordPns);

            // 去除逗號
            strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
            //result.setHighLight(strHighLight);
            
            SetNewsAndDesign(result, strData);
            
            return result;
        }
        
        // 20180829 供應商查詢
        List<Integer> supplierIds = OmSearchLogic.retSupplier(strData);
        if(supplierIds.size() > 0)
        {
        	
        	List<String> OmList = new ArrayList<String>();
        	
        	// for id Record
            for(Integer res : supplierIds)
            {
            	OmList.add(res.toString());
            }
        	
        	OrderManager om = new OrderManager();

        	result = om.QueryNewPageIdV2(inventory, lead, rohs, mfs, abbreviation, OmList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, isLogin, isPaid);
    	    
        	SetNewsAndDesign(result, strData);
        	
            return result;
        }
        
        // 分類搜尋
        if("".equals(strData) && catalog_ids != null && catalog_ids.size() > 0)
        {
        
        	List<String> catalogPns = OmSearchLogic.Catalog(catalog_ids);
        	List<IndexRate> aRet = new ArrayList<IndexRate>();
        	
        	// for Pns Record
            int count = 0;
            for(String res : catalogPns)
            {
            	IndexRate ir = new IndexRate(res, 0, res, 0, 0,  1);
			
				aRet.add(ir);
				
            	count++;
            	
            	if(count > 200)
            		break;
            }
        	
        	OrderManager om = new OrderManager();

        	result = om.QueryNewPageCatalogV2(inventory, lead, rohs, mfs, abbreviation, aRet, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, isLogin, isPaid);
            
      
            // 去除逗號
            //result.setHighLight(strHighLight);
        	
        	SetNewsAndDesign(result, strData);
        	
            return result;
        }
	
	 // 取出製造商
        List<String> core_mfs = new ArrayList<String>();
	    MfsAlternate retAlter = KeywordLogic.ExtractMfsName(strData, core_mfs, abbreviation);
	
	    strUntaged = retAlter.getStrData().trim();
	
		strTaged = retAlter.getTagData().trim();
	    
	    
	    //abbreviation = retAlter.getSupplier();
	    
	    // 分析輸入的查詢
	    Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData, strUntaged, strTaged, retAlter.getMfs(), retAlter.getSupplier());
	    // 加亮
	    String strHighLight = "";
	    for(String stoken:keyQuery.getKeyword())
	    {
	    	strHighLight += stoken + ",";
	    }
	    // Log 紀錄
	    LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
	    
	    // 純料號的結果
	    List<String> pnList = new ArrayList<String>();
	    // Redis的結果
	    List<IndexResult> redisResult = new ArrayList<IndexResult>();
	    // Fuzzy的結果
	    List<IndexResult> fuzzyResult = new ArrayList<IndexResult>();
	    
	    // 料號排序的結果
	    List<IndexResult> sortedIndexResult = null;
	    
	    // 如果輸入的查詢有問題，回傳空的結果
	    if(keyQuery.getCount()== 0)
	    {
	    	result = new OrderResultDetail();
	    	result.setTotalCount(0);
	    	result.setPns(new String[0]);
	    	//result.setPkg(new String[0]);
	    	//result.setSupplier(new String[0]);
	    	
	    	SetNewsAndDesign(result, strData);
	    	
	    	return result;
	    }
	    
	    // 純料號的方式
	    boolean bSearchPn = false;
	    List<KeywordKind> kinds = keyQuery.getKind();
	    for(KeywordKind kind : kinds)
	    {
	    	if(kind.equals(KeywordKind.IsPn))
	    		bSearchPn = true;
	    }
	    if(kinds.size() > 3)
	    	bSearchPn = false;
	
	    if (bSearchPn == true) {
	    	// 計時
	    	StopWatch watch = new StopWatch("PmSearch");
	    	
	    	pnList = PmSearchLogic.PmSearch(keyQuery);
	    
	    	// 
	    	//core_mfs = retAlter.getMfs();
	    	
	    	// 為了修正從pm_pn中找不到的問題
	    	if(!pnList.contains(strUntaged))
	    		pnList.add(strUntaged);
	    	
	    	// 20160223 料號預先排序應該只限於排序料號
	    	HashMap<String, Integer> hashPnWeight = FuzzyManagerModel.OrderPn(pnList);
			sortedIndexResult = SortUtil.SortIndexResultSimple(hashPnWeight, 0);
	    	
	    	watch.getElapseTime(keyQuery, pnList);
	    	
	    	nSearchType = 1;
	    }
	    
	    if (bSearchPn == false) // Redis的交集
	    {
	    	StopWatch watch = new StopWatch("RedisSearch");
	    	
	    	try
	    	{
	    		redisResult = RedisSearchLogic.getRedisSearchId(keyQuery);
	    	}
	    	catch(Exception e)
	    	{
	    		List<String> sErr = new ArrayList<String>();
	    		sErr.add(e.getMessage());
	    		watch.getElapseTime(keyQuery, sErr);
	    	}
	    	watch.getElapseTimeIndexResult(keyQuery, redisResult);
	    	
	    }
	    
	 // 20160223 for more percisely pn search
	    if(pnList.size() == 0)
	    {
	        // 不夠的再由FuzzySearch補充
	        if(pnList.size() + redisResult.size() < 50)
	        {
	        	StopWatch watch = new StopWatch("FuzzySearch");
	        	if(nSearchType == 1)	// 以純料號搜尋
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearch(keyQuery);
	        	else
	        	{
	        		// 2018/08/02 續用  for AAV501100B00000G
	        		// 废止 fuzzy search
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearchId(keyQuery);
	        		// reorder 
	        		redisResult = SortUtil.RegroupIndexResult(redisResult, fuzzyResult);
	        		// 直接加在下面
	        		redisResult.addAll(fuzzyResult);
	        	}
	        	
	        	watch.getElapseTimeIndexResult(keyQuery, fuzzyResult);
	        }
	    }
	    
	    
	    
	    // 利用hash排除重複
	    Map<String, Integer> uniqPn = new HashMap<String, Integer>();
	    // 交給排序模組
	    List<String> OmList = new ArrayList<String>();
	  
	    int nTotalCount = 0;
	    
	    if(nSearchType == 1) // search by pm and/or fuzzy
	    {
	    	// 最後整理出的唯一料號表
	        List<String> sPnReturn = new ArrayList<String>();
	        
	        // 純料號的先
	        for(IndexResult res : sortedIndexResult)
	        {
	        	uniqPn.put(res.getPn(), 1);
	        	sPnReturn.add(res.getPn());
	        }
	        //sPnReturn.addAll(pnList);
	        
	        // FuzzySearch
	        for(IndexResult tuple : fuzzyResult)
	        {
	        	if(!uniqPn.containsKey(tuple.getPn()))
	        	{
	        		uniqPn.put(tuple.getPn(), 1);
	        		sPnReturn.add(tuple.getPn());
	        	}
	        }
	      
	        
	        OmList.addAll(sPnReturn);
	        
	    }
	    else
	    {
	    	List<String> pageList = new ArrayList<String> ();
	    	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
	    	
	    	int nCount = 0;
	
	    	for(IndexResult tuple : redisResult)
	    	{
	    		OmList.add(tuple.getPn());
	    		
	    		nCount++;
	    		
	    		// 先取500筆以上即可
	        	if(nCount > 1000)
	        		break;
	    	}
	    	
	    }
	
	    StopWatch watch = new StopWatch("OrderManager");
	    
	    OrderManager om = new OrderManager();

	    if(nSearchType == 1)
	    	result = om.QueryNewPageV2(inventory, lead, rohs, mfs, abbreviation, OmList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, core_mfs, isLogin, isPaid);
	    else
	    	result = om.QueryNewPageIdV2(inventory, lead, rohs, mfs, abbreviation, OmList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, isLogin, isPaid);
	    
	    // 用 elasticSearch
	    if(result.getTotalCount() == 0)
	    {
	    	List<String> eIndex = ElasticQuery(strData);
	    	
	    	//if(eIndex.size() == 0)
	    	//	return result;
	    	
		    List<String> OmList1 = new ArrayList<String>();
		  
		    Set<String> uniqueGas = new HashSet<String>(eIndex);
		    OmList1.addAll(uniqueGas);
		    
		    // sort pn
		    List<IndexRate> recordPns = SortUtil.SortPnByCount(OmList1);
		    List<String> pageList = new ArrayList<String>();
		    int count = 0;
            for(IndexRate res : recordPns)
            {
            	pageList.add(res.getPn());
            	count++;
            	
            	if(count > 500)
            		break;
            }
		 
	    	if(OmList1.size() > 0)
	    		result = om.QueryNewPageV2(inventory, lead, rohs, mfs, abbreviation, pageList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, core_mfs, isLogin, isPaid);
	    		
	    		//result = om.QueryNewPageMfsV2(inventory, lead, rohs, mfs, abbreviation, recordPns, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, isLogin, isPaid);
	    }
	    
	    
	    // 20180517 关于模糊搜索推荐型号 用fuzzy
	    if(result.getTotalCount() == 0)
	    {
	    	List<String> pageList = OmSearchLogic.getFuzzyPns(strData);
	    	
	    	if(pageList.size() > 0)
	    		result = om.QueryNewPageV2(inventory, lead, rohs, mfs, abbreviation, pageList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, amount, currencies, catalog_ids, core_mfs, isLogin, isPaid);
	    }
	    
	    	
	  
	    watch.getElapseTimeOrderResult(keyQuery, OmList);
	
	    // 去除逗號
	    strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
	    //result.setHighLight(strHighLight);
	    
	    if(retAlter.getMfs().size() > 0)
	    {
	    	List<Mfs> returnMfs = new ArrayList<Mfs>();
	    	for(Integer id : retAlter.getMfs())
	    	{
	    		for (Mfs mfsinstance : result.getMfsStandard()) {      
	            	// mfs
	    			if(mfsinstance.getId().equals(id))
	                {
	                	returnMfs.add(mfsinstance);
	                }
	            }
	        }
	    
	    	if(returnMfs.size() > 0)
	    		result.setMfsStandard(returnMfs);
	    }
	    
	    SetNewsAndDesign(result, strData);
	    
	
	    return result;
	}



	/**
	 * @param result
	 * 2018/08/03
	 */
	private void SetNewsAndDesign(OrderResultDetail result, String qry) {
		// 2018/08/03 搜尋新聞
	    if(result.getTotalCount() > 0) {
	    	Map<String, List<ProductNews>> pn_news = new HashMap<String, List<ProductNews>>();
	    	Map<String, List<ProductDesign>> pn_design = new HashMap<String, List<ProductDesign>>();
	    	
	    	List<ProductDesign> mDesign;
	    	
	    	int newsPage = 0;
	    	int designPage = 0;
	    	
	    	HashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>> productList = result.getProductList();
	    	
	    	for (String key : productList.keySet()) {
	    		List<ProductNews> pnList = ElasticQueryNews(key, qry, newsPage);
	    		if(pnList.size() > 0)
	    			newsPage++;
	    		pn_news.put(key, pnList);
	    		
	    		List<ProductDesign> deList = ElasticQueryDesign(key, qry, designPage);
	    		if(deList.size() > 0)
	    			designPage++;
	    		pn_design.put(key, deList);
	    	}
	    	
	    	result.setPn_news(pn_news);
	    	result.setPn_Design(pn_design);
	    } else {
	    	Map<String, List<ProductNews>> pn_news = new HashMap<String, List<ProductNews>>();
	    	Map<String, List<ProductDesign>> pn_design = new HashMap<String, List<ProductDesign>>();
	    	
	    	result.setPn_news(pn_news);
	    	result.setPn_Design(pn_design);
	    }
	}
	    
    
    /* 20160706 ------------------            詳情頁深度搜尋 */
    public OrderResultDetail QueryFuzzyRecordByDeptSearchDetail(String strData, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> pkg,
			int hasStock,
			int noStock,
			int hasPrice,
			int hasInquery,
			int currentPage, 
			int pageSize)
	{
        // 回傳值
        OrderResultDetail result = null;
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        String strUntaged = "";
        String strTaged = "";
        
     // if cached_mfs
        List<IndexRate> cachedPnsMfs = KeywordLogic.getCacheMfs(strData);
        if(cachedPnsMfs.size() > 0)
        {
        	Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        	
        	// 加亮
            String strHighLight = "";
            for(String stoken:keyQuery.getKeyword())
            {
            	strHighLight += stoken + ",";
            }
            // Log 紀錄
            LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
  
            List<String> recordPns = new ArrayList<String>();
            
            // for Pns Record
            int count = 0;
            for(IndexRate res : cachedPnsMfs)
            {
            	recordPns.add(res.getPn());
            	count++;
            	
            	if(count > 200)
            		break;
            }

            // 計時
            StopWatch watch = new StopWatch("PmSearch");

            	
            watch.getElapseTime(keyQuery, recordPns);

            int nTotalCount = 0;

        
            watch = new StopWatch("OrderManager");
            
            OrderManager om = new OrderManager();
           
            result = om.getProductByGroupInStoreDeepDetailNewPagingMfs(inventory, lead, rohs, mfs, abbreviation, cachedPnsMfs, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize);
            	
            watch.getElapseTimeOrderResult(keyQuery, recordPns);

            // 去除逗號
            strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
            //result.setHighLight(strHighLight);
            

            return result;
        }
        
        // if cached
        List<IndexResult> cachedPns = KeywordLogic.getCache(strData);
        if(cachedPns.size() > 0)
        {
        	Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        	
        	// 加亮
            String strHighLight = "";
            for(String stoken:keyQuery.getKeyword())
            {
            	strHighLight += stoken + ",";
            }
            // Log 紀錄
            LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
  
            List<String> recordPns = new ArrayList<String>();
            
            // for Pns Record
            int count = 0;
            for(IndexResult res : cachedPns)
            {
            	recordPns.add(res.getPn());
            	count++;
            	
            	if(count > 200)
            		break;
            }

            // 計時
            StopWatch watch = new StopWatch("PmSearch");

            	
            watch.getElapseTime(keyQuery, recordPns);

            int nTotalCount = 0;

        
            watch = new StopWatch("OrderManager");
            
            OrderManager om = new OrderManager();
           
            result = om.getProductByGroupInStoreDeepDetailNewPaging(inventory, lead, rohs, mfs, abbreviation, cachedPns, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize);
            	
            watch.getElapseTimeOrderResult(keyQuery, recordPns);

            // 去除逗號
            strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
            //result.setHighLight(strHighLight);
            

            return result;
        }
        
        // 取出製造商
        MfsAlternate retAlter = KeywordLogic.ExtractMfs(strData, mfs, abbreviation);
  
        strUntaged = retAlter.getStrData().trim();
   
    	strTaged = retAlter.getTagData().trim();
        
        
        //abbreviation = retAlter.getSupplier();
        
        // 分析輸入的查詢
        Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData, strUntaged, strTaged, retAlter.getMfs(), retAlter.getSupplier());
        // 加亮
        String strHighLight = "";
        for(String stoken:keyQuery.getKeyword())
        {
        	strHighLight += stoken + ",";
        }
        // Log 紀錄
        LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
        
        // 純料號的結果
        List<String> pnList = new ArrayList<String>();
        // Redis的結果
        List<IndexResult> redisResult = new ArrayList<IndexResult>();
        // Fuzzy的結果
        List<IndexResult> fuzzyResult = new ArrayList<IndexResult>();
        
        // 料號排序的結果
        List<IndexResult> sortedIndexResult = null;
        
        // 如果輸入的查詢有問題，回傳空的結果
        if(keyQuery.getCount()== 0)
        {
        	result = new OrderResultDetail();
        	result.setTotalCount(0);
        	result.setPns(new String[0]);
        	//result.setPkg(new String[0]);
        	//result.setSupplier(new String[0]);
        	return result;
        }
        
        // 純料號的方式
        boolean bSearchPn = false;
        List<KeywordKind> kinds = keyQuery.getKind();
        for(KeywordKind kind : kinds)
        {
        	if(kind.equals(KeywordKind.IsPn))
        		bSearchPn = true;
        }

        if (bSearchPn == true) {
        	// 計時
        	StopWatch watch = new StopWatch("PmSearch");
        	
        	pnList = PmSearchLogic.PmSearch(keyQuery);
        
        	// 
        	mfs = retAlter.getMfs();
        	
        	// 為了修正從pm_pn中找不到的問題
        	if(!pnList.contains(strUntaged.toUpperCase()))
        		pnList.add(strUntaged.toUpperCase());
        	
        	// 20160223 料號預先排序應該只限於排序料號
        	HashMap<String, Integer> hashPnWeight = FuzzyManagerModel.OrderPn(pnList);
    		sortedIndexResult = SortUtil.SortIndexResultSimple(hashPnWeight, 0);
        	
        	watch.getElapseTime(keyQuery, pnList);
        	
        	nSearchType = 1;
        }
        
        if (bSearchPn == false) // Redis的交集
        {
        	StopWatch watch = new StopWatch("RedisSearch");
        	
        	try
        	{
        		redisResult = RedisSearchLogic.getRedisSearchId(keyQuery);
        	}
        	catch(Exception e)
        	{
        		List<String> sErr = new ArrayList<String>();
        		sErr.add(e.getMessage());
        		watch.getElapseTime(keyQuery, sErr);
        	}
        	watch.getElapseTimeIndexResult(keyQuery, redisResult);
        	
        }
        
     // 20160223 for more percisely pn search
        if(pnList.size() == 0)
        {
	        // 不夠的再由FuzzySearch補充
	        if(pnList.size() + redisResult.size() < 50)
	        {
	        	StopWatch watch = new StopWatch("FuzzySearch");
	        	if(nSearchType == 1)	// 以純料號搜尋
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearch(keyQuery);
	        	else
	        	{
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearchId(keyQuery);
	        		// reorder 
	        		//redisResult = SortUtil.RegroupIndexResult(redisResult, fuzzyResult);
	        		// 直接加在下面
	        		redisResult.addAll(fuzzyResult);
	        	}
	        	
	        	watch.getElapseTimeIndexResult(keyQuery, fuzzyResult);
	        }
        }
        
        
        
        // 利用hash排除重複
        Map<String, Integer> uniqPn = new HashMap<String, Integer>();
        // 交給排序模組
        List<String> OmList = new ArrayList<String>();
      
        int nTotalCount = 0;
        
        if(nSearchType == 1) // search by pm and/or fuzzy
        {
        	// 最後整理出的唯一料號表
            List<String> sPnReturn = new ArrayList<String>();
            
            // 純料號的先
            for(IndexResult res : sortedIndexResult)
            {
            	uniqPn.put(res.getPn(), 1);
            	sPnReturn.add(res.getPn());
            }
            //sPnReturn.addAll(pnList);
            
            // FuzzySearch
            for(IndexResult tuple : fuzzyResult)
            {
            	if(!uniqPn.containsKey(tuple.getPn()))
            	{
            		uniqPn.put(tuple.getPn(), 1);
            		sPnReturn.add(tuple.getPn());
            	}
            }
          
            
            OmList.addAll(sPnReturn);
            
        }
        else
        {
        	List<String> pageList = new ArrayList<String> ();
        	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
        	
        	int nCount = 0;

        	for(IndexResult tuple : redisResult)
        	{
        		OmList.add(tuple.getPn());
        		
        		nCount++;
        		
        		// 先取500筆以上即可
            	if(nCount > 1000)
            		break;
        	}
        	
        }
    
        StopWatch watch = new StopWatch("OrderManager");
        
        OrderManager om = new OrderManager();
        if(nSearchType == 1)	// 以純料號搜尋
        {
        	result = om.getProductByGroupInStoreDeepDetail(inventory, lead, rohs, mfs, abbreviation, OmList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, sortedIndexResult);
        	if(result.getTotalCount() == 0)
        		return QueryFuzzyRecordByDeptSearchDetailAgain(strData, inventory, lead, rohs, mfs, abbreviation, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize);
        }
        else
        	result = om.getProductByGroupInStoreIdDeepDetail(inventory, lead, rohs, mfs, abbreviation, OmList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize);
        
        watch.getElapseTimeOrderResult(keyQuery, OmList);

        // 去除逗號
        strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
        //result.setHighLight(strHighLight);
        
        if(retAlter.getMfs().size() > 0)
        {
        	List<Mfs> returnMfs = new ArrayList<Mfs>();
        	for(Integer id : retAlter.getMfs())
        	{
        		for (Mfs mfsinstance : result.getMfsStandard()) {      
                	// mfs
        			if(mfsinstance.getId().equals(id))
                    {
                    	returnMfs.add(mfsinstance);
                    }
                }
            }
        
        	if(returnMfs.size() > 0)
        		result.setMfsStandard(returnMfs);
        }

        return result;
	}
    
    
    /* 20160706 ------------------            詳情頁深度搜尋 */
    public OrderResultDetail QueryFuzzyRecordByDeptSearchDetailAgain(String strData, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> pkg,
			int hasStock,
			int noStock,
			int hasPrice,
			int hasInquery,
			int currentPage, 
			int pageSize)
	{
        // 回傳值
        OrderResultDetail result = null;
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        // 分析輸入的查詢
        Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        // 加亮
        String strHighLight = "";
        for(String stoken:keyQuery.getKeyword())
        {
        	strHighLight += stoken + ",";
        }
        // Log 紀錄
        LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
        
        // 純料號的結果
        List<String> pnList = new ArrayList<String>();
        // Redis的結果
        List<IndexResult> redisResult = new ArrayList<IndexResult>();
        // Fuzzy的結果
        List<IndexResult> fuzzyResult = new ArrayList<IndexResult>();
        
        // 料號排序的結果
        List<IndexResult> sortedIndexResult = null;
        
        // 如果輸入的查詢有問題，回傳空的結果
        if(keyQuery.getCount()== 0)
        {
        	result = new OrderResultDetail();
        	result.setTotalCount(0);
        	result.setPns(new String[0]);
        	//result.setPkg(new String[0]);
        	//result.setSupplier(new String[0]);
        	return result;
        }
        
        
        
        if (keyQuery.getCount() > 1) // Redis的交集
        {
        	StopWatch watch = new StopWatch("RedisSearch");
        	
        	try
        	{
        		redisResult = RedisSearchLogic.getRedisSearchId(keyQuery);
        	}
        	catch(Exception e)
        	{
        		List<String> sErr = new ArrayList<String>();
        		sErr.add(e.getMessage());
        		watch.getElapseTime(keyQuery, sErr);
        	}
        	watch.getElapseTimeIndexResult(keyQuery, redisResult);
        	
        }
        
     // 20160223 for more percisely pn search
        if(pnList.size() == 0)
        {
	        // 不夠的再由FuzzySearch補充
	        if(pnList.size() + redisResult.size() < 50)
	        {
	        	StopWatch watch = new StopWatch("FuzzySearch");
	        	if(nSearchType == 1)	// 以純料號搜尋
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearch(keyQuery);
	        	else
	        	{
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearchId(keyQuery);
	        		// reorder 
	        		//redisResult = SortUtil.RegroupIndexResult(redisResult, fuzzyResult);
	        		// 直接加在下面
	        		redisResult.addAll(fuzzyResult);
	        	}
	        	
	        	watch.getElapseTimeIndexResult(keyQuery, fuzzyResult);
	        }
        }
        
        
        
        // 利用hash排除重複
        Map<String, Integer> uniqPn = new HashMap<String, Integer>();
        // 交給排序模組
        List<String> OmList = new ArrayList<String>();
      
        int nTotalCount = 0;
        
        if(nSearchType == 1) // search by pm and/or fuzzy
        {
        	// 最後整理出的唯一料號表
            List<String> sPnReturn = new ArrayList<String>();
            
            // 純料號的先
            for(IndexResult res : sortedIndexResult)
            {
            	uniqPn.put(res.getPn(), 1);
            	sPnReturn.add(res.getPn());
            }
            //sPnReturn.addAll(pnList);
            
            // FuzzySearch
            for(IndexResult tuple : fuzzyResult)
            {
            	if(!uniqPn.containsKey(tuple.getPn()))
            	{
            		uniqPn.put(tuple.getPn(), 1);
            		sPnReturn.add(tuple.getPn());
            	}
            }
          
            
            OmList.addAll(sPnReturn);
            
        }
        else
        {
        	List<String> pageList = new ArrayList<String> ();
        	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
        	
        	int nCount = 0;

        	for(IndexResult tuple : redisResult)
        	{
        		OmList.add(tuple.getPn());
        		
        		nCount++;
        		
        		// 先取500筆以上即可
            	if(nCount > 500)
            		break;
        	}
        	
        }
    
        StopWatch watch = new StopWatch("OrderManager");
        
        OrderManager om = new OrderManager();
        if(nSearchType == 1)	// 以純料號搜尋
        	result = om.getProductByGroupInStoreDeepDetail(inventory, lead, rohs, mfs, abbreviation, OmList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize, sortedIndexResult);
        else
        	result = om.getProductByGroupInStoreIdDeepDetail(inventory, lead, rohs, mfs, abbreviation, OmList, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize);
        
        watch.getElapseTimeOrderResult(keyQuery, OmList);

        // 去除逗號
        strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
        //result.setHighLight(strHighLight);
        
        

        return result;
	}
	
	
	public OrderResult QueryFuzzyRecordByListPage(String strData, int currentPage, int pageSize)
	{
        // 回傳值
        OrderResult result = null;
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        // 分析輸入的查詢
        Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        // 加亮
        String strHighLight = "";
        for(String stoken:keyQuery.getKeyword())
        {
        	strHighLight += stoken + ",";
        }
        // Log 紀錄
        LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
        
        // 純料號的結果
        List<String> pnList = new ArrayList<String>();
        // Redis的結果
        List<IndexResult> redisResult = new ArrayList<IndexResult>();
        // Fuzzy的結果
        List<IndexResult> fuzzyResult = new ArrayList<IndexResult>();
        // 料號排序的結果
        List<IndexResult> sortedIndexResult = null;
        
        // 如果輸入的查詢有問題，回傳空的結果
        if(keyQuery.getCount()== 0)
        {
        	result = new OrderResult();
        	result.setTotalCount(0);
        	return result;
        }
        
        // 純料號的方式
        if (keyQuery.getCount() == 1 && keyQuery.getKind().get(0).equals(KeywordKind.IsPn)) {
        	// 計時
        	StopWatch watch = new StopWatch("PmSearch");
        	
        	pnList = PmSearchLogic.PmSearch(keyQuery);
        	
        	// 為了修正從pm_pn中找不到的問題
        	if(!pnList.contains(strData.toUpperCase()))
        		pnList.add(strData.toUpperCase());
        	
        	// 20160223 料號預先排序應該只限於排序料號
        	HashMap<String, Integer> hashPnWeight = FuzzyManagerModel.OrderPn(pnList);
    		sortedIndexResult = SortUtil.SortIndexResultSimple(hashPnWeight, 0);
        	
        	watch.getElapseTime(keyQuery, pnList);
        	
        	nSearchType = 1;
        }
        
        if (keyQuery.getCount() > 1) // Redis的交集
        {
        	StopWatch watch = new StopWatch("RedisSearch");
        	
        	try
        	{
        		redisResult = RedisSearchLogic.getRedisSearchId(keyQuery);
        	}
        	catch(Exception e)
        	{
        		List<String> sErr = new ArrayList<String>();
        		sErr.add(e.getMessage());
        		watch.getElapseTime(keyQuery, sErr);
        	}
        	watch.getElapseTimeIndexResult(keyQuery, redisResult);
        	
        }
        
        // 20160223 for more percisely pn search
        if(pnList.size() == 0)
        {
	        // 不夠的再由FuzzySearch補充
	        if(pnList.size() + redisResult.size() < 50)
	        {
	        	StopWatch watch = new StopWatch("FuzzySearch");
	        	if(nSearchType == 1)	// 以純料號搜尋
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearch(keyQuery);
	        	else
	        	{
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearchId(keyQuery);
	        		// reorder 
	        		//redisResult = SortUtil.RegroupIndexResult(redisResult, fuzzyResult);
	        		// 直接加在下面
	        		redisResult.addAll(fuzzyResult);
	        	}
	        	
	        	watch.getElapseTimeIndexResult(keyQuery, fuzzyResult);
	        }
        }
        
        
        
        // 利用hash排除重複
        Map<String, Integer> uniqPn = new HashMap<String, Integer>();
        // 交給排序模組
        List<String> OmList = new ArrayList<String>();
      
        int nTotalCount = 0;
        
        if(nSearchType == 1) // search by pm and/or fuzzy
        {
        	// 最後整理出的唯一料號表
            List<String> sPnReturn = new ArrayList<String>();
            
            // 純料號的先
            for(IndexResult res : sortedIndexResult)
            {
            	uniqPn.put(res.getPn(), 1);
            	sPnReturn.add(res.getPn());
            }
            //sPnReturn.addAll(pnList);
            
            // FuzzySearch
            for(IndexResult tuple : fuzzyResult)
            {
            	if(!uniqPn.containsKey(tuple.getPn()))
            	{
            		uniqPn.put(tuple.getPn(), 1);
            		sPnReturn.add(tuple.getPn());
            	}
            }
            
            //// 20160127 料號預先排序
    		//HashMap<String, Integer> hashPnWeight = FuzzyManagerModel.OrderPn(sPnReturn);
    		//List<IndexResult> sortedIndexResult = SortUtil.SortIndexResultSimple(hashPnWeight, 0);
    		
    		// 20160129 改用深度搜尋的分頁法
    		//for(IndexResult res : sortedIndexResult)
    		OmList.addAll(sPnReturn);
    		
    		//for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
            //{
            //	if(i<sortedIndexResult.size())
            //	{
            //		OmList.add(sortedIndexResult.get(i).getPn());
            //	}
            //}
            
            //nTotalCount = sortedIndexResult.size();
          
            
            //for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
            //{
            //	if(i<sPnReturn.size())
            //	{
            //		OmList.add(sPnReturn.get(i));
            //	}
            //}
            
            //nTotalCount = sPnReturn.size();
        }
        else
        {
        	List<String> pageList = new ArrayList<String> ();
        	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
        	
        	int nCount = 0;

        	for(IndexResult tuple : redisResult)
        	{
        		OmList.add(tuple.getPn());
        		
        		nCount++;
        		
        		// 先取500筆以上即可
            	if(nCount > 500)
            		break;
        	}
        	
        }
        
        OmList = CommonUtil.removeSpaceList(OmList);
//        20160304 fix paging bug 
//        {
//        	List<String> pageList = new ArrayList<String> ();
//        	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
//        	
//        	int gPage = 0;
//        	int gWeight = 0;
//        	for(IndexResult tuple : redisResult)
//        	{
//        		if(!uniqPn.containsKey(tuple.getPn()))
//            	{
//        			pageList.add(tuple.getPn());
//        			
//            		uniqPn.put(tuple.getPn(), 1);
//            		
//            		
//            		if(gWeight != tuple.getWeight())
//            		{
//        				if(gPage > 0)
//        				{
//        					pageMap.put(gPage - 1, pageList);
//        					
//        					pageList = new ArrayList<String> ();
//        				}
//        				
//        				gPage++;
//        				gWeight = tuple.getWeight();
//            		}
//            	}
//        	}
//        	// do it again
//        	if(redisResult.size() > 0)
//        	{
//        		pageMap.put(gPage - 1, pageList);
//
//        	}
//        	
//        	int aPage = 0;
//        	// set area by weight
//        	for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
//            {
//            	if(i<gPage)
//            	{
//            		List<String> pList = pageMap.get(i);
//            		OmList.addAll(pList);
//            	}
//            }
//        	
//        	nTotalCount = gPage;
//        }
    	
   
        StopWatch watch = new StopWatch("OrderManager");
        
        OrderManager om = new OrderManager();
        if(nSearchType == 1)	// 以純料號搜尋
        {
        	//result = om.getProductByGroupInStore(OmList);
        	
        	// 20160129 改用深度搜尋的分頁法
        	result = om.getProductByGroupInStoreDeep(0, 0, 0, null, null, OmList, currentPage, pageSize);
        	
        	if(result.getTotalCount() == 0)
        		return QueryFuzzyRecordByListPageAgain(strData, currentPage, pageSize);
        }
        else
        	result = om.getProductByGroupInStoreIdDeep(0, 0, 0, null, null, OmList, currentPage, pageSize);
//          20160304 fix paging bug 
//        {
//        	result = om.getProductByGroupInStoreId(OmList);
//        	result.setTotalCount(nTotalCount);
//        }
        
        watch.getElapseTimeOrderResult(keyQuery, OmList);

        // 去除逗號
        strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
        result.setHighLight(strHighLight);
        
        return result;
	}
	
	
	public OrderResult QueryFuzzyRecordByListPageAgain(String strData, int currentPage, int pageSize)
	{
        // 回傳值
        OrderResult result = null;
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        // 分析輸入的查詢
        Keyword keyQuery = KeywordLogic.GetAnalyzedKeywords(strData);
        // 加亮
        String strHighLight = "";
        for(String stoken:keyQuery.getKeyword())
        {
        	strHighLight += stoken + ",";
        }
        // Log 紀錄
        LogQueryHistory.InsertQueryLog(keyQuery, currentPage);
        
        // 純料號的結果
        List<String> pnList = new ArrayList<String>();
        // Redis的結果
        List<IndexResult> redisResult = new ArrayList<IndexResult>();
        // Fuzzy的結果
        List<IndexResult> fuzzyResult = new ArrayList<IndexResult>();
        // 料號排序的結果
        List<IndexResult> sortedIndexResult = null;
        
        // 如果輸入的查詢有問題，回傳空的結果
        if(keyQuery.getCount()== 0)
        {
        	result = new OrderResult();
        	result.setTotalCount(0);
        	return result;
        }
        
        
        if (keyQuery.getCount() > 1) // Redis的交集
        {
        	StopWatch watch = new StopWatch("RedisSearch");
        	
        	try
        	{
        		redisResult = RedisSearchLogic.getRedisSearchId(keyQuery);
        	}
        	catch(Exception e)
        	{
        		List<String> sErr = new ArrayList<String>();
        		sErr.add(e.getMessage());
        		watch.getElapseTime(keyQuery, sErr);
        	}
        	watch.getElapseTimeIndexResult(keyQuery, redisResult);
        	
        }
        
        // 20160223 for more percisely pn search
        if(pnList.size() == 0)
        {
	        // 不夠的再由FuzzySearch補充
	        if(pnList.size() + redisResult.size() < 50)
	        {
	        	StopWatch watch = new StopWatch("FuzzySearch");
	        	if(nSearchType == 1)	// 以純料號搜尋
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearch(keyQuery);
	        	else
	        	{
	        		fuzzyResult = FuzzySearchLogic.getFuzzySearchId(keyQuery);
	        		// reorder 
	        		//redisResult = SortUtil.RegroupIndexResult(redisResult, fuzzyResult);
	        		// 直接加在下面
	        		redisResult.addAll(fuzzyResult);
	        	}
	        	
	        	watch.getElapseTimeIndexResult(keyQuery, fuzzyResult);
	        }
        }
        
        
        
        // 利用hash排除重複
        Map<String, Integer> uniqPn = new HashMap<String, Integer>();
        // 交給排序模組
        List<String> OmList = new ArrayList<String>();
      
        int nTotalCount = 0;
        
        if(nSearchType == 1) // search by pm and/or fuzzy
        {
        	// 最後整理出的唯一料號表
            List<String> sPnReturn = new ArrayList<String>();
            
            // 純料號的先
            for(IndexResult res : sortedIndexResult)
            {
            	uniqPn.put(res.getPn(), 1);
            	sPnReturn.add(res.getPn());
            }
            //sPnReturn.addAll(pnList);
            
            // FuzzySearch
            for(IndexResult tuple : fuzzyResult)
            {
            	if(!uniqPn.containsKey(tuple.getPn()))
            	{
            		uniqPn.put(tuple.getPn(), 1);
            		sPnReturn.add(tuple.getPn());
            	}
            }
            
            //// 20160127 料號預先排序
    		//HashMap<String, Integer> hashPnWeight = FuzzyManagerModel.OrderPn(sPnReturn);
    		//List<IndexResult> sortedIndexResult = SortUtil.SortIndexResultSimple(hashPnWeight, 0);
    		
    		// 20160129 改用深度搜尋的分頁法
    		//for(IndexResult res : sortedIndexResult)
    		OmList.addAll(sPnReturn);
    		
    		//for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
            //{
            //	if(i<sortedIndexResult.size())
            //	{
            //		OmList.add(sortedIndexResult.get(i).getPn());
            //	}
            //}
            
            //nTotalCount = sortedIndexResult.size();
          
            
            //for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
            //{
            //	if(i<sPnReturn.size())
            //	{
            //		OmList.add(sPnReturn.get(i));
            //	}
            //}
            
            //nTotalCount = sPnReturn.size();
        }
        else
        {
        	List<String> pageList = new ArrayList<String> ();
        	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
        	
        	int nCount = 0;

        	for(IndexResult tuple : redisResult)
        	{
        		OmList.add(tuple.getPn());
        		
        		nCount++;
        		
        		// 先取500筆以上即可
            	if(nCount > 500)
            		break;
        	}
        	
        }
        
        OmList = CommonUtil.removeSpaceList(OmList);
//        20160304 fix paging bug 
//        {
//        	List<String> pageList = new ArrayList<String> ();
//        	Map<Integer, List<String>> pageMap = new HashMap<Integer, List<String>>();
//        	
//        	int gPage = 0;
//        	int gWeight = 0;
//        	for(IndexResult tuple : redisResult)
//        	{
//        		if(!uniqPn.containsKey(tuple.getPn()))
//            	{
//        			pageList.add(tuple.getPn());
//        			
//            		uniqPn.put(tuple.getPn(), 1);
//            		
//            		
//            		if(gWeight != tuple.getWeight())
//            		{
//        				if(gPage > 0)
//        				{
//        					pageMap.put(gPage - 1, pageList);
//        					
//        					pageList = new ArrayList<String> ();
//        				}
//        				
//        				gPage++;
//        				gWeight = tuple.getWeight();
//            		}
//            	}
//        	}
//        	// do it again
//        	if(redisResult.size() > 0)
//        	{
//        		pageMap.put(gPage - 1, pageList);
//
//        	}
//        	
//        	int aPage = 0;
//        	// set area by weight
//        	for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
//            {
//            	if(i<gPage)
//            	{
//            		List<String> pList = pageMap.get(i);
//            		OmList.addAll(pList);
//            	}
//            }
//        	
//        	nTotalCount = gPage;
//        }
    	
   
        StopWatch watch = new StopWatch("OrderManager");
        
        OrderManager om = new OrderManager();
        if(nSearchType == 1)	// 以純料號搜尋
        {
        	//result = om.getProductByGroupInStore(OmList);
        	
        	// 20160129 改用深度搜尋的分頁法
        	result = om.getProductByGroupInStoreDeep(0, 0, 0, null, null, OmList, currentPage, pageSize);
        }
        else
        	result = om.getProductByGroupInStoreIdDeep(0, 0, 0, null, null, OmList, currentPage, pageSize);
//          20160304 fix paging bug 
//        {
//        	result = om.getProductByGroupInStoreId(OmList);
//        	result.setTotalCount(nTotalCount);
//        }
        
        watch.getElapseTimeOrderResult(keyQuery, OmList);

        // 去除逗號
        strHighLight = strHighLight.substring(0, strHighLight.length() - 1);
        result.setHighLight(strHighLight);
        
        return result;
	}

//	
//	public List<String> GetQueryByEachWord(String strData, Connection conn, CRFClassifier<CoreLabel> segmenter)
//	{
//		long startTime = System.currentTimeMillis();
//		
//		long elapsedSqlTime = 0L;
//		
//		String[] strFullword = null;
//		List<String> sList = new ArrayList<String>();
//		
//		List<IndexRate> sFullCompare = new ArrayList<IndexRate>();
//		
//		String sNumber = "20000";
//		int nTotal = 20;
//		
//		if (strData != null && !strData.isEmpty())
//		{
//			strData = strData.toUpperCase();
//			strFullword = strData.replaceAll("^[,\\s]+", "").split("[,\\s]+");
//		}
//		else
//			return sList;
//		
//		String sCombine = "";
//		
//		String strSql = "";
//		
//		HashMap<String,Float> PnWeightMap = new HashMap<String,Float>();
//		ValueComparator bvc =  new ValueComparator(PnWeightMap);
//        TreeMap<String,Float> sorted_map = new TreeMap<String,Float>(bvc);
//        
//        // 找各關鍵字對應的料號，如果重複，要排在前面
//        HashMap<String, Integer> PnKeywordMap = new HashMap<String, Integer>();
//        IntComparator ivc =  new IntComparator(PnKeywordMap);
//        TreeMap<String,Integer> int_map = new TreeMap<String,Integer>(ivc);
//        
//        // 找各關鍵字對應的料號，如果重複，要排在前面
//        HashMap<Integer, Integer> PnOrderMap = new HashMap<Integer, Integer>();
//        OrdComparator ovc =  new OrdComparator(PnOrderMap);
//        TreeMap<Integer,Integer> ord_map = new TreeMap<Integer,Integer>(ovc);
//        
//        HashMap<Integer, String> hashPn = new HashMap<Integer, String>();
//        
//        
//        // 取得關鍵字調整
//        List<IndexAdj> adjust = FuzzyManagerModel.GetAdjust();
//        // 取得縮寫字調整
//        List<IndexShort> breif = FuzzyManagerModel.GetShort();
//        
//     // 20150729 for 精密搜尋
//    	int order = 0;
//    	int orderCount = 0;
//    	
//    	ArrayList<Integer> aryPreOrder = new ArrayList<Integer>();
//    	
//    	ArrayList<Integer> aryOrderCount = new ArrayList<Integer>();
//    	
//    	
//
//        if (strFullword != null) {
//        	// 增加縮寫字如:TI => Texas Instruments
//        	ArrayList<String> keywords = new ArrayList<String>();
//        	
//        	for (String stoken : strFullword)
//        	{
//        		keywords.add(stoken);
//        		
//        		for(IndexShort element : breif)
//        		{
//        			if(element.getWord().equalsIgnoreCase(stoken))
//        			{
//        				keywords.add(element.getAlterword());
//        				keywords.remove(stoken);	// 置換字要不要移除，之後再討論
//        			}
//        		}
//        	}
//        	
//        	
//			for (String stoken : keywords) {
//				
//				// 記錄第幾個查詢字
//				order++;
//				orderCount = 0;
//				aryPreOrder.clear();
//
//				strSql = "";
//				
//				if (SkipWord(stoken) || stoken.length() == 0)
//					continue;
//				
//				String sKeyword = "";
//				String sFullword = "";
//				sList.clear();
//				
//				sKeyword = stoken;
//
//				List<String> segmented = segmenter.segmentString(stoken);
//				
//				//List<String> segmented = new ArrayList<String>();
//				//segmented.add(stoken);
//	
//				// 2015/08/19 云云認為模糊搜尋搜出無關緊要的
//				/*
//				if (segmented != null) {
//					for (String element : segmented) {
//
//						if (SkipWord(element) || element.length() == 0)
//							continue;
//
//						sList.add(element);
//						
//
//					}
//				}
//				*/
//				
//				String strInverseArray[] = stoken.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
//				String strInverseString = "";
//				
//				// 去除尾部
//				if(strInverseArray.length > 1)
//				{
//					for(int i=0; i<strInverseArray.length - 1; i++)
//					{
//						strInverseString += strInverseArray[i].toString();
//					}
//				}
//				
//				
//				sList.add(strInverseString);
//				
//				// 2015/08/19 云云認為模糊搜尋搜出無關緊要的 end
//				
//				// 再加一個不要段字的
//				sList.remove(stoken);
//				//sList.add(stoken);
//				strSql += "(select pn, weight, fullword, kind, page, " + order + " from qeindex where word like '"
//						+ stoken + "%' and weight >= 0.5 limit " + sNumber + ") ";
//				strSql += " union ";
//				
//				// 目前先都不要用like
//				for (int i = 0; i < sList.size(); i++) {
//					
//					if(!stoken.equals((sList).get(i)))
//					{
//						
//						strSql += "(select pn, weight, fullword, kind, page, " + order + " from qeindex where word like '"
//								+ sList.get(i) + "%' and weight >= 0.5 order by weight desc limit " + sNumber + ") ";
//						strSql += " union ";
//						
//					}
//				}
//				
//			
//
//				strSql = strSql.substring(0, strSql.length() - 7);
//				
//				long startSqlTime = System.currentTimeMillis();
//				
//				List<IndexRate> sIndexRate = GetAllIndexRate(strSql, conn);
//				
//				long stopSqlTime = System.currentTimeMillis();
//				elapsedSqlTime += stopSqlTime - startSqlTime;
//				
//				sCombine += strSql + ":" + elapsedSqlTime + ";";
//				
//				// 調整權證正確性
//				for(IndexRate iter:sIndexRate)
//				{
//					String lngWord = "";
//					String shtWord = "";
//					float addWeight = 0f;
//					
//					// 查詢字與關鍵字比對長度後對換
//					// 方便string compare時找頻率
//					if(iter.getFullword().length() >= sKeyword.length())
//					{
//						lngWord = iter.getFullword();
//						shtWord = sKeyword;
//						
//						// 部分相同
//						addWeight = 0.5f;
//					}
//					else
//					{
//						lngWord = sKeyword;
//						shtWord = iter.getFullword();
//						
//						// 查詢字大於關鍵字
//						addWeight = 0.15f;
//					}
//					
//					if(CountStringOccurrences(lngWord, shtWord) >= 1)
//					{
//						// 完全符合時提高
//						if(lngWord.length() == shtWord.length())
//						{
//							// 找完全符合的(但不要重複)
//							if(!aryPreOrder.contains(iter.getPage()))
//							{
//								iter.setWeight(iter.getWeight() + 5);
//							
//								aryPreOrder.add(iter.getPage());
//								sFullCompare.add(iter);
//								orderCount++;
//							}
//
//						}
//						else
//							iter.setWeight(iter.getWeight() + addWeight);
//					}
//					else
//						iter.setWeight(0.01f);
//					
//					// 調整關鍵字(依照類別)
//					for(IndexAdj adj:adjust)
//					{
//						if(adj.getWord().equalsIgnoreCase(iter.getFullword()) && adj.getKind()==iter.getKind())
//							iter.setWeight(iter.getWeight() + adj.getAdjust());
//
//						if(adj.getWord().equalsIgnoreCase(iter.getFullword()) && adj.getKind()!=iter.getKind())
//							iter.setWeight(iter.getWeight() - adj.getAdjust());
//					
//					}
//					
//					if(PnWeightMap.containsKey(iter.getPn()))
//		            {
//						Float fWeight = PnWeightMap.get(iter.getPn());
//						
//						// 改成自然對數，減少數字太大的影響力
//						PnWeightMap.put(iter.getPn(), (float)Math.log(fWeight+iter.getWeight()));
//
//		            }
//		            else {
//		            	// 改成自然對數，減少數字太大的影響力
//		            	PnWeightMap.put(iter.getPn(), (float)Math.log(iter.getWeight()));
//		            }
//				}
//				
//				// 不同關鍵字，但是有相同料號，將會排在前面給前端
//				for (Map.Entry<String, Float> entry  : PnWeightMap.entrySet()) {
//					
//					String key = entry.getKey();
//					Float value = entry.getValue();
//					
//					int weight = 0;
//					
//					if(CountStringOccurrences(stoken, key) >= 1)
//					{
//						// 完全符合時提高
//						if(stoken.length() == stoken.length())
//							weight = 2;
//						else
//							weight = 1;
//					}
//					else
//						weight = 0;
//				    
//					if(weight > 0)	// 到達一定權重再加
//					{
//						if(PnKeywordMap.containsKey(key))
//			            {
//							Integer iCount = PnKeywordMap.get(key);
//							
//							PnKeywordMap.put(key, iCount+weight);
//	
//			            }
//						
//			            else {
//	
//			            	PnKeywordMap.put(key, weight);
//			            }
//					}
//					
//				}
//				
//				// 把這個關鍵字完全符合的數量記錄起來
//				aryOrderCount.add(orderCount);
//				
//			}
//		}
//        
//        int nOrderNumber = 0;
//        // 先處理完全符合的
//        for(IndexRate order1:sFullCompare)
//		{
//        	if(PnOrderMap.containsKey(order1.getPage()))
//        	{
//        		int count = PnOrderMap.get(order1.getPage());
//				PnOrderMap.put(order1.getPage(), count + 1);
//				
//				nOrderNumber++;
//        	}
//            else
//            {
//            	hashPn.put(order1.getPage(), order1.getPn());
//            	PnOrderMap.put(order1.getPage(), 1);
//            }
//		}
//        
//        ord_map.putAll(PnOrderMap);
//        
//        int_map.putAll(PnKeywordMap);
//        
//        // 處理一項查詢中，有兩個以上關鍵字符合者
//        for(Map.Entry<String,Integer> entry : int_map.entrySet()) {
//			if(entry.getValue() > 1)
//			{
//				Float value = PnWeightMap.get(entry.getKey());
//				
//				PnWeightMap.put(entry.getKey(), value * entry.getValue());
//			}
//
//		}
//		
//		// sort 後傳回
//		sorted_map.putAll(PnWeightMap);
//
//		List<String> sPnReturn = new ArrayList<String>();
//		
//		int iCount = 0;
//		
//		if(nOrderNumber >= 1)
//		{
//			for(Map.Entry<Integer,Integer> entry : ord_map.entrySet()) {
//				if(!sPnReturn.contains(hashPn.get(entry.getKey())))
//				{
//					if(iCount > nTotal)
//						break;
//					
//					if(entry.getValue()==1)
//						break;
//					
//					sPnReturn.add(hashPn.get(entry.getKey()));
//					iCount++;
//					
//					
//				}
//			}
//		}
//		
//		
//		// 模糊搜尋讓出來
//		if(nOrderNumber == 0)
//		{
//			// 各取幾個?
//			int iTake = 0;
//			
//			int iKeyword = 0;
//			int keyword_take = 0;
//			
//			
//			if(aryOrderCount.size() > 0)
//			{
//				iTake = (nTotal - 2) / aryOrderCount.size();
//
//				for(IndexRate order1:sFullCompare)
//				{
//					if(iCount > nTotal)
//						break;
//					
//					if(keyword_take > iTake)
//					{
//						keyword_take = 0;
//						iKeyword++;
//						
//						continue;
//					}
//					
//					if(order1.getOrder() > iKeyword)
//					{
//						keyword_take = 0;
//						iKeyword++;
//						
//						continue;
//					}
//						
//					
//					if(!sPnReturn.contains(hashPn.get(order1.getPage())))
//		        	{
//		        		if(order1.getOrder() == iKeyword)
//		        		{
//		        			sPnReturn.add(hashPn.get(order1.getPage()));
//		        			
//		        			keyword_take++;
//		        			iCount++;
//		        		}
//		        		
//		        	}
//		         
//				}
//			}
//		}
//		
//		
//		for(Map.Entry<String,Float> entry : sorted_map.entrySet()) {
//			if(!sPnReturn.contains(entry.getKey()))
//			{
//				if(iCount > nTotal)
//					break;
//				
//				sPnReturn.add(entry.getKey());
//				iCount++;
//				
//				
//			}
//		}
//		
//		long stopTime = System.currentTimeMillis();
//	    long elapsedTime = stopTime - startTime;
//	    
//	    // log query history
//	    //InsertQueryLog(strData, "AllTime: " + elapsedTime + ", SqlTime : " + elapsedSqlTime + ", Sql : " + sCombine + "; " + sorted_map.toString(), conn);
//	    //InsertQueryLog(strData, "AllTime: " + elapsedTime + ", SqlTime : " + elapsedSqlTime + ", Sql : " + sCombine, conn);
//	    InsertQueryLog(strData, "AllTime: " + elapsedTime + ", SqlTime : " + elapsedSqlTime + ", Sql : " + sCombine + "; " + sPnReturn.toString(), conn);
//
//		return sPnReturn;
//		
//	}
	
	
//	public List<String> GetQuery(String strData, Connection conn, CRFClassifier<CoreLabel> segmenter) {
//		
//		long startTime = System.currentTimeMillis();
//		
//		long elapsedSqlTime = 0L;
//		
//		String[] strFullword = null;
//		List<String> sList = new ArrayList<String>();
//		String sNumber = "20000";
//		int nTotal = 20;
//		
//		String sCombine = "";
//		
//		String delimiters = "[\\p{Punct}\\s]+";
//
//		if (strData != null && !strData.isEmpty())
//		{
//			strData = strData.toUpperCase();
//			strFullword = strData.split(delimiters);
//		}
//		else
//			return sList;
//
//		String strSql = "";
//		
//		HashMap<String,Float> PnWeightMap = new HashMap<String,Float>();
//		ValueComparator bvc =  new ValueComparator(PnWeightMap);
//        TreeMap<String,Float> sorted_map = new TreeMap<String,Float>(bvc);
//        
//        // 取得關鍵字調整
//        List<IndexAdj> adjust = GetAdjust(conn);
//
//		if (strFullword != null) {
//			for (String stoken : strFullword) {
//
//				strSql = "";
//				
//				if (SkipWord(stoken) || stoken.length() == 0)
//					continue;
//				
//				String sKeyword = "";
//				String sFullword = "";
//				sList.clear();
//				
//				sKeyword = stoken;
//
//				List<String> segmented = segmenter.segmentString(stoken);
//				
//				//List<String> segmented = new ArrayList<String>();
//				//segmented.add(stoken);
//	
//				if (segmented != null) {
//					for (String element : segmented) {
//
//						if (SkipWord(element) || element.length() == 0)
//							continue;
//
//						sList.add(element);
//						
//
//					}
//				}
//				
//				// 目前先都不要用like
//				// 再加一個不要段字的
//				sList.remove(stoken);
//				//sList.add(stoken);
//				strSql += "(select pn, weight, fullword, kind from qeindex where word = '"
//						+ stoken + "' and weight >= 0.5 order by weight desc limit " + sNumber + ") ";
//				strSql += " union ";
//				
//				for (int i = 0; i < sList.size(); i++) {
//					
//					if(!stoken.equals((sList).get(i)))
//					{
//						
//						strSql += "(select pn, weight, fullword, kind from qeindex where word = '"
//								+ sList.get(i) + "' and weight >= 0.5 order by weight desc limit " + sNumber + ") ";
//						strSql += " union ";
//						
//					}
//				}
//				
//				/*
//				// 再加一個不要段字的
//				sList.remove(stoken);
//				//sList.add(stoken);
//				strSql += "(select pn, weight, fullword, kind from qeindex where word like '"
//						+ stoken + "%' order by weight desc limit " + sNumber + ") ";
//				strSql += " union ";
//				
//				for (int i = 0; i < sList.size(); i++) {
//					
//					if(!stoken.equals((sList).get(i)))
//					{
//						// 為了效能，先排除數字跟短字的模糊搜尋 
//						if(isNumeric(sList.get(i)) || sList.get(i).length() < 4)
//						{
//							strSql += "(select pn, weight, fullword, kind from qeindex where word = '"
//									+ sList.get(i) + "' order by weight desc limit " + sNumber + ") ";
//							strSql += " union ";
//						}
//						else
//						{
//							strSql += "(select pn, weight, fullword, kind from qeindex where word like '"
//									+ sList.get(i) + "%' order by weight desc limit " + sNumber + ") ";
//							strSql += " union ";
//						}
//					}
//				}
//				
//				*/
//
//				strSql = strSql.substring(0, strSql.length() - 7);
//				
//				long startSqlTime = System.currentTimeMillis();
//				
//				List<IndexRate> sIndexRate = GetAllIndexRate(strSql, conn);
//				
//				long stopSqlTime = System.currentTimeMillis();
//				elapsedSqlTime += stopSqlTime - startSqlTime;
//				
//				sCombine += strSql + ":" + elapsedSqlTime + ";";
//				
//				// 調整權證正確性
//				for(IndexRate iter:sIndexRate)
//				{
//					String lngWord = "";
//					String shtWord = "";
//					float addWeight = 0f;
//					
//					// 查詢字與關鍵字比對長度後對換
//					// 方便string compare時找頻率
//					if(iter.getFullword().length() >= sKeyword.length())
//					{
//						lngWord = iter.getFullword();
//						shtWord = sKeyword;
//						
//						// 部分相同
//						addWeight = 2.5f;
//					}
//					else
//					{
//						lngWord = sKeyword;
//						shtWord = iter.getFullword();
//						
//						// 查詢字大於關鍵字
//						addWeight = 0.75f;
//					}
//					
//					if(CountStringOccurrences(lngWord, shtWord) >= 1)
//					{
//						// 完全符合時提高
//						if(lngWord.length() == shtWord.length())
//							iter.setWeight(iter.getWeight() + 5);
//						else
//							iter.setWeight(iter.getWeight() + addWeight);
//					}
//					else
//						iter.setWeight(0.01f);
//					
//					// 調整關鍵字(依照類別)
//					for(IndexAdj adj:adjust)
//					{
//						if(adj.getWord().equalsIgnoreCase(iter.getFullword()) && adj.getKind()==iter.getKind())
//							iter.setWeight(iter.getWeight() + adj.getAdjust());
//
//						if(adj.getWord().equalsIgnoreCase(iter.getFullword()) && adj.getKind()!=iter.getKind())
//							iter.setWeight(iter.getWeight() - 2);
//					
//					}
//					
//					if(PnWeightMap.containsKey(iter.getPn()))
//		            {
//						Float fWeight = PnWeightMap.get(iter.getPn());
//						
//						PnWeightMap.put(iter.getPn(), fWeight+iter.getWeight());
//
//		            }
//		            else {
//
//		            	PnWeightMap.put(iter.getPn(), iter.getWeight());
//		            }
//				}
//				
//				
//			}
//		}
//		
//		// sort 後傳回
//		sorted_map.putAll(PnWeightMap);
//		
//			
//		
//		List<String> sPnReturn = new ArrayList<String>();
//		
//		int iCount = 0;
//		
//		for(Map.Entry<String,Float> entry : sorted_map.entrySet()) {
//			sPnReturn.add(entry.getKey());
//			iCount++;
//			
//			if(iCount >= nTotal)
//				break;
//		}
//		
//		long stopTime = System.currentTimeMillis();
//	    long elapsedTime = stopTime - startTime;
//	    
//	    // log query history
//	    //InsertQueryLog(strData, "AllTime: " + elapsedTime + ", SqlTime : " + elapsedSqlTime + ", Sql : " + sCombine + "; " + sorted_map.toString(), conn);
//	    //InsertQueryLog(strData, "AllTime: " + elapsedTime + ", SqlTime : " + elapsedSqlTime + ", Sql : " + sCombine, conn);
//	    InsertQueryLog(strData, "AllTime: " + elapsedTime + ", SqlTime : " + elapsedSqlTime + ", Sql : " + sCombine + "; " + sPnReturn.toString(), conn);
//
//		return sPnReturn;
//	}
	
	
	
	protected boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}

	
	protected int execUpdate(String strSql, Connection con) {

		int snum = 0;
		try {
			Statement stmt = null;
			ResultSet rs = null;

			try {
				stmt = con.createStatement();
				snum = stmt.executeUpdate(strSql);

			}

			finally {

				DbHelper.attemptClose(rs);
				DbHelper.attemptClose(stmt);
				
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return snum;
	}
	
	
	
	protected boolean SkipWord(String strIn) {
		boolean bHave = false;
	
		if(SkipWord == null)
			SkipWord = strSkipWord.split(" ");

		for (String str : SkipWord) {
			if (strIn.trim().equalsIgnoreCase(str))
				bHave = true;
		}

		return bHave;
	}
	
	protected List<IndexRate> GetAllIndexRate(String strSql, Connection con) {

		List<IndexRate> sList = new ArrayList<IndexRate>();

		try {

			Statement stmt = null;
			ResultSet rs = null;

			try {
				stmt = con.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					sList.add(new IndexRate(rs.getString(1), rs.getFloat(2), rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getInt(6)));
				// System.out.println(rs.getString(0));
			}

			finally {

				DbHelper.attemptClose(rs);
				DbHelper.attemptClose(stmt);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return sList;
	}
	
	protected int CountStringOccurrences(String text, String pattern) {
		// Loop through all instances of the string 'text'.
		int count = 0;
		int i = 0;
		while ((i = text.indexOf(pattern, i)) != -1) {
			i += pattern.length();
			count++;
		}
		return count;
	}
	
	protected Map<String, String> segmentData(String strData, CRFClassifier<CoreLabel> segmenter) {
		String [] strFullword = null;

        List<String> sList = new ArrayList<String>();
        List<String> sFullword = new ArrayList<String>();

        Map<String, String> scoreMap = new HashMap<String, String>();
        
        String delimiters = "[\\p{Punct}\\s]+";

        if(strData != null && !strData.isEmpty())
            strFullword = strData.split(delimiters);

        if(strFullword != null) {
            for (String stoken : strFullword) {

                if (SkipWord(stoken) || stoken.length() == 0)
                    continue;

                List<String> segmented = segmenter.segmentString(stoken);
                //System.out.println(segmented);


                if(segmented != null) {
                    for (String element : segmented) {

                        if (SkipWord(element) || element.length() == 0)
                            continue;

                        sList.add(element);
                        sFullword.add(stoken);
                        //System.out.println(element);
                    }
                }
            }
        }

        for(int i=0; i<sList.size(); i++)
        {
            float weight = 0.0f;

            int count = CountStringOccurrences(sFullword.get(i), sList.get(i));

            try {
                weight = (float) sList.get(i).length() / (float) sFullword.get(i).length() * count;
            }
            catch (Exception ex)
            {
                Logger lgr = Logger.getLogger(fuzzysearch.class.getName());
                lgr.log(Level.SEVERE, ex.getMessage(), ex);

                weight = 0.01f;
            }

            if(weight > 1)
                weight = 0.8f;


            if(scoreMap.containsKey(sList.get(i).toUpperCase()))
            {

                String sValue = scoreMap.get(sList.get(i).toUpperCase());
                String [] token = sValue.split(",");

                double score = Double.parseDouble(token[0]);

                score += weight;

                if(score > 1.0f)
                    score = Math.sqrt(score);

                String s = Double.toString(score);

                if(s.length() > 4)
                    s = s.substring(0, 4);

                s += "," + sFullword.get(i);

                scoreMap.put(sList.get(i).toUpperCase(), s);

            }
            else {
                String s = Float.toString(weight) + "," + sFullword.get(i);
                scoreMap.put(sList.get(i).toUpperCase(), s);
            }
        }

        return scoreMap;
	}
	
	protected void InsertPostgrel(String word, int page, float weight,
			int kind, String pn, String mfs, String catalog, String fullword, Connection conWriter) {
		PreparedStatement pst = null;
		
		

		try {


			String strSql = "INSERT INTO qeindex(word, page, weight, kind, pn, mfs, catalog, fullword) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
            pst = conWriter.prepareStatement(strSql);
            pst.setString(1, word);
            pst.setInt(2, page);
            pst.setFloat(3, weight);
            pst.setInt(4, kind);
            pst.setString(5, pn);
            pst.setString(6, mfs);
            pst.setString(7, catalog);
            pst.setString(8, fullword);
            pst.executeUpdate();

		} catch (SQLException ex) {
			Logger lgr = Logger.getLogger(fuzzysearch.class.getName());
			lgr.log(Level.SEVERE, ex.getMessage(), ex);

		} finally {

			DbHelper.attemptClose(pst);
		}
	}
	
	protected void InsertAllWord(String pid, int kind, String pn, String mfs,
			String catalog, Map<String, String> scoreMap, Connection conn) {
		Iterator it = scoreMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			// System.out.println(pair.getKey() + " = " + pair.getValue());

			String[] token = pair.getValue().toString().split(",");

			InsertPostgrel(pair.getKey().toString(), Integer.parseInt(pid),
					Float.parseFloat(token[0]), kind, pn, mfs, catalog, token[1], conn);

			it.remove(); // avoids a ConcurrentModificationException
		}
	}

	public int GetMaxIndexID() {
		// TODO Auto-generated method stub
		
		String strSql = "select page from qeindex order by page desc limit 1";
		
		int pid = 0;
		
		try {

			Connection con = DbHelper.connectFm();
			
			Statement stmt = null;
			ResultSet rs = null;

			try {
				stmt = con.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					pid = Integer.parseInt(rs.getString(1));
				// System.out.println(rs.getString(0));
			}

			finally {

				DbHelper.attemptClose(rs);
				DbHelper.attemptClose(stmt);
				DbHelper.attemptClose(con);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return pid;
	}

	public int GetIndexIDStatus(int pid) {
		// TODO Auto-generated method stub
		
		String strSql = "select * from qeindex where page = " + pid;
	
		int Status = 0;
		
		try {

			Connection con = DbHelper.connectFm();
			
			Statement stmt = null;
			ResultSet rs = null;

			try {
				stmt = con.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					Status = 1;
				// System.out.println(rs.getString(0));
			}

			finally {

				DbHelper.attemptClose(rs);
				DbHelper.attemptClose(stmt);
				DbHelper.attemptClose(con);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Status;
	}
	
	// 20160415 多料號搜尋(Leo更改規格 to Map<pn,Map<mfs,Map<supplier_id,List<pid>>>>)
	public QueryResult QueryProductByMultipleSearch(String[] parts)
	{
		// 回傳值
        QueryResult result = new QueryResult();
        
        /*
        LinkedHashMap<String, Map<String, List<Integer>>> resultMapMfs1 = new LinkedHashMap<String, Map<String, List<Integer>>>();
        LinkedHashMap<String, Map<String, List<Integer>>> resultMapMfs2 = new LinkedHashMap<String, Map<String, List<Integer>>>();
        LinkedHashMap<String, Map<String, List<Integer>>> resultMapMfs3 = new LinkedHashMap<String, Map<String, List<Integer>>>();
        
        LinkedHashMap<String, Map<String, List<Integer>>> resultMapSupplier1 = new LinkedHashMap<String, Map<String, List<Integer>>>();
        LinkedHashMap<String, Map<String, List<Integer>>> resultMapSupplier2 = new LinkedHashMap<String, Map<String, List<Integer>>>();
        LinkedHashMap<String, Map<String, List<Integer>>> resultMapSupplier3 = new LinkedHashMap<String, Map<String, List<Integer>>>();
        
        result.setPidListGroupMfs1(resultMapMfs1);
        result.setPidListGroupMfs2(resultMapMfs2);
        result.setPidListGroupMfs3(resultMapMfs3);
        
        result.setPidListGroupSupplier1(resultMapSupplier1);
        result.setPidListGroupSupplier2(resultMapSupplier2);
        result.setPidListGroupSupplier3(resultMapSupplier3);
        */
        
        
        LinkedHashMap<String, Map<String, Map<Integer, Integer>>> returnMapMfs1 = new LinkedHashMap<String, Map<String, Map<Integer, Integer>>>();
    	LinkedHashMap<String, Map<String, Map<Integer, Integer>>> returnMapMfs2 = new LinkedHashMap<String, Map<String, Map<Integer, Integer>>>();
    	LinkedHashMap<String, Map<String, Map<Integer, Integer>>> returnMapMfs3 = new LinkedHashMap<String, Map<String, Map<Integer, Integer>>>();
   
    	result.setPidListGroupMfs1(returnMapMfs1);
    	result.setPidListGroupMfs2(returnMapMfs2);
    	result.setPidListGroupMfs3(returnMapMfs3);
    	
    	
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        // 分析輸入的查詢
        ArrayList<MultiKeyword> keyQuery = KeywordLogic.GetAnalyzedKeywords(parts);
     
        
        // 如果輸入的查詢有問題，回傳空的結果
        if(keyQuery.size()== 0)
        {
        	return result;
        }
        // 先用pm搜尋
        keyQuery = PmSearchLogic.PmSearchMulti(keyQuery);
        
        // 再查詢非完全匹配的
        //keyQuery = PmSearchLogic.PmSearchMultiLike(keyQuery);
        
        // 再查詢完全不匹配的
        //keyQuery = RedisSearchLogic.RedisSearchMulti(keyQuery);
        
        // 決定誰該
        OrderManager om = new OrderManager();
        
        result = om.formatFromMultiKeyword(keyQuery);
        
		return result;
	}

	// 20160517 搜尋參數
		public Map<String,Map<String,MultipleParam>> QueryParamterByMultipleSearch(String[] parts)
		{
			// 回傳值
			Map<String,Map<String,MultipleParam>> result = new HashMap<String,Map<String,MultipleParam>>();
	        
	   
	     
	        // 用何種方式搜索
	        int nSearchType = 0;
	        
	        // 分析輸入的查詢
	        ArrayList<MultiKeyword> keyQuery = KeywordLogic.GetAnalyzedKeywords(parts);
	     
	        
	        // 如果輸入的查詢有問題，回傳空的結果
	        if(keyQuery.size()== 0)
	        {
	        	return result;
	        }
	        // 先用pm搜尋
	        keyQuery = PmSearchLogic.PmSearchMulti(keyQuery);
	        
	        // 再查詢非完全匹配的
	        //keyQuery = PmSearchLogic.PmSearchMultiLike(keyQuery);
	        
	        // 再查詢完全不匹配的
	        //keyQuery = RedisSearchLogic.RedisSearchMulti(keyQuery);
	        
	        // 決定誰該
	        OrderManager om = new OrderManager();
	        
	        result = om.formatParamMultiKeyword(keyQuery);
	        
			return result;
		}


	public QueryResult QueryProductByMultipleSearchJson(String parts) {
		// 回傳值
        QueryResult result = new QueryResult();
        
     
        
        
        LinkedHashMap<String, Map<String, Map<Integer, Integer>>> returnMapMfs1 = new LinkedHashMap<String, Map<String, Map<Integer, Integer>>>();
    	LinkedHashMap<String, Map<String, Map<Integer, Integer>>> returnMapMfs2 = new LinkedHashMap<String, Map<String, Map<Integer, Integer>>>();
    	LinkedHashMap<String, Map<String, Map<Integer, Integer>>> returnMapMfs3 = new LinkedHashMap<String, Map<String, Map<Integer, Integer>>>();
   
    	result.setPidListGroupMfs1(returnMapMfs1);
    	result.setPidListGroupMfs2(returnMapMfs2);
    	result.setPidListGroupMfs3(returnMapMfs3);
    	
    	
     
        // 用何種方式搜索
        int nSearchType = 0;
        
        // 分析輸入的查詢
        ArrayList<MultiKeyword> keyQuery = KeywordLogic.GetAnalyzedKeywordsJson(parts);
     
        
        // 如果輸入的查詢有問題，回傳空的結果
        if(keyQuery.size()== 0)
        {
        	return result;
        }
        // 先用pm搜尋
        keyQuery = PmSearchLogic.PmSearchMulti(keyQuery);
        
        // 再查詢非完全匹配的
        //keyQuery = PmSearchLogic.PmSearchMultiLike(keyQuery);
        
        // 再查詢完全不匹配的
        //keyQuery = RedisSearchLogic.RedisSearchMulti(keyQuery);
        
        // 決定誰該
        OrderManager om = new OrderManager();
        
        result = om.formatFromMultiKeyword(keyQuery);
        
		return result;
	}



	public ProcurementSet02 Procurement02(String strData, List<Integer> mfs_ids, int amount, int isLogin, int isPaid) {
		// TODO Auto-generated method stub
		OrderManager om = new OrderManager();
		
		ProcurementSet02 pro = om.Procurement02(strData, mfs_ids, amount, isLogin, isPaid);
		
		return pro;
	}
}
