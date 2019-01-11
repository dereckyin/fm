package com.gecpp.fm;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import com.caucho.hessian.server.HessianServlet;
import com.gecpp.fm.Util.LogQueryHistory;
import com.gecpp.p.product.domain.Mfs;

public class ServiceImpl extends HessianServlet implements IFuzzySearch {
 
	// 20160218 function deprecated
	//protected static fuzzysearch fm = null;

	@Override 
	public void init(ServletConfig config) 
	{
		// 20160218 not using connection pool anymore
		/*
		if(fm == null)
		{
			fm = new fuzzysearch();
			fm.loadParams();
			fm.connectPostgrel();
		}
		*/
		
		try {
			super.init(config);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public int DeleteFuzzyRecord(int pid) {
		// 20160218 function deprecated
		
		//FuzzyInstance fi = new FuzzyInstance();
		//return fi.DeleteFuzzyRecord(pid);
		return -1;
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
		// 20160218 function deprecated
		//FuzzyInstance fi = new FuzzyInstance();
		//return fi.InsertFuzzyRecord(pid, pn, mfs, catalog, description, param, fm.GetDbConnection(), fm.getSegmenter());
		return -1;
	}

	@Override
	public List<String> QueryFuzzyRecord(String fuzzyString) {

		// 20160218 function deprecated
		//FuzzyInstance fi = new FuzzyInstance();
		//List<String> list = fi.GetQueryByEachWord(fuzzyString, fm.GetDbConnection(), fm.getSegmenter());
		////List<String> list = fi.GetQuery(fuzzyString, fm.GetDbConnection(), fm.getSegmenter());
		////list.add(fm.DebugGetQuery(fuzzyString));
		//return list;
		return null;
	}
	
	@Override
	public OrderResult QueryFuzzyRecordByListPage(String fuzzyString,
			int currentPage, int pageSize) {
		// TODO Auto-generated method stub
		FuzzyInstance fi = new FuzzyInstance();
		
		OrderResult result = fi.QueryFuzzyRecordByListPage(fuzzyString, currentPage, pageSize);
		
		LogQueryHistory.InsertLog("ServiceImpl", "QueryFuzzyRecordByListPage()");
		
		return result;
	}
	
	@Override
	public OrderResult QueryFuzzyRecordByDeptSearch(String pn, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			int currentPage, 
			int pageSize)
	{
		FuzzyInstance fi = new FuzzyInstance();
		
		OrderResult result = fi.QueryFuzzyRecordByDeptSearch(pn, inventory, lead, rohs, mfs, abbreviation, currentPage, pageSize);
		
		LogQueryHistory.InsertLog("ServiceImpl", "QueryFuzzyRecordByDeptSearch()");
		
		return result;
	}

	@Override
	public int GetMaxIndexID() {
		// TODO Auto-generated method stub
		FuzzyInstance fi = new FuzzyInstance();
		// 20160218 function deprecated
		//return fi.GetMaxIndexID(fm.GetDbConnection());
		return fi.GetMaxIndexID();
	}

	@Override
	public int GetIndexIDStatus(int pid) {
		// TODO Auto-generated method stub
		FuzzyInstance fi = new FuzzyInstance();
		
		return fi.GetIndexIDStatus(pid);
	}
	
	@Override
	public void destroy()
	{
		// 20160218 function deprecated
		//fm.closePostgrel();
		super.destroy();
	}

	@Override
	public QueryResult getProductByMultipleSearch(String[]  parts)
	{
		FuzzyInstance fi = new FuzzyInstance();
		
		QueryResult result = fi.QueryProductByMultipleSearch(parts);
		
		LogQueryHistory.InsertLog("ServiceImpl", "getProductByMultipleSearch()");
		
		return result;
	}
	
	@Override
	public QueryResult getProductByMultipleSearchJson(String  parts)
	{
		FuzzyInstance fi = new FuzzyInstance();
		
		QueryResult result = fi.QueryProductByMultipleSearchJson(parts);
		
		LogQueryHistory.InsertLog("ServiceImpl", "getProductByMultipleSearchJson()");
		
		return result;
	}
	
	@Override
	public Map<String,Map<String,MultipleParam>> findParamByPn(List<String> pns)
	{
		String[] stockArr = new String[pns.size()];
		stockArr = pns.toArray(stockArr);
		
		FuzzyInstance fi = new FuzzyInstance();
		
		Map<String,Map<String,MultipleParam>> result = fi.QueryParamterByMultipleSearch(stockArr);
		
		LogQueryHistory.InsertLog("ServiceImpl", "findParamByPn()");
		
		return result;
	}
	
	@Override
	public OrderResultDetail QueryFuzzyRecordByDeptSearchDetail(String strData, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> pkg,
			int hasStock,
			int noStock,
			int hasPrice,
			int hasInquery,
			int currentPage, 
			int pageSize)
	{
		FuzzyInstance fi = new FuzzyInstance();
		OrderResultDetail result = fi.QueryFuzzyRecordByDeptSearchDetail(strData, inventory, lead, rohs, mfs, abbreviation, pkg, hasStock, noStock, hasPrice, hasInquery, currentPage, pageSize);
		
		LogQueryHistory.InsertLog("ServiceImpl", "QueryFuzzyRecordByDeptSearchDetail()");
		
		return result;
	}
	
	@Override
	public OrderResultDetail QueryNewPageV1(String strData, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> pkg,
			int hasStock,
			int noStock,
			int hasPrice,
			int hasInquery,
			int amount,					// 起訂量
			List<String> currencies,	// 幣別
			List<Integer> catalog_ids,	// 分類ID
			int currentPage, 
			int pageSize)
	{
		FuzzyInstance fi = new FuzzyInstance();
		OrderResultDetail result = fi.QueryNewPageV1(strData, inventory, lead, rohs, mfs, abbreviation, pkg, hasStock, noStock, hasPrice, hasInquery, amount, currencies, catalog_ids, currentPage, pageSize);
		
		LogQueryHistory.InsertLog("ServiceImpl", "QueryNewPageV1()");
		
		return result;
	}
	
	@Override
	public OrderResultDetail QueryNewPageV2(String strData, 
			int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> pkg,
			int hasStock,
			int noStock,
			int hasPrice,
			int hasInquery,
			int amount,					// 起訂量
			List<String> currencies,	// 幣別
			List<Integer> catalog_ids,	// 分類ID
			int isLogin,				// 登陸狀況
			int isPaid,					// 付費狀況
			int currentPage, 
			int pageSize)
	{
		/*
		QueryParam para = new QueryParam();
		para.strData = strData;
		para.inventory = inventory;
		para.lead = lead;
		para.rohs = rohs;
		para.mfs = mfs;
		para.abbreviation = abbreviation;
		para.pkg = pkg;
		para.hasStock = hasStock;
		para.noStock = noStock;
		para.hasPrice = hasPrice;
		para.hasInquery = hasInquery;
		para.amount = amount;
		para.currencies = currencies;
		para.catalog_ids = catalog_ids;
		para.isLogin = isLogin;
		para.isPaid = isPaid;
		para.currentPage = currentPage;
		para.pageSize = pageSize;
		
		OrderResultDetail result = null;
		
		String json = "";
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		try {
			json = ow.writeValueAsString(para);
		} catch (JsonGenerationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if(!json.equalsIgnoreCase(""))
		{
			try {
				result = QueryCacheUtil.QueryCache.get(json);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(result == null)
		{
			FuzzyInstance fi = new FuzzyInstance();
			result = fi.QueryNewPageV2(strData, inventory, lead, rohs, mfs, abbreviation, pkg, hasStock, noStock, hasPrice, hasInquery, amount, currencies, catalog_ids, isLogin, isPaid, currentPage, pageSize);
		
		}
		
		*/
		FuzzyInstance fi = new FuzzyInstance();
		OrderResultDetail result = fi.QueryNewPageV2(strData, inventory, lead, rohs, mfs, abbreviation, pkg, hasStock, noStock, hasPrice, hasInquery, amount, currencies, catalog_ids, isLogin, isPaid, currentPage, pageSize);
	
		LogQueryHistory.InsertLog("ServiceImpl", "QueryNewPageV2()");
		
		return result;
	}
	
	
	
	// 1.      输入：料号
		// 输出：制造商列表、制造商数量
		// 排序：按现有搜索结果页制造商显示顺序
	@Override
	public ProcurementSet01 ProcurementQuery01(String strData)
	{
		FuzzyInstance fi = new FuzzyInstance();
		
		OrderResultDetail result = fi.QueryNewPageV1(strData, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, null, null, 1, 10);
		
		ProcurementSet01 pro = new ProcurementSet01();
		
		ArrayList<Mfs> lst = new ArrayList<Mfs>();
		lst.addAll(result.getMfsStandard());
		
		pro.setMfsList(lst);
		pro.setMfsCount(lst.size());
		
		LogQueryHistory.InsertLog("ServiceImpl", "ProcurementQuery01()");
		
		return pro;
		
	}
	
	// 2.      输入：料号+制造商+起订量（默认1000）
	// 输出(登录前)：料号、制造商、起订量、合作供应商、库存数量、税后单价、原币、原币单价、发货地，MOQ
	// 输出(登录后付费前)：料号、制造商、起订量国际供应商、库存数量、税后单价、原币、原币单价、发货地，MOQ
	// 输出(付费后)：料号、制造商、起订量、所有供应商、库存数量、税后单价、原币、原币单价、发货地，MOQ
	// 排序：价格升序+库存降序
	public ProcurementSet02 ProcurementQuery02(String strData, List<Integer> mfs_ids, int amount, 
			int isLogin,				// 登陸狀況
			int isPaid					// 付費狀況
			)
	{		
		FuzzyInstance fi = new FuzzyInstance();
		
		ProcurementSet02 result = fi.Procurement02(strData,  mfs_ids, amount, isLogin, isPaid);
		
		LogQueryHistory.InsertLog("ServiceImpl", "ProcurementQuery02()");
		
		return result;
	}
	
	//3.    输入a：料号+制造商+起订量+发货地
	//	输入b：料号+制造商+起订量+发货地+供应商
	//	输入c：料号+制造商+起订量+发货地+供应商比较数量
	//	输出(登录前)：料号、制造商、起订量、合作供应商、库存数量、税后单价、原币、原币单价、发货地，MOQ
	//	输出(登录后付费前)：料号、制造商、起订量、国际供应商、库存数量、税后单价、原币、原币单价、发货地，MOQ
	//	输出(付费后)：料号、制造商、起订量、所有供应商、库存数量、税后单价、原币、原币单价、发货地，MOQ
	//	排序：价格升序+库存降序
	public ProcurementSet02 ProcurementQuery03(String strData, List<Integer> mfs_ids, Integer amount, String region, List<Integer> supplier_ids, int stock_amount,
			int isLogin,				// 登陸狀況
			int isPaid					// 付費狀況
			)
	{
		FuzzyInstance fi = new FuzzyInstance();
		
		ProcurementSet02 result = fi.Procurement02(strData,  mfs_ids, amount, isLogin, isPaid);
		
		LogQueryHistory.InsertLog("ServiceImpl", "ProcurementQuery03()");
		
		return result;
	}
}

