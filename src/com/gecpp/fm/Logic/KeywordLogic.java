package com.gecpp.fm.Logic;

import java.sql.Connection;

import org.json.*;

import java.util.UUID;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gecpp.fm.Dao.IndexAdj;
import com.gecpp.fm.Dao.IndexShort;
import com.gecpp.fm.Dao.Keyword;
import com.gecpp.fm.Dao.Keyword.KeywordKind;
import com.gecpp.fm.Dao.Keyword.NLP;
import com.gecpp.fm.Dao.MultiKeyword;
import com.gecpp.fm.Util.CommonUtil;
import com.gecpp.fm.model.FuzzyManagerModel;
import com.gecpp.fm.model.OrderManagerModel;

public class KeywordLogic {
	
	private static String strSkipWord = ", . ; + - | / \\ ' \" : ? < > [ ] { } ! @ # $ % ^ & * ( ) ~ ` _ － ‐ ， （ ）";
	private static String[] SkipWord = null;
	
	
	public static ArrayList<MultiKeyword> GetAnalyzedKeywordsJson(String strInput)
	{
		ArrayList<MultiKeyword> keywords = new ArrayList<MultiKeyword>();
		
		if (strInput != null && strInput.length() != 0)
        {
			
			String json = "{\"results\":" + strInput + "}";
			
			JSONObject obj = null;
			JSONArray array = null;
			
			try {
				obj = new JSONObject(json);
				array = obj.getJSONArray("results");
				
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			String price = "";
			for (int i = 0; i < array.length(); i++) {
				
                JSONObject row = null;
                
				try {
					row = array.getJSONObject(i);
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
              
				MultiKeyword key = new MultiKeyword(); 
				
				String pn = "";
				String mfs = "";
				String pkg = "";
				
				// 2016/03/29  完全滿足需>0
				int amount = 1;

				try
				{
					pn = row.getString("pn");
				}
				catch (Exception e)
				{
					System.out.print(e.getMessage());
				}
				
				try
				{
					mfs = row.getString("mfs");
				}
				catch (Exception e)
				{
					System.out.print(e.getMessage());
				}
				
				try
				{
					pkg = row.getString("pkg");
				}
				catch (Exception e)
				{
					System.out.print(e.getMessage());
				}

				try
				{
					amount = Integer.parseInt(row.getString("num"));
				}
				catch (Exception e)
				{
					System.out.print(e.getMessage());
				}

				
				key.setCount(amount);
				key.setKeyword(pn);
				key.setPkg(pkg);
				key.setMfs(mfs);
				
				keywords.add(key);
			}
        }
		
		return keywords;
	}
	
	public static ArrayList<MultiKeyword> GetAnalyzedKeywords(String [] strInput)
	{
		ArrayList<MultiKeyword> keywords = new ArrayList<MultiKeyword>();
		
		if (strInput != null && strInput.length != 0)
        {
			for(String str : strInput)
			{
				// 2016/03/14  修正大小寫問題
				str = str.toUpperCase();
				// 2016/04/05 以五個_分隔
				String [] split = str.split("_____");
				//String [] split = str.split(",");
				
				MultiKeyword key = new MultiKeyword(); 
				String pn;
				
				// 2016/03/29  完全滿足需>0
				int amount = 1;
				
				if(split.length > 1)
				{
					pn = split[0];
					try
					{
						amount = Integer.parseInt(split[1].trim());
					}
					catch (Exception e)
					{
						System.out.print(e.getMessage());
					}
				}
				else
					pn = split[0];
				
				key.setCount(amount);
				key.setKeyword(pn);
				
				keywords.add(key);
			}
        }
		
		return keywords;
	}
	
	public static Keyword GetAnalyzedKeywords(String strInput)
	{
		String[] keywordArray = null;
		int i = 0;
		
		// 20160806 處理空白與料號的問題
		if (strInput != null && !strInput.isEmpty())
		{
			// 2016/02/16 修正大小寫問題
			strInput = strInput.toUpperCase();
			
			keywordArray = strInput.replaceAll("^[,\\s]+", "").split("[\\s]+");
			
			//if(keywordArray.length == 1)
			//{
				if(OrderManagerModel.IsRealPn(strInput))
				{
					Keyword key = new Keyword();
					
					List<String> word = new ArrayList<String>();
					List<KeywordKind> kind = new ArrayList<KeywordKind>();
					List<NLP> nlp = new ArrayList<NLP>();
					
					String uuid = UUID.randomUUID().toString().replaceAll("-", "");
					
					word.add(strInput);
					kind.add(KeywordKind.IsPn);
					nlp.add(NLP.NotNLP);
		        	
		        	key.setUuid(uuid);
		        	key.setInputdata(strInput);
		        	 key.setKeyword(word);
		             key.setKind(kind);
		             key.setNlp(nlp);
		             key.setCount(1);
		             
		             return key;
				}
			//}
		}
		
		// 預先處理
		if (strInput != null && !strInput.isEmpty())
		{
			// 2016/02/16 修正大小寫問題
			strInput = strInput.toUpperCase();
			keywordArray = strInput.replaceAll("^[,\\s]+", "").split("[\\s]+");
		}
		
		// 20160114
		// 處理料號無法搜尋到的問題(因為料號搜尋也跑去增加字典)
		if(keywordArray.length > 1)
		{
			// 字典分析
			if (strInput != null && !strInput.isEmpty())
	        {
				// 2016/02/16 修正大小寫問題
				//strInput = strInput.toUpperCase();
				strInput = TransDict(strInput);
	        }
		}
		
		Keyword key = new Keyword();
		
		List<String> word = new ArrayList<String>();
		List<KeywordKind> kind = new ArrayList<KeywordKind>();
		List<NLP> nlp = new ArrayList<NLP>();
		
		if (strInput != null && !strInput.isEmpty())
			keywordArray = strInput.replaceAll("^[,\\s]+", "").split("[\\s]+");
        
		if (keywordArray != null) {
        	String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        	
        	key.setUuid(uuid);
        	key.setInputdata(strInput);
        	
        	// 關鍵字調整已經由字典取代了(解決TI的問題)
        	ArrayList<String> keywords = keywordAdjust(keywordArray);
        	//ArrayList<String> keywords = new ArrayList<String>(Arrays.asList(keywordArray));
      
            for (String stoken : keywords)
            {
            	if (SkipWord(stoken) || stoken.length() == 0)
                    continue;
            	
            	word.add(stoken);
            	
            	if(OrderManagerModel.IsPn(stoken))
            		kind.add(KeywordKind.IsPn);
            	else
            		kind.add(KeywordKind.NotPn);
            	
            	if(IsNLP(stoken))
            		nlp.add(NLP.IsNLP);
            	else
            		nlp.add(NLP.NotNLP);
            	
            	i++;
            	
            	// 先限定搜尋10個關鍵字
            	if(i > 10)
            		break;
            }
            
            // 20160503 one word search always isPn
            // 20160916 修正搜尋漢字問題
            // 20161110 非ascii不是 pn
            if(i==1)
            {
            	//if(CommonUtil.isAllASCII(strInput))
            	//	kind.set(0, KeywordKind.IsPn);
            	//else
            	//	kind.set(0, KeywordKind.NotPn);
            	if(!CommonUtil.isAllASCII(strInput))
            		kind.set(0, KeywordKind.NotPn);
            }
            
            key.setKeyword(word);
            key.setKind(kind);
            key.setNlp(nlp);
            key.setCount(i);
		}
		
		return key;
	}
	
	private static boolean IsNLP(String pnKey)
	{
		String strInverseArray[] = pnKey.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
		if(strInverseArray.length < 2) // 非中英夾雜有可能是單字
			return true;
		else
			return false;
		
	}
	
	private static String TransDict(String strInput)
	{
		List<IndexShort> breif = FuzzyManagerModel.GetShort();
		
		String strNeedAdd = "";
		
		// 要做到雙邊
		List<String> leftTokens = new ArrayList<String>();
		List<String> rightTokens = new ArrayList<String>();
		
		for(IndexShort element : breif)
	    {
			leftTokens.add(element.getWord().toUpperCase());
			rightTokens.add(element.getAlterword().toUpperCase());
	    }

        for(int i=0; i<leftTokens.size(); i++) {
        	try
            {
	            String patternString = "\\b(" + leftTokens.get(i) + ")\\b";
	            Pattern pattern = Pattern.compile(patternString);
	            Matcher matcher = pattern.matcher(strInput);
	
	            if (matcher.find()) {
	            	strNeedAdd += " " + rightTokens.get(i);
            }
            }
            catch(Exception e)
            {
            	
            }
        }
        
        for(int i=0; i<rightTokens.size(); i++) {
            String patternString = "\\b(" + rightTokens.get(i) + ")\\b";
            
            try
            {
            	Pattern pattern = Pattern.compile(patternString);
            	Matcher matcher = pattern.matcher(strInput);
            	
            	if (matcher.find()) {
                	strNeedAdd += " " + leftTokens.get(i);
                }
            }
            catch(Exception e)
            {
            	
            }
        }
        
        
        return strInput + strNeedAdd;
	}

	private static ArrayList<String> keywordAdjust(String[] keywordArray) {
		// 取得關鍵字調整
		List<IndexAdj> adjust = FuzzyManagerModel.GetAdjust();
		// 取得縮寫字調整
		//List<IndexShort> breif = FuzzyManagerModel.GetShort();
		
       // 增加縮寫字如:TI => Texas Instruments
		ArrayList<String> keywords = new ArrayList<String>();
		
		for (String stoken : keywordArray)
		{
			keywords.add(stoken);
			
		    for(IndexAdj element : adjust)
		    {
		        if(element.getWord().equalsIgnoreCase(stoken))
		        {
		            keywords.add(element.getAlterword());
		            keywords.remove(stoken);	// 置換字要不要移除，之後再討論
		        }
		    }
		}
		return keywords;
	}
	
	protected static boolean SkipWord(String strIn) {
		boolean bHave = false;
	
		if(SkipWord == null)
			SkipWord = strSkipWord.split(" ");

		for (String str : SkipWord) {
			if (strIn.trim().equalsIgnoreCase(str))
				bHave = true;
		}

		return bHave;
	}
	

}
