package com.gecpp.fm.Logic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gecpp.fm.Dao.IndexRate;
import com.gecpp.fm.Dao.IndexResult;
import com.gecpp.fm.Dao.Keyword;
import com.gecpp.fm.Dao.Keyword.KeywordKind;
import com.gecpp.fm.Util.SortUtil;
import com.gecpp.fm.model.FuzzyManagerModel;


public class FuzzySearchLogic {
	
	public static List<IndexResult> getFuzzySearch(Keyword keyQuery)
	{
		List<String> keywordToken = keyQuery.getKeyword();
		List<KeywordKind> keywordKind = keyQuery.getKind();
		
		int order = 0;										// 第幾個關鍵字
		int nNumber = 1000;									// 需搜尋出來的總數
		//int limitNumber = nNumber / keyQuery.getCount();	// 各關鍵字分配到的查詢量
		
		// 料號與權重對照表
		HashMap<String, Integer> hashPnWeight = new HashMap<String, Integer>();
		
		// 涵蓋關鍵字次數表
		HashMap<String, Integer> hashIndexCount = new HashMap<String, Integer>();
		
		for (String stoken : keywordToken) {
			order++;
			//List<IndexRate> sIndexRate = FuzzyManagerModel.GetAllIndexRate(stoken, order, limitNumber);
			List<IndexRate> sIndexRate = FuzzyManagerModel.GetAllIndexRate(stoken, order, nNumber);
		
			// 先整理一次取出唯一的料號(順便加進料號與id對照)
			Set<String> uniquePn = new HashSet<String>();
			for(IndexRate iter:sIndexRate)
	        {
				uniquePn.add(iter.getPn());
				
				// 順便放到料號與權重對照表中
				hashPnWeight.put(iter.getPn(), Math.round(iter.getWeight()));
	        }
			
			// 依照次數加入料號
			for(String pn : uniquePn)
			{
				if(hashIndexCount.containsKey(pn))
	            {
	                int nCount = hashIndexCount.get(pn) + 1;

	                hashIndexCount.put(pn, nCount);

	            }
	            else {
	            	hashIndexCount.put(pn, 1);
	            }
	        }
		}
		
		List<IndexResult> fmResult = SortUtil.SortIndexResult(hashIndexCount,  hashPnWeight);
        
        return fmResult;
	}
	
	public static List<IndexResult> getFuzzySearchId(Keyword keyQuery)
	{
		List<String> keywordToken = keyQuery.getKeyword();
		List<KeywordKind> keywordKind = keyQuery.getKind();
		
		int order = 0;										// 第幾個關鍵字
		int nNumber = 1000;									// 需搜尋出來的總數
		//int limitNumber = nNumber / keyQuery.getCount();	// 各關鍵字分配到的查詢量
		
		// 料號與權重對照表
		HashMap<String, Integer> hashPnWeight = new HashMap<String, Integer>();
		
		// 涵蓋關鍵字次數表
		HashMap<String, Integer> hashIndexCount = new HashMap<String, Integer>();
		
		for (String stoken : keywordToken) {
			order++;
			//List<IndexRate> sIndexRate = FuzzyManagerModel.GetAllIndexRate(stoken, order, limitNumber);
			//List<IndexRate> sIndexRate = FuzzyManagerModel.GetAllIndexRate(stoken, order, nNumber);
			List<IndexRate> sIndexRate = FuzzyManagerModel.GetEzIndexRate(stoken, order, nNumber);
		
			// 先整理一次取出唯一的料號(順便加進料號與id對照)
			Set<String> uniqueId = new HashSet<String>();
			for(IndexRate iter:sIndexRate)
	        {
				String id = Integer.toString(iter.getPage());
				// 順便放到料號與權重對照表中
				hashPnWeight.put(id, Math.round(iter.getWeight()));
	       
				if(hashIndexCount.containsKey(id))
	            {
	                int nCount = hashIndexCount.get(id) + 1;

	                hashIndexCount.put(id, nCount);

	            }
	            else {
	            	hashIndexCount.put(id, 1);
	            }
	        }
		}
		
		List<IndexResult> fmResult = SortUtil.SortIndexResult(hashIndexCount,  hashPnWeight);
        
        return fmResult;
	}
}
