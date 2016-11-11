package com.gecpp.fm.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
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
	
	public static boolean IsRealPn(String pnKey) {
		
		List<String> sList = new ArrayList<>();

		String strSql = "SELECT count(pn) FROM pm_product where pn = '" + pnKey.replace("'","''") + "' limit 20";
		
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

	}
	
	public static boolean IsPn(String pnKey) {
		
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
		
		// 20160427 認定是否為pn放至最寬鬆
		// 20160526 sky suggest not using pm_supplier_pn anymore
		/*
		String strSql = "(select pn from pm_supplier_pn where supplier_pn_key like '" + pnKey + "' limit 20)  "
		+ " UNION (SELECT pn FROM pm_pn where pn_key like '" + pnKey + "' limit 20) ORDER BY pn limit 20";
		*/

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

	}
	
public static List<String> getPnsByPnKey(String pnKey) {
		
		List<String> sList = null;
		List<String> fmList = null;
		
		pnKey = CommonUtil.parsePnKey(pnKey);
		
		String strSql = "(select pn from pm_supplier_pn where supplier_pn_key like '" + pnKey + "' limit 50) "
		+ " UNION (SELECT pn FROM pm_pn where pn_key like '" + pnKey + "' limit 50) ORDER BY pn limit 50";

		sList = DbHelper.getList(strSql, Site.pm);
		
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
		
		List<String> sList = null;
		
		pnKey = CommonUtil.parsePnKey(pnKey);
		
		// 20161103 改進查詢效能 semiconductors
		//String strSql = "select distinct word from qeindex where kind = 0 and word like  '" + pnKey + "' limit 100 ";
		String strSql = "select word from qeindex where kind = 0 and word like  '" + pnKey + "' limit 20 ";

		sList = DbHelper.getList(strSql, Site.fm);
		
		sList = CommonUtil.removeSpaceList(sList);

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
		
		Mfs sList = new Mfs();
		
		String strSql = "SELECT id, name, description, logo, url, up_name, created_time, updated_time, advertise, priority  FROM pm_mfs_standard where id = " + id;
	
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
		return sList;

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
