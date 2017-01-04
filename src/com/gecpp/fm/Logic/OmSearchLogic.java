package com.gecpp.fm.Logic;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gecpp.fm.MultipleParam;
import com.gecpp.fm.Dao.IndexPrice;
import com.gecpp.fm.Dao.IndexResult;
import com.gecpp.fm.Dao.Product;
import com.gecpp.fm.Dao.ProductConfig;
import com.gecpp.fm.model.OrderManagerModel;
import com.gecpp.p.product.domain.Config;
import com.gecpp.p.product.domain.Store;
import com.gecpp.p.product.domain.Supplier;
import com.gecpp.p.product.domain.SupplierConfig;
import com.gecpp.pm.product.util.PriceHelper;

import dev.xwolf.framework.common.json.JSONUtils;
import dev.xwolf.framework.common.util.StringUtils;

public class OmSearchLogic {
	
	// 20160513價格庫存另外查
	/*
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
	private static final String getAllInfoByPn_Multi = "select inventory, offical_price, id, pn, supplier_pn, mfs, supplier_id, lead, rohs, mfs_id, pkg, name "
			+ "from "
			+ "( "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name , c.TYPE "
			+ "			FROM pm_product b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ "			where b.pn in(*******) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ "union  "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name , c.TYPE "
			+ "			FROM pm_supplier_product_c1s  b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ "			where b.pn in(*******) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ ") result "
			+ "order by pn, TYPE ";
	
	private static final String getAllInfoById_Multi = "select inventory, offical_price, id, pn, supplier_pn, mfs, supplier_id, lead, rohs, mfs_id, pkg, name "
			+ "from "
			+ "( "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name , c.TYPE "
			+ "			FROM pm_product b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ "			where b.id in(*******) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ "union  "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name , c.TYPE "
			+ "			FROM pm_supplier_product_c1s  b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ "			where b.id in(*******) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ ") result "
			+ "order by pn, TYPE ";
	
	
	private static final String getAllInfoByPn_head = "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name "
			+ "FROM pm_product b "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ " where b.pn in(";
	
	private static final String getAllInfoById_head = "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name "
			+ "FROM pm_product b "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ " where b.id in(";
	
	
	
	private static final String getAllInfoByPn_foot = ") and b.supplier_id = c.id AND b.status is null and  (c.status='1' OR c.status  IS NULL) "; //order by b.pn, c.TYPE";
	
	// 20160526 use pm_store_price_select
	/*
	private static final String getAllPrice_head = "SELECT product_id, inventory, offical_price FROM pm_store_price where id in (SELECT max(id) FROM pm_store_price where product_id IN (";
	
	private static final String getAllPrice_foot = ") GROUP BY product_id)";
	*/
	
	private static final String getAllPrice_head = "SELECT product_id, inventory, offical_price FROM pm_store_price_select where product_id in (";
	
	private static final String getAllPrice_foot = ") ";
	
	private static final String getAllInfoDetailById_head_Multi = "select s_id, region, region_code, product_id, inventory, offical_price, price, delivery_place, delivery_circle, moq, store_attach, currency, model, supplier_model, created_time, id, uuid, "
			+ "pn, supplier_pn, mfs, mfs_id,  supplier_id, shop_id, pkg, catalog, description, param, status, lead, embgo, rohs, config_service, grab_url, doc_url, pic_url, update_attribute, updated_time,  "
			+ "is_complete, name, local_name, prority, abbreviation, mfs_base, advertise, site_error "
			+ "from "
			+ "( "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach,a.currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, b.mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error  "
			+ "FROM pm_product b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.id in(*******) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL) "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach,a.currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, b.mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error  "
			+ "FROM pm_supplier_product_c1s b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.id in(*******) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ ") result "
			+ "order by pn, prority, mfs_base, created_time desc  limit 400 ";
	
	
	private static final String getAllInfoDetailByPn_head_Multi = "select s_id, region, region_code, product_id, inventory, offical_price, price, delivery_place, delivery_circle, moq, store_attach, currency, model, supplier_model, created_time, id, uuid, "
			+ "pn, supplier_pn, mfs, mfs_id,  supplier_id, shop_id, pkg, catalog, description, param, status, lead, embgo, rohs, config_service, grab_url, doc_url, pic_url, update_attribute, updated_time,  "
			+ "is_complete, name, local_name, prority, abbreviation, mfs_base, advertise, site_error "
			+ "from "
			+ "( "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach,a.currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, b.mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error  "
			+ "FROM pm_product b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL) "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach,a.currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, b.mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error  "
			+ "FROM pm_supplier_product_c1s b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL)  "
			+ ") result "
			+ "order by pn, prority, mfs_base, created_time desc  limit 400 ";
	
	
	private static final String getAllInfoDetailById_head = "SELECT a.id as s_id,a.region,a.region_code,"
            + "a.product_id,a.inventory,"
            + "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach,a.currency,"
            + " a.pn as model,a.supplier_pn "
            + "as supplier_model,a.created_time, "
            + "  b.id, b.uuid, b.pn, b.supplier_pn, b.mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg,"
            + " b.catalog,  b.description, "
            + " b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service, "
            + "  b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete,"
            + " c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error "
            // + " extract(epoch FROM (now()-a.created_time)) as mark   "
            + "  FROM pm_product b  LEFT JOIN pm_store_price_select a on a.product_id = b.id "
            + "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)"
            + " LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
            + " where b.id in(";
	
	private static final String getAllInfoDetailByPn_head = "SELECT a.id as s_id,a.region,a.region_code,"
            + "a.product_id,a.inventory,"
            + "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach,a.currency,"
            + " a.pn as model,a.supplier_pn "
            + "as supplier_model,a.created_time, "
            + "  b.id, b.uuid, b.pn, b.supplier_pn, b.mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg,"
            + " b.catalog,  b.description, "
            + " b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service, "
            + "  b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete,"
            + " c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error "
            // + " extract(epoch FROM (now()-a.created_time)) as mark   "
            + "  FROM pm_product b  LEFT JOIN pm_store_price_select a on a.product_id = b.id "
            + "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)"
            + " LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
            + " where b.pn in(";

    private static final String getAllInfoByPn_getPn = "";

    private static final String getAllInfoDetailByPn_foot = ") and b.supplier_id = c.id AND b.status is null and (c.status='1' OR c.status  IS NULL) order by b.pn, prority, mfs_base, a.created_time desc limit 400";

    private static int confCount = 0;
    private static Config config = null;
    private static List<ProductConfig> supplierConfig = null;
	
	public static String getFormatPn(List<String> notRepeatPns)
	{
		String pnSql = "";
		
		if(notRepeatPns == null)
			return pnSql;

        int pnsCount = notRepeatPns.size();

        if (notRepeatPns != null && pnsCount > 0) {
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("'");
            for (int i = 0; i < pnsCount; i++) {
                String s = notRepeatPns.get(i).replace("'","''");
                stringBuilder.append(s).append("','");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 2);
        } else {
            pnSql = "";
        }

        return pnSql;
	}
	
	public static String getFormatId(List<String> pns)
	{
		String pnSql = "";
		
		if(pns == null)
			return pnSql;

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
	
	public static String getFormatIdFromProdcut(List<Product> pns)
	{
		String pnSql = "";
		
		if(pns == null)
			return pnSql;

        int pnsCount = pns.size();

        if (pns != null && pnsCount > 0) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < pnsCount; i++) {
            	String s = "";
            	try{
                	s = Integer.toString(pns.get(i).getId());
            	}
            	catch(Exception e)
            	{
            		continue;
            	}
                stringBuilder.append(s).append(",");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 1);
        } else {
            pnSql = "";
        }

        return pnSql;
	}
	
	
	private static String getListId(List<Integer> pns)
	{
		String pnSql = "";

		if(pns == null)
			return pnSql;
		
        int pnsCount = pns.size();

        if (pns != null && pnsCount > 0) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < pnsCount; i++) {
                String s = pns.get(i).toString();
                stringBuilder.append(s).append(",");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 1);
        } else {
            pnSql = "";
        }

        return pnSql;
	}
	
	
	public static String getAllInforByPnList(String strPn, int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation)
	{
		/*
		String strSql = getAllInfoByPn_head +  strPn  
				+ getAllInfoByPn_foot;
		
		// 20160518 為了深度查詢的效能，篩選全不做在sql
		//if(inventory > 0)
		//	strSql += " and inventory > 0 ";
			
		//if(lead > 0)
		//	strSql += " and lead = 'f' ";
		
		//if(rohs > 0)
		//	strSql += " and rohs = 't' ";
		
		//if(!getListId(mfs).trim().isEmpty())
		//	strSql += " and mfs_id in(" + getListId(mfs) + ") ";
		
		//if(!getListId(abbreviation).trim().isEmpty())
		//	strSql += " and b.supplier_id in(" + getListId(abbreviation) + ") ";
		
		strSql += " order by b.pn, c.TYPE  ";
		*/
		
		// 20161029 for 分表
		String strSql = getAllInfoByPn_Multi.replace("*******", strPn);
	
		return strSql;
	}
	

	
	public static List<Product> getPriceByProductList(List<Product> unPriceProductList)
	{		
		if(unPriceProductList == null)
			return unPriceProductList;
		
		if(unPriceProductList.size() == 0)
			return unPriceProductList;
		
		String strIds = getFormatIdFromProdcut(unPriceProductList);
		String strSql = getAllPrice_head +  strIds  
				+ getAllPrice_foot;
		
		List<IndexPrice> priceList = OrderManagerModel.getPriceByProdcut(strSql);
		
		
		for(IndexPrice plist : priceList)
		{
			for(Product pro : unPriceProductList)
			{
				if(pro.getId() == plist.getId())
				{
					
					pro.setInventory(plist.getInventory());
					pro.setPrice(plist.getPrice());

					break;
				}
			}
		}
		
		return unPriceProductList;
		
	}
	
	public static List<Product> getPriceByProductList(List<Product> unPriceProductList, int inventory)
	{
		if(inventory == 0)
			return unPriceProductList;
		
		if(unPriceProductList == null)
			return unPriceProductList;
		
		if(unPriceProductList.size() == 0)
			return unPriceProductList;
		
		String strIds = getFormatIdFromProdcut(unPriceProductList);
		String strSql = getAllPrice_head +  strIds  
				+ getAllPrice_foot;
		
		List<IndexPrice> priceList = OrderManagerModel.getPriceByProdcut(strSql);
		
		List<Product> newProductList = new ArrayList<Product>();
		
		for(IndexPrice plist : priceList)
		{
			for(Product pro : unPriceProductList)
			{
				if(pro.getId() == plist.getId())
				{
					if(inventory != 0)
					{
						if(plist.getInventory() >= inventory)
						{
							pro.setInventory(plist.getInventory());
							pro.setPrice(plist.getPrice());
							newProductList.add(pro);
						}
					}
					else
					{
						pro.setInventory(plist.getInventory());
						pro.setPrice(plist.getPrice());

					}
					
					break;
				}
			}
		}
		
		return newProductList;
		
	}
	
	
	public static List<Product> getPriceByProductList(List<Product> unPriceProductList, int inventory, boolean bPrice)
	{
		if(inventory == 0 && bPrice == false)
			return unPriceProductList;
		
		if(unPriceProductList == null)
			return unPriceProductList;
		
		if(unPriceProductList.size() == 0)
			return unPriceProductList;
		
		String strIds = getFormatIdFromProdcut(unPriceProductList);
		String strSql = getAllPrice_head +  strIds  
				+ getAllPrice_foot;
		
		List<IndexPrice> priceList = OrderManagerModel.getPriceByProdcut(strSql);
		
		List<Product> newProductList = new ArrayList<Product>();
		
		for(IndexPrice plist : priceList)
		{
			for(Product pro : unPriceProductList)
			{
				if(pro.getId() == plist.getId())
				{
					if(inventory != 0)
					{
						if(plist.getInventory() >= inventory)
						{
							pro.setInventory(plist.getInventory());
							pro.setPrice(plist.getPrice());
							newProductList.add(pro);
						}
					}
					else
					{
						if(bPrice)
						{
							pro.setInventory(plist.getInventory());
							pro.setPrice(plist.getPrice());
							newProductList.add(pro);
						}
						else
						{
							pro.setInventory(plist.getInventory());
							pro.setPrice(plist.getPrice());
						}

					}
					
					break;
				}
			}
		}
		
		return newProductList;
		
	}
	
	public static List<com.gecpp.p.product.domain.Product> getPriceByProductListDetail(List<com.gecpp.p.product.domain.Product> unPriceProductList, int inventory, int hasStock, int noStock, int hasPrice, int hasInquery)
	{

		if(unPriceProductList == null)
			return unPriceProductList;
		
		if(unPriceProductList.size() == 0)
			return unPriceProductList;
		
		//String strIds = getFormatIdFromProdcut(unPriceProductList);
		//String strSql = getAllPrice_head +  strIds  
		//		+ getAllPrice_foot;
		
		
		
		//List<IndexPrice> priceList = OrderManagerModel.getPriceByProdcut(strSql);
		
		List<com.gecpp.p.product.domain.Product> newProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		
		boolean bAddToNewList;
		
		//for(IndexPrice plist : priceList)
		//{
			for(com.gecpp.p.product.domain.Product pro : unPriceProductList)
			{
				//if(pro.getId() == plist.getId())
				//{
					bAddToNewList = false;

					String price = "";
					Integer stock = 0;
					
					try
		            {
						String s = pro.getStoreList().get(0).getOfficalPrice();
						if(s != null)
							price = s;
		            }
		            catch(Exception e)
		            {}
					
					try
		            {
						Integer s = pro.getStoreList().get(0).getInventory();
						if(s != null)
							stock = s;
		            }
		            catch(Exception e)
		            {}
					
					
					
					// ‘有价格’‘可询价’均未勾选
					if(hasPrice == 0 && hasInquery == 0)
					{
						if(hasStock == 0 && noStock == 0)
							bAddToNewList = true;

						if(hasStock != 0)
						{
							if(stock != 0)
								bAddToNewList = true;
						}
						
						if(noStock != 0)
						{
							if(stock == 0)
								bAddToNewList = true;
						}
						
						if(hasStock != 0 && noStock != 0)
							bAddToNewList = true;
					}

					// 勾选有价格
					if(hasPrice != 0)
					{
						if(!price.equalsIgnoreCase("") && hasStock == 0 && noStock == 0)
							bAddToNewList = true;
						
						if(!price.equalsIgnoreCase("") && hasStock != 0)
						{
							if(stock != 0)
								bAddToNewList = true;
						}
						
						if(!price.equalsIgnoreCase("") && noStock != 0)
						{
							if(stock == 0)
								bAddToNewList = true;
						}
						
						if(!price.equalsIgnoreCase("") && hasStock != 0 && noStock != 0)
							bAddToNewList = true;
					}
					
					// 勾选可询价
					if(hasInquery != 0)
					{
						if(price.equalsIgnoreCase("") && stock > 0 && hasStock == 0 && noStock == 0)
							bAddToNewList = true;
						
						if(price.equalsIgnoreCase("") && stock > 0 && hasStock != 0)
						{
							if(stock != 0)
								bAddToNewList = true;
						}
						
						if(price.equalsIgnoreCase("") && stock > 0 && noStock != 0)
						{
							if(stock == 0)
								bAddToNewList = true;
						}
						
						if(price.equalsIgnoreCase("") && stock > 0 && hasStock != 0 && noStock != 0)
							bAddToNewList = true;
						
						// 20161111
						if(price.equalsIgnoreCase(""))
							bAddToNewList = true;
					}
					
					// 同时勾选有价格和可询价
					if(hasPrice != 0 && hasInquery != 0)
					{
						if(hasPrice != 0 && hasInquery != 0 && hasStock == 0 && noStock == 0)
							bAddToNewList = true;
						
						if(hasPrice != 0 && hasInquery != 0 && hasStock != 0)
						{
							if(stock != 0)
								bAddToNewList = true;
						}
						
						if(hasPrice != 0 && hasInquery != 0 && noStock != 0)
						{
							if(stock == 0)
								bAddToNewList = true;
						}
						
						if(hasPrice != 0 && hasInquery != 0 && hasStock != 0 && noStock != 0)
							bAddToNewList = true;
					}
				
					
					if(bAddToNewList == true && inventory != 0)
					{
						if(stock >= inventory)
				            bAddToNewList = true;
						else
							bAddToNewList = false;
					}
					
					
					if(bAddToNewList)
						newProductList.add(pro);
					
					//break;
				//}
			}
		//}
		
		return newProductList;
		
	}
	
	
	public static List<Product> getPriceByProductList(List<Product> unPriceProductList, int lead, int rohs, List<Integer> mfs_id, List<Integer> supplier_id)
	{
		List<Product> newProductList = new ArrayList<Product>();
		
		for (Product pro : unPriceProductList) {
			
			String sLead = "";
            String sRohs = "";
            int mfsId = 0;
            int supplierId = 0;
            
            boolean bAddToNewList = true;
            
            try
            {
            	sLead = pro.getLead().trim();
            }
            catch(Exception e)
            {}
            
            try
            {
            	sRohs = pro.getRohs().trim();
            }
            catch(Exception e)
            {}
            
            try
            {
            	mfsId = pro.getMfs_id();
            }
            catch(Exception e)
            {}
            
            try
            {
            	supplierId = pro.getSupplierid();
            }
            catch(Exception e)
            {}
            
            if(lead > 0)
            {
            	if(!sLead.equalsIgnoreCase("f"))
            		bAddToNewList = false;
            }
            
            if(rohs > 0)
            {
            	if(!sRohs.equalsIgnoreCase("t"))
            		bAddToNewList = false;
            }
            
            if(mfs_id != null)
            {
            	boolean bFound = false;
            	
            	for(Integer id:mfs_id)
            	{
            		if(mfsId == id)
            		{
            			bFound = true;
            			break;
            		}
            	}
            	
            	if(mfs_id.size() == 0)
            		bFound = true;
            	
            	if(bFound == false)
            		bAddToNewList = false;
            }
            
            if(supplier_id != null)
            {
            	boolean bFound = false;
            	
            	for(Integer id:supplier_id)
            	{
            		if(supplierId == id)
            		{
            			bFound = true;
            			break;
            		}
            	}
            	
            	if(supplier_id.size() == 0)
            		bFound = true;
            	
            	if(bFound == false)
            		bAddToNewList = false;
            }
            
            if(bAddToNewList)
            	newProductList.add(pro);
        }
		
		
		return newProductList;
		
	}
	
	
	public static List<com.gecpp.p.product.domain.Product> getPriceByProductListDetail(List<com.gecpp.p.product.domain.Product> unPriceProductList, int lead, int rohs, List<Integer> mfs_id, List<Integer> supplier_id, List<String> pkg )
	{
		List<com.gecpp.p.product.domain.Product> newProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		
		for (com.gecpp.p.product.domain.Product pro : unPriceProductList) {
			
			boolean sLead = false;
			boolean sRohs = false;
            int mfsId = 0;
            int supplierId = 0;
            String sPkg = "";
            
            boolean bAddToNewList = true;
            
            try
            {
            	sLead = pro.isLead();
            }
            catch(Exception e)
            {}
            
            try
            {
            	sRohs = pro.isRohs();
            }
            catch(Exception e)
            {}
            
            try
            {
            	mfsId = pro.getMfsId();
            }
            catch(Exception e)
            {}
            
            try
            {
            	supplierId = pro.getSupplierId();
            }
            catch(Exception e)
            {}
            
            try
            {
            	sPkg = pro.getPkg();
            }
            catch(Exception e)
            {}
            
            if(lead > 0)
            {
            	if(sLead)
            		bAddToNewList = false;
            }
            
            if(rohs > 0)
            {
            	if(sRohs)
            		bAddToNewList = false;
            }
            
            if(mfs_id != null)
            {
            	boolean bFound = false;
            	
            	for(Integer id:mfs_id)
            	{
            		if(mfsId == id)
            		{
            			bFound = true;
            			break;
            		}
            	}
            	
            	if(mfs_id.size() == 0)
            		bFound = true;
            	
            	if(bFound == false)
            		bAddToNewList = false;
            }
            
            if(supplier_id != null)
            {
            	boolean bFound = false;
            	
            	for(Integer id:supplier_id)
            	{
            		if(supplierId == id)
            		{
            			bFound = true;
            			break;
            		}
            	}
            	
            	if(supplier_id.size() == 0)
            		bFound = true;
            	
            	if(bFound == false)
            		bAddToNewList = false;
            }
            
            
            if(pkg != null)
            {
            	boolean bFound = false;
            	
            	for(String pg:pkg)
            	{
            		if(sPkg.equalsIgnoreCase(pg))
            		{
            			bFound = true;
            			break;
            		}
            	}
            	
            	if(pkg.size() == 0)
            		bFound = true;
            	
            	if(bFound == false)
            		bAddToNewList = false;
            }
            
            
            
            if(bAddToNewList == true)
            	newProductList.add(pro);
        }
		
		
		return newProductList;
		
	}
	
	
	
	public static String getAllInforByIdList(String strPn, int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation)
	{
		/*
		String strSql = getAllInfoById_head +  strPn 
				+ getAllInfoByPn_foot;
		
		// 20160518 為了深度查詢的效能，篩選全不做在sql
		//if(inventory > 0)
		//	strSql += " and inventory > 0 ";
			
		//if(lead > 0)
		//	strSql += " and lead = 'f' ";
		
		//if(rohs > 0)
		//	strSql += " and rohs = 't' ";
		
		//if(!getListId(mfs).trim().isEmpty())
		//	strSql += " and mfs_id in(" + getListId(mfs) + ") ";
		
		//if(!getListId(abbreviation).trim().isEmpty())
		//	strSql += " and b.supplier_id in(" + getListId(abbreviation) + ") ";
		
		strSql += " order by b.pn, c.TYPE ";
		*/
		
		// 20161029 for 分表
		String strSql = getAllInfoById_Multi.replace("*******", strPn);
	
		return strSql;
	}
	
	public static String getAllInforDetailByIdList(String strPn, int inventory, 
			int lead, 
			int rohs, 
			List<Integer> mfs, 
			List<Integer> abbreviation)
	{
		// 20161029 for 分表
		String strSql = getAllInfoDetailById_head_Multi.replace("*******", strPn);
		
		//String strSql = getAllInfoDetailById_head +  strPn 
		//		+ getAllInfoDetailByPn_foot;
		
		// 20160518 為了深度查詢的效能，篩選全不做在sql
		//if(inventory > 0)
		//	strSql += " and inventory > 0 ";
			
		//if(lead > 0)
		//	strSql += " and lead = 'f' ";
		
		//if(rohs > 0)
		//	strSql += " and rohs = 't' ";
		
		//if(!getListId(mfs).trim().isEmpty())
		//	strSql += " and mfs_id in(" + getListId(mfs) + ") ";
		
		//if(!getListId(abbreviation).trim().isEmpty())
		//	strSql += " and b.supplier_id in(" + getListId(abbreviation) + ") ";
		
		//strSql += " order by b.pn, c.TYPE ";
	
		return strSql;
	}
	
	public static int pageCountId(List<Product> plist)
	{
    	
    	int product_id = 0;
   
    	int gPage = -1;
    	for(Product product : plist)
    	{
    		if(product_id != product.getId())
    		{
    			gPage++;

    			product_id = product.getId();
    		}
    	
    	}
    	
    	
    	return gPage + 1;
	}
	
	public static int pageCount(List<Product> plist)
	{
    	
    	String product_id = "";
   
    	int gPage = -1;
    	for(Product product : plist)
    	{
    		if(!product_id.equalsIgnoreCase(product.getPn()))
    		{
    			gPage++;

    			product_id = product.getPn();
    		}
    	
    	}
    	
    	
    	return gPage + 1;
	}
	
	public static int pageCountDetail(List<com.gecpp.p.product.domain.Product> plist)
	{
    	
    	String product_id = "";
   
    	int gPage = -1;
    	for(com.gecpp.p.product.domain.Product product : plist)
    	{
    		if(!product_id.equalsIgnoreCase(product.getPn()))
    		{
    			gPage++;

    			product_id = product.getPn();
    		}
    	
    	}
    	
    	
    	return gPage + 1;
	}
	
	public static List<Product> pageDataId(List<Product> plist, int currentPage, 
			int pageSize)
	{
		List<Product> pageList = new ArrayList<Product> ();
    	Map<Integer, List<Product>> uniqPn = new HashMap<Integer, List<Product>>();
    	
    	int product_id = 0;
   
    	int gPage = -1;
    	for(Product product : plist)
    	{
    		if(product_id != product.getId())
    		{
    			gPage++;
    			
    			List<Product> id_product = new ArrayList<Product>();
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    			
    			product_id = product.getId();
    		}
    		else
    		{
    			List<Product> id_product = uniqPn.get(gPage);
    			
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    		}
    	}
    	
    	// set area by weight
    	for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
        {
        	if(i<gPage+1)
        	{
        		List<Product> pList = uniqPn.get(i);
        		pageList.addAll(pList);
        	}
        }
    	
    	return pageList;
	}
	
	public static List<Product> pageData(List<Product> plist, int currentPage, 
			int pageSize)
	{
		List<Product> pageList = new ArrayList<Product> ();
    	Map<Integer, List<Product>> uniqPn = new HashMap<Integer, List<Product>>();
    	
    	String product_id = "";
   
    	int gPage = -1;
    	for(Product product : plist)
    	{
    		if(!product_id.equalsIgnoreCase(product.getPn()))
    		{
    			gPage++;
    			
    			List<Product> id_product = new ArrayList<Product>();
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    			
    			product_id = product.getPn();
    		}
    		else
    		{
    			List<Product> id_product = uniqPn.get(gPage);
    			
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    		}
    	}
    	
    	// set area by weight
    	for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
        {
        	if(i<gPage+1)
        	{
        		List<Product> pList = uniqPn.get(i);
        		pageList.addAll(pList);
        	}
        }
    	
    	return pageList;
	}
	
	public static List<com.gecpp.p.product.domain.Product> pageDataDetail(List<com.gecpp.p.product.domain.Product> plist, int currentPage, 
			int pageSize)
	{
		List<com.gecpp.p.product.domain.Product> pageList = new ArrayList<com.gecpp.p.product.domain.Product> ();
    	Map<Integer, List<com.gecpp.p.product.domain.Product>> uniqPn = new HashMap<Integer, List<com.gecpp.p.product.domain.Product>>();
    	
    	String product_id = "";
   
    	int gPage = -1;
    	for(com.gecpp.p.product.domain.Product product : plist)
    	{
    		if(!product_id.equalsIgnoreCase(product.getPn()))
    		{
    			gPage++;
    			
    			List<com.gecpp.p.product.domain.Product> id_product = new ArrayList<com.gecpp.p.product.domain.Product>();
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    			
    			product_id = product.getPn();
    		}
    		else
    		{
    			List<com.gecpp.p.product.domain.Product> id_product = uniqPn.get(gPage);
    			
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    		}
    	}
    	
    	// set area by weight
    	for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
        {
        	if(i<gPage+1)
        	{
        		List<com.gecpp.p.product.domain.Product> pList = uniqPn.get(i);
        		pageList.addAll(pList);
        	}
        }
    	
    	return pageList;
	}
	
	// *************************************************************20160714 ******************************************************
	public static List<Product> pageDataOcto(List<Product> plist, int currentPage, 
			int pageSize)
	{
		List<Product> pageList = new ArrayList<Product> ();
    	Map<Integer, List<Product>> uniqPn = new HashMap<Integer, List<Product>>();
    	
    	String product_id = "";
   
    	int gPage = -1;
    	for(Product product : plist)
    	{
    		if(!product_id.equalsIgnoreCase(product.getPn().trim() + product.getMfs().trim()))
    		{
    			gPage++;
    			
    			List<Product> id_product = new ArrayList<Product>();
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    			
    			product_id = product.getPn().trim() + product.getMfs().trim();
    		}
    		else
    		{
    			List<Product> id_product = uniqPn.get(gPage);
    			
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    		}
    	}
    	
    	// set area by weight
    	for(int i=(currentPage - 1) * pageSize; i < currentPage * pageSize; i++)
        {
        	if(i<gPage+1)
        	{
        		List<Product> pList = uniqPn.get(i);
        		pageList.addAll(pList);
        	}
        }
    	
    	return pageList;
	}
	
	
	public static List<com.gecpp.p.product.domain.Product> findProductsByPns(String idsSql) {
		// TODO Auto-generated method stub
		List<com.gecpp.p.product.domain.Product> returnList = new ArrayList<com.gecpp.p.product.domain.Product>();
		
		

        if(!idsSql.equalsIgnoreCase(""))
        {
        	// 20161029 for 分表
        	String selectProductListById = getAllInfoDetailByPn_head_Multi.replace("*******", idsSql);
            // String selectProductListById = getAllInfoDetailByPn_head + idsSql + getAllInfoDetailByPn_foot;
            List<Map<String, Object>> proMapList = OrderManagerModel.queryForList(selectProductListById);
            returnList = formatToProductList(proMapList);
        } 
		
		return returnList;
	}
	
	public static List<com.gecpp.p.product.domain.Product> findProductsByIds(String idsSql) {
		// TODO Auto-generated method stub
		List<com.gecpp.p.product.domain.Product> returnList = new ArrayList<com.gecpp.p.product.domain.Product>();
		
		if(!idsSql.equalsIgnoreCase(""))
        {
			// 20161029 for 分表
			String selectProductListById = getAllInfoDetailById_head_Multi.replace("*******", idsSql);
            // String selectProductListById = getAllInfoDetailById_head + idsSql + getAllInfoDetailByPn_foot;
            List<Map<String, Object>> proMapList = OrderManagerModel.queryForList(selectProductListById);
            
            returnList = formatToProductList(proMapList);
          
        }
		
		return returnList;
	}
	
	
	
	private static List<com.gecpp.p.product.domain.Product> formatToProductList(List<Map<String, Object>> products) {

        List<com.gecpp.p.product.domain.Product> productMap = new ArrayList<com.gecpp.p.product.domain.Product>();
       

        //每遍历一次时，先从productMap根据产品id拿到对应的产品List，如果没有，则新建一个，如果有，则追加进去
        for (Map map : products) {
        	com.gecpp.p.product.domain.Product product = new com.gecpp.p.product.domain.Product();
            Long Id = (Long) map.get("id");
            Integer productId = Id.intValue();

           
            product.setId((long)productId);
            product.setUuid((String) map.get("uuid"));
            product.setPn((String) map.get("pn"));
            product.setMfs((String) map.get("mfs"));
            product.setPkg((String) map.get("pkg"));
            product.setCatalog((String) map.get("catalog"));
            String param = ((String) map.get("param"));
            product.setPicUrl((String) map.get("pic_url"));
            product.setUpdatedTime((Date) map.get("updated_time"));
            product.setParam(param);
            if(map.get("mfs_id")!=null){
            	 product.setMfsId(Integer.parseInt(map.get("mfs_id").toString()));
            }
           

            product.setDocUrl((String) map.get("doc_url"));
            product.setGrabUrl((String) map.get("grab_url"));
            product.setSupplierPn((String) map.get("supplier_pn"));
            product.setDescription((String) map.get("description"));
            product.setShopId((String) map.get("shop_id"));
            product.setSupplierId(((Integer) map.get("supplier_id")));
            Object isLeadObj = map.get("lead");
            boolean isLead = isLeadObj == null ? false : (Boolean) isLeadObj;
            product.setLead(isLead);
            Object isrohsObj = map.get("rohs");
            boolean isrohs = isrohsObj == null ? false : (Boolean) isrohsObj;
            product.setRohs(isrohs);

            // 供应商表&制造商表
            List<Store> storeList = new ArrayList<Store>();
            Supplier supplier = new Supplier();
            supplier.setId(((Integer) map.get("supplier_id")));
            supplier.setName((String) map.get("name"));
            supplier.setLocalName((String) map.get("local_name"));
            supplier.setAbbreviation((String) map.get("abbreviation"));
            supplier.setSiteError((String) map.get("site_error"));
            
     
            try{
				String ob = map.get("advertise").toString();
				char f =  ob.charAt(0);
				
				supplier.setAdvertise(f);
			}
			catch(Exception e)
			{
				//e.printStackTrace();
			}
            
            product.setSupplier(supplier);
            product.setBaseMfs((String) map.get("mfs_base"));
            product.setDeliveryPlace((String) map.get("delivery_place"));
            product.setDeliveryCircle((String) map.get("delivery_circle"));
            Object isCompleteObj = map.get("is_complete");
            boolean isComplete = isCompleteObj == null ? false : (Boolean) isCompleteObj;
            product.setIsComplete(isComplete);
            // 价格表
            Store store = new Store();
            Long storeId = (Long) map.get("s_id");
            if (storeId != null) {
                store.setId((long) storeId.intValue());
                store.setDelivery((String) map.getOrDefault("delivery_circle", ""));
                store.setPn((String) map.get("model"));
                store.setSupplierPn((String) map.get("supplier_model"));
                // Double allTime = (Double) map.get("mark");
                store.setCreatedTime((Date) map.get("created_time"));

                Object inventory = map.get("inventory");
                if (inventory != null) {
                    store.setInventory(Integer.parseInt(inventory.toString()));
                }
                store.setMoq((Integer) map.getOrDefault("moq", 1));
                String priceStr = (String) map.get("price");
                // 天、小时、分钟
                if (!StringUtils.isNullOrEmpty(priceStr)) {
                    List<Map> priceList = JSONUtils.listFormJSONStr(priceStr);
                    store.setPriceList(priceList);
                }
                String officalPriceStr = (String) map.get("offical_price");
                if (!StringUtils.isNullOrEmpty(officalPriceStr)) {
                    officalPriceStr = officalPriceStr.replaceAll("priceStr", "price");
                    List<Map> officalPriceList = JSONUtils.listFormJSONStr(officalPriceStr);
                    store.setOfficalPriceList(officalPriceList);
                    store.setOfficalPrice(officalPriceStr);
                }
                if (map.get("product_id") != null) {
                    Long proId = (Long) map.get("product_id");
                    store.setProductId((long) proId.intValue());
                }
                store.setRegion((String) map.get("region"));
                store.setRegionCode((String) map.get("region_code"));
                store.setSpec((String) map.get("spec"));

                String strStoreAttach = (String) map.get("store_attach");
                if (strStoreAttach != null) {
                    store.setStoreAttach(strStoreAttach);
                }

                store.setCurrency((String) map.getOrDefault("currency", "RMB"));
                storeList.add(store);
                product.setStoreList(storeList);
            }
            
            product = getLocalPrice(product);

            productMap.add(product);
        }

        return productMap;
    }
	
	public static com.gecpp.p.product.domain.Product getLocalPrice(com.gecpp.p.product.domain.Product pro) {
	    List<Store> storeList = pro.getStoreList();
	    if (storeList != null && storeList.size() > 0) {
	        for (Store store : storeList) {

	            String currency = store.getCurrency();
	            float saleRateStr = getSaleRate(
                        pro.getSupplierId(), currency);

	            List priceList = store.getPriceList();

	            if (priceList == null || priceList.size() == 0) {

	                priceList = new ArrayList();

	                List officalPriceList = store.getOfficalPriceList();

	                if (officalPriceList != null && officalPriceList.size() > 0) {
	                    for (int i = 0; i < officalPriceList.size(); i++) {
	                        Map rangePriceMap = (Map) officalPriceList.get(i);

	                        Object priceObj = rangePriceMap.get("price");
	                        if (priceObj == null) {
	                            priceObj = rangePriceMap.get("priceStr");
	                        }
	                        double price = 0d;
	                        if (priceObj != null) {
	                            // price = Double.valueOf(priceObj.toString());
	                            price = StringUtils.parseDouble(priceObj
	                                    .toString());
	                        }
	                        // Integer amount =
	                        // Integer.valueOf(rangePriceMap.get("amount").toString());
	                        Object amountObj = rangePriceMap.get("amount");
	                        if (amountObj != null) {
	                            Integer amount = StringUtils.parseInt(amountObj
	                                    .toString());
	                            
	                            double priceCount = PriceHelper.multiply(price,
	                                    saleRateStr);
	                            priceCount = Math.round(PriceHelper.multiply(
	                                    priceCount, 10000)) / 10000.0000;

	                            Map priceRangePrice = new LinkedHashMap();
	                            priceRangePrice.put("amount", amount);
	                            priceRangePrice.put("price", priceCount);
	                            priceList.add(priceRangePrice);
	                        }
	                    }

	                    store.setPriceList(priceList);
	                }
	            }

	            // 算standStepPrice
	            Map standStepPrice = PriceHelper.getStandStepPrice(priceList);
	            store.setStrwPirce(standStepPrice);
	        }
	    }

	    return pro;
	}
	
	public static float getSaleRate(int supplierId, String officialCurrency) {

        SupplierConfig supplierConfig = getConfig(supplierId);
        float tax = supplierConfig.getTax();

        //update lhp 2015-06-11 s
//        String distCurrency = this.config.getDefaultCurrency();
//        float exchangeRate = this.config.getExchangeRate(officialCurrency, distCurrency);

        String distCurrency = supplierConfig.getLocalCurrency();
        if (StringUtils.isNullOrEmpty(officialCurrency)) {
            officialCurrency = supplierConfig.getOfficialCurrency();
        }

        boolean officialCurrencyIsNull = StringUtils.isNullOrEmpty(officialCurrency);
        if (officialCurrencyIsNull) {
            //logger.error("-----supplier_id:{}----error----not have officialCurrency config.", supplierId);
        }

        float exchangeRate = supplierConfig.getExchangeRate();
        if (!officialCurrencyIsNull) {
            exchangeRate = config.getExchangeRate(officialCurrency, distCurrency);
        }

        float profit = supplierConfig.getProfitRate();
        float result = (1 + tax) * (1 + profit) * exchangeRate;
        return result;

    }
	
	private static List<ProductConfig> GetPmProductConfig()
	{
		String strSql = "select id,supplier_id, inventory_src, exchange_rate, COALESCE(official_currency, '') official_currency, COALESCE(local_currency, '') local_currency, tax, profit_rate,is_dynamic,update_interval from pm_product_config  ";
		List<Map<String, Object>> proList = OrderManagerModel.queryForList(strSql);
		
		List<ProductConfig> returnList = new ArrayList<ProductConfig>();

		for (Map map : proList) {
			
			float exchange_rate = 0;
			try{
				Object ob = map.get("exchange_rate");
				exchange_rate = Float.parseFloat(ob.toString());
			}
			catch(Exception e)
			{
				//e.printStackTrace();
			}
			
			float profit_rate = 0;
			try{
				Object ob = map.get("profit_rate");
				profit_rate = Float.parseFloat(ob.toString());
			}
			catch(Exception e)
			{
				//e.printStackTrace();
			}
			
			float tax = 0;
			try{
				Object ob = map.get("tax");
				tax = Float.parseFloat(ob.toString());
			}
			catch(Exception e)
			{
				//e.printStackTrace();
			}
			
			ProductConfig conf = new ProductConfig((int)map.get("id"), 
													(int)map.get("supplier_id"), 
													exchange_rate, 
													(String)map.get("official_currency"), 
													(String)map.get("local_currency"),
													profit_rate,
													tax);
			
			returnList.add(conf);
			
        }
		
		return returnList;
	}
	
	public static SupplierConfig getConfig(int supplierId) {

		confCount++;
		
		if(confCount == 100000)
		{
			config = null;
			supplierConfig = null;
		}
		
		if(config == null)
		{
			String strSql = "select key,value from pm_config";
	
			List<Map<String, Object>> proMapList = OrderManagerModel.queryForList(strSql);
			
			List<Map<String, String>> result = new ArrayList<Map<String, String>>();
			for (Map map : proMapList) {
				result.add(map);
	        }
			
	        List<Map<String, String>> params = result;
	
	        config = new Config(params);
	        
	        confCount = 0;
		}
		
		if(supplierConfig == null)
		{
			supplierConfig = GetPmProductConfig();
		}
		
		ProductConfig pro = findProductConfig(supplierId);
		SupplierConfig conf = null;
       
		if(pro != null)
		{
			conf = new SupplierConfig();
			conf.setId(pro.getId());
			conf.setSupplierId(pro.getSupplier_id());
			
			conf.setExchangeRate(pro.getExchange_rate());
			
			conf.setOfficialCurrency(pro.getOfficial_currency());
			conf.setLocalCurrency(pro.getLocal_currency());
			
			conf.setProfitRate(pro.getProfit_rate());
			
			conf.setTax(pro.getTax());
		}
	

        SupplierConfig supplierConfig = conf;
        if (supplierConfig == null) {
            supplierConfig = new SupplierConfig();
        }

        supplierConfig.setDefaultConfig(config);
        return supplierConfig;

    }
	
	private static ProductConfig findProductConfig(int SupplierId)
	{
		ProductConfig returnPro = null;
		
		for(ProductConfig pro : supplierConfig)
		{
			if(pro.getSupplier_id() == SupplierId)
			{
				returnPro = pro;
				break;
			}
		}
		
		return returnPro;
	}
}
