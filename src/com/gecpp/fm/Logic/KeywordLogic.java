package com.gecpp.fm.Logic;

import java.sql.*;
import java.util.*;

import org.json.*;

import java.util.UUID;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gecpp.fm.Dao.IndexAdj;
import com.gecpp.fm.Dao.IndexRate;
import com.gecpp.fm.Dao.IndexResult;
import com.gecpp.fm.Dao.IndexShort;
import com.gecpp.fm.Dao.Keyword;
import com.gecpp.fm.Dao.Keyword.KeywordKind;
import com.gecpp.fm.Dao.Keyword.NLP;
import com.gecpp.fm.Dao.MfsAlternate;
import com.gecpp.fm.Dao.MfsAlternateDao;
import com.gecpp.fm.Dao.MultiKeyword;
import com.gecpp.fm.Dao.StandardCatalogAlternate;
import com.gecpp.fm.Dao.StandardCatalogDao;
import com.gecpp.fm.Util.CommonUtil;
import com.gecpp.fm.Util.DbHelper;
import com.gecpp.fm.Util.DbHelper.Site;
import com.gecpp.fm.model.FuzzyManagerModel;
import com.gecpp.fm.model.OrderManagerModel;

import org.apache.commons.lang3.StringUtils;

import static com.gecpp.fm.Util.DbHelper.attemptClose;
import java.util.logging.*;

public class KeywordLogic {
	
	private static String strSkipWord = ", . ; + - | / \\ ' \" : ? < > [ ] { } ! @ # $ % ^ & * ( ) ~ ` _ － ‐ ， （ ）";
	private static String[] SkipWord = null;
	
	private static ArrayList<MfsAlternateDao> cachedMfs = null;
	private static ArrayList<StandardCatalogDao> cachedCatalog = null;
	private static long cacheTime = System.currentTimeMillis();
	
	
	public static MfsAlternate ExtractMfs(String strData, List<Integer> mfs, List<Integer> supplier)
    {
		refreshMfsAlternate();
		String tagData = "";
		List<String> tagMfs = new ArrayList<String>();
		
		if(mfs == null)
			mfs = new ArrayList<Integer>();
		
		if(supplier == null)
			supplier = new ArrayList<Integer>();
		
		for(MfsAlternateDao dao : cachedMfs)
    	{
    		
   
            String patternString = "(?<!\\S)" + dao.getName().toUpperCase() + "(?!\\S)";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(strData.toUpperCase());

            while (matcher.find()) {
            	if(dao.getKind() == 1)
            	{
    				mfs.add(dao.getId());
    				tagMfs.add(dao.getFullname());
            	}
    			
    			
    			if(dao.getKind() == 2)
    				supplier.add(dao.getId());
    			
    			strData = strData.toUpperCase().replaceAll("(?<!\\S)" + dao.getName().toUpperCase() + "(?!\\S)", "");
    			
    			tagData += "|||" + dao.getKind() + dao.getName().toUpperCase() + "|||" + " ";
            }
            
    	}
    	
    	MfsAlternate retAlter = new MfsAlternate();
    	
    	retAlter.setStrData(strData);
    	retAlter.setMfs(mfs);
    	retAlter.setSupplier(supplier);
    	retAlter.setTagData(tagData);
    	retAlter.setTagMfs(tagMfs);
    	
    	return retAlter;
    }
	
	public static StandardCatalogAlternate ExtractCatalogName(String strData, List<Integer> catalog)
    {
		refreshCatalog();
		refreshMfsAlternate();
		
		String tagData = "";
		List<String> tagCatalog = new ArrayList<String>();
		List<Integer> tagCatalogId = new ArrayList<Integer>();
		List<Integer> mfsId = new ArrayList<Integer>();
		
		if(catalog == null)
			catalog = new ArrayList<Integer>();
		
		// get catalog
		for(StandardCatalogDao dao : cachedCatalog)
    	{
            String patternString = "(?<!\\S)" + dao.getChinese_name().toUpperCase() + "(?!\\S)";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(strData.toUpperCase());

            while (matcher.find()) {
            	
    			catalog.add(dao.getCatalog_id());
    			tagCatalog.add(dao.getChinese_name());
    			
    			strData = strData.toUpperCase().replaceAll("(?<!\\S)" + dao.getChinese_name().toUpperCase() + "(?!\\S)", "");
    			
    			tagData += "|||3" + dao.getChinese_name().toUpperCase() + "|||" + " ";
            }
            
    	}
		
		// get mfs
		for(MfsAlternateDao dao : cachedMfs)
    	{

            String patternString = "(?<!\\S)" + dao.getName().toUpperCase() + "(?!\\S)";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(strData.toUpperCase());

            while (matcher.find()) {
    			mfsId.add(dao.getId());
    			strData = strData.toUpperCase().replaceAll("(?<!\\S)" + dao.getName().toUpperCase() + "(?!\\S)", "");
    	
            }
            
    	}
    	
		StandardCatalogAlternate retAlter = new StandardCatalogAlternate();
    	
    	retAlter.setStrData(strData);
    	retAlter.setCatalog(catalog);
    	retAlter.setTagCatalog(tagCatalog);
    	retAlter.setTagData(tagData);
    	retAlter.setMfs(mfsId);
    	
    	return retAlter;
    }
	
	public static MfsAlternate ExtractMfsName(String strData, List<String> mfs, List<Integer> supplier)
    {
		refreshMfsAlternate();
		String tagData = "";
		List<String> tagMfs = new ArrayList<String>();
		List<Integer> tagMfsId = new ArrayList<Integer>();
		
		if(mfs == null)
			mfs = new ArrayList<String>();
		
		if(supplier == null)
			supplier = new ArrayList<Integer>();
		
		for(MfsAlternateDao dao : cachedMfs)
    	{
    		
   
            String patternString = "(?<!\\S)" + dao.getName().toUpperCase() + "(?!\\S)";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(strData.toUpperCase());

            while (matcher.find()) {
            	if(dao.getKind() == 1)
            	{
    				mfs.add(dao.getName());
    				tagMfsId.add(dao.getId());
    				tagMfs.add(dao.getFullname());
            	}
    			
    			
    			if(dao.getKind() == 2)
    				supplier.add(dao.getId());
    			
    			strData = strData.toUpperCase().replaceAll("(?<!\\S)" + dao.getName().toUpperCase() + "(?!\\S)", "");
    			
    			tagData += "|||" + dao.getKind() + dao.getName().toUpperCase() + "|||" + " ";
            }
            
    	}
    	
    	MfsAlternate retAlter = new MfsAlternate();
    	
    	retAlter.setStrData(strData);
    	retAlter.setMfs(tagMfsId);
    	retAlter.setSupplier(supplier);
    	retAlter.setTagData(tagData);
    	retAlter.setTagMfs(tagMfs);
    	
    	return retAlter;
    }
	
	private static long getHash(String str) {

        long h = 98764321261L;
        int l = str.length();
        char[] chars = str.toCharArray();

        for (int i = 0; i < l; i++) {
            h = 31*h + chars[i];
        }
        return h;
    }
	
	public static List<IndexResult> getCache(String sInput)
	{
		List<IndexResult> aRet = new ArrayList<IndexResult>();
		
		Long id = getMd5(sInput);

		PreparedStatement pst = null;
		Connection conn = null;
		ResultSet rs = null;

		String strSql = "select pn, cnt from ezindex_cache_pn where id = ? order by cnt desc limit 500";
		try
		{
			conn = DbHelper.connectFm();
			pst = conn.prepareStatement(strSql);
			pst.setLong(1, id);

			rs = pst.executeQuery();

			while (rs.next()) {
				IndexResult res = new IndexResult();
				res.setPn(rs.getString(1));
				res.setCount(rs.getInt(2));
				aRet.add(res);
			}
			

		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally {
			attemptClose(rs);
			attemptClose(pst);
			attemptClose(conn);
		}

		return aRet;
	}
	
	public static List<IndexRate> getCacheMfsCatalog(String sInput)
	{
		List<IndexRate> aRet = new ArrayList<IndexRate>();
		
		List<String> core_mfs = new ArrayList<String>();
		List<Integer> abbreviation = new ArrayList<Integer>();
		boolean bFound = false;
		
		StandardCatalogAlternate retAlter = null;
		String strAlter = "";
		Integer nCatalogId = 0;
			
		// for ti, recom
		String newInput = "";
		String [] sTerms = sInput.split(" ");
		for(String term : sTerms)
		{
			retAlter = KeywordLogic.ExtractCatalogName(term, abbreviation);
			if(retAlter.getCatalog().size() > 0)
			{
				if(!sInput.toUpperCase().contains(retAlter.getTagCatalog().get(0)))
					newInput += retAlter.getTagCatalog().get(0) + " ";
				else
					newInput += term + " ";
				
				strAlter = retAlter.getTagCatalog().get(0);
				nCatalogId = retAlter.getCatalog().get(0);
				
				bFound = true;
			}
			else
				newInput += term + " ";
		}
		
		sInput = newInput.trim();

	    if(!bFound)
	    	return aRet;
	    
	    if(sInput.length() > strAlter.length())
	    	return aRet;

		PreparedStatement pst = null;
		Connection conn = null;
		ResultSet rs = null;
		
		if(core_mfs.size() == 0)
			return aRet;

		String strSql = "select pn, '" + strAlter + "', count from ez_mfs_count where id = ? order by count desc limit 500";
		try
		{
			conn = DbHelper.connectFm();
			pst = conn.prepareStatement(strSql);
			pst.setLong(1, 0L);

			rs = pst.executeQuery();

			while (rs.next()) {
				IndexRate res = new IndexRate(rs.getString(1), 0, rs.getString(2), 0, 0, rs.getInt(3));
				
				aRet.add(res);
			}
			

		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally {
			attemptClose(rs);
			attemptClose(pst);
			attemptClose(conn);
		}

		return aRet;
	}
	
	public static List<IndexRate> getCacheMfs(String sInput)
	{
		List<IndexRate> aRet = new ArrayList<IndexRate>();
		
		List<String> core_mfs = new ArrayList<String>();
		List<Integer> abbreviation = new ArrayList<Integer>();
		boolean bFound = false;
		
		MfsAlternate retAlter = null;
		String strAlter = "";
		Long nMfs = 0L;
			
		// for ti, recom
		String newInput = "";
		String [] sTerms = sInput.split(" ");
		for(String term : sTerms)
		{
			retAlter = KeywordLogic.ExtractMfsName(term, core_mfs, abbreviation);
			if(retAlter.getTagMfs().size() > 0)
			{
				if(!sInput.toUpperCase().contains(retAlter.getTagMfs().get(0)))
					newInput += retAlter.getTagMfs().get(0) + " ";
				else
					newInput += term + " ";
				
				strAlter = retAlter.getTagMfs().get(0);
				nMfs = retAlter.getMfs().get(0).longValue();
				
				bFound = true;
			}
			else
				newInput += term + " ";
		}
		
		sInput = newInput.trim();

	    if(!bFound)
	    	return aRet;
	    
	    if(sInput.length() > strAlter.length())
	    	return aRet;

		PreparedStatement pst = null;
		Connection conn = null;
		ResultSet rs = null;
		
		if(core_mfs.size() == 0)
			return aRet;

		String strSql = "select pn, '" + strAlter + "', count from ez_mfs_count where id = ? order by count desc limit 500";
		try
		{
			conn = DbHelper.connectFm();
			pst = conn.prepareStatement(strSql);
			pst.setLong(1, nMfs);

			rs = pst.executeQuery();

			while (rs.next()) {
				IndexRate res = new IndexRate(rs.getString(1), 0, rs.getString(2), 0, 0, rs.getInt(3));
				
				aRet.add(res);
			}
			

		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally {
			attemptClose(rs);
			attemptClose(pst);
			attemptClose(conn);
		}

		return aRet;
	}
	
	public static Long getMd5(String strInput)
	{
		String [] params = strInput.toUpperCase().split("\\s+");
        Arrays.sort(params);

        String paraStr = "";
        for(String sr : params)
        	paraStr += sr + ",";
        
        paraStr = paraStr.substring(0, paraStr.length() - 1);

        long nMd5 = getHash(paraStr);
        
        return nMd5;
	}
	
private static void refreshCatalog() {
		
		long nowDate = System.currentTimeMillis();
		
		// 12個小時更換一次cache
		if(nowDate - cacheTime > 12 * 60 * 60 * 1000)
		{
			cachedCatalog = null;
		}
		
		if(cachedCatalog == null)
		{
			cachedCatalog = new ArrayList<StandardCatalogDao>();
			cacheTime = nowDate;
			
			String strSql = "select catalog_id, chinese_name from  pm_standar_catalog order by length(chinese_name) desc";
		
			try {
	
				Connection conn = null;
				Statement stmt = null;
				ResultSet rs = null;
	
				try {
	
						conn = DbHelper.connectPm();
				
					
					stmt = conn.createStatement();
					rs = stmt.executeQuery(strSql);
					while (rs.next())
					{
						StandardCatalogDao dao = new StandardCatalogDao();
						dao.setCatalog_id(rs.getInt(1));
						dao.setChinese_name(rs.getString(2));
						
						cachedCatalog.add(dao);
					}
					
				}
	
				finally {
	
					DbHelper.attemptClose(rs);
					DbHelper.attemptClose(stmt);
					DbHelper.attemptClose(conn);
				}
	
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			

		}
		

	}
	
	private static void refreshMfsAlternate() {
		
		long nowDate = System.currentTimeMillis();
		
		// 12個小時更換一次cache
		if(nowDate - cacheTime > 12 * 60 * 60 * 1000)
		{
			cachedMfs = null;
		}
		
		if(cachedMfs == null)
		{
			cachedMfs = new ArrayList<MfsAlternateDao>();
			cacheTime = nowDate;
			
			String strSql = "select id, kind, name, fullname from  ezindex_alternate order by length(name) desc";
		
			try {
	
				Connection conn = null;
				Statement stmt = null;
				ResultSet rs = null;
	
				try {
	
						conn = DbHelper.connectFm();
				
					
					stmt = conn.createStatement();
					rs = stmt.executeQuery(strSql);
					while (rs.next())
					{
						MfsAlternateDao dao = new MfsAlternateDao();
						dao.setId(rs.getInt(1));
						dao.setKind(rs.getInt(2));
						dao.setName(rs.getString(3));
						dao.setFullname(rs.getString(4));
						
						cachedMfs.add(dao);
					}
					
				}
	
				finally {
	
					DbHelper.attemptClose(rs);
					DbHelper.attemptClose(stmt);
					DbHelper.attemptClose(conn);
				}
	
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			

		}
		

	}

	public static ArrayList<MultiKeyword> GetAnalyzedKeywordsJson(String strInput)
	{
		ArrayList<MultiKeyword> keywords = new ArrayList<MultiKeyword>();
		
		if (strInput != null && strInput.length() != 0)
        {
			// 20170907 設定限制與log
			int count = 0;
			
			Logger.getLogger (KeywordLogic.class.getName()).log(Level.INFO, strInput);
			
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
					pn = row.getString("pn").trim();
					pn = pn.replaceAll(" ", "");
				}
				catch (Exception e)
				{
					System.out.print(e.getMessage());
				}
				
				try
				{
					mfs = row.getString("mfs").trim();
				}
				catch (Exception e)
				{
					System.out.print(e.getMessage());
				}
				
				try
				{
					pkg = row.getString("pkg").trim();
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

				if("".equalsIgnoreCase(pn))
					continue;
				
				key.setCount(amount);
				key.setKeyword(pn);
				key.setPkg(pkg);
				key.setMfs(mfs);
				
				keywords.add(key);
				
				
				// 20170907 設定限制與log
				count++;
				if(count > 50)
					break;
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
					pn = split[0].trim();
					pn = pn.replaceAll(" ", "");
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
					
					strInput = strInput.replaceAll(" ", "");
					
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
			strInput = strInput.replaceAll(" ", "");
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

	public static Keyword GetAnalyzedKeywords(String strInput, String strUntaged, String strTaged, List<Integer> mfs_list, List<Integer> supplier_list)
	{
		String[] keywordArray = null;
		int i = 0;
		
		DbHelper db = new DbHelper();
		
		// 2016/02/16 修正大小寫問題
		strUntaged = strUntaged.toUpperCase();
		
		if(!strUntaged.trim().equals(""))
			keywordArray = strUntaged.replaceAll("^[,\\s]+", "").split("[\\s]+");
		else
		{
			int count = 40;
			List<String> goodPn = new ArrayList<String>();
			
			if(mfs_list.size() > 0)
				try{
					count = 40 / mfs_list.size();
				}	catch(Exception e)
				{
					
				}
				
			//String strSql = "select pn from qeindexweight where weight > 50 and weight < 60 order by weight desc limit 20";
			for(int id: mfs_list)
			{
				String strSql = "select pn from ez_mfs_count where id = " + id + " order by count desc limit " + count;
				goodPn.addAll(db.getList(strSql, Site.fm));
			}
			
			try{
				keywordArray = goodPn.toArray(new String[0]);
			}
			catch(Exception e)
			{}
			
			if(keywordArray.length == 0)
			{
				goodPn.add(strInput);
				keywordArray = goodPn.toArray(new String[0]);
			}
		}
			//keywordArray = strInput.replaceAll("^[,\\s]+", "").split("[\\s]+");
	
			
		
		// 20160806 處理空白與料號的問題
		if (strUntaged != null && !strUntaged.isEmpty())
		{
			// for pn with space inside
			if(OrderManagerModel.IsRealPn(strUntaged))
			{
				Keyword key = new Keyword();
				
				List<String> word = new ArrayList<String>();
				List<KeywordKind> kind = new ArrayList<KeywordKind>();
				List<NLP> nlp = new ArrayList<NLP>();
				
				String uuid = UUID.randomUUID().toString().replaceAll("-", "");
				
				strUntaged = strUntaged.replaceAll(" ", "");
				
				word.add(strUntaged);
				kind.add(KeywordKind.IsPn);
				nlp.add(NLP.NotNLP);
	        	
				key.setUuid(uuid);
				key.setInputdata(strUntaged);
				key.setKeyword(word);
				key.setKind(kind);
				key.setNlp(nlp);
				key.setCount(1);
	             
	            return key;
			}
			
			// for pn with space inside
			if(OrderManagerModel.IsRealPn(strInput))
			{
				Keyword key = new Keyword();
				
				List<String> word = new ArrayList<String>();
				List<KeywordKind> kind = new ArrayList<KeywordKind>();
				List<NLP> nlp = new ArrayList<NLP>();
				
				String uuid = UUID.randomUUID().toString().replaceAll("-", "");
				
				strUntaged = strUntaged.replaceAll(" ", "");
				
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
			
		}
		
		// 預先處理
		if (strUntaged != null && !strUntaged.isEmpty())
		{
			// 2016/02/16 修正大小寫問題
			strUntaged = strUntaged.toUpperCase();
			strUntaged = strUntaged.replaceAll(" ", "");
			
		}

		// 20160114
		// 處理料號無法搜尋到的問題(因為料號搜尋也跑去增加字典)
		if(!(keywordArray.length == 1 && keywordArray[0].isEmpty()))
		{
			// 字典分析
			if (strUntaged != null && !strUntaged.isEmpty())
	        {
				// 2016/02/16 修正大小寫問題
				//strInput = strInput.toUpperCase();
				strUntaged = TransDict(strUntaged);
	        }
		}
		
		Keyword key = new Keyword();
		
		List<String> word = new ArrayList<String>();
		List<KeywordKind> kind = new ArrayList<KeywordKind>();
		List<NLP> nlp = new ArrayList<NLP>();
		
		if(strTaged != null && !strTaged.isEmpty())
		{
			String [] tagArray = strTaged.split("\\|\\|\\|");
			for(String stoken:tagArray)
			{
				if (SkipWord(stoken) || stoken.length() == 0)
                    continue;
				
				word.add(stoken.substring(1, stoken.length()));
            	
            	kind.add(KeywordKind.NotPn);
            	
            	if(IsNLP(stoken))
            		nlp.add(NLP.IsNLP);
            	
			}
				
		}
		
		if (!(keywordArray.length == 1 && keywordArray[0].isEmpty())) {
        	String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        	
        	key.setUuid(uuid);
        	key.setInputdata(strUntaged);
        	
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
            	if(!CommonUtil.isAllASCII(strUntaged))
            		kind.set(0, KeywordKind.NotPn);
            }
            
            key.setKeyword(word);
            key.setKind(kind);
            key.setNlp(nlp);
            key.setCount(i);
		}
		
		return key;
	}
	
	private static boolean IsCompond(String pnKey)
	{
		String strInverseArray[] = pnKey.split(" ");
		if(strInverseArray.length < 2) // 非中英夾雜有可能是單字
			return false;
		else
			return true;
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
