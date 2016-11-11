package com.gecpp.fm.Logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import com.gecpp.fm.OrderResult;
import com.gecpp.fm.Dao.IndexRate;
import com.gecpp.fm.Dao.IndexResult;
import com.gecpp.fm.Dao.Keyword;
import com.gecpp.fm.Dao.Keyword.KeywordKind;
import com.gecpp.fm.Dao.MultiKeyword;
import com.gecpp.fm.Util.JedisHelper;
import com.gecpp.fm.Util.SortUtil;
import com.gecpp.fm.model.FuzzyManagerModel;
import com.gecpp.om.OrderManager;

import redis.clients.jedis.Jedis;

public class RedisSearchLogic {
	/*
	public static List<IndexResult> getRedisSearch(Keyword keyQuery)
	{
		// 取出查詢字列表
		List<String> keywordToken = keyQuery.getKeyword();
		// 成雙組合
		List<String> keywordPair = tokenPair(keywordToken);
        
		// 料號與權重對照表
		HashMap<String, Integer> hashPnWeight = new HashMap<String, Integer>();
		// 涵蓋關鍵字次數表
		HashMap<String, Integer> hashIndexCount = new HashMap<String, Integer>();
		
        Jedis jedis = new Jedis(JedisHelper.loadRedisServer(), 6379);
        
        for(String pair:keywordPair)
        {
        	StringTokenizer st = new StringTokenizer(pair);
        	int count = st.countTokens();
        	
        	pair = pair.trim();
        	String [] tokenKeys = pair.split(" ");
        	
        	Set<String> setId = jedis.sinter(tokenKeys);
        	
        	// 同時增加次數與權重
        	for(String id : setId)
        	{
        		String pn = jedis.get("pn_" + id);
        		int weight = 0;
        		try
        		{
        			weight = Integer.parseInt(jedis.get("cn_" + id));
        		}
        		catch(Exception e)
        		{}
        		
        		hashIndexCount.put(pn, count);
        		hashPnWeight.put(pn, weight);
        	}
        }
        
        jedis.close();
		
        List<IndexResult> rdResult = SortUtil.SortIndexResult(hashIndexCount,  hashPnWeight);
        
        return rdResult;
	}
	*/
	
	public static List<IndexResult> getRedisSearchId(Keyword keyQuery)
	{
		// 取出查詢字列表
		List<String> keywordToken = keyQuery.getKeyword();
		// 成雙組合
		List<String> keywordPair = tokenPair(keywordToken);
        
		// 料號與權重對照表
		HashMap<String, Integer> hashPnWeight = new HashMap<String, Integer>();
		// 涵蓋關鍵字次數表
		HashMap<String, Integer> hashIndexCount = new HashMap<String, Integer>();
		
		// 查詢結果限定筆數
		int nCount = 0;
		
        Jedis jedis = new Jedis(JedisHelper.loadRedisServer(), 6379);
        
        List<IndexResult> rdResult = new ArrayList<IndexResult>();
        
        for(String pair:keywordPair)
        {
        	StringTokenizer st = new StringTokenizer(pair);
        	int count = st.countTokens();
        	
        	pair = pair.trim();
        	String [] tokenKeys = pair.split(" ");
        	
        	Set<String> setId = jedis.sinter(tokenKeys);
        	
        	// 同時增加次數與權重
        	for(String id : setId)
        	{
        		int weight = 0;
        		try
        		{
        			weight = Integer.parseInt(jedis.get("cn_" + id));
        		}
        		catch(Exception e)
        		{}
        		
        		hashIndexCount.put(id, count);
        		hashPnWeight.put(id, weight);
        		
        		
        		nCount++;
        	}
        	

    		rdResult.addAll(SortUtil.SortIndexResultSimple(hashPnWeight, count));
    		
    		hashIndexCount.clear();
    		hashPnWeight.clear();
        	
        	// 先取800筆以上即可
        	if(nCount > 800)
        		break;
        }
        
        jedis.close();
		
        
        return rdResult;
	}
	
	public static List<IndexResult> getRedisSearchIdMulti(String keyQuery)
	{
		// 取出查詢字列表
		String [] words = keyQuery.replaceAll("^[,\\s]+", "").split("[\\s]+");
		List<String> keywordToken = Arrays.asList(words);
		// 成雙組合
		List<String> keywordPair = tokenPair(keywordToken);
        
		// 料號與權重對照表
		HashMap<String, Integer> hashPnWeight = new HashMap<String, Integer>();
		// 涵蓋關鍵字次數表
		HashMap<String, Integer> hashIndexCount = new HashMap<String, Integer>();
		
		// 查詢結果限定筆數
		int nCount = 0;
		
        Jedis jedis = new Jedis(JedisHelper.loadRedisServer(), 6379);
        
        List<IndexResult> rdResult = new ArrayList<IndexResult>();
        
        for(String pair:keywordPair)
        {
        	StringTokenizer st = new StringTokenizer(pair);
        	int count = st.countTokens();
        	
        	pair = pair.trim();
        	String [] tokenKeys = pair.split(" ");
        	
        	Set<String> setId = jedis.sinter(tokenKeys);
        	
        	// 同時增加次數與權重
        	for(String id : setId)
        	{
        		int weight = 0;
        		try
        		{
        			weight = Integer.parseInt(jedis.get("cn_" + id));
        		}
        		catch(Exception e)
        		{}
        		
        		hashIndexCount.put(id, count);
        		hashPnWeight.put(id, weight);
        		
        		
        		nCount++;
        	}
        	

    		rdResult.addAll(SortUtil.SortIndexResultSimple(hashPnWeight, count));
    		
    		hashIndexCount.clear();
    		hashPnWeight.clear();
        	
        	// 先取50筆以上即可
        	if(nCount > 50)
        		break;
        }
        
        jedis.close();
		
        
        return rdResult;
	}
	

	protected static List<String> tokenPair(List<String> keywordToken) {
		
		List<String> keywordPair = new ArrayList<String>();
		
		String[] array = keywordToken.toArray(new String[keywordToken.size()]);
        String to[] = new String[array.length];

        for (int i = 2; i <= array.length; i++) {
            String [] strTemp = comb(array, to, i, array.length, i).split("\n");
            for(String each : strTemp)
            	keywordPair.add(each);
        }
        
        // reverse the result
        Collections.reverse(keywordPair);
        
        return keywordPair;
	}
	
	protected static String comb(String[] from, String[] to, int len, int m, int n) {
        String result = "";
        if (n == 0) {
            for (int i = 0; i < len; i++) {
                result += " " + to[i];
            }
            result += "\n";
        } else {
            to[n - 1] = from[m - 1];

            if (m > n - 1) {
                result = comb(from, to, len, m - 1, n - 1);
            }
            if (m > n) {
                result = comb(from, to, len, m - 1, n) + result;
            }
        }
        return result;
    }

	public static ArrayList<MultiKeyword> RedisSearchMulti(
			ArrayList<MultiKeyword> keywords) {
		// TODO Auto-generated method stub
		// 回傳值
		OrderResult result = null;
		OrderManager om = new OrderManager();
		
		for(MultiKeyword key : keywords)
		{
			if(key.getSearchtype() == 1)
				continue;
			
			if(key.getSearchtype() == 2)
				continue;
			
			List<IndexResult> redisResult = new ArrayList<IndexResult>();
			
			if(redisResult.size() > 0 )
			{
				key.setSearchtype(3);
				key.setPkey(om.getProductByMultiRedis(redisResult.get(0).getPn()));
			}
		}
		return keywords;
	}

}
