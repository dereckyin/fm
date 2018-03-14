package com.gecpp.fm.Logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.gecpp.fm.OrderResult;
import com.gecpp.fm.Dao.Keyword;
import com.gecpp.fm.Dao.MultiKeyword;
import com.gecpp.fm.Dao.Keyword.KeywordKind;
import com.gecpp.fm.Util.CommonUtil;
import com.gecpp.fm.Util.JedisHelper;
import com.gecpp.fm.model.OrderManagerModel;
import com.gecpp.om.OrderManager;

import redis.clients.jedis.Jedis;

public class PmSearchLogic {
	
	public static List<String> PmSearch(Keyword keywords)
	{
		List<String> pns = new ArrayList<String>();
		List<String> keys = keywords.getKeyword();
		List<KeywordKind> kinds = keywords.getKind();
		
		// 料號與權重對照表
		HashMap<String, Integer> hashPnWeight = new HashMap<String, Integer>();
		// 涵蓋關鍵字次數表
		HashMap<String, Integer> hashIndexCount = new HashMap<String, Integer>();
		
		for(int i=0; i<keys.size(); i++)
		{
			if(kinds.get(i).equals(KeywordKind.IsPn))
			{
				String pnkey = CommonUtil.parsePnKey(keys.get(i));
				List<String> tokenpns = OrderManagerModel.getPnsByPnKey(pnkey, keys.get(i));
				
				pns.addAll(tokenpns);
			}
		}
        
		return pns;
		
	}
	
	// 20160112 多料號搜尋
	// 完全匹配
	public static ArrayList<MultiKeyword> PmSearchMulti(ArrayList<MultiKeyword> keywords)
	{
		// 回傳值
		OrderResult result = null;
		OrderManager om = new OrderManager();
		
		keywords = om.getProductByMultikeyword(keywords);
		
		return keywords;
	}
	
	// 20160112 多料號搜尋
	// 非完全匹配的
	public static ArrayList<MultiKeyword> PmSearchMultiLike(ArrayList<MultiKeyword> keywords)
	{
		// 回傳值
		OrderResult result = null;
		OrderManager om = new OrderManager();
		
		keywords = om.getProductByMultikeywordLike(keywords);
		
		return keywords;
	}
	
}
