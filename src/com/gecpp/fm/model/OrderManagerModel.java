package com.gecpp.fm.model;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gecpp.fm.Dao.IndexPrice;
import com.gecpp.fm.Dao.Product;
import com.gecpp.fm.Util.CommonUtil;
import com.gecpp.fm.Util.DbHelper;
import com.gecpp.fm.Util.DbHelper.Site;
import com.gecpp.p.product.domain.Mfs;

public class OrderManagerModel {
	
	private static HashMap<Integer, Mfs> cachedMfs = null;
	private static long cacheTime = System.currentTimeMillis();
	
	public static boolean IsRealPn(String pnKey) {
		
		if("".equalsIgnoreCase(pnKey.trim()))
			return false;
		
		DbHelper db = new DbHelper();
		
		List<String> sList = new ArrayList<>();

		//String strSql = "SELECT pn FROM pm_product where pn like '" + pnKey.replace("'","''") + "%' limit 20";
		String strSql = "select pn from auto_cache_pn where pn like '" + pnKey.replace("'","''") + "%' limit 10";
		
		sList = db.getList(strSql, Site.fm);
		
		int nCount = 0;
		if(sList.size() > 0)
		{
			try
			{
				for(String pn : sList)
					if((CommonUtil.similarity(pn, pnKey) > 0.5) && pnKey.length() > 3)
						nCount++;
			}
			catch (Exception e)
			{
				System.out.print(e.getMessage());
			}
			
		}
		
		// 20170929 是否有尚未加入cache的料號
		strSql = "select pn from pm_product where pn = '" + pnKey.replace("'","''") + "' limit 10";
		sList = db.getList(strSql, Site.pm);
		if(sList.size() > 0)
			nCount = 1;
		
		// 20180829 加入supplier_pn
		strSql = "select pn from fm_product where supplier_pn = '" + pnKey.replace("'","''") + "' limit 10";
		sList = db.getList(strSql, Site.fm);
		if(sList.size() > 0)
			nCount = 1;
	
		return nCount > 0 ? true: false;
	}
	
	public static boolean IsPn(String pnKey) {
		/*
		DbHelper db = new DbHelper();
		
		String strSql = "SELECT cast(A.NUM as float)/cast(A.DENOM as float) " +
						" FROM " +
						" ( " +
						" SELECT  " +
						"     (SELECT sum(count) " +
						"         FROM ezindex_kind " +
						"         WHERE word like '" + pnKey.replace("'","''") + "%' and kind = 0) " +
						"         AS NUM, " +
						"     (SELECT avg(count) " +
						"         FROM ezindex_kind " +
						"         WHERE word like '" + pnKey.replace("'","''") + "%') " +
						"         AS DENOM " +
						" )A";
		
		List<String> sList = new ArrayList<>();
		sList = db.getList(strSql, Site.fm);
		
		float nCount = 0L;
		if(sList.size() > 0)
		{
			try
			{
				nCount = Float.parseFloat(sList.get(0).trim());
			}
			catch (Exception e)
			{
				System.out.print(e.getMessage());
			}
			
		}
		*/
		
		return IsRealPn(pnKey);
		
		/*
		// 20160304 放寬pn認定條件
		if(pnKey.length() <= 3)
		{
			return false;
		}
		
		
		String strInverseArray[] = pnKey.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
		if(strInverseArray.length < 2 && !CommonUtil.IsNumeric(pnKey)) // 非中英夾雜有可能是單字
		{
			return false;
		}

		List<String> sList = new ArrayList<>();
		
		pnKey = CommonUtil.parsePnKey(pnKey);
		
		
		*/
		
		// 20160427 認定是否為pn放至最寬鬆
		// 20160526 sky suggest not using pm_supplier_pn anymore
		/*
		String strSql = "(select pn from pm_supplier_pn where supplier_pn_key like '" + pnKey + "' limit 20)  "
		+ " UNION (SELECT pn FROM pm_pn where pn_key like '" + pnKey + "' limit 20) ORDER BY pn limit 20";
		*/

		/*
		String strSql = "SELECT count(pn) FROM pm_pn where pn_key like '" + pnKey + "' limit 20";
		
		sList = DbHelper.getList(strSql, Site.pm);
		
		int nCount = 0;
		if(sList.size() > 0)
		{
			try
			{
				nCount = Integer.parseInt(sList.get(0).trim());
			}
			catch (Exception e)
			{
				System.out.print(e.getMessage());
			}
			
		}
		
		return nCount > 0 ? true: false;
		
		*/

	}

public static List<String> getPnsByPnKey(String pnKey, String orgKey) {
	
	List<String> sList = null;
	List<String> fmList = null;
	List<String> fmOrg = null;
	
	DbHelper db = new DbHelper();
	
	pnKey = CommonUtil.parsePnKey(pnKey);
	
	String strSql = "(select pn from pm_supplier_pn where supplier_pn_key like '" + pnKey + "' limit 1) "
	+ " UNION (SELECT pn FROM pm_pn where pn_key like '" + pnKey.replace("'","''") + "' limit 1) ORDER BY pn limit 1";

	sList = db.getList(strSql, Site.pm);
	
	sList = CommonUtil.removeSpaceList(sList);
	
	// 20161103 改進查詢效能 semiconductors
	fmOrg = getPnsByPnKeyFuzzy(orgKey + "%");
	for (String x : fmOrg){
		   if (!sList.contains(x))
			   sList.add(x);
		}
	
	
	fmList = getPnsByPnKeyFuzzy(pnKey);
	for (String x : fmList){
		   if (!sList.contains(x) && sList.size() < 50)
			   sList.add(x);
		}
	
	fmList = getPnsByPnKeyFuzzy(orgKey);
	for (String x : fmList){
		   if (!sList.contains(x) && sList.size() < 50)
			   sList.add(x);
		}

	return sList;

}
	
public static List<String> getPnsByPnKey(String pnKey) {
		
		List<String> sList = null;
		List<String> fmList = null;
		
		DbHelper db = new DbHelper();
		
		pnKey = CommonUtil.parsePnKey(pnKey);
		
		String strSql = "(select pn from pm_supplier_pn where supplier_pn_key like '" + pnKey + "' limit 1) "
		+ " UNION (SELECT pn FROM pm_pn where pn_key like '" + pnKey.replace("'","''") + "' limit 1) ORDER BY pn limit 1";

		sList = db.getList(strSql, Site.pm);
		
		sList = CommonUtil.removeSpaceList(sList);
		
		// 20161103 改進查詢效能 semiconductors
		fmList = getPnsByPnKeyFuzzy(pnKey);
		
		for (String x : fmList){
			   if (!sList.contains(x))
				   sList.add(x);
			}

		return sList;

	}
	
	public static List<String> getPnsByPnKeyFuzzy(String pnKey) {
		
		List<String> sList = new ArrayList<String>(); 
		
		DbHelper db = new DbHelper();
	
		//pnKey = CommonUtil.parsePnKey(pnKey);
		
		// 20161103 改進查詢效能 semiconductors
		//String strSql = "select distinct word from qeindex where kind = 0 and word like  '" + pnKey + "' limit 100 ";
		//String strSql = "select word from qeindex where kind = 0 and word like  '" + pnKey + "' limit 20 ";
		String strSql = "select pn from pn_weight where pn like  '" + pnKey.replace("'","''") + "' order by count desc limit 40 ";

		sList = db.getList(strSql, Site.fm);
		
		sList = CommonUtil.removeSpaceList(sList);
		
		// 沒查到
		if(sList.size() == 0)
		{
			strSql = "select pn from auto_cache_pn where pn like  '" + pnKey.replace("'","''") + "' limit 10 ";

			sList = db.getList(strSql, Site.fm);
			
			sList = CommonUtil.removeSpaceList(sList);
		}
		
		// 沒查到再找supplier_pn
		if(sList.size() == 0)
		{
			strSql = "select pn from fm_product where supplier_pn like  '" + pnKey.replace("'","''") + "' limit 10 ";

			sList = db.getList(strSql, Site.fm);
			
			sList = CommonUtil.removeSpaceList(sList);
		}

		return sList;

	}
	
	public static List<Integer> getArrayIndex(String strSql){
		
		List<Integer> sList = new ArrayList<>();
		
		
		
		try {

			Connection conn = null;
			Statement stmt = null;
			ResultSet rs = null;

			try {

				conn = DbHelper.connectFm();
				
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				 
				 
				 while(rs.next()) { 
					 Array page = rs.getArray(1);
					 Integer[] intPage = (Integer[])page.getArray();
					 
					 for (int index = 0; index < intPage.length; index++)
					 {
						 sList.add(intPage[index]);
					 }
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
		return sList;
		
		
		

	}
		
		
		
	
	
	public static List<IndexPrice> getPriceByProdcut(String strSql) {
		
		List<IndexPrice> sList = new ArrayList<>();
		
		
		try {

			Connection conn = null;
			Statement stmt = null;
			ResultSet rs = null;

			try {

					conn = DbHelper.connectPm();
			
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					sList.add(new IndexPrice(rs.getInt(1), rs.getInt(2), rs.getString(3)));
				
			}

			finally {

				DbHelper.attemptClose(rs);
				DbHelper.attemptClose(stmt);
				DbHelper.attemptClose(conn);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return sList;

	}
	
	public static Mfs getMfsById(Integer id) {
		
		long nowDate = System.currentTimeMillis();
		
		// 12個小時更換一次cache
		if(nowDate - cacheTime > 12 * 60 * 60 * 1000)
		{
			cachedMfs = null;
		}
		
		if (cachedMfs == null) {
			cachedMfs = new HashMap<Integer, Mfs>();
			cacheTime = nowDate;
        }
		
		Mfs cached = cachedMfs.get(id);
		
		if(cached == null)
		{
			Mfs sList = new Mfs();
			
			String strSql = "SELECT id, name, cooperation description, logo, url, up_name, created_time, updated_time, advertise, priority  FROM pm_mfs_standard where id = " + id;
		
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
						sList.setId(rs.getInt(1));
						sList.setName(rs.getString(2));
						sList.setDescription(rs.getString(3));
						sList.setLogo(rs.getString(4));
						sList.setUrl(rs.getString(5));
						sList.setUpName(rs.getString(6));
						sList.setCreatedTime(rs.getDate(7));
						sList.setUpdatedTime(rs.getDate(8));
						sList.setAdvertise(rs.getString(9));
						sList.setPriority(rs.getInt(10));
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
			
			cachedMfs.put(id, sList);
			
			cached = sList;
		}
		
		return cached;

	}
	
	
	public static List<Map<String, Object>> queryForList(String strSql) {
		
		List<Map<String, Object>> sList = new ArrayList<Map<String, Object>>();
		
		Vector<String> columnNames = new Vector<String>();
		
		try {

			Connection conn = null;
			Statement stmt = null;
			ResultSet rs = null;

			try {

					conn = DbHelper.connectPm();
			
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				
				if (rs != null) {
			        ResultSetMetaData columns = rs.getMetaData();
			        int i = 0;
			        while (i < columns.getColumnCount()) {
			          i++;
			          //System.out.print(columns.getColumnName(i) + "\t");
			          columnNames.add(columns.getColumnName(i));
			        }
			        //System.out.print("\n");

			        while (rs.next()) {
			        	Map<String, Object> value = new HashMap<String, Object>();
			        	
			          for (i = 0; i < columnNames.size(); i++) {
			        	  value.put(columnNames.get(i), rs.getObject(columnNames.get(i)));
			          
			          }
			          
			          sList.add(value);
			          
			        }

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
		return sList;

	}
}
