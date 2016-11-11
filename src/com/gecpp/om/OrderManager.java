package com.gecpp.om;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.gecpp.fm.MultipleParam;
import com.gecpp.fm.OrderResult;
import com.gecpp.fm.OrderResultDetail;
import com.gecpp.fm.QueryResult;
import com.gecpp.fm.fuzzysearch;
import com.gecpp.fm.Dao.MultiKeyword;
import com.gecpp.fm.Dao.Product;
import com.gecpp.fm.Logic.OmSearchLogic;
import com.gecpp.fm.Util.CommonUtil;
import com.gecpp.fm.Util.DbHelper;
import com.gecpp.fm.Util.SortUtil;
import com.gecpp.fm.model.OrderManagerModel;
import com.gecpp.p.product.domain.Mfs;
import com.gecpp.p.product.domain.Supplier;


class OrdManaerComparator implements Comparator<String> {

    Map<String, Integer> base;
    public OrdManaerComparator(Map<String, Integer> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}


public class OrderManager {
	
	static final  boolean OCTO_BUILD = false;
	
	private String []  pns = null;
	private String []  m_pkg = null;
	private String [] m_supplier = null;
	private List<String> m_mfs = null;
	
	private List<Mfs> m_returnMfs = null;
	private List<Supplier> m_returnSupplier = null;
	
	// 20160112 多料號搜尋
	// 20160513價格庫存另外查
	/*
	private static final String getAllInfoByPn_headMulti = "SELECT a.inventory, a.offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, c.abbreviation as supplier, b.pkg "
			+ "FROM pm_product b  LEFT JOIN pm_store_price a on a.product_id = b.id and (a.valid =1 OR a.valid IS NULL) "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ " where b.pn in(";
	
	private static final String getAllInfoByPn_head = "SELECT a.inventory, a.offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id "
			+ "FROM pm_product b  LEFT JOIN pm_store_price a on a.product_id = b.id and (a.valid =1 OR a.valid IS NULL) "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ " where b.pn in(";
	
	private static final String getAllInfoById_head = "SELECT a.inventory, a.offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id "
			+ "FROM pm_product b  LEFT JOIN pm_store_price a on a.product_id = b.id and (a.valid =1 OR a.valid IS NULL) "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ " where b.id in(";
	*/
	
	// 20161029 for 分表
	private static final String getAllInfoByPn_headMulti_parts = "select inventory, offical_price, id, pn, supplier_pn, mfs, supplier_id, supplier, pkg, description "
			+ "from  "
			+ "( "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, c.abbreviation as supplier, b.pkg, b.description , c.TYPE "
			+ "			FROM pm_product b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "			where b.pn in('*******') and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ "union  "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, c.abbreviation as supplier, b.pkg, b.description , c.TYPE "
			+ "			FROM pm_supplier_product_c1s b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  " 
			+ "			where b.pn in('*******') and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ ") result "
			+ "order by pn, TYPE ";
	
	private static final String getAllInfoByPn_headMultiLike_parts = "select inventory, offical_price, id, pn, supplier_pn, mfs, supplier_id, supplier, pkg, description "
			+ "from  "
			+ "( "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, c.abbreviation as supplier, b.pkg, b.description , c.TYPE "
			+ "			FROM pm_product b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "			where b.pn in(( SELECT pn FROM pm_supplier_pn WHERE supplier_pn_key like ('*******') LIMIT 20 ) UNION ( SELECT pn FROM pm_pn WHERE pn_key like ('*******')  LIMIT 20 ) ORDER BY pn LIMIT 20) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ "union  "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, c.abbreviation as supplier, b.pkg, b.description , c.TYPE "
			+ "			FROM pm_supplier_product_c1s b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  " 
			+ "			where b.pn in(( SELECT pn FROM pm_supplier_pn WHERE supplier_pn_key like ('*******') LIMIT 20 ) UNION ( SELECT pn FROM pm_pn WHERE pn_key like ('*******')  LIMIT 20 ) ORDER BY pn LIMIT 20) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ ") result "
			+ "order by pn, TYPE ";
	
	private static final String getAllInfoByPn_headMulti = "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, c.abbreviation as supplier, b.pkg, b.description "
			+ "FROM pm_product b "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ " where b.pn in(";
	
	private static final String getAllInfoByPn_head = "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id "
			+ "FROM pm_product b "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ " where b.pn in(";
	
	private static final String getAllInfoById_head = "SELECT  0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id "
			+ "FROM pm_product b "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ " where b.id in(";
	
	private static final String getAllInfoByPn_foot = ") and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL) order by b.pn, c.TYPE  ";
	
	//private Connection om_conn = null;
	
	//private Connection fm_conn = null;

	
	
	
	protected Connection getOmPgSqlConnection() throws Exception {
        
        Connection conn = DbHelper.connectPm();
        return conn;
    }
	


    
    public OrderResult getProductByGroupInStoreId(List<String> notRepeatPns) {
		
		if(notRepeatPns == null)
		{
			OrderResult result = new OrderResult();
			LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			result.setPidList(returnMap);
			return result;
		}
			
		if(notRepeatPns.size() == 0)
		{
			OrderResult result = new OrderResult();
			LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			result.setPidList(returnMap);
			return result;
		}
		
		String pnsSql = createIdSql(notRepeatPns);
		
		List<Product> pkey = new ArrayList<>();
		
		pkey = getAllInforByIdLike(pnsSql);
        
		pkey = dealWithWebPListRepeat(pkey);
		
		OrderResult result = formatFromProductList(pkey);

        result = orderProductList(result);
        
		
		return result;
	}
    
    /* 深度搜尋 by PN */
    public OrderResult getProductByGroupInStoreDeep(int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> notRepeatPns,
			int currentPage, 
			int pageSize)
    {
    	if(notRepeatPns == null)
		{
			OrderResult result = new OrderResult();
			LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			result.setPidList(returnMap);
			result.setPns(new String[0]);
			return result;
		}
			
		if(notRepeatPns.size() == 0)
		{
			OrderResult result = new OrderResult();
			LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			result.setPidList(returnMap);
			result.setPns(new String[0]);
			return result;
		}
		
		List<Product> plist = new ArrayList<>();
		List<Product> OmList = new ArrayList<>();
		
		String pnsSql = OmSearchLogic.getFormatPn(notRepeatPns);
		String strSql = OmSearchLogic.getAllInforByPnList(pnsSql, inventory, lead, rohs, mfs, abbreviation);
		
		plist = formatToProductList(strSql);
		
		OrderResult result = null;
		
		if(OCTO_BUILD == true)
		{
			// 20160514 change to search price next time
			plist = OmSearchLogic.getPriceByProductList(plist, inventory, true);
			plist = OmSearchLogic.getPriceByProductList(plist, lead, rohs, mfs, abbreviation);
			
			//InsertQueryLog("getProductByGroupInStoreDeep", strSql, om_conn);
			
			plist = dealWithWebPListRepeat(plist);
			
			// 分頁在此做
			OmList = OmSearchLogic.pageDataOcto(plist, currentPage, pageSize);
			
			result = formatFromProductListOcto(OmList);
		}
		else
		{
			// 20160514 change to search price next time
			plist = OmSearchLogic.getPriceByProductList(plist, inventory);
			plist = OmSearchLogic.getPriceByProductList(plist, lead, rohs, mfs, abbreviation);
			
			//InsertQueryLog("getProductByGroupInStoreDeep", strSql, om_conn);
			
			plist = dealWithWebPListRepeat(plist);
			
			// 分頁在此做
			OmList = OmSearchLogic.pageData(plist, currentPage, pageSize);
			
			result = formatFromProductList(OmList);
		}

        result = orderProductList(result);
        result.setTotalCount(OmSearchLogic.pageCount(plist));
        
        
		
		return result;
    }
    
    
    /* 深度搜尋 by PN */
    /* 20160706 ------------------            詳情頁深度搜尋 by PN */
    public OrderResultDetail getProductByGroupInStoreDeepDetail(int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> notRepeatPns,
			List<String> pkg,
			int hasStock,
			int noStock,
			int hasPrice,
			int hasInquery,
			int currentPage, 
			int pageSize)
    {
    	if(notRepeatPns == null)
		{
    		OrderResultDetail result = new OrderResultDetail();
			//LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
    		LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>> returnMap = new LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>>();
			//result.setPidList(returnMap);
    		result.setProductList(returnMap);
			result.setPns(new String[0]);
			//result.setPkg(new String[0]);
			//result.setSupplier(new String[0]);
			return result;
		}
			
		if(notRepeatPns.size() == 0)
		{
			OrderResultDetail result = new OrderResultDetail();
			//LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>> returnMap = new LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>>();
			//result.setPidList(returnMap);
			result.setProductList(returnMap);
			result.setPns(new String[0]);
			//result.setPkg(new String[0]);
			//result.setSupplier(new String[0]);
			return result;
		}
		
		List<com.gecpp.p.product.domain.Product> plist = new ArrayList<>();
		List<com.gecpp.p.product.domain.Product> OmList = new ArrayList<>();
		
		String pnsSql = OmSearchLogic.getFormatPn(notRepeatPns);
		//String strSql = OmSearchLogic.getAllInforByPnList(pnsSql, inventory, lead, rohs, mfs, abbreviation);
		
		plist = OmSearchLogic.findProductsByPns(pnsSql);
		
		// 20160920
		m_returnMfs = getMfsListDetail(plist);
		m_returnSupplier = getSupplierListDetail(plist);
		
		// 20160514 change to search price next time
		plist = OmSearchLogic.getPriceByProductListDetail(plist, inventory, hasStock, noStock, hasPrice, hasInquery);
		plist = OmSearchLogic.getPriceByProductListDetail(plist, lead, rohs, mfs, abbreviation, pkg);
		
		//InsertQueryLog("getProductByGroupInStoreDeep", strSql, om_conn);
		
		plist = dealWithWebPListRepeatDetail(plist);
		
		// 分頁在此做
		OmList = OmSearchLogic.pageDataDetail(plist, currentPage, pageSize);
		
		OrderResultDetail result = formatFromProductListDetail(OmList);
		
		// 20160920
		result.setSuppliers(m_returnSupplier);
        result.setMfsStandard(m_returnMfs);

        result = orderProductListDetail(result);
        result.setTotalCount(OmSearchLogic.pageCountDetail(plist));

        
		
		return result;
    }
    
    
    /* 深度搜尋 by Id */
    public OrderResult getProductByGroupInStoreIdDeep(int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> notRepeatPns,
			int currentPage, 
			int pageSize)
    {
    	if(notRepeatPns == null)
		{
			OrderResult result = new OrderResult();
			LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			result.setPidList(returnMap);
			return result;
		}
			
		if(notRepeatPns.size() == 0)
		{
			OrderResult result = new OrderResult();
			LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			result.setPidList(returnMap);
			return result;
		}
		
		
		List<Product> plist = new ArrayList<>();
		List<Product> OmList = new ArrayList<>();
		
		String pnsSql = OmSearchLogic.getFormatId(notRepeatPns);
		String strSql = OmSearchLogic.getAllInforByIdList(pnsSql, inventory, lead, rohs, mfs, abbreviation);
		
		plist = formatToProductList(strSql);
		// 20160514 change to search price next time
		plist = OmSearchLogic.getPriceByProductList(plist, inventory);
		plist = OmSearchLogic.getPriceByProductList(plist, lead, rohs, mfs, abbreviation);
		
		//InsertQueryLog("getProductByGroupInStoreDeep", strSql, om_conn);
		
		// 20160422 料號排序應該只限於排序料號
		//plist = dealWithWebPListRepeat(plist);
		plist = dealWithIdList(plist, notRepeatPns);
		
		// 分頁在此做
		OmList = OmSearchLogic.pageData(plist, currentPage, pageSize);
		
		OrderResult result = formatFromProductList(OmList);

        result = orderProductList(result);
        result.setTotalCount(OmSearchLogic.pageCount(plist));
        
 
		
		return result;
    }
    
    
    /* 深度搜尋 by Id */
    /* 20160706 ------------------            詳情頁深度搜尋 by ID */
    public OrderResultDetail getProductByGroupInStoreIdDeepDetail(int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation, 
			List<String> notRepeatPns,
			List<String> pkg,
			int hasStock,
			int noStock,
			int hasPrice,
			int hasInquery,
			int currentPage, 
			int pageSize)
    {
    	if(notRepeatPns == null)
		{
    		OrderResultDetail result = new OrderResultDetail();
			//LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
    		LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>> returnMap = new LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>>();
    		//result.setPidList(returnMap);
			result.setProductList(returnMap);
			result.setPns(new String[0]);
			//result.setPkg(new String[0]);
			//result.setSupplier(new String[0]);
			return result;
		}
			
		if(notRepeatPns.size() == 0)
		{
			OrderResultDetail result = new OrderResultDetail();
			//LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>> returnMap = new LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>>();
			//result.setPidList(returnMap);
			result.setProductList(returnMap);
			result.setPns(new String[0]);
			//result.setPkg(new String[0]);
			//result.setSupplier(new String[0]);
			return result;
		}
		
		
		List<com.gecpp.p.product.domain.Product> plist = new ArrayList<>();
		List<com.gecpp.p.product.domain.Product> OmList = new ArrayList<>();
		
		String pnsSql = OmSearchLogic.getFormatId(notRepeatPns);
		//String strSql = OmSearchLogic.getAllInforDetailByIdList(pnsSql, inventory, lead, rohs, mfs, abbreviation);
		
		plist = OmSearchLogic.findProductsByIds(pnsSql);
		
		// 20160920
		m_returnMfs = getMfsListDetail(plist);
		m_returnSupplier = getSupplierListDetail(plist);
		
		// 20160514 change to search price next time
		plist = OmSearchLogic.getPriceByProductListDetail(plist, inventory, hasStock, noStock, hasPrice, hasInquery);
		plist = OmSearchLogic.getPriceByProductListDetail(plist, lead, rohs, mfs, abbreviation, pkg);
		
		//InsertQueryLog("getProductByGroupInStoreDeep", strSql, om_conn);
		
		// 20160422 料號排序應該只限於排序料號
		//plist = dealWithWebPListRepeat(plist);
		plist = dealWithIdListDetail(plist, notRepeatPns);
		
		// 分頁在此做
		OmList = OmSearchLogic.pageDataDetail(plist, currentPage, pageSize);
		
		OrderResultDetail result = formatFromProductListDetail(OmList);
		
		// 20160920
		result.setSuppliers(m_returnSupplier);
        result.setMfsStandard(m_returnMfs);

        result = orderProductListDetail(result);
        result.setTotalCount(OmSearchLogic.pageCountDetail(plist));
        
 
		
		return result;
    }
    
    // 20160112 多料號搜尋
    public ArrayList<MultiKeyword> getProductByMultikeywordLike(ArrayList<MultiKeyword> notRepeatPns) {
		
		if(notRepeatPns == null)
		{
			return notRepeatPns;
		}
			
		if(notRepeatPns.size() == 0)
		{
			return notRepeatPns;
		}
		
		
		
		// 先把所有料號給pm去搜尋
		for (int i = 0; i < notRepeatPns.size(); i++) {
			
			// 之前搜尋不到的才查詢
			if(notRepeatPns.get(i).getPkey().size() != 0)
			{
				continue;
			}
			
			List<Product> pkey = new ArrayList<>();
            String s = notRepeatPns.get(i).getKeyword();
            
            String pnkey = CommonUtil.parsePnKey(s);
 
            pkey = getAllInforByPnMultiLike(pnkey);
            
            if(pkey.size() != 0)
            	notRepeatPns.get(i).setSearchtype(2);
            
            notRepeatPns.get(i).setPkey(pkey);
        }
	
		
		return notRepeatPns;
	}
    
    // 20160112 多料號搜尋
    public ArrayList<MultiKeyword> getProductByMultikeyword(ArrayList<MultiKeyword> notRepeatPns) {
		
		if(notRepeatPns == null)
		{
			return notRepeatPns;
		}
			
		if(notRepeatPns.size() == 0)
		{
			return notRepeatPns;
		}
		
		
		
		// 先把所有料號給pm去搜尋
		for (int i = 0; i < notRepeatPns.size(); i++) {
			List<Product> pkey = new ArrayList<>();
            String s = notRepeatPns.get(i).getKeyword();
            
            String pnkey = CommonUtil.parsePnKeyNoLike(s);
 
            pkey = getAllInforByPnMulti(pnkey);
            
            if(pkey.size() != 0)
            	notRepeatPns.get(i).setSearchtype(1);
            else
            	notRepeatPns.get(i).setSearchtype(0);
            
            notRepeatPns.get(i).setPkey(pkey);
        }
	
        
		
		return notRepeatPns;
	}
    
 // 20160112 多料號搜尋
    public List<Product> getProductByMultiRedis(String pn) {
		
		
		
		List<Product> pkey = new ArrayList<>();
        String s = pn;
            
        String pnkey = CommonUtil.parsePnKeyNoLike(s);
 
        pkey = getAllInforByPnMulti(pnkey);
            
		
		return pkey;
	}

	public OrderResult getProductByGroupInStore(List<String> notRepeatPns) {
		
		if(notRepeatPns == null)
		{
			OrderResult result = new OrderResult();
			LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			result.setPidList(returnMap);
			return result;
		}
			
		if(notRepeatPns.size() == 0)
		{
			OrderResult result = new OrderResult();
			LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
			result.setPidList(returnMap);
			return result;
		}
		
		
		List<Product> plist = new ArrayList<>();
		
		
		//String pnsSql = createPnSql(notRepeatPns);
		
		for (int i = 0; i < notRepeatPns.size(); i++) {
			List<Product> pkey = new ArrayList<>();
            String s = notRepeatPns.get(i);
 
            pkey = getAllInforByPnLike(s);
            
            plist.addAll(pkey);
        }
		
		// 20160514 change to search price next time
		plist = OmSearchLogic.getPriceByProductList(plist, 0);
		//plist = getAllInforByPnFuzzy(pnsSql);
		
		plist = dealWithWebPListRepeat(plist);
		
		OrderResult result = formatFromProductList(plist);

        result = orderProductList(result);
        
		
		return result;
	}
	
	// 2016/02/16 新增依照製造商排序
	

	private OrderResult orderProductList(OrderResult result)
    {
        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = result.getPidList();
        //returnMap = sortHashMapByValuesD(returnMap);

        result.setPidList(returnMap);
        
        // 20150908依照歡平所需的欄位給予
        // setup 当前页所有产品的id列表(List<PID>)
        List<Integer> lstPID = GetPID(returnMap);
        
        result.setIds(lstPID);
        result.setPns(pns);

        return result;
    }
	
	private OrderResultDetail orderProductListDetail(OrderResultDetail result)
    {
		LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>> returnMap = result.getProductList();
        //returnMap = sortHashMapByValuesD(returnMap);

        //result.setPidList(returnMap);
        
        // 20150908依照歡平所需的欄位給予
        // setup 当前页所有产品的id列表(List<PID>)
        List<Integer> lstPID = GetPIDDetail(returnMap);
        
        //result.setIds(lstPID);
        result.setPns(pns);
        result.setMfsList(m_mfs);

        return result;
    }
	
	private List<Integer> GetPID(LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> passedMap)
	{
		List<Integer> returnPID = new ArrayList<Integer>();
		List<String> returnPns = new ArrayList<String>();
		
        for (Map.Entry<String, LinkedHashMap<String, List<Integer>>> entry : passedMap.entrySet()) {
            String key = entry.getKey();
            
            returnPns.add(key);
           
            Map<String, List<Integer>> value = entry.getValue();


            for(Map.Entry<String, List<Integer>> subentry : value.entrySet())
            {
                String subkey = subentry.getKey();

                List<Integer> subvalue = subentry.getValue();

                for(Integer listvalue:subvalue)
                {
                	if(!returnPID.contains(listvalue))
                		returnPID.add(listvalue);
                }
            }
        }
        
        pns = new String[returnPns.size()];
        
        for(int i=0; i<returnPns.size(); i++)
        {
        	pns[i] = returnPns.get(i);
        }
        
        return returnPID;
        
	}
	
	
	private List<Integer> GetPIDDetail(LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>> passedMap)
	{
		List<Integer> returnPID = new ArrayList<Integer>();
		List<String> returnPns = new ArrayList<String>();
		Vector<String> returnMfs = new Vector();
		
        for (Map.Entry<String, Map<String, List<com.gecpp.p.product.domain.Product>>> entry : passedMap.entrySet()) {
            String key = entry.getKey();
            
            returnPns.add(key);
           
            Map<String, List<com.gecpp.p.product.domain.Product>> value = entry.getValue();


            for(Map.Entry<String, List<com.gecpp.p.product.domain.Product>> subentry : value.entrySet())
            {
                String subkey = subentry.getKey();
                
                returnMfs.add(subkey);

                List<com.gecpp.p.product.domain.Product> subvalue = subentry.getValue();

                for(com.gecpp.p.product.domain.Product listvalue:subvalue)
                {
                	if(!returnPID.contains(listvalue.getId().intValue()))
                		returnPID.add(listvalue.getId().intValue());

                }
            }
        }
        
        pns = new String[returnPns.size()];
        
        for(int i=0; i<returnPns.size(); i++)
        {
        	pns[i] = returnPns.get(i);
        }
        
        m_mfs = new ArrayList(returnMfs);
        
        return returnPID;
        
	}
	
	
	
	private LinkedHashMap sortHashMapByValuesPDetail(LinkedHashMap<String, LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>> passedMap) {

        // 找各料號下面的項目多寡，多的排前面
        HashMap<String, Integer> PnOrderMap = new HashMap<String, Integer>();
        OrdManaerComparator ovc =  new OrdManaerComparator(PnOrderMap);
        TreeMap<String,Integer> ord_map = new TreeMap<String,Integer>(ovc);

        for (Map.Entry<String, LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>> entry : passedMap.entrySet()) {
            String key = entry.getKey();
            int count = 0;
            Map<String, List<com.gecpp.p.product.domain.Product>> value = entry.getValue();

            //System.out.println(key + ":");

            for(Map.Entry<String, List<com.gecpp.p.product.domain.Product>> subentry : value.entrySet())
            {
                String subkey = subentry.getKey();

                //System.out.println("    " + subkey + ":");

                List<com.gecpp.p.product.domain.Product> subvalue = subentry.getValue();
                
                for(com.gecpp.p.product.domain.Product item : subvalue)
                {
                	// 20160516 count all for 
                	if(OCTO_BUILD == true)
                	{
	                	if(item.getStorePrice().getOfficalPrice() != null)
	                		if(!item.getStorePrice().getOfficalPrice().isEmpty())
	                			count++;
                	}
                	else
                	{
                		count++;
                	}
                }

                
            }

            PnOrderMap.put(key, count);

        }

        ord_map.putAll(PnOrderMap);

        LinkedHashMap<String, LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>>();


        for(Map.Entry<String,Integer> entry : ord_map.entrySet()) {

        	LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>> value = passedMap.get(entry.getKey());

            // 20160221 order by mfs
            returnMap.put(entry.getKey(), SortUtil.RegroupIndexResultByMfsProductDetail(value));
        }

        return returnMap;
    }
	
	
	
	private LinkedHashMap sortHashMapByValuesP(LinkedHashMap<String, LinkedHashMap<String, List<Product>>> passedMap) {

        // 找各料號下面的項目多寡，多的排前面
        HashMap<String, Integer> PnOrderMap = new HashMap<String, Integer>();
        OrdManaerComparator ovc =  new OrdManaerComparator(PnOrderMap);
        TreeMap<String,Integer> ord_map = new TreeMap<String,Integer>(ovc);

        for (Map.Entry<String, LinkedHashMap<String, List<Product>>> entry : passedMap.entrySet()) {
            String key = entry.getKey();
            int count = 0;
            Map<String, List<Product>> value = entry.getValue();

            //System.out.println(key + ":");

            for(Map.Entry<String, List<Product>> subentry : value.entrySet())
            {
                String subkey = subentry.getKey();

                //System.out.println("    " + subkey + ":");

                List<Product> subvalue = subentry.getValue();
                
                for(Product item : subvalue)
                {
                	// 20160516 count all for 
                	if(OCTO_BUILD == true)
                	{
	                	if(item.getPrice() != null)
	                		if(!item.getPrice().isEmpty())
	                			count++;
                	}
                	else
                	{
                		count++;
                	}
                }

                
            }

            PnOrderMap.put(key, count);

        }

        ord_map.putAll(PnOrderMap);

        LinkedHashMap<String, LinkedHashMap<String, List<Product>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Product>>>();


        for(Map.Entry<String,Integer> entry : ord_map.entrySet()) {

        	LinkedHashMap<String, List<Product>> value = passedMap.get(entry.getKey());

            // 20160221 order by mfs
            returnMap.put(entry.getKey(), SortUtil.RegroupIndexResultByMfsProduct(value));
        }

        return returnMap;
    }

    private LinkedHashMap sortHashMapByValuesD(LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> passedMap) {

        // 找各料號下面的項目多寡，多的排前面
        HashMap<String, Integer> PnOrderMap = new HashMap<String, Integer>();
        OrdManaerComparator ovc =  new OrdManaerComparator(PnOrderMap);
        TreeMap<String,Integer> ord_map = new TreeMap<String,Integer>(ovc);

        for (Map.Entry<String, LinkedHashMap<String, List<Integer>>> entry : passedMap.entrySet()) {
            String key = entry.getKey();
            int count = 0;
            Map<String, List<Integer>> value = entry.getValue();

            //System.out.println(key + ":");

            for(Map.Entry<String, List<Integer>> subentry : value.entrySet())
            {
                String subkey = subentry.getKey();

                //System.out.println("    " + subkey + ":");

                List<Integer> subvalue = subentry.getValue();
                
                Set<Integer> foo = new HashSet<Integer>(subvalue);

                count += foo.size();
            }

            PnOrderMap.put(key, count);

        }

        ord_map.putAll(PnOrderMap);

        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();


        for(Map.Entry<String,Integer> entry : ord_map.entrySet()) {

        	LinkedHashMap<String, List<Integer>> value = passedMap.get(entry.getKey());
            
            // 整理一下 id(刪除重複的)
            for(Map.Entry<String, List<Integer>> listentry:value.entrySet()){
            	List<Integer> listvalue = listentry.getValue();
            	List<Integer> newList = new ArrayList<Integer>();
            	
            	for(Integer idvalue : listvalue)
            	{
            		if(!newList.contains(idvalue))
            			newList.add(idvalue);
            	}
            	
            	// 再把整理過的id list放回去
            	listentry.setValue(newList);
            }

            // 20160221 order by mfs
            returnMap.put(entry.getKey(), SortUtil.RegroupIndexResultByMfs(value));
        }

        return returnMap;
    }
    
    
 // 20160112 多料號搜尋
    public Map<String,Map<String,MultipleParam>> formatParamMultiKeyword(ArrayList<MultiKeyword> notRepeatPns)
    {
    	Map<String,Map<String,MultipleParam>> result = new HashMap<String,Map<String,MultipleParam>>();

    	for(MultiKeyword keys : notRepeatPns)
    	{
    		String pn = keys.getKeyword();

    		List<Product> plist = keys.getPkey();
    		
    		Map<String,MultipleParam> mfsGroupMapInt = result.get(pn);
    		if(mfsGroupMapInt == null) {
            	mfsGroupMapInt = new HashMap<String, MultipleParam>();
            	result.put(pn, mfsGroupMapInt);
            }
    		
    		for (Product pro : plist) {
    		
    			String mfs = pro.getMfs();
   
    			if (mfs != null && mfs.trim().length() > 0) {

	                MultipleParam supplierMap = mfsGroupMapInt.get(mfs);
	                if (supplierMap == null) {
	                	supplierMap = new MultipleParam();
	                	mfsGroupMapInt.put(mfs, supplierMap);
	                }
	                
	                
	                List<String> desc = supplierMap.getDescriptions();
	                if (desc == null) {
	                	desc = new ArrayList<String>();
	                	supplierMap.setDescriptions(desc);
	                }
	                
	                List<String> pkgs = supplierMap.getPkgs();
	                if (pkgs == null) {
	                	pkgs = new ArrayList<String>();
	                	supplierMap.setPkgs(pkgs);
	                }
	                
	                boolean bPkg = false;
	                boolean bDesc = false;
	                
	                String sPkg = "";
	                String sDesc = "";
	                
	                try
	                {
	                	sPkg = pro.getPkg().trim();
	                }
	                catch(Exception e)
	                {}
	                
	                try
	                {
	                	sDesc = pro.getDesciption().trim();
	                }
	                catch(Exception e)
	                {}
	                
	                if(!sPkg.equalsIgnoreCase(""))
	                {
		                for(String pkg : pkgs)
		                {
		                	if(pkg.equalsIgnoreCase(sPkg))
		                	{
		                		bPkg = true;
		                		break;
		                	}
		                }
		                
		                if(bPkg == false)
		                {
		                	pkgs.add(sPkg);
		                }
	                }
	                
	                if(!sDesc.equalsIgnoreCase(""))
	                {
		                for(String des : desc)
		                {
		                	if(des.equalsIgnoreCase(sDesc))
		                	{
		                		bDesc = true;
		                		break;
		                	}
		                }
		                
		                if(bDesc == false)
		                {
		                	desc.add(pro.getDesciption().trim());
		                }
	                }
	            }
    		}
    		
    	}
    	
    	return result;
    }
    
    // 20160112 多料號搜尋
    public QueryResult formatFromMultiKeyword(ArrayList<MultiKeyword> notRepeatPns)
    {
    	QueryResult result = new QueryResult();
    	
    	/*
    	LinkedHashMap<String, Map<String, List<Integer>>> returnMapMfs1 = new LinkedHashMap<String, Map<String, List<Integer>>>();
    	LinkedHashMap<String, Map<String, List<Integer>>> returnMapMfs2 = new LinkedHashMap<String, Map<String, List<Integer>>>();
    	LinkedHashMap<String, Map<String, List<Integer>>> returnMapMfs3 = new LinkedHashMap<String, Map<String, List<Integer>>>();
   
    	LinkedHashMap<String, Map<String, List<Integer>>> returnMapSupplier1 = new LinkedHashMap<String, Map<String, List<Integer>>>();
    	LinkedHashMap<String, Map<String, List<Integer>>> returnMapSupplier2 = new LinkedHashMap<String, Map<String, List<Integer>>>();
    	LinkedHashMap<String, Map<String, List<Integer>>> returnMapSupplier3 = new LinkedHashMap<String, Map<String, List<Integer>>>();
    	
    	result.setPidListGroupMfs1(returnMapMfs1);
    	result.setPidListGroupMfs2(returnMapMfs2);
    	result.setPidListGroupMfs3(returnMapMfs3);
    	
    	result.setPidListGroupSupplier1(returnMapSupplier1);
    	result.setPidListGroupSupplier2(returnMapSupplier2);
    	result.setPidListGroupSupplier3(returnMapSupplier3);
    	*/
    	
    	
    	LinkedHashMap<String, Map<String, Map<Integer, Integer>>> returnMapMfs1 = new LinkedHashMap<String, Map<String, Map<Integer, Integer>>>();
    	LinkedHashMap<String, Map<String, Map<Integer, Integer>>> returnMapMfs2 = new LinkedHashMap<String, Map<String, Map<Integer, Integer>>>();
    	LinkedHashMap<String, Map<String, Map<Integer, Integer>>> returnMapMfs3 = new LinkedHashMap<String, Map<String, Map<Integer, Integer>>>();
   
    	result.setPidListGroupMfs1(returnMapMfs1);
    	result.setPidListGroupMfs2(returnMapMfs2);
    	result.setPidListGroupMfs3(returnMapMfs3);
    	
    	
    	for(MultiKeyword key : notRepeatPns)
    	{
    		int amount = key.getCount();
    		String pn = key.getKeyword();
    		String pkg = key.getPkg();
    		String mfs = key.getMfs();
    		
    		List<Product> plist = key.getPkey();
    		
    		
    		
    		
    		// 20160508加入封裝與製造商篩選資訊
    		List<Product> orzList = new ArrayList<Product>();
    		
    		if(pkg != null)
    		{
	    		if(!pkg.equalsIgnoreCase(""))
	    		{
		    		for(Product pro : plist)
		    		{
		    			String sPkg = "";
		    			try{
		    				sPkg = pro.getPkg().trim();
		    			}
		    			catch(Exception e)
		    			{
		    				
		    			}
		    			if(sPkg.equalsIgnoreCase(pkg))
		    				orzList.add(pro);
		    		}
		    		
		    		plist.clear();
		    		plist.addAll(orzList);
	    		}
    		}
    		
    		orzList.clear();
    		
    		if(mfs != null)
    		{
	    		if(!mfs.equalsIgnoreCase(""))
	    		{
		    		for(Product pro : plist)
		    		{
		    			String sMfs = "";
		    			try{
		    				sMfs = pro.getMfs().trim();
		    			}
		    			catch(Exception e)
		    			{
		    				
		    			}
		    			if(sMfs.equalsIgnoreCase(mfs))
		    				orzList.add(pro);
		    		}
		    		
		    		plist.clear();
		    		plist.addAll(orzList);
	    		}
    		}
    		
    		// 選取不到就算完全不符合
    		if(plist.size() == 0)
    		{
    			returnMapMfs3.put(pn, formatMapFromProductListMfsSupplier(pn, plist));
    			continue;
    		}
    		
    		// 完全匹配
    		if(key.getSearchtype() == 1)
    		{
    			
    			List<Product> newList = new ArrayList<Product>();
    			
    			
    			
    			
    			for(Product product :plist)
    			{
    				//Map<String, String> res = CommonUtil.ParsePrice(product.getPrice());
    				// 2016/03/14  也需有價格資訊
    				if(product.getInventory() >= amount && product.getPrice() != "" && product.getPrice() != null)
    					newList.add(product);
    			}
    			
    			// 符合庫存
    			if(newList.size() > 0)
    			{
    				//returnMapMfs1.put(pn, formatMapFromProductListMfs(pn, newList));
    				//returnMapSupplier1.put(pn, formatMapFromProductListSupplier(pn, newList));
    				returnMapMfs1.put(pn, formatMapFromProductListMfsSupplier(pn, newList));
    			}
    			else	// 部分匹配
    			{
    				//returnMapMfs2.put(pn, formatMapFromProductListMfs(pn, plist));
    				//returnMapSupplier2.put(pn, formatMapFromProductListSupplier(pn, plist));
    				returnMapMfs2.put(pn, formatMapFromProductListMfsSupplier(pn, plist));
    			}
    		}
    		
    		// 料號不符，部分匹配
    		if(key.getSearchtype() == 2)
    		{
    			//returnMapMfs2.put(pn, formatMapFromProductListMfs(pn, plist));
    			//returnMapSupplier2.put(pn, formatMapFromProductListSupplier(pn, plist));
    			returnMapMfs2.put(pn, formatMapFromProductListMfsSupplier(pn, plist));
    		}
    		
    		// 完全不匹配 
    		if(key.getSearchtype() == 0 || key.getSearchtype() == 3)
    		{
    			//returnMapMfs3.put(pn, formatMapFromProductListMfs(pn, plist));
    			//returnMapSupplier3.put(pn, formatMapFromProductListSupplier(pn, plist));
    			returnMapMfs3.put(pn, formatMapFromProductListMfsSupplier(pn, plist));
    		}
    		
    	}
    	
    	return result;
    }
    
    // 20160415 多料號搜尋(Leo更改規格 to Map<pn,Map<mfs,Map<supplier_id,List<pid>>>>)
    private Map<String, Map<Integer, Integer>> formatMapFromProductListMfsSupplier(String pnkey, List<Product> plist) {
    	
    	Map<String, Map<Integer, Integer>> mfsGroupMapInt = new  LinkedHashMap<String, Map<Integer, Integer>>();
    
    	for (Product pro : plist) {
    		
    		String mfs = pro.getMfs();
    		Integer supplier_id = pro.getSupplierid();

    		if (mfs != null && mfs.trim().length() > 0) {

                if(mfsGroupMapInt == null) {
                	mfsGroupMapInt = new LinkedHashMap<String, Map<Integer, Integer>>();
                	
                }
                
                Map<Integer, Integer> supplierMap = mfsGroupMapInt.get(mfs);
                if (supplierMap == null) {
                	supplierMap = new HashMap<Integer, Integer>();
                	mfsGroupMapInt.put(mfs, supplierMap);
                }
                
                Integer idlist = supplierMap.get(supplier_id);
                
                // 取id大者
                if(idlist == null)
                {
                	idlist = pro.getId();
                	supplierMap.put(supplier_id, idlist);
                }
                else
                {
                	if(idlist < pro.getId());
                	supplierMap.put(supplier_id, pro.getId());
                }
                
            }
    	}
    	
    	return mfsGroupMapInt;
    }
    
    // 20160112 多料號搜尋
    private Map<String, List<Integer>> formatMapFromProductListMfs(String pnkey, List<Product> plist) {
    	
    	LinkedHashMap<String, Map<String, List<Integer>>> returnMap = new LinkedHashMap<String, Map<String, List<Integer>>>();
    	
    	Map<String, List<Integer>> mfsGroupMapInt = new  LinkedHashMap<String, List<Integer>>();
    
    	for (Product pro : plist) {
    		
    		String mfs = pro.getMfs();
    		
    		
    		if (mfs != null && mfs.trim().length() > 0) {
                
                
                if(mfsGroupMapInt == null) {
                	mfsGroupMapInt = new LinkedHashMap<String, List<Integer>>();
                	returnMap.put(pnkey, mfsGroupMapInt);
                }
                
                List<Integer> listInt = mfsGroupMapInt.get(mfs);
                if (listInt == null) {
                	listInt = new ArrayList<Integer>();
                	mfsGroupMapInt.put(mfs, listInt);
                }
            
                listInt.add(pro.getId());
            }
    	}
    	
    	return mfsGroupMapInt;
    }
    
 // 20160112 多料號搜尋Supplier
    private Map<String, List<Integer>> formatMapFromProductListSupplier(String pnkey, List<Product> plist) {
    	
    	LinkedHashMap<String, Map<String, List<Integer>>> returnMap = new LinkedHashMap<String, Map<String, List<Integer>>>();
    	
    	Map<String, List<Integer>> mfsGroupMapInt = new  LinkedHashMap<String, List<Integer>>();
    
    	for (Product pro : plist) {
    		
    		String supplier = pro.getSupplier();
    		
    		
    		if (supplier != null && supplier.trim().length() > 0) {
                
                
                if(mfsGroupMapInt == null) {
                	mfsGroupMapInt = new LinkedHashMap<String, List<Integer>>();
                	returnMap.put(pnkey, mfsGroupMapInt);
                }
                
                List<Integer> listInt = mfsGroupMapInt.get(supplier);
                if (listInt == null) {
                	listInt = new ArrayList<Integer>();
                	mfsGroupMapInt.put(supplier, listInt);
                }
            
                listInt.add(pro.getId());
            }
    	}
    	
    	return mfsGroupMapInt;
    }
    
    private List<Product> orderFromProductList(List<Product> plist) {
		OrderResult result = new OrderResult();
		// 20160407 以supplier數量排序  
		//儲存supplier數量
        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> supplierMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
        LinkedHashMap<String, LinkedHashMap<String, List<Product>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Product>>>();
        List<Product> orderReturnList = new ArrayList<Product>();

        Map<Integer, Product> pnProductMap = new HashMap<Integer, Product>();

        LinkedHashMap<String, String> pnMap = new LinkedHashMap<String, String>();

//        List<Product> needUpdatedProducts = new ArrayList<>();
     // 根据pn进行存储
        for (Product pro : plist) {
            String pnkey = pro.getPn();
            if (pnMap.get(pnkey) == null) {
                pnMap.put(pnkey, pnkey);
                
            }

            Integer id = pro.getId();
            boolean addToListflag = true;

            pnProductMap.put(id, pro);


            if (addToListflag) {

            	LinkedHashMap<String, List<Integer>> groupSupplierMap = supplierMap.get(pnkey);
            	LinkedHashMap<String, List<Product>> mfsGroupMapInt = returnMap.get(pnkey);
                
                String mfs = pro.getMfs();
                if (mfs == null) {
                    mfs = pro.getMfs();
                }

                if (mfs != null && mfs.trim().length() > 0) {
                    if (groupSupplierMap == null) {
                    	groupSupplierMap = new LinkedHashMap<String, List<Integer>>();
                        supplierMap.put(pnkey, groupSupplierMap);
                    }
                    
                    if(mfsGroupMapInt == null) {
                    	mfsGroupMapInt = new LinkedHashMap<String, List<Product>>();
                    	returnMap.put(pnkey, mfsGroupMapInt);
                    }

                    List<Integer> listSupplier = groupSupplierMap.get(mfs);
                    if (listSupplier == null) {
                    	listSupplier = new ArrayList<Integer>();
                        groupSupplierMap.put(mfs, listSupplier);
                    }
                    
                    List<Product> listInt = mfsGroupMapInt.get(mfs);
                    if (listInt == null) {
                    	listInt = new ArrayList<Product>();
                    	mfsGroupMapInt.put(mfs, listInt);
                    }
                
                    boolean addSupplier = true;
                    for(Integer supplierId : listSupplier)
                    {
                    	//if(supplierId == pro.getSupplierid())
                    	if(supplierId == pro.getId())
                    	{
                    		addSupplier = false;
                    		break;
                    	}
                    }
                    if(addSupplier)
                    {
                    	//listSupplier.add(pro.getSupplierid());
                    	listSupplier.add(pro.getId());
                    	listInt.add(pro);
                    }
                    
                }
            }

        }
        
        // 20160501 把無價格的排除不計算
        returnMap = sortHashMapByValuesP(returnMap);
        
        //supplierMap = sortHashMapByValuesD(supplierMap);
        
        // ReOrder returnMap by supplier order
        
        // *************************************************************20160714 ******************************************************
        if(OCTO_BUILD == false)
		{
	        for (Map.Entry<String, LinkedHashMap<String, List<Product>>> entry : returnMap.entrySet()) {
	            String key = entry.getKey();
	          
	            Map<String, List<Product>> value = returnMap.get(key);
	        
	        
	          for(Map.Entry<String, List<Product>> subentry : value.entrySet())
	          {
	              String subkey = subentry.getKey();
	        
	        
	              List<Product> subvalue = subentry.getValue();
	        
	              orderReturnList.addAll(subvalue);
	              
	          }
	         }
		}
        else
        {
	        // *************************************************************20160714 ******************************************************
	        // order like octoparts
	        // 
	        LinkedHashMap<String, List<Product>> newType = new LinkedHashMap<String, List<Product>>();
	        
	        for (Map.Entry<String, LinkedHashMap<String, List<Product>>> entry : returnMap.entrySet()) {
	            String key = entry.getKey();
	          
	            Map<String, List<Product>> value = returnMap.get(key);
	
	
	            for(Map.Entry<String, List<Product>> subentry : value.entrySet())
	            {
	                String subkey = subentry.getKey();
	
	  
	                List<Product> subvalue = subentry.getValue();
	
	                newType.put(key + subkey, subvalue);
	                
	            }
	        }
	        
	        newType = SortUtil.RegroupIndexResultByMfsProduct(newType);
	        
	        for (Map.Entry<String, List<Product>> entry : newType.entrySet()) {
	            String key = entry.getKey();
	          
	            List<Product> value = newType.get(key);
	
	            orderReturnList.addAll(value);
	        }
        }

        return orderReturnList;
    }
    
    private List<com.gecpp.p.product.domain.Product> orderFromProductListDetail(List<com.gecpp.p.product.domain.Product> plist) {
		OrderResult result = new OrderResult();
		// 20160407 以supplier數量排序  
		//儲存supplier數量
        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> supplierMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
        LinkedHashMap<String, LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>>();
        List<com.gecpp.p.product.domain.Product> orderReturnList = new ArrayList<com.gecpp.p.product.domain.Product>();

        Map<Integer, com.gecpp.p.product.domain.Product> pnProductMap = new HashMap<Integer, com.gecpp.p.product.domain.Product>();

        LinkedHashMap<String, String> pnMap = new LinkedHashMap<String, String>();

//        List<Product> needUpdatedProducts = new ArrayList<>();
     // 根据pn进行存储
        for (com.gecpp.p.product.domain.Product pro : plist) {
            String pnkey = pro.getPn();
            if (pnMap.get(pnkey) == null) {
                pnMap.put(pnkey, pnkey);
                
            }

            Integer id = pro.getId().intValue();
            boolean addToListflag = true;

            pnProductMap.put(id, pro);


            if (addToListflag) {

            	LinkedHashMap<String, List<Integer>> groupSupplierMap = supplierMap.get(pnkey);
            	LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>> mfsGroupMapInt = returnMap.get(pnkey);
                
                String mfs = pro.getMfs();
                if (mfs == null) {
                    mfs = pro.getMfs();
                }

                if (mfs != null && mfs.trim().length() > 0) {
                    if (groupSupplierMap == null) {
                    	groupSupplierMap = new LinkedHashMap<String, List<Integer>>();
                        supplierMap.put(pnkey, groupSupplierMap);
                    }
                    
                    if(mfsGroupMapInt == null) {
                    	mfsGroupMapInt = new LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>();
                    	returnMap.put(pnkey, mfsGroupMapInt);
                    }

                    List<Integer> listSupplier = groupSupplierMap.get(mfs);
                    if (listSupplier == null) {
                    	listSupplier = new ArrayList<Integer>();
                        groupSupplierMap.put(mfs, listSupplier);
                    }
                    
                    List<com.gecpp.p.product.domain.Product> listInt = mfsGroupMapInt.get(mfs);
                    if (listInt == null) {
                    	listInt = new ArrayList<com.gecpp.p.product.domain.Product>();
                    	mfsGroupMapInt.put(mfs, listInt);
                    }
                
                    boolean addSupplier = true;
                    for(Integer supplierId : listSupplier)
                    {
                    	//if(supplierId == pro.getSupplierid())
                    	if(supplierId == pro.getId().intValue())
                    	{
                    		addSupplier = false;
                    		break;
                    	}
                    }
                    if(addSupplier)
                    {
                    	//listSupplier.add(pro.getSupplierid());
                    	listSupplier.add(pro.getId().intValue());
                    	listInt.add(pro);
                    }
                    
                }
            }

        }
        
        // 20160501 把無價格的排除不計算
        returnMap = sortHashMapByValuesPDetail(returnMap);
        
        //supplierMap = sortHashMapByValuesD(supplierMap);
        
        // ReOrder returnMap by supplier order
        
        // *************************************************************20160714 ******************************************************
        if(OCTO_BUILD == false)
		{
	        for (Map.Entry<String, LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>> entry : returnMap.entrySet()) {
	            String key = entry.getKey();
	          
	            Map<String, List<com.gecpp.p.product.domain.Product>> value = returnMap.get(key);
	        
	        
	          for(Map.Entry<String, List<com.gecpp.p.product.domain.Product>> subentry : value.entrySet())
	          {
	              String subkey = subentry.getKey();
	        
	        
	              List<com.gecpp.p.product.domain.Product> subvalue = subentry.getValue();
	        
	              orderReturnList.addAll(subvalue);
	              
	          }
	         }
		}
        else
        {
	        // *************************************************************20160714 ******************************************************
	        // order like octoparts
	        // 
	        LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>> newType = new LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>();
	        
	        for (Map.Entry<String, LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>> entry : returnMap.entrySet()) {
	            String key = entry.getKey();
	          
	            Map<String, List<com.gecpp.p.product.domain.Product>> value = returnMap.get(key);
	
	
	            for(Map.Entry<String, List<com.gecpp.p.product.domain.Product>> subentry : value.entrySet())
	            {
	                String subkey = subentry.getKey();
	
	  
	                List<com.gecpp.p.product.domain.Product> subvalue = subentry.getValue();
	
	                newType.put(key + subkey, subvalue);
	                
	            }
	        }
	        
	        newType = SortUtil.RegroupIndexResultByMfsProductDetail(newType);
	        
	        for (Map.Entry<String, List<com.gecpp.p.product.domain.Product>> entry : newType.entrySet()) {
	            String key = entry.getKey();
	          
	            List<com.gecpp.p.product.domain.Product> value = newType.get(key);
	
	            orderReturnList.addAll(value);
	        }
        }

        return orderReturnList;
    }
    
    private OrderResultDetail formatFromProductListDetail(List<com.gecpp.p.product.domain.Product> plist) {
    	OrderResultDetail result = new OrderResultDetail();
        LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>> resultMap = new LinkedHashMap<String, Map<String, List<com.gecpp.p.product.domain.Product>>>();
        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();

        Map<Integer, com.gecpp.p.product.domain.Product> pnProductMap = new HashMap<Integer, com.gecpp.p.product.domain.Product>();

        LinkedHashMap<String, String> pnMap = new LinkedHashMap<String, String>();
        
        // 設定 pkg
        Set<String> returnPkg = new HashSet<String>();
    	// 設定Supplier
        Set<Integer> idSupplier = new HashSet<Integer>();
        List<Supplier> returnSupplier = new ArrayList<Supplier>();
        
        Set<Integer> idMfs = new HashSet<Integer>();
        List<Mfs> returnMfs = new ArrayList<Mfs>();

//        List<Product> needUpdatedProducts = new ArrayList<>();
        // 根据pn进行存储
        for (com.gecpp.p.product.domain.Product pro : plist) {
            String pnkey = pro.getPn();
            if (pnMap.get(pnkey) == null) {
                pnMap.put(pnkey, pnkey);
                
            }

            Integer id = pro.getId().intValue();
            boolean addToListflag = true;

            pnProductMap.put(id, pro);


            if (addToListflag) {

            	Map<String, List<com.gecpp.p.product.domain.Product>> mfsGroupMap = resultMap.get(pnkey);
            	LinkedHashMap<String, List<Integer>> mfsGroupMapInt = returnMap.get(pnkey);
                
                String mfs = pro.getMfs();
                if (mfs == null) {
                    mfs = pro.getMfs();
                }

                if (mfs != null && mfs.trim().length() > 0) {
                    if (mfsGroupMap == null) {
                        mfsGroupMap = new LinkedHashMap<String, List<com.gecpp.p.product.domain.Product>>();
                        resultMap.put(pnkey, mfsGroupMap);
                    }
                    
                    if(mfsGroupMapInt == null) {
                    	mfsGroupMapInt = new LinkedHashMap<String, List<Integer>>();
                    	returnMap.put(pnkey, mfsGroupMapInt);
                    }

                    List<com.gecpp.p.product.domain.Product> list = mfsGroupMap.get(mfs);
                    if (list == null) {
                        list = new ArrayList<com.gecpp.p.product.domain.Product>();
                        mfsGroupMap.put(mfs, list);
                    }
                    
                    List<Integer> listInt = mfsGroupMapInt.get(mfs);
                    if (listInt == null) {
                    	listInt = new ArrayList<Integer>();
                    	mfsGroupMapInt.put(mfs, listInt);
                    }
                
                    list.add(pro);
                    listInt.add(pro.getId().intValue());
                    
                    // package
                    returnPkg.add(pro.getPkg());
                    // mfs
                    if(!idMfs.contains(pro.getMfsId()))
                    {
                    	idMfs.add(pro.getMfsId());
                    	returnMfs.add(OrderManagerModel.getMfsById(pro.getMfsId()));
                    }
                    
                    // supplier
                    if(!idSupplier.contains(pro.getSupplierId()))
                    {
                    	idSupplier.add(pro.getSupplierId());
                    	returnSupplier.add(pro.getSupplier());
                    }
                }
            }

        }
        
        result.setProductList(resultMap);
        
        m_pkg = new String[returnPkg.size()];
        m_pkg = returnPkg.toArray(m_pkg);
        
        //m_supplier = new String[returnSupplier.size()];
        //m_supplier = returnSupplier.toArray(m_supplier);
        
        result.setSuppliers(returnSupplier);
        result.setMfsStandard(returnMfs);
        
        Set<String> keySet = pnMap.keySet();
       

        return result;
    }

	private OrderResult formatFromProductList(List<Product> plist) {
		OrderResult result = new OrderResult();
        LinkedHashMap<String, LinkedHashMap<String, List<Product>>> resultMap = new LinkedHashMap<String, LinkedHashMap<String, List<Product>>>();
        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();

        Map<Integer, Product> pnProductMap = new HashMap<Integer, Product>();

        LinkedHashMap<String, String> pnMap = new LinkedHashMap<String, String>();

//        List<Product> needUpdatedProducts = new ArrayList<>();
        // 根据pn进行存储
        for (Product pro : plist) {
            String pnkey = pro.getPn();
            if (pnMap.get(pnkey) == null) {
                pnMap.put(pnkey, pnkey);
                
            }

            Integer id = pro.getId();
            boolean addToListflag = true;

            pnProductMap.put(id, pro);


            if (addToListflag) {

            	LinkedHashMap<String, List<Product>> mfsGroupMap = resultMap.get(pnkey);
            	LinkedHashMap<String, List<Integer>> mfsGroupMapInt = returnMap.get(pnkey);
                
                String mfs = pro.getMfs();
                if (mfs == null) {
                    mfs = pro.getMfs();
                }

                if (mfs != null && mfs.trim().length() > 0) {
                    if (mfsGroupMap == null) {
                        mfsGroupMap = new LinkedHashMap<String, List<Product>>();
                        resultMap.put(pnkey, mfsGroupMap);
                    }
                    
                    if(mfsGroupMapInt == null) {
                    	mfsGroupMapInt = new LinkedHashMap<String, List<Integer>>();
                    	returnMap.put(pnkey, mfsGroupMapInt);
                    }

                    List<Product> list = mfsGroupMap.get(mfs);
                    if (list == null) {
                        list = new ArrayList<Product>();
                        mfsGroupMap.put(mfs, list);
                    }
                    
                    List<Integer> listInt = mfsGroupMapInt.get(mfs);
                    if (listInt == null) {
                    	listInt = new ArrayList<Integer>();
                    	mfsGroupMapInt.put(mfs, listInt);
                    }
                
                    list.add(pro);
                    listInt.add(pro.getId());
                }
            }

        }
        
        result.setPidList(returnMap);
        
        Set<String> keySet = pnMap.keySet();
       

        return result;
    }
	
	// *************************************************************20160714 ******************************************************
	private OrderResult formatFromProductListOcto(List<Product> plist) {
		OrderResult result = new OrderResult();
        LinkedHashMap<String, LinkedHashMap<String, List<Product>>> resultMap = new LinkedHashMap<String, LinkedHashMap<String, List<Product>>>();
        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();

        Map<Integer, Product> pnProductMap = new HashMap<Integer, Product>();

        LinkedHashMap<String, String> pnMap = new LinkedHashMap<String, String>();

//        List<Product> needUpdatedProducts = new ArrayList<>();
        // 根据pn进行存储
        for (Product pro : plist) {
        	
            String pnkey = pro.getPn().trim() + "_____" + pro.getMfs().trim();
            
            if (pnMap.get(pnkey) == null) {
                pnMap.put(pnkey, pnkey);
                
            }

            Integer id = pro.getId();
            boolean addToListflag = true;

            pnProductMap.put(id, pro);


            if (addToListflag) {

            	LinkedHashMap<String, List<Product>> mfsGroupMap = resultMap.get(pnkey);
            	LinkedHashMap<String, List<Integer>> mfsGroupMapInt = returnMap.get(pnkey);
                
                String mfs = pro.getMfs();
                if (mfs == null) {
                    mfs = pro.getMfs();
                }

                if (mfs != null && mfs.trim().length() > 0) {
                    if (mfsGroupMap == null) {
                        mfsGroupMap = new LinkedHashMap<String, List<Product>>();
                        resultMap.put(pnkey, mfsGroupMap);
                    }
                    
                    if(mfsGroupMapInt == null) {
                    	mfsGroupMapInt = new LinkedHashMap<String, List<Integer>>();
                    	returnMap.put(pnkey, mfsGroupMapInt);
                    }

                    List<Product> list = mfsGroupMap.get(mfs);
                    if (list == null) {
                        list = new ArrayList<Product>();
                        mfsGroupMap.put(mfs, list);
                    }
                    
                    List<Integer> listInt = mfsGroupMapInt.get(mfs);
                    if (listInt == null) {
                    	listInt = new ArrayList<Integer>();
                    	mfsGroupMapInt.put(mfs, listInt);
                    }
                
                    list.add(pro);
                    listInt.add(pro.getId());
                }
            }

        }
        
        // delete under line
        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> returnDelMap = new LinkedHashMap<String, LinkedHashMap<String, List<Integer>>>();
        for (Map.Entry<String, LinkedHashMap<String, List<Integer>>> entry : returnMap.entrySet()) {
            
        	String key = entry.getKey();
            
            String [] split = key.split("_____");
            
            returnDelMap.put(split[0], entry.getValue());
        }
        
        result.setPidList(returnDelMap);
        
        Set<String> keySet = pnMap.keySet();
       

        return result;
    }
	
	private String createIdSql(List<String> pns) {
        String pnSql = "";

        int pnsCount = pns.size();

        if (pns != null && pnsCount > 0) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < pnsCount; i++) {
                String s = pns.get(i);
                stringBuilder.append(s).append(",");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 1);
        } else {
            pnSql = "";
        }

        return pnSql;
    }
	
	private String createPnSql(List<String> pns) {
        String pnSql = "";

        int pnsCount = pns.size();

        if (pns != null && pnsCount > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            //只取前面的20个料号
            pnsCount = pnsCount > 20 ? 20 : pnsCount;
            for (int i = 0; i < pnsCount; i++) {
                String s = pns.get(i);
                stringBuilder.append("'").append(s).append("',");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 1);
        } else {
            pnSql = "''";
        }

        return pnSql;
    }
	/*
	private List<Product> getAllInforByPnFuzzy(String pnkey) {
		 
		String strSql = getAllInfoByPn_head +  "( SELECT pn FROM pm_supplier_pn WHERE supplier_pn_key in (" + pnkey + ") LIMIT 20 ) "
						+ "UNION ( SELECT pn FROM pm_pn WHERE pn_key in (" + pnkey + ")  LIMIT 20 ) ORDER BY pn LIMIT 20"  
						+ getAllInfoByPn_foot;
		
		
		long startSqlTime = System.currentTimeMillis();
		
		List<Product> plist = formatToProductList(strSql);
		
		long stopSqlTime = System.currentTimeMillis();
		long elapsedSqlTime = stopSqlTime - startSqlTime;
		
		//InsertQueryLog("getAllInforByPnFuzzy", "Time:" + elapsedSqlTime + strSql, fm_conn);
        
		return plist;
	}
	*/
	
	// 20160112 多料號搜尋
	private List<Product> getAllInforByPnMulti(String pnkey) {
		 
		//String strSql = getAllInfoByPn_headMulti +  "'" + pnkey + "' "  
		//		+ getAllInfoByPn_foot;
		
		// 20161029 for 分表
		String strSql = getAllInfoByPn_headMulti_parts.replace("*******", pnkey);
		
		
		long startSqlTime = System.currentTimeMillis();
		
		List<Product> plist = formatToProductListMulti(strSql);
		// 20160514 change to search price next time
		plist = OmSearchLogic.getPriceByProductList(plist);
		
		long stopSqlTime = System.currentTimeMillis();
		long elapsedSqlTime = stopSqlTime - startSqlTime;
		
		//InsertQueryLog("getAllInforByPnFuzzy", "Time:" + elapsedSqlTime + strSql, fm_conn);
        
		return plist;
	}
	
	// 20160112 多料號搜尋
		private List<Product> getAllInforByPnMultiLike(String pnkey) {
			 
			//String strSql = getAllInfoByPn_headMulti +  "( SELECT pn FROM pm_supplier_pn WHERE supplier_pn_key like ('" + pnkey + "') LIMIT 20 ) "
			//				+ "UNION ( SELECT pn FROM pm_pn WHERE pn_key like ('" + pnkey + "')  LIMIT 20 ) ORDER BY pn LIMIT 20"  
			//				+ getAllInfoByPn_foot;
			
			// 20161029 for 分表
			String strSql = getAllInfoByPn_headMultiLike_parts.replace("*******", pnkey);
			
			
			long startSqlTime = System.currentTimeMillis();
			
			List<Product> plist = formatToProductListMulti(strSql);
			// 20160514 change to search price next time
			plist = OmSearchLogic.getPriceByProductList(plist);
			
			long stopSqlTime = System.currentTimeMillis();
			long elapsedSqlTime = stopSqlTime - startSqlTime;
			
			//InsertQueryLog("getAllInforByPnFuzzy", "Time:" + elapsedSqlTime + strSql, fm_conn);
	        
			return plist;
		}
	
	
	private List<Product> getAllInforByPnLike(String pnkey) {
		 
		String strSql = getAllInfoByPn_head +  "'" + pnkey + "' "  
						+ getAllInfoByPn_foot;
		
		
		long startSqlTime = System.currentTimeMillis();
		
		List<Product> plist = formatToProductList(strSql);
		// 20160514 change to search price next time
		plist = OmSearchLogic.getPriceByProductList(plist, 0);
		
		long stopSqlTime = System.currentTimeMillis();
		long elapsedSqlTime = stopSqlTime - startSqlTime;
		
		//InsertQueryLog("getAllInforByPnFuzzy", "Time:" + elapsedSqlTime + strSql, fm_conn);
        
		return plist;
	}
	
	private List<Product> getAllInforByIdLike(String pnkey) {
		 
		String strSql = getAllInfoById_head +  pnkey
						+ getAllInfoByPn_foot;
		
		
		long startSqlTime = System.currentTimeMillis();
		
		List<Product> plist = formatToProductList(strSql);
		// 20160514 change to search price next time
		plist = OmSearchLogic.getPriceByProductList(plist, 0);
		
		long stopSqlTime = System.currentTimeMillis();
		long elapsedSqlTime = stopSqlTime - startSqlTime;
		
		//InsertQueryLog("getAllInforByPnFuzzy", "Time:" + elapsedSqlTime + strSql, fm_conn);
        
		return plist;
	}
	
	
	private boolean IsNullOrEmpty(String value)
	{
	  if (value != null)
	    return value.length() == 0;
	  else
	    return true;
	}
	
	// 20160422 針對模糊搜尋的排序
	private List<com.gecpp.p.product.domain.Product> dealWithIdListDetail(List<com.gecpp.p.product.domain.Product> productList, List<String> notRepeatPns) {
		List<com.gecpp.p.product.domain.Product> orderReturnList = new ArrayList<com.gecpp.p.product.domain.Product>();
		List<String> orderPn = new ArrayList<String>();
		LinkedHashSet hs = new LinkedHashSet();
		Map<String, LinkedHashSet<com.gecpp.p.product.domain.Product>> groupProductList = new HashMap<String, LinkedHashSet<com.gecpp.p.product.domain.Product>>();
		
		LinkedHashSet idHs = new LinkedHashSet();
		
		for(String sid : notRepeatPns)
		{
			int pid = Integer.parseInt(sid);
			
			for(com.gecpp.p.product.domain.Product pro : productList)
			{
				if(pro.getId() == pid)
				{
					String sPn = pro.getPn();
					if(!sPn.isEmpty())
					{
						hs.add(sPn);
						
						LinkedHashSet<com.gecpp.p.product.domain.Product> listProduct = groupProductList.get(sPn);
						if (listProduct == null) {
							listProduct = new LinkedHashSet<com.gecpp.p.product.domain.Product>();
							groupProductList.put(sPn, listProduct);
		                }
		            
						// 20160511 fix duplicate ids
						if(!idHs.contains(pro.getId()))
						{
							idHs.add(pro.getId());
							listProduct.add(pro);
						}
					}
				}
			}
		}
		
		Iterator<String> itr = hs.iterator();
        while(itr.hasNext()){
        	orderReturnList.addAll(groupProductList.get(itr.next()));
        }
		
		return orderReturnList;
	}
	
	// 20160422 針對模糊搜尋的排序
		private List<Product> dealWithIdList(List<Product> productList, List<String> notRepeatPns) {
			List<Product> orderReturnList = new ArrayList<Product>();
			List<String> orderPn = new ArrayList<String>();
			LinkedHashSet hs = new LinkedHashSet();
			Map<String, LinkedHashSet<Product>> groupProductList = new HashMap<String, LinkedHashSet<Product>>();
			
			LinkedHashSet idHs = new LinkedHashSet();
			
			for(String sid : notRepeatPns)
			{
				int pid = Integer.parseInt(sid);
				
				for(Product pro : productList)
				{
					if(pro.getId() == pid)
					{
						String sPn = pro.getPn();
						if(!sPn.isEmpty())
						{
							hs.add(sPn);
							
							LinkedHashSet<Product> listProduct = groupProductList.get(sPn);
							if (listProduct == null) {
								listProduct = new LinkedHashSet<Product>();
								groupProductList.put(sPn, listProduct);
			                }
			            
							// 20160511 fix duplicate ids
							if(!idHs.contains(pro.getId()))
							{
								idHs.add(pro.getId());
								listProduct.add(pro);
							}
						}
					}
				}
			}
			
			Iterator<String> itr = hs.iterator();
	        while(itr.hasNext()){
	        	orderReturnList.addAll(groupProductList.get(itr.next()));
	        }
			
			return orderReturnList;
		}
		
	// 20160416 deprecated
	private List<Product> dealWithWebPListRepeat(List<Product> productList) {
		
		
        List<Product> resultProductList = new ArrayList<>();

        Map<String, List<Product>> productMap = new HashMap<>();

        for (Product product : productList) {
            String mfs = product.getMfs();
            if (IsNullOrEmpty(mfs)) {
                continue;
            }
            mfs = mfs.toUpperCase();
            String pn = product.getPn();
            int supplierId = product.getSupplierid();
            String sPn = product.getSupplierpn();
            
            sPn = sPn == null ? "" : sPn;
      
            int productId = product.getId();

            String mapKey = pn + mfs + supplierId + sPn;

            List<Product> productStoreList = productMap.get(mapKey);
            if (productStoreList == null) {
                productStoreList = new ArrayList<>();
                productStoreList.add(product);
                productMap.put(mapKey, productStoreList);
                resultProductList.add(product);
            } else {
                boolean existFlat = false;
                int sProductId = 0;
                Product dupProduct = null;
                for (Product sProduct : productStoreList) {
                    sProductId = sProduct.getId();
                    //产品id不同，即为重复。
                    if (productId != sProductId) {
                        existFlat = true;
                        
                        dupProduct = sProduct;
                        
                        break;
                    }
                }
                
             // take bigger id product
                if (existFlat) {
                	if(productId > sProductId)
                	{
                		productStoreList.remove(dupProduct);
                		resultProductList.remove(dupProduct);
                		
                		productStoreList.add(product);
                        resultProductList.add(product);
                	}
                }
                
                //若不存在该产品，则加入
                if (!existFlat) {
                    productStoreList.add(product);
                    resultProductList.add(product);
                }
                
            }
        }

        //return resultProductList;
        
		//return orderFromProductList(productList);
        return orderFromProductList(resultProductList);
    }
	
	
private List<com.gecpp.p.product.domain.Product> dealWithWebPListRepeatDetail(List<com.gecpp.p.product.domain.Product> productList) {
		
		
        List<com.gecpp.p.product.domain.Product> resultProductList = new ArrayList<>();

        Map<String, List<com.gecpp.p.product.domain.Product>> productMap = new HashMap<>();

        for (com.gecpp.p.product.domain.Product product : productList) {
            String mfs = product.getMfs();
            if (IsNullOrEmpty(mfs)) {
                continue;
            }
            mfs = mfs.toUpperCase();
            String pn = product.getPn();
            int supplierId = product.getSupplierId();
            String sPn = product.getSupplierPn();
            
            sPn = sPn == null ? "" : sPn;
      
            int productId = product.getId().intValue();

            String mapKey = pn + mfs + supplierId + sPn;

            List<com.gecpp.p.product.domain.Product> productStoreList = productMap.get(mapKey);
            if (productStoreList == null) {
                productStoreList = new ArrayList<>();
                productStoreList.add(product);
                productMap.put(mapKey, productStoreList);
                resultProductList.add(product);
            } else {
                boolean existFlat = false;
                int sProductId = 0;
                com.gecpp.p.product.domain.Product dupProduct = null;
                for (com.gecpp.p.product.domain.Product sProduct : productStoreList) {
                    sProductId = sProduct.getId().intValue();
                    //产品id不同，即为重复。
                    if (productId != sProductId) {
                        existFlat = true;
                        
                        dupProduct = sProduct;
                        
                        break;
                    }
                }
                
             // take bigger id product
                if (existFlat) {
                	if(productId > sProductId)
                	{
                		productStoreList.remove(dupProduct);
                		resultProductList.remove(dupProduct);
                		
                		productStoreList.add(product);
                        resultProductList.add(product);
                	}
                }
                
                //若不存在该产品，则加入
                if (!existFlat) {
                    productStoreList.add(product);
                    resultProductList.add(product);
                }
                
            }
        }

        //return resultProductList;
        
		//return orderFromProductList(productList);
        return orderFromProductListDetail(resultProductList);
    }
	
	
	private List<Product> formatToProductList(String strSql) {

		List<Product> sList = new ArrayList<>();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectPm();

			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					sList.add(new Product(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7), rs.getString(8), rs.getString(9), rs.getInt(10), rs.getString(11), rs.getString(12)));
	
			} catch (Exception e) {
				e.printStackTrace();
				
				Logger.getLogger (OrderManager.class.getName()).log(Level.WARNING, strSql);
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
	
	// 20160112 多料號搜尋
	private List<Product> formatToProductListMulti(String strSql) {

		List<Product> sList = new ArrayList<>();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectPm();
		

			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(strSql);
				while (rs.next())
					sList.add(new Product(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7), rs.getString(8), rs.getString(9), rs.getString(10)));
	
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
	
	
	private List<Supplier> getSupplierListDetail(List<com.gecpp.p.product.domain.Product> plist) {
    
/*    	
        // 20161105  改成固定 advertise = 1
		List<Supplier> returnSupplier = new ArrayList<Supplier>();
		
		String strSql = "select * from pm_supplier where advertise = '1' ";
        List<Map<String, Object>> proMapList = OrderManagerModel.queryForList(strSql);
        
        for (Map map : proMapList) {
	        Supplier supplier = new Supplier();
	        supplier.setId(((Integer) map.get("id")));
	        supplier.setName((String) map.get("name"));
	        supplier.setLocalName((String) map.get("local_name"));
	        supplier.setAbbreviation((String) map.get("abbreviation"));
	        supplier.setSiteError((String) map.get("site_error"));
	        
	        returnSupplier.add(supplier);
        }
 */
		
		

        // 設定Supplier
        Set<Integer> idSupplier = new HashSet<Integer>();
        List<Supplier> returnSupplier = new ArrayList<Supplier>();
//        List<Product> needUpdatedProducts = new ArrayList<>();
        // 根据pn进行存储
        for (com.gecpp.p.product.domain.Product pro : plist) {
           
                    
            // supplier
            if(!idSupplier.contains(pro.getSupplierId()))
            {
            	idSupplier.add(pro.getSupplierId());
            	
            	if(pro.getSupplier().getName() != null)
            		if(pro.getSupplier().getAdvertise() == '1')
            			returnSupplier.add(pro.getSupplier());
            }
        }
    
  

        return returnSupplier;
    }
	
	private List<Mfs> getMfsListDetail(List<com.gecpp.p.product.domain.Product> plist) {
	    
		Set<Integer> idMfs = new HashSet<Integer>();
        List<Mfs> returnMfs = new ArrayList<Mfs>();
 

//        List<Product> needUpdatedProducts = new ArrayList<>();
        // 根据pn进行存储
        for (com.gecpp.p.product.domain.Product pro : plist) {
           
                    
        	// mfs
            if(!idMfs.contains(pro.getMfsId()))
            {
            	idMfs.add(pro.getMfsId());
            	if(OrderManagerModel.getMfsById(pro.getMfsId()).getName() != null)
            			returnMfs.add(OrderManagerModel.getMfsById(pro.getMfsId()));
            }
        }
    
   

        return returnMfs;
    }
}
