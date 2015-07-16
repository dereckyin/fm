package com.gecpp.fm;


import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.caucho.hessian.server.HessianServlet;

public class ServiceImpl extends HessianServlet implements IFuzzySearch {
 
	protected static fuzzysearch fm = null;

	@Override 
	public void init(ServletConfig config) 
	{
		if(fm == null)
		{
			fm = new fuzzysearch();
			fm.loadParams();
			fm.connectPostgrel();
		}
		
		try {
			super.init(config);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public int DeleteFuzzyRecord(int pid) {
		FuzzyInstance fi = new FuzzyInstance();
		
		return fi.DeleteFuzzyRecord(pid, fm.GetDbConnection());
	}

	@Override
	public int InsertFuzzyRecord(
			int pid,
			String pn,
			String mfs,
			String catalog,
			String description,
			String param
								){
		FuzzyInstance fi = new FuzzyInstance();
		return fi.InsertFuzzyRecord(pid, pn, mfs, catalog, description, param, fm.GetDbConnection(), fm.getSegmenter());
	}

	@Override
	public List<String> QueryFuzzyRecord(String fuzzyString) {

		FuzzyInstance fi = new FuzzyInstance();
		List<String> list = fi.GetQuery(fuzzyString, fm.GetDbConnection(), fm.getSegmenter());
		//list.add(fm.DebugGetQuery(fuzzyString));
		return list;
	}
	
	@Override
	public void destroy()
	{
		fm.closePostgrel();
		super.destroy();
	}

}
