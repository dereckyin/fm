package com.gecpp.fm.Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.gecpp.fm.fuzzysearch;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DbHelper {
	
	public static enum Site {
	    pm, fm 
	} 
	 
	
	static final  boolean DEBUG_BUILD = false;
	
	private static String OmUrl;
	private static String OmUser;
	private static String OmPwd;
	
	private static String fmUrl;
	private static String fmUser;
	private static String fmPwd;
	
	private static ComboPooledDataSource cpdsPm = null;
	private static ComboPooledDataSource cpdsFm = null;
	
	// 讀取redis
	private static String RedisUrl;
	
	private static void loadParams() {
		
		Context envurl;
		String entry;

		try {
			
			if(DEBUG_BUILD == true)
			{
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("om.param.url.debug");
				OmUrl = entry;
	
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("om.param.user.debug");
				OmUser = entry;
				
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("om.param.pwd.debug");
				OmPwd = entry;
				
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("fm.param.url.debug");
				fmUrl = entry;
	
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("fm.param.user.debug");
				fmUser = entry;
				
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("fm.param.pwd.debug");
				fmPwd = entry;
				
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("om.param.redis.debug");
				RedisUrl = entry;
			}
			else
			{
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("om.param.url");
				OmUrl = entry;
	
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("om.param.user");
				OmUser = entry;
				
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("om.param.pwd");
				OmPwd = entry;
				
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("fm.param.url");
				fmUrl = entry;
	
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("fm.param.user");
				fmUser = entry;
				
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("fm.param.pwd");
				fmPwd = entry;
				
				envurl = (Context) new InitialContext().lookup("java:comp/env");
				entry = (String) envurl.lookup("om.param.redis");
				RedisUrl = entry;
			}
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void attemptClose(ResultSet o) {
		try {
			if (o != null)
				o.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void attemptClose(Statement o) {
		try {
			if (o != null)
				o.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void attemptClose(Connection o) {
		try {
			if (o != null)
				o.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Connection connectFm(){
    
		Connection pmConn = null;
		loadParams();
		
		try
        {
			if(DEBUG_BUILD != true)
			{
				if(cpdsFm == null)
				{
					cpdsFm = new ComboPooledDataSource();
					
					cpdsFm.setDriverClass("org.postgresql.Driver"); //loads the jdbc driver
					cpdsFm.setJdbcUrl(fmUrl);
					cpdsFm.setUser(fmUser);
					cpdsFm.setPassword(fmPwd);
		
			        // the settings below are optional -- c3p0 can work with defaults
					cpdsFm.setMinPoolSize(3);
					cpdsFm.setAcquireIncrement(3);
					cpdsFm.setMaxPoolSize(100);
					cpdsFm.setMaxStatements(0);
					
					cpdsFm.setMaxIdleTime(60);
					
					//LogQueryHistory.InsertLog("fuzzysearch", "connectFm()");
				}
				
				pmConn = cpdsFm.getConnection();
			}
			else
			{
	            String driver = "org.postgresql.Driver";
	            String url = fmUrl;
	            String username = fmUser;
	            String password = fmPwd;
	            Class.forName(driver); // load MySQL driver
	            
	            pmConn = DriverManager.getConnection(url, fmUser, fmPwd);
			}

            pmConn.setAutoCommit(true);

        } catch (Exception ee)
        {
            System.out.print(ee.getMessage());
        }
		
		return pmConn;
    }
	
	public static void Insert(String sql, List<String> params)
	{
		Connection conn = null;
		PreparedStatement pst = null;
		int i = 1;
	
		try {
				
			conn = DbHelper.connectFm();

			
            pst = conn.prepareStatement(sql);
            for(String param : params)
            {
            	pst.setString(i, param);
            	i++;
            }
            
            pst.executeUpdate();

		} catch (SQLException ex) {
			Logger lgr = Logger.getLogger(fuzzysearch.class.getName());
			lgr.log(Level.SEVERE, ex.getMessage(), ex);

		} finally {

			DbHelper.attemptClose(pst);
			DbHelper.attemptClose(conn);

		}
	}
	
	public static void InsertLog(String sql, List<String> params)
	{
		Connection conn = null;
		PreparedStatement pst = null;
		int i = 1;
	
		try {
				
			conn = DbHelper.connectFm();

			
            pst = conn.prepareStatement(sql);
            for(String param : params)
            {
            	if(i==2)
            		pst.setInt(i,  Integer.parseInt(param));
            	else if(i==4)
            		pst.setInt(i,  Integer.parseInt(param));
            	else
            		pst.setString(i, param.substring(0, Math.min(param.length(), 127)));
            	
            	i++;
            }
            
            pst.executeUpdate();

		} catch (SQLException ex) {
			Logger lgr = Logger.getLogger(fuzzysearch.class.getName());
			lgr.log(Level.SEVERE, ex.getMessage(), ex);

		} finally {

			DbHelper.attemptClose(pst);
			DbHelper.attemptClose(conn);

		}
	}
	
	
	public List<String> getList(String strSql, Site side ) {
		
		List<String> sList = new ArrayList<>();
		
		try {

			Connection conn = null;
			Statement stmt = null;
			ResultSet rs = null;

			try {
				if(side == Site.pm)
				{
					conn = DbHelper.connectPm();
				}
				else
				{
					conn = DbHelper.connectFm();
				}
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					sList.add(rs.getString(1));
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
	
	public static Connection connectPm(){
		
		Connection pmConn = null;
		loadParams();
		
		try
        {
			if(DEBUG_BUILD != true)
			{
				if(cpdsPm == null)
				{
					cpdsPm = new ComboPooledDataSource();
				
					cpdsPm.setDriverClass("org.postgresql.Driver"); //loads the jdbc driver
					cpdsPm.setJdbcUrl(OmUrl);
					cpdsPm.setUser(OmUser);
					cpdsPm.setPassword(OmPwd);
		
			        // the settings below are optional -- c3p0 can work with defaults
					cpdsPm.setMinPoolSize(3);
					cpdsPm.setAcquireIncrement(3);
					cpdsPm.setMaxPoolSize(100);
					cpdsPm.setMaxStatements(0);
					
					cpdsPm.setMaxIdleTime(60);
					
					//LogQueryHistory.InsertLog("fuzzysearch", "connectPm()");
	
				}
				
				pmConn = cpdsPm.getConnection();
			}
			else
			{
	            String driver = "org.postgresql.Driver";
	            String url = OmUrl;
	            String username = OmUser;
	            String password = OmPwd;
	            Class.forName(driver); // load MySQL driver
	            pmConn = DriverManager.getConnection(url, OmUser, OmPwd);
            }
            
			pmConn.setAutoCommit(true);
            

        } catch (Exception ee)
        {
            System.out.print(ee.getMessage());
        }
		
		return pmConn;
    }
}
