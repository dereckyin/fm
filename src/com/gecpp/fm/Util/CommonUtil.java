package com.gecpp.fm.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.gecpp.fm.fuzzysearch;

public class CommonUtil {
	
	// 20160127
	public static String parsePnSql(List<String> pns) {
		String pnSql = "";

        int pnsCount = pns.size();

        if (pns != null && pnsCount > 0) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < pnsCount; i++) {
                String s = pns.get(i);
                s = s.replace("'", "''");
                stringBuilder.append(s).append("', '");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 3);
        } else {
            pnSql = "";
        }

        return "'" + pnSql;

    }
	
	public static boolean isAllASCII(String input) {
	    boolean isASCII = true;
	    for (int i = 0; i < input.length(); i++) {
	        int c = input.charAt(i);
	        if (c > 0x7F) {
	            isASCII = false;
	            break;
	        }
	    }
	    return isASCII;
	}
	
	public static String parsePnKey(String pn) {
        String pnKey = pn;

        pnKey = pnKey.replaceAll("\"", "&quot;");
        pnKey = pnKey.replaceAll("\'", "&apos;");
        pnKey = pnKey.trim();

        pnKey = org.apache.commons.lang3.StringUtils.replaceEach(pnKey,
                new String[]{" ", "/", "+", "?", "%", "#", "&", "=", "-", "(", ")", "\'", ".", "quot;", "apos;", "\"", "_"},
                new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""});

        pnKey = pnKey.replace("|", "/"); // 对在页面里将modelname中/转换成|,在此处转换回/
        pnKey = pnKey.replace("<", "");
        pnKey = pnKey.replace(">", "");

        // 去除传参前后空格
        pnKey = pnKey.toUpperCase().trim();
        pnKey = pnKey + "%";

        return pnKey;
    }
	
	
	public static String parsePnKeyNoLike(String pn) {
        String pnKey = pn;

        pnKey = pnKey.replaceAll("\"", "&quot;");
        pnKey = pnKey.replaceAll("\'", "&apos;");
        pnKey = pnKey.trim();

        // 20160127 修正完全比對找不到的問題
        //pnKey = org.apache.commons.lang3.StringUtils.replaceEach(pnKey,
        //        new String[]{" ", "/", "+", "?", "%", "#", "&", "=", "-", "(", ")", "\'", ".", "quot;", "apos;", "\""},
        //        new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""});

        pnKey = pnKey.replace("|", "/"); // 对在页面里将modelname中/转换成|,在此处转换回/
        pnKey = pnKey.replace("<", "");
        pnKey = pnKey.replace(">", "");

        // 去除传参前后空格
        pnKey = pnKey.toUpperCase().trim();

        return pnKey;
    }
	
	public static List<String> removeSpaceList(List<String> inList)
	{
		List<String> strOut = new ArrayList<String>();
		
		for(String str : inList)
		{
			if(str == null)
				continue;
			
			str = str.replaceAll(" ", "");
			str = str.trim();
			
			// 防止奇怪的符號
			if(str.length() < 3)
				continue;
			
			if(!str.isEmpty())
				strOut.add(str);
		}
		
		return strOut;
	}
	
	
	public static Map<String, String> ParsePrice(String strData)
    {
        String [] strFullword = null;

        List<String> sList = new ArrayList<String>();
        List<String> sFullword = new ArrayList<String>();

        Map<String, String> scoreMap = new HashMap<String, String>();
        
        strData = "{" + strData + "}";
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


                        strFullword = val.split(" ");

                        if (strFullword != null) {
                            for (String stoken : strFullword) {



                                stoken = stoken.replace(" ", "");


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
	
	public static boolean IsNumeric(String strIn)
	{
		 try
		 {
			 double d = Double.parseDouble(strIn);
		 }
		 catch(NumberFormatException e)
		 {
			 return false;
		 }
		 
		 return true;
	}
	
}
