package com.gecpp.fm.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.gecpp.fm.Dao.IndexAdj;
import com.gecpp.fm.Dao.IndexRate;
import com.gecpp.fm.Dao.IndexShort;
import com.gecpp.fm.Util.CommonUtil;
import com.gecpp.fm.Util.DbHelper;

public class FuzzyManagerModel {
	
	public static List<IndexAdj> GetAdjust()
	{
		String strSql = "select word, alterword, kind, adjust from qeindexadj";
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		List<IndexAdj> sList = new ArrayList<IndexAdj>();

		try {

			conn = DbHelper.connectFm();
			

			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					sList.add(new IndexAdj(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getFloat(4)));
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			DbHelper.attemptClose(rs);
			DbHelper.attemptClose(stmt);
			DbHelper.attemptClose(conn);
		}

		return sList;
	}
	
	public static List<IndexShort> GetShort()
	{
		String strSql = "select word, alterword from qeindexshort";
		
		List<IndexShort> sList = new ArrayList<IndexShort>();

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {

			conn = DbHelper.connectFm();
	

			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					sList.add(new IndexShort(rs.getString(1), rs.getString(2)));
				
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			DbHelper.attemptClose(rs);
			DbHelper.attemptClose(stmt);
			DbHelper.attemptClose(conn);
		}

		return sList;
	}
	
	// 20160127 料號預先排序
	public static HashMap<String, Integer> OrderPn(List<String> pns)
	{
		HashMap<String, Integer> hashPnWeight = new HashMap<String, Integer>();
		
		String pnSql = CommonUtil.parsePnSql(pns);
		
		String strSql;
		
		strSql = "select pn, weight from qeindexweight where pn in (" + pnSql + ") ";
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = DbHelper.connectFm();
			
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					hashPnWeight.put(rs.getString(1), rs.getInt(2));
				// System.out.println(rs.getString(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			DbHelper.attemptClose(rs);
			DbHelper.attemptClose(stmt);
			DbHelper.attemptClose(conn);
		}
		
		// 再檢查一次，看有沒有漏掉的值
		for(String pn : pns)
		{
			if(!hashPnWeight.containsKey(pn))
				hashPnWeight.put(pn, 0);
		}
		
		return hashPnWeight;
	}
	
	public static List<IndexRate> GetAllIndexRate(String stoken, int order, int limitNumber)
	{
		String strSql;
		
		strSql = "select pn, weight, fullword, kind, page, " + order + " from qeindex where word = '"
                + stoken.replace("'","''") + "' order by weight desc limit " + limitNumber;
		
		List<IndexRate> sList = new ArrayList<IndexRate>();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			conn = DbHelper.connectFm();
		
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					sList.add(new IndexRate(rs.getString(1), rs.getFloat(2), rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getInt(6)));
				// System.out.println(rs.getString(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			DbHelper.attemptClose(rs);
			DbHelper.attemptClose(stmt);
			DbHelper.attemptClose(conn);
		}

		return sList;
	}
}
