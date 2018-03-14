package com.gecpp.fm;

import java.util.List;
import java.util.Map;

public interface IFuzzySearch {
	
	/**
	 * DeleteFuzzyRecord，刪除該ID之索引
	 * @param pid
	 * @return 1 success, 0 fail
	 */
	public int DeleteFuzzyRecord(int pid);
	
	/**
	 * InsertFuzzyRecord，新增或更新傳入ID之索引
	 * @param pid
	 * @param pn
	 * @param mfs
	 * @param catalog
	 * @param description
	 * @param param
	 * @return 1 success, 0 fail
	 * 如果FuzzyDB中已有該筆pid資料，則會更新
	 */
	public int InsertFuzzyRecord(
			int pid,
			String pn,
			String mfs,
			String catalog,
			String description,
			String param
);
	
	/**
	 * 舊的zzySearch查詢查詢
	 * @param fuzzyString
	 * @return
	 */
	public List<String> QueryFuzzyRecord(String fuzzyString);
	
	
	/**
	 * QueryFuzzyRecordByListPage，是依照om排序過，且可以回傳分頁過後的料號資訊
	 */
	public OrderResult QueryFuzzyRecordByListPage(String fuzzyString, int currentPage, int pageSize);
	
	/**
	 * QueryFuzzyRecordByDeptSearch，2015/11/30深度搜尋
	 */
	public OrderResult QueryFuzzyRecordByDeptSearch(String pn, int inventory, int lead, int rohs, List<Integer> mfs, List<Integer> abbreviation, int currentPage, int pageSize);
	
	/** 
	 * GetMaxIndexID，可以查詢目前索引建立最後的ID
	 * @return  最後的ID
	 */
	public int GetMaxIndexID();
	
	/** GetIndexIDStatus，可以查詢該索引ID建立的狀態(已建OR未建) 1 為已建 , 0 為未建
	 * 
	 * @param pid
	 * @return  1 為已建 , 0 為未建
	 */
	public int GetIndexIDStatus(int pid);
	
	
	/**
	 * getProductByMultipleSearch，2016/01/07 多料號搜索
	 */
	public QueryResult getProductByMultipleSearch(String[]  parts);
	
	
	/**
	 * getProductByMultipleSearchJson，2016/05/08 多料號搜索Json
	 */
	public QueryResult getProductByMultipleSearchJson(String  parts);
	
	/**
	 * getProductByMultipleSearchJson，2016/05/17  多料号搜索入口新页面需求
	 */
	public Map<String,Map<String,MultipleParam>> findParamByPn(List<String> pns);
	
	/**
	 * QueryFuzzyRecordByDeptSearchDetail，2016/07/06  詳情頁查詢
	 */
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
			int pageSize);
	
	/**
	 * QueryNewPageV1 ，2017/06/15  新網頁 詳情頁查詢
	 */
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
			int pageSize);
	
	/**
	 * QueryNewPageV1 ，2017/06/15  新網頁 詳情頁查詢
	 */
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
			int pageSize);
	
	// 1.      输入：料号
	// 输出：制造商列表、制造商数量
	// 排序：按现有搜索结果页制造商显示顺序
	public ProcurementSet01 ProcurementQuery01(String strData);
	
	// 2.      输入：料号+制造商+起订量（默认1000）
	// 输出(登录前)：料号、制造商、起订量、合作供应商、库存数量、税后单价、原币、原币单价、发货地，MOQ
	// 输出(登录后付费前)：料号、制造商、起订量国际供应商、库存数量、税后单价、原币、原币单价、发货地，MOQ
	// 输出(付费后)：料号、制造商、起订量、所有供应商、库存数量、税后单价、原币、原币单价、发货地，MOQ
	// 排序：价格升序+库存降序
	public ProcurementSet02 ProcurementQuery02(String strData, List<Integer> mfs_ids, int amount, 
			int isLogin,				// 登陸狀況
			int isPaid					// 付費狀況
			);
	
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
			);
}


