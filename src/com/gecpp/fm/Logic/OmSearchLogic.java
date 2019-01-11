package com.gecpp.fm.Logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.gecpp.fm.MultipleParam;
import com.gecpp.fm.Dao.IndexPrice;
import com.gecpp.fm.Dao.IndexRate;
import com.gecpp.fm.Dao.IndexResult;
import com.gecpp.fm.Dao.MfsAlternateDao;
import com.gecpp.fm.Dao.Product;
import com.gecpp.fm.Dao.ProductConfig;
import com.gecpp.fm.Util.CommonUtil;
import com.gecpp.fm.Util.DbHelper;
import com.gecpp.fm.Util.DbHelper.Site;
import com.gecpp.fm.model.OrderManagerModel;
import com.gecpp.p.product.domain.Catalog;
import com.gecpp.p.product.domain.Config;
import com.gecpp.p.product.domain.Store;
import com.gecpp.p.product.domain.Supplier;
import com.gecpp.p.product.domain.SupplierConfig;
import com.gecpp.pm.product.util.PriceHelper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import dev.xwolf.framework.common.json.JSONUtils;
import dev.xwolf.framework.common.util.StringUtils;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class OmSearchLogic {
	
	private static List<String> cachedSupplier = null;
	private static long cacheTime = System.currentTimeMillis();
	
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
	
	// 20170428 for 
	private static String sSql_head = "SELECT a.id as s_id,a.region,a.region_code, "
            + "a.product_id,a.inventory, "
            + "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
            + "a.pn as model,a.supplier_pn  "
            + "as supplier_model,a.created_time,  "
            + "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs ,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
            + "b.catalog,  b.description,  "
            + "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
            + "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
            + "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error  "
            + "FROM ";
    private static String sSql_body = " b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
            + "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
            + "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
            + "where b.pn in(";

    private static String sSql_tail = ") and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL) order by b.pn, c.type, d.name, a.created_time desc  limit 2000 ";
    
	
	// 20161029 for 分表
	private static final String getAllInfoByPn_Multi = "select inventory, offical_price, id, pn, supplier_pn, mfs, supplier_id, lead, rohs, mfs_id, pkg, name "
			+ "from "
			+ "( "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name , c.TYPE "
			+ "			FROM pm_product b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ "			where b.pn in(*******) and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union  "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name , c.TYPE "
			+ "			FROM pm_supplier_product_c1s  b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ "			where b.pn in(*******) and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ ") result "
			+ "order by pn, TYPE ";
	
	private static final String getAllInfoById_Multi = "select inventory, offical_price, id, pn, supplier_pn, mfs, supplier_id, lead, rohs, mfs_id, pkg, name "
			+ "from "
			+ "( "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name , c.TYPE "
			+ "			FROM pm_product b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ "			where b.id in(*******) and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union  "
			+ "SELECT 0 as inventory, '' as offical_price, b.id, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs, b.supplier_id, b.lead, b.rohs, b.mfs_id, b.pkg, c.name , c.TYPE "
			+ "			FROM pm_supplier_product_c1s  b  "
			+ "			LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id)  "
			+ "			LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c  "
			+ "			where b.id in(*******) and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
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
	
	
	
	private static final String getAllInfoByPn_foot = ") and b.supplier_id = c.id AND b.status is null and  (c.status in ('1', '2') OR c.status  IS NULL) "; //order by b.pn, c.TYPE";
	
	// 20160526 use pm_store_price_select
	/*
	private static final String getAllPrice_head = "SELECT product_id, inventory, offical_price FROM pm_store_price where id in (SELECT max(id) FROM pm_store_price where product_id IN (";
	
	private static final String getAllPrice_foot = ") GROUP BY product_id)";
	*/
	
	private static final String getAllPrice_head = "SELECT product_id, inventory, offical_price FROM pm_store_price_select where product_id in (";
	
	private static final String getAllPrice_foot = ") ";
	
	private static final String getAllInfoDetailById_head_Multi = "select s_id, region, region_code, product_id, inventory, offical_price, price, delivery_place, delivery_circle, moq, store_attach, currency, model, supplier_model, created_time, id, uuid, "
			+ "pn, supplier_pn, mfs, mfs_id,  supplier_id, shop_id, pkg, catalog, description, param, status, lead, embgo, rohs, config_service, grab_url, doc_url, pic_url, update_attribute, updated_time,  "
			+ "is_complete, name, local_name, prority, abbreviation, mfs_base, advertise, site_error, catalog_id, tax, type, cooperation, supplier_type, supplier_status "
			+ "from "
			+ "( "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs ,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_product b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.id in(*******) and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL) "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_c1s b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.id in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_c1s' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_octopart b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.id in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_octopart' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_findchips b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.id in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_findchips' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_463 b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.id in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_463' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_findic b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.id in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_findic' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_ickey b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.id in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_ickey' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ ") result "
			+ "order by pn, prority, mfs_base, created_time desc, type  limit 2000 ";
	
	
	private static final String getAllInfoDetailByPn_head_Multi = "select s_id, region, region_code, product_id, inventory, offical_price, price, delivery_place, delivery_circle, moq, store_attach, currency, model, supplier_model, created_time, id, uuid, "
			+ "pn, supplier_pn, mfs, mfs_id,  supplier_id, shop_id, pkg, catalog, description, param, status, lead, embgo, rohs, config_service, grab_url, doc_url, pic_url, update_attribute, updated_time,  "
			+ "is_complete, name, local_name, prority, abbreviation, mfs_base, advertise, site_error, catalog_id, tax, type, cooperation, supplier_type, supplier_status "
			+ "from "
			+ "( "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_product b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL) "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_c1s b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_c1s' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_octopart b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_octopart' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_findchips b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_findchips' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_463 b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_463' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_findic b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_findic' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_ickey b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_ickey' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ ") result "
			+ "order by pn, prority, mfs_base, created_time desc, type  limit 2000 ";
	
	private static final String getAllInfoDetailBySupplier_head_Multi = "select s_id, region, region_code, product_id, inventory, offical_price, price, delivery_place, delivery_circle, moq, store_attach, currency, model, supplier_model, created_time, id, uuid, "
			+ "pn, supplier_pn, mfs, mfs_id,  supplier_id, shop_id, pkg, catalog, description, param, status, lead, embgo, rohs, config_service, grab_url, doc_url, pic_url, update_attribute, updated_time,  "
			+ "is_complete, name, local_name, prority, abbreviation, mfs_base, advertise, site_error, catalog_id, tax, type, cooperation, supplier_type, supplier_status "
			+ "from "
			+ "( "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_product b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.supplier_id in(*******) and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL) limit 2000 "
			+ ") result "
			+ "order by pn, prority, mfs_base, created_time desc, type  limit 2000 ";
	
	private static final String getAllInfoDetailByPn_head_Multi_Mfs_Supplier = "select s_id, region, region_code, product_id, inventory, offical_price, price, delivery_place, delivery_circle, moq, store_attach, currency, model, supplier_model, created_time, id, uuid, "
			+ "pn, supplier_pn, mfs, mfs_id,  supplier_id, shop_id, pkg, catalog, description, param, status, lead, embgo, rohs, config_service, grab_url, doc_url, pic_url, update_attribute, updated_time,  "
			+ "is_complete, name, local_name, prority, abbreviation, mfs_base, advertise, site_error, catalog_id, tax, type, cooperation, supplier_type, supplier_status "
			+ "from "
			+ "( "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_product b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_c1s b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_c1s' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_octopart b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_octopart' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_findchips b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_findchips' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_463 b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_463' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_findic b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_findic' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ "union "
			+ "SELECT a.id as s_id,a.region,a.region_code, "
			+ "a.product_id,a.inventory, "
			+ "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency, "
			+ "a.pn as model,a.supplier_pn  "
			+ "as supplier_model,a.created_time,  "
			+ "b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg, "
			+ "b.catalog,  b.description,  "
			+ "b.param, b.status, b.lead, b.embgo, b.rohs, b.config_service,  "
			+ "b.grab_url, b.doc_url, b.pic_url,  b.update_attribute, b.updated_time, b.is_complete, "
			+ "c.name,c.local_name , c.type as prority, c.abbreviation ,d.name as mfs_base, c.advertise, c.site_error, b.catalog_id, e.tax, c.type, c.cooperation, c.supplier_type, coalesce(c.status, '1')  supplier_status  "
			+ "FROM pm_supplier_product_ickey b  LEFT JOIN pm_store_price_select a on a.product_id = b.id  "
			+ "LEFT JOIN pm_product_config e on(e.supplier_id=b.supplier_id) "
			+ "LEFT JOIN pm_mfs_standard d on (b.mfs_id = d.id),  pm_supplier c   "
			+ "where b.pn in(*******) and b.supplier_id = c.id and b.supplier_id in (select supplier_id from pm_supplier_table_relation where table_name = 'pm_supplier_product_ickey' ) AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL)  "
			+ ") result where 1=1 ";
			
	
	
	private static final String getAllInfoDetailById_head = "SELECT a.id as s_id,a.region,a.region_code,"
            + "a.product_id,a.inventory,"
            + "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency,"
            + " a.pn as model,a.supplier_pn "
            + "as supplier_model,a.created_time, "
            + "  b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg,"
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
            + "a.offical_price,a.price,e.delivery_place,e.delivery_circle,a.moq,a.store_attach, case when trim(a.currency) <> '' then a.currency else e.official_currency end as currency,"
            + " a.pn as model,a.supplier_pn "
            + "as supplier_model,a.created_time, "
            + "  b.id, b.uuid, b.pn, b.supplier_pn, CASE WHEN trim(d.NAME) <> '' THEN d.NAME ELSE b.mfs END as mfs,b.mfs_id,  b.supplier_id,b.shop_id, b.pkg,"
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

    private static final String getAllInfoDetailByPn_foot = ") and b.supplier_id = c.id AND b.status is null and (c.status in ('1', '2') OR c.status  IS NULL) order by b.pn, prority, mfs_base, a.created_time desc limit 2000";

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
                
                if(i>20)
                	break;
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 2);
        } else {
            pnSql = "";
        }

        return pnSql;
	}
	
private static void refreshCacheSupplier() {
		
		long nowDate = System.currentTimeMillis();
		
		// 12個小時更換一次cache
		if(nowDate - cacheTime > 12 * 60 * 60 * 1000)
		{
			cachedSupplier = null;
		}
		
		if(cachedSupplier == null)
		{
			cachedSupplier = new ArrayList<String>();
			cacheTime = nowDate;
			
			String strSql = "select upper(abbreviation) from pm_supplier";
		
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
						cachedSupplier.add(rs.getString(1));
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
		}
		

	}
	
	public static String getFormatPnPage(List<IndexResult> notRepeatPns, int page, int pagesize)
	{
		String pnSql = "";
		
		if(notRepeatPns == null)
			return pnSql;
		
		List<IndexResult> newPageId = new ArrayList<IndexResult>();

        int from = Math.max(0,page*pagesize);
        int to = Math.min(notRepeatPns.size(),(page+1)*pagesize);

     // set area by weight
    	for(int i=(page - 1) * pagesize; i < page * pagesize; i++)
        {
        	if(i<notRepeatPns.size())
        	{
        		newPageId.add(notRepeatPns.get(i));
        	}
        }
    	
        notRepeatPns = newPageId;
        
        int pnsCount = notRepeatPns.size();

        if (notRepeatPns != null && pnsCount > 0) {
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("'");
            for (int i = 0; i < pnsCount; i++) {
                String s = notRepeatPns.get(i).getPn().replace("'","''");
                stringBuilder.append(s).append("','");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 2);
        } else {
            pnSql = "";
        }

        return pnSql;
	}
	
	private static List<String> maxSplit(String formula)
	{
		String bestFit = "";
		List<String> retResult = new ArrayList<String>();
		
		//insert "1" in atom-atom boundry 
		//formula = formula.replaceAll("(?<=[A-Z])(?=[A-Z])|(?<=[a-z])(?=[A-Z])|(?<=\\D)$", "1");

		//split at letter-digit or digit-letter boundry
		String regex = "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)";
		String[] atoms = formula.split(regex);
		
		for(String atom : atoms)
		{
			bestFit += atom;
			List<String> result = getFuzzyPn(bestFit, 10);
			if(result.size() == 0)
				break;
			else
				retResult = result;
		}
		
		return retResult;
	}
	
	private static String removeLastChar(String s) {
	    return (s == null || s.length() == 0)
	      ? ""
	      : (s.substring(0, s.length() - 1));
	}
	
	private static List<String> backwardSplit(String formula)
	{
		String bestFit = removeLastChar(formula);
		List<String> retResult = new ArrayList<String>();


		while(!"".equalsIgnoreCase(bestFit))
		{
			List<String> result = getFuzzyPn(bestFit, 10);
			if(result.size() != 0)
			{
				retResult = result;
				break;
			}
			else
				bestFit = removeLastChar(bestFit);
		}
		
		return retResult;
	}
	
	public static List<String> getFuzzyPns(String strData)
	{
		List<String> result = backwardSplit(strData);
		
		if(result.size() == 0)
			result= getFuzzyPn(strData.substring(0, 1), 100);
				
		List<ExtractedResult> pns =  FuzzySearch.extractTop(strData, result, 10);
		
		List<String> retPn = new ArrayList<String>();
		
		for(ExtractedResult res : pns)
			retPn.add(res.getString());
		
		return retPn;
	}
	
	public static String getFormatPnPageMfs(List<IndexRate> notRepeatPns, int page, int pagesize)
	{
		String pnSql = "";
		
		if(notRepeatPns == null)
			return pnSql;
		
		List<IndexRate> newPageId = new ArrayList<IndexRate>();

        int from = Math.max(0,page*pagesize);
        int to = Math.min(notRepeatPns.size(),(page+1)*pagesize);

     // set area by weight
    	for(int i=(page - 1) * pagesize; i < page * pagesize; i++)
        {
        	if(i<notRepeatPns.size())
        	{
        		newPageId.add(notRepeatPns.get(i));
        	}
        }
    	
        notRepeatPns = newPageId;
        
        int pnsCount = notRepeatPns.size();

        if (notRepeatPns != null && pnsCount > 0) {
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("'");
            for (int i = 0; i < pnsCount; i++) {
                String s = notRepeatPns.get(i).getPn().replace("'","''");
                stringBuilder.append(s).append("','");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 2);
        } else {
            pnSql = "";
        }

        return pnSql;
	}
	
	
	public static String getFormatPnPageV1(List<String> notRepeatPns, int page, int pagesize)
	{
		String pnSql = "";
		
		if(notRepeatPns == null)
			return pnSql;
		
		List<String> newPageId = new ArrayList<String>();

        int from = Math.max(0,page*pagesize);
        int to = Math.min(notRepeatPns.size(),(page+1)*pagesize);

     // set area by weight
    	for(int i=(page - 1) * pagesize; i < page * pagesize; i++)
        {
        	if(i<notRepeatPns.size())
        	{
        		newPageId.add(notRepeatPns.get(i));
        	}
        }
    	
        notRepeatPns = newPageId;
        
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
	
	private static String getArrayIndexSql(String [] pns)
	{
		String pnSql = "";
		
        if (pns != null && pns.length > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            
            stringBuilder.append("SELECT ARRAY (");

            for (String pn : pns) {
                stringBuilder.append("SELECT UNNEST(page) from ezindex_kind where word = '" + pn.replace("'","''") + "' INTERSECT ");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 11);
            
            pnSql += " limit 2000) ";
            
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
	
	
	private static List<Map<String, Object>> fetchFromTable(String table, String pn) {
        String sql = sSql_head + table + sSql_body + pn + sSql_tail;
        
        List<Map<String, Object>> sList = new ArrayList<Map<String, Object>>();
		Vector<String> columnNames = new Vector<String>();

		try {

			Connection conn = null;
			Statement stmt = null;
			ResultSet rs = null;

			try {

					conn = DbHelper.connectPm();
			
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				
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
	
	public static List<Map<String, Object>> fetchFromAllTables(final String strPn) throws InterruptedException, ExecutionException {
        // Create a ListeningExecutorService (Guava) by wrapping a
        // normal ExecutorService (Java)
		
		DbHelper db = new DbHelper();
		
        ListeningExecutorService executor =
                MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

        List<ListenableFuture<List<Map<String, Object>>>> list =
                new ArrayList<ListenableFuture<List<Map<String, Object>>>>();
        // For each table, create an independent thread that will
        // query just that table and return a set of user IDs from it
        List<String> tables = new ArrayList<String>();
        
        tables.addAll(db.getList("SELECT distinct table_name from pm_supplier_table_relation", Site.pm));
        
        if(!tables.contains("pm_product"))
        	tables.add("pm_product");
        
        for (final String table : tables) {

            ListenableFuture<List<Map<String, Object>>> future = executor.submit(new Callable<List<Map<String, Object>>>() {
                public List<Map<String, Object>> call() throws Exception {
                    return fetchFromTable(table, strPn);
                }
            });
            // Add the future to the list
            list.add(future);
        }
        // We want to know when ALL the threads have completed,
        // so we use a Guava function to turn a list of ListenableFutures
        // into a single ListenableFuture
        ListenableFuture<List<List<Map<String, Object>>>> combinedFutures = Futures.allAsList(list);

        // The get on the combined ListenableFuture will now block until
        // ALL the individual threads have completed work.
        List<List<Map<String, Object>>> tableSets = combinedFutures.get();

        // Now all we have to do is combine the individual sets into a
        // single result
        List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
        for (List<Map<String, Object>> tableSet: tableSets) {
            userList.addAll(tableSet);
        }

        return userList;
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
						{
							// 20170927 FIX [] PRICE
							if("[]".equalsIgnoreCase(s.replaceAll(" ", "")))
								s = "";
							price = s;
						}
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
	
	
	public static String getPriceByProductListDetailCount(List<com.gecpp.p.product.domain.Product> unPriceProductList)
	{

		if(unPriceProductList == null)
			return "0,0,0,0";
		
		if(unPriceProductList.size() == 0)
			return "0,0,0,0";
		

		
		int nhasStock = 0;
		int nnoStock = 0;
		int nhasPrice = 0;
		int nhasInquery = 0;
		

		for(com.gecpp.p.product.domain.Product pro : unPriceProductList)
		{

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
				
				
				if(!price.equalsIgnoreCase(""))
					nhasPrice++;
				
				if(stock == 0)
					nnoStock++;
				
				if(stock != 0)
					nhasStock++;
				
				if(price.equalsIgnoreCase("") && stock > 0 )
					nhasInquery++;
		}
		
		return nhasStock + "," + nnoStock + "," + nhasPrice + "," + nhasInquery;
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
	
	public static List<Integer> getArrayIndex(String [] keywords)
	{
		List<Integer> retList = new ArrayList<Integer>();
		
		String strSql = getArrayIndexSql(keywords);
		
		retList = OrderManagerModel.getArrayIndex(strSql);
		
		return retList;
	}
	
	public static int pageCountDetail(List<com.gecpp.p.product.domain.Product> plist)
	{
    	
    	String product_id = "";
   
    	int gPage = -1;
    	for(com.gecpp.p.product.domain.Product product : plist)
    	{
    		if(product.getPn() == null)
    			continue;
    		
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
		// 20180621 展現下架供應商排序要置後
		List<com.gecpp.p.product.domain.Product> upperList = new ArrayList<com.gecpp.p.product.domain.Product> ();
		List<com.gecpp.p.product.domain.Product> lowerList = new ArrayList<com.gecpp.p.product.domain.Product> ();
		
		for(com.gecpp.p.product.domain.Product product : plist)
		{
			if(product.getSupplier().getStatus().equalsIgnoreCase("2"))
				lowerList.add(product);
			else
				upperList.add(product);
		}
		
		List<com.gecpp.p.product.domain.Product> pageList = new ArrayList<com.gecpp.p.product.domain.Product> ();
    	Map<Integer, List<com.gecpp.p.product.domain.Product>> uniqPn = new HashMap<Integer, List<com.gecpp.p.product.domain.Product>>();
    	
    	String product_id = "";
   
    	int gPage = -1;
    	for(com.gecpp.p.product.domain.Product product : upperList)
    	{
    		if(product.getPn() == null)
    			continue;
    		
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
    	
    	product_id = "";
    	for(com.gecpp.p.product.domain.Product product : lowerList)
    	{
    		if(product.getPn() == null)
    			continue;
    		
    		if(!product_id.equalsIgnoreCase(product.getPn()  + " "))
    		{
    			gPage++;
    			
    			List<com.gecpp.p.product.domain.Product> id_product = new ArrayList<com.gecpp.p.product.domain.Product>();
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    			
    			product_id = product.getPn() + " ";
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
    		String pn = "";
    		String mfs = "";
    		try
    		{
    			pn = product.getPn().trim();
    		}
    		catch(Exception e){}
    		
    		try
    		{
    			mfs = product.getMfs().trim();
    		}
    		catch(Exception e){}
    		
    		if(!product_id.equalsIgnoreCase(pn + mfs))
    		{
    			gPage++;
    			
    			List<Product> id_product = new ArrayList<Product>();
    			id_product.add(product);
    			
    			uniqPn.put(gPage, id_product);
    			
    			product_id = pn + mfs;
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
	/*	
		if(!idsSql.equalsIgnoreCase(""))
        {
			// 2017/04/28 for 分表
			List<Map<String, Object>> proMapList = null;
			try {
				proMapList = fetchFromAllTables(idsSql);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
            returnList = formatToProductList(proMapList);
        } 
        
        */

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
	
	protected static void WriteToCache(final long cacheid, final List<Map<String, Object>> plist)
    {
		//20170919 build cache system 

		Thread thread = new Thread(){
		    public void run(){

		        try {
		        	FileOutputStream fileOut = new FileOutputStream("d:\\temp\\" + cacheid + ".json");
					ObjectOutputStream out = new ObjectOutputStream(fileOut);
					out.writeObject(plist);
					out.close();
					fileOut.close();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		  };

		  thread.start();
		
    }
	
	protected static List<Map<String, Object>> ReadFromCache(long cacheid)
    {
		//20170919 build cache system 
		List<Map<String, Object>> plist = null;
    	String strPath = "d:\\temp\\" + cacheid + ".json";

    	if(CommonUtil.IsFileExist(strPath))
    	{
	        try {
	        	FileInputStream fis = new FileInputStream(strPath);
	            ObjectInputStream ois = new ObjectInputStream(fis);
	            try {
					plist = (List<Map<String, Object>>) ois.readObject();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            ois.close();
	            fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally
	        {
				
	        }
    	}
		
		return plist;
    }
	
	public static List<com.gecpp.p.product.domain.Product> findProcurementByPns02(String idsSql) {
		
		List<com.gecpp.p.product.domain.Product> returnList = new ArrayList<com.gecpp.p.product.domain.Product>();
		
		idsSql = idsSql.trim().replaceAll("'", "").toUpperCase();
		
		idsSql = "'" + idsSql + "'";
	
        if(!idsSql.equalsIgnoreCase(""))
        {
        	List<Map<String, Object>> proMapList = null;

    		// 20161029 for 分表
        	String selectProductListById = getAllInfoDetailByPn_head_Multi.replace("*******", idsSql);
        	
            // String selectProductListById = getAllInfoDetailByPn_head + idsSql + getAllInfoDetailByPn_foot;
            proMapList = OrderManagerModel.queryForList(selectProductListById);
            
            returnList = formatToProductList(proMapList);
        } 
		return returnList;
	}
	
	public static List<com.gecpp.p.product.domain.Product> findProductsByPnsV2(String idsSql) {
		// TODO Auto-generated method stub
		List<com.gecpp.p.product.domain.Product> returnList = new ArrayList<com.gecpp.p.product.domain.Product>();
	/*	
		if(!idsSql.equalsIgnoreCase(""))
        {
			// 2017/04/28 for 分表
			List<Map<String, Object>> proMapList = null;
			try {
				proMapList = fetchFromAllTables(idsSql);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
            returnList = formatToProductList(proMapList);
        } 
        
        */

        if(!idsSql.equalsIgnoreCase(""))
        {
        	List<Map<String, Object>> proMapList = null;
        	//20170919 build cache system 
    		long cacheid = KeywordLogic.getMd5(idsSql);
    		/*
    		//20170919 build cache system 
    		List<Map<String, Object>> plist = ReadFromCache(cacheid);
    		if(plist == null)
    		{
	        	// 20161029 for 分表
	        	String selectProductListById = getAllInfoDetailByPn_head_Multi.replace("*******", idsSql);
	        	
	        	selectProductListById = selectProductListById.replace("(c.status in ('1', '2') OR c.status  IS NULL)", " 1=1 ");
	            // String selectProductListById = getAllInfoDetailByPn_head + idsSql + getAllInfoDetailByPn_foot;
	            proMapList = OrderManagerModel.queryForList(selectProductListById);
    		}
    		else
    			proMapList = plist;
    		
    		if(plist == null)
    			WriteToCache(cacheid, proMapList);
    			*/
    		
    		
    		
    		// 20161029 for 分表
        	String selectProductListById = getAllInfoDetailByPn_head_Multi.replace("*******", idsSql);
        	
        	selectProductListById = selectProductListById.replace("(c.status in ('1', '2') OR c.status  IS NULL)", " 1=1 ");
            // String selectProductListById = getAllInfoDetailByPn_head + idsSql + getAllInfoDetailByPn_foot;
            proMapList = OrderManagerModel.queryForList(selectProductListById);
            
            
            
            
            returnList = formatToProductList(proMapList);
        } 

		
		return returnList;
	}
	
	public static List<com.gecpp.p.product.domain.Product> findProductsByPnsMfsSupplier(String idsSql, List<Integer> mfs, List<Integer> supplier) {
		// TODO Auto-generated method stub
		List<com.gecpp.p.product.domain.Product> returnList = new ArrayList<com.gecpp.p.product.domain.Product>();
	/*	
		if(!idsSql.equalsIgnoreCase(""))
        {
			// 2017/04/28 for 分表
			List<Map<String, Object>> proMapList = null;
			try {
				proMapList = fetchFromAllTables(idsSql);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
            returnList = formatToProductList(proMapList);
        } 
        
        */

        if(!idsSql.equalsIgnoreCase(""))
        {
        	// 20161029 for 分表
        	String selectProductListById = getAllInfoDetailByPn_head_Multi_Mfs_Supplier.replace("*******", idsSql);
            // String selectProductListById = getAllInfoDetailByPn_head + idsSql + getAllInfoDetailByPn_foot;
        	if(mfs != null)
        		if(mfs.size() > 0)
        			selectProductListById += " and mfs_id in (" + getFormatedId(mfs) + ") ";
        	if(supplier != null)	
	        	if(supplier.size() > 0)
	        		selectProductListById += " and mfs_id in (" + getFormatedId(supplier) + ") ";
        	
        	selectProductListById += " order by pn, prority, mfs_base, created_time desc, type  limit 2000 ";
            List<Map<String, Object>> proMapList = OrderManagerModel.queryForList(selectProductListById);
            returnList = formatToProductList(proMapList);
        } 

		
		return returnList;
	}
	
	public static List<com.gecpp.p.product.domain.Product> findProductsByPnsMfsSupplierV2(String idsSql, List<Integer> mfs, List<Integer> supplier) {
		// TODO Auto-generated method stub
		List<com.gecpp.p.product.domain.Product> returnList = new ArrayList<com.gecpp.p.product.domain.Product>();
	/*	
		if(!idsSql.equalsIgnoreCase(""))
        {
			// 2017/04/28 for 分表
			List<Map<String, Object>> proMapList = null;
			try {
				proMapList = fetchFromAllTables(idsSql);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
            returnList = formatToProductList(proMapList);
        } 
        
        */

        if(!idsSql.equalsIgnoreCase(""))
        {
        	// 20161029 for 分表
        	String selectProductListById = getAllInfoDetailByPn_head_Multi_Mfs_Supplier.replace("*******", idsSql);
            // String selectProductListById = getAllInfoDetailByPn_head + idsSql + getAllInfoDetailByPn_foot;
        	if(mfs != null)
        		if(mfs.size() > 0)
        			selectProductListById += " and mfs_id in (" + getFormatedId(mfs) + ") ";
        	if(supplier != null)	
	        	if(supplier.size() > 0)
	        		selectProductListById += " and mfs_id in (" + getFormatedId(supplier) + ") ";
        	
        	selectProductListById += " order by pn, prority, mfs_base, created_time desc, type  limit 2000 ";
        	
        	selectProductListById = selectProductListById.replace("(c.status in ('1', '2') OR c.status  IS NULL)", " 1=1 ");
        	
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
	
	public static List<com.gecpp.p.product.domain.Product> findProductsByIdsV2(String idsSql) {
		// TODO Auto-generated method stub
		List<com.gecpp.p.product.domain.Product> returnList = new ArrayList<com.gecpp.p.product.domain.Product>();
		
		if(!idsSql.equalsIgnoreCase(""))
        {
			// 20161029 for 分表
			String selectProductListById = getAllInfoDetailById_head_Multi.replace("*******", idsSql);
			
			selectProductListById = selectProductListById.replace("(c.status in ('1', '2') OR c.status  IS NULL)", " 1=1 ");
			
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
            
            // 20170615
            product.setCatalogId((Integer) map.get("catalog_id"));
            
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
            supplier.setType((Integer) map.get("type"));
            
            
            supplier.setStatus((String) map.get("supplier_status"));
            
            // pseudo using these fields
            supplier.setCode((Integer) map.get("cooperation"));
            supplier.setPriority((Integer) map.get("supplier_type"));
            // got the correct properties
            supplier.setCooperation((Integer) map.get("cooperation"));
            supplier.setSupplierType((Integer) map.get("supplier_type"));
     
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
            	
            	//20170615
            	int tax = 0;
            	try
            	{
            		NumberFormat formatter = new DecimalFormat("###.###########");  

            		String f = formatter.format(map.get("tax"));
            		
            		tax = Float.parseFloat(f) > 0.1 ? 1 : 0;
            	}
            	catch(Exception e)
            	{}
            	
            	store.setTax(tax);
            	
                store.setId(storeId);
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
	
	private static String GetSudoPrice(List officalPriceList) {
		// TODO Auto-generated method stub
		String PriceList = "";
		for (int i = 0; i < officalPriceList.size(); i++) {
            Map rangePriceMap = (Map) officalPriceList.get(i);
            Object amountObj = rangePriceMap.get("amount");
            if (amountObj != null) {
                Integer amount = StringUtils.parseInt(amountObj
                        .toString());
                
                if(amount == 10)
                {
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
                    
                    
                    PriceList = "p10:" + price;
                }
                	
            }
            
            
		}
		
			
		return PriceList;
	}
	
	private static List AddSudoPrice(List officalPriceList) {
		// TODO Auto-generated method stub
		boolean bHaveTen = false;
		
		for (int i = 0; i < officalPriceList.size(); i++) {
            Map rangePriceMap = (Map) officalPriceList.get(i);
            Object amountObj = rangePriceMap.get("amount");
            if (amountObj != null) {
                Integer amount = StringUtils.parseInt(amountObj
                        .toString());
                
                Integer nearest = ((amount+9)/10)*10;
                
                if(amount == 10)
                	bHaveTen = true;
                
                if(amount != 10 && nearest == 10)
                {
                	Map priceRangePrice = new LinkedHashMap();
                    priceRangePrice.put("amount", 10);
                    
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
                    
                    priceRangePrice.put("price", price);
                    officalPriceList.add(priceRangePrice);
                    
                    bHaveTen = true;
                }
                	
            }
            
            
		}
		
		if(bHaveTen == false)
		{
			Map priceRangePrice = new LinkedHashMap();
			priceRangePrice.put("amount", 10);
			priceRangePrice.put("price", 0.0f);
            officalPriceList.add(priceRangePrice);
		}
			
			
		return officalPriceList;
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
	                	
	                	// 20170623
	                	//officalPriceList = AddSudoPrice(officalPriceList);
	                	//String plist = GetSudoPrice(officalPriceList);

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
        	try {
        		exchangeRate = config.getExchangeRate(officialCurrency, distCurrency);
        	}
        	catch(Exception e) {}
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
		
		if(confCount == 10000)
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
	
	public static List<com.gecpp.p.product.domain.Product> excludeProductList(List<com.gecpp.p.product.domain.Product> big, List<com.gecpp.p.product.domain.Product> small)
	{
		List<com.gecpp.p.product.domain.Product> returnList = new ArrayList<com.gecpp.p.product.domain.Product>();
		for(com.gecpp.p.product.domain.Product pro : big)
		{
			if(!small.contains(pro))
				returnList.add(pro);
		}
		
		return returnList;
	}
	
	public static List<com.gecpp.p.product.domain.Product> getSupplierListCount(
			List<com.gecpp.p.product.domain.Product> plist, int isLogin, int isPaid) {

		// 國內供應商
		List<com.gecpp.p.product.domain.Product> midProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		// 國外非合作供應商
		Map<String, com.gecpp.p.product.domain.Product> noneProductList = new HashMap<String, com.gecpp.p.product.domain.Product>();
		// 國內非合作供應商
		Map<String, com.gecpp.p.product.domain.Product> domainProductList = new HashMap<String, com.gecpp.p.product.domain.Product>();
		
		// 非合作供應商對照表
		List<String> cooperation_ProductList = new ArrayList<String>();
		
		for (com.gecpp.p.product.domain.Product pro : plist) {
			
            Supplier sup = pro.getSupplier();
            int cooperation = 0;
            int supplier_type = 0;
            int status = 0;
            
            String pn_mfs = pro.getPn() + pro.getMfs();
            
            boolean bAddToNewList = false;
            
            try
            {
            	cooperation = sup.getCode();
            }
            catch(Exception e)
            {}
            
            try
            {
            	supplier_type = sup.getPriority();
            }
            catch(Exception e)
            {}
            
            try
            {
            	String strStatus = sup.getStatus();
				if(strStatus != null)
					status = Integer.parseInt(strStatus);
            }
            catch(Exception e)
            {}
   
            // 未登录状态(isLogin = 0)
            if(isLogin == 0)
			{
				if(cooperation == 1 && (status == 1 || status == 2))
				{
					bAddToNewList = true;
					
					if(!cooperation_ProductList.contains(pn_mfs))
						cooperation_ProductList.add(pn_mfs);
				}
				
				// 預存一筆非合作廠商
				if(cooperation != 1 && (status == 1 || status == 2))
				{
					if(supplier_type == 2)	// 國外
					{
						if(!noneProductList.containsKey(pn_mfs))
							noneProductList.put(pn_mfs, pro);
					}
					
					if(supplier_type == 1)	// 國內
					{
						if(!domainProductList.containsKey(pn_mfs))
							domainProductList.put(pn_mfs, pro);
					}
				}
			}

			// 已登录未付费(isLogin =1, isPaid = 0)
			if(isLogin == 1 && isPaid == 0)
			{
				if((cooperation == 1 || cooperation == 2) && supplier_type == 2 && (status == 1 || status == 2))
					bAddToNewList = true;
				
				if((cooperation == 1 || cooperation == 2) && supplier_type == 1 && (status == 1 || status == 2))
				{
					midProductList.add(pro);
				}
			}
			
        }
		
		
		// 未合作供應商
		for(String pn_mfs : cooperation_ProductList)
		{
			noneProductList.remove(pn_mfs);
			domainProductList.remove(pn_mfs);
		}
		
		// 排除有國外供應商者
		for (Map.Entry<String, com.gecpp.p.product.domain.Product> entry : noneProductList.entrySet())
		{
			domainProductList.remove(entry.getKey());
		}
		
		// 未合作供應商加入(國外)
		for (Map.Entry<String, com.gecpp.p.product.domain.Product> entry : noneProductList.entrySet())
		{
			com.gecpp.p.product.domain.Product pro = entry.getValue();
			midProductList.add(pro);
		}
		
		// 無國外供應商者加入中間價
		List<com.gecpp.p.product.domain.Product> domainMiddleProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		for (Map.Entry<String, com.gecpp.p.product.domain.Product> entry : domainProductList.entrySet())
		{
			com.gecpp.p.product.domain.Product pro = entry.getValue();
			domainMiddleProductList.add(pro);
		}
		
		
		midProductList.addAll(domainMiddleProductList);
		
		
		return midProductList;
	}
	
	public static List<com.gecpp.p.product.domain.Product> getOrgSupplierListDetail(
			List<com.gecpp.p.product.domain.Product> plist, int isLogin) {

		List<com.gecpp.p.product.domain.Product> newProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		
		for (com.gecpp.p.product.domain.Product pro : plist) {
			
            Supplier sup = pro.getSupplier();
            int cooperation = 0;
            int supplier_type = 0;
            int status = 0;
            
            String pn_mfs = pro.getPn() + pro.getMfs();
            
            boolean bAddToNewList = false;
            
            try
            {
            	cooperation = sup.getCode();
            }
            catch(Exception e)
            {}
            
            try
            {
            	supplier_type = sup.getPriority();
            }
            catch(Exception e)
            {}
            
            try
            {
            	String strStatus = sup.getStatus();
				if(strStatus != null)
					status = Integer.parseInt(strStatus);
            }
            catch(Exception e)
            {}
   

			if((cooperation == 1 || cooperation == 2) && (supplier_type == 1 || supplier_type == 2) && (status == 1 || status == 2))
				bAddToNewList = true;
			
			
			// 管理员登录状态(isLogin=2)
			if(isLogin != 2 && status == 0)
				bAddToNewList = false;
		
			
			if(bAddToNewList)
			{
				newProductList.add(pro);
			}
        }
		
		
		return newProductList;
	}
	
	public static List<com.gecpp.p.product.domain.Product> getSupplierListDetail(
			List<com.gecpp.p.product.domain.Product> plist, int isLogin, int isPaid) {

		List<com.gecpp.p.product.domain.Product> newProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		// 國內供應商
		List<com.gecpp.p.product.domain.Product> midProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		// 國外非合作供應商
		Map<String, com.gecpp.p.product.domain.Product> noneProductList = new HashMap<String, com.gecpp.p.product.domain.Product>();
		// 國內非合作供應商
		Map<String, com.gecpp.p.product.domain.Product> domainProductList = new HashMap<String, com.gecpp.p.product.domain.Product>();
		
		// 20180531  如果合作供应商<5个，则展现(5-合作供应商数量）个非合作供应商
		// 如果当前产品没有合作供应商则展现最多不超过5条非合作供应商数据
		// 5筆非合作供應商
		Map<String, List<com.gecpp.p.product.domain.Product>> FiveProductList = new HashMap<String, List<com.gecpp.p.product.domain.Product>>();
		// 合作供應商
		Map<String, List<com.gecpp.p.product.domain.Product>> CorpProductList = new HashMap<String, List<com.gecpp.p.product.domain.Product>>();
		
		// 非合作供應商對照表
		List<String> cooperation_ProductList = new ArrayList<String>();
		
		for (com.gecpp.p.product.domain.Product pro : plist) {
			
            Supplier sup = pro.getSupplier();
            int cooperation = 0;
            int supplier_type = 0;
            int status = 0;
            
            String pn_mfs = pro.getPn() + pro.getMfs();
            
            boolean bAddToNewList = false;
            
            try
            {
            	cooperation = sup.getCode();
            }
            catch(Exception e)
            {}
            
            try
            {
            	supplier_type = sup.getPriority();
            }
            catch(Exception e)
            {}
            
            try
            {
            	String strStatus = sup.getStatus();
				if(strStatus != null)
					status = Integer.parseInt(strStatus);
            }
            catch(Exception e)
            {}
   
            // 未登录状态(isLogin = 0)
            if(isLogin == 0)
			{
				if(cooperation == 1 && (status == 1 || status == 2))
				{
					bAddToNewList = true;
					
					if(!cooperation_ProductList.contains(pn_mfs))
						cooperation_ProductList.add(pn_mfs);
				}
				
				// 預存一筆非合作廠商
				// 20180531  如果合作供应商<5个，则展现(5-合作供应商数量）个非合作供应商
				// 如果当前产品没有合作供应商则展现最多不超过5条非合作供应商数据
				if(cooperation != 1 && (status == 1 || status == 2))
				{
					if(supplier_type == 2)	// 國外
					{
						if(!noneProductList.containsKey(pn_mfs))
							noneProductList.put(pn_mfs, pro);
					}
					
					if(supplier_type == 1)	// 國內
					{
						if(!domainProductList.containsKey(pn_mfs))
							domainProductList.put(pn_mfs, pro);
					}
					
					// 先把非合作供應商資料存起來
					// 20180709  增加一个条件限制，非合作供应商需要限制为国际供应商（supplier_type=2）
					if(supplier_type == 2)
					{
						if(FiveProductList.containsKey(pn_mfs))
						{
							List<com.gecpp.p.product.domain.Product> value = FiveProductList.get(pn_mfs);
							value.add(pro);
						}
						else
						{
							List<com.gecpp.p.product.domain.Product> value = new ArrayList<com.gecpp.p.product.domain.Product>();
							value.add(pro);
							FiveProductList.put(pn_mfs, value);
						}
					}
				}
				else
				{
					// 把合作供應商資料存起來
					if(CorpProductList.containsKey(pn_mfs))
					{
						List<com.gecpp.p.product.domain.Product> value = CorpProductList.get(pn_mfs);
						value.add(pro);
					}
					else
					{
						List<com.gecpp.p.product.domain.Product> value = new ArrayList<com.gecpp.p.product.domain.Product>();
						value.add(pro);
						CorpProductList.put(pn_mfs, value);
					}
				}
			}

			// 已登录未付费(isLogin =1, isPaid = 0)
			if(isLogin == 1 && isPaid == 0)
			{
				if((cooperation == 1 || cooperation == 2) && supplier_type == 2 && (status == 1 || status == 2))
					bAddToNewList = true;
				
				if((cooperation == 1 || cooperation == 2) && supplier_type == 1 && (status == 1 || status == 2))
				{
					//中間價
					sup.setCode(0);
					sup.setPriority(0);
					pro.setSupplier(sup);
					midProductList.add(pro);
				}
			}
			
			// 已登录已付费(isLogin =1, isPaid = 1)
			if(isLogin == 1 && isPaid == 1)
			{
				if((cooperation == 1 || cooperation == 2) && (supplier_type == 1 || supplier_type == 2) && (status == 1 || status == 2))
					bAddToNewList = true;
			}
			
			// 管理员登录状态(isLogin=2)
			if(isLogin == 2)
				bAddToNewList = true;
			
			
			if(bAddToNewList)
			{
				// 清除之前借用的狀態
				sup.setCode(0);
				sup.setPriority(0);
				pro.setSupplier(sup);
				newProductList.add(pro);
			}
        }
		
		// 未登录状态(isLogin = 0)
		// 20180531  如果合作供应商<5个，则展现(5-合作供应商数量）个非合作供应商
		// 如果当前产品没有合作供应商则展现最多不超过5条非合作供应商数据
		if(isLogin == 0)
		{
			// 先整理並計算合作共應商的狀態
			Map<String, List<com.gecpp.p.product.domain.Product>> PreFinalProductList = new HashMap<String, List<com.gecpp.p.product.domain.Product>>();
			
			for(com.gecpp.p.product.domain.Product pro : newProductList)
			{
				String pn_mfs = pro.getPn() + pro.getMfs();
				
				// 把合作供應商資料存起來
				if(PreFinalProductList.containsKey(pn_mfs))
				{
					List<com.gecpp.p.product.domain.Product> value = PreFinalProductList.get(pn_mfs);
					value.add(pro);
				}
				else
				{
					List<com.gecpp.p.product.domain.Product> value = new ArrayList<com.gecpp.p.product.domain.Product>();
					value.add(pro);
					PreFinalProductList.put(pn_mfs, value);
				}
			}
			
			for (Map.Entry<String, List<com.gecpp.p.product.domain.Product>> entry : PreFinalProductList.entrySet())
			{
				List<com.gecpp.p.product.domain.Product> prolist = entry.getValue();
				
				// 20180531  如果合作供应商<5个，则展现(5-合作供应商数量）个非合作供应商
				if(prolist.size() < 5)
				{
					if(FiveProductList.containsKey(entry.getKey()))
					{
						List<com.gecpp.p.product.domain.Product> no_corp_pro = FiveProductList.get(entry.getKey());
						for(com.gecpp.p.product.domain.Product p : no_corp_pro)
						{
							if(prolist.size() < 6)
							{
								// 清除之前借用的狀態
								Supplier sup = p.getSupplier();
								sup.setCode(0);
								sup.setPriority(0);
								p.setSupplier(sup);
								prolist.add(p);
							}
						}
						
					}
				}
			}
			
			List<com.gecpp.p.product.domain.Product> newvalue = new ArrayList<com.gecpp.p.product.domain.Product>();
			for (Map.Entry<String, List<com.gecpp.p.product.domain.Product>> entry : PreFinalProductList.entrySet())
			{
				List<com.gecpp.p.product.domain.Product> prolist = entry.getValue();
				newvalue.addAll(prolist);
			}
			
			newProductList = newvalue;
			
		}
		
		// 整理中間價
		midProductList = GetMidPriceProduct(midProductList);
		
		newProductList.addAll(midProductList);
		
		// 未合作供應商
		for(String pn_mfs : cooperation_ProductList)
		{
			noneProductList.remove(pn_mfs);
			domainProductList.remove(pn_mfs);
		}
		
		// 排除有國外供應商者
		for (Map.Entry<String, com.gecpp.p.product.domain.Product> entry : noneProductList.entrySet())
		{
			domainProductList.remove(entry.getKey());
		}
		
		// 未合作供應商加入(國外)
		for (Map.Entry<String, com.gecpp.p.product.domain.Product> entry : noneProductList.entrySet())
		{
			com.gecpp.p.product.domain.Product pro = entry.getValue();
			newProductList.add(pro);
		}
		
		// 無國外供應商者加入中間價
		List<com.gecpp.p.product.domain.Product> domainMiddleProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		for (Map.Entry<String, com.gecpp.p.product.domain.Product> entry : domainProductList.entrySet())
		{
			com.gecpp.p.product.domain.Product pro = entry.getValue();
			domainMiddleProductList.add(pro);
		}
		
		domainMiddleProductList = GetMidPriceProduct(domainMiddleProductList);
		
		newProductList.addAll(domainMiddleProductList);
		
		
		return newProductList;
	}
	
	public static List<com.gecpp.p.product.domain.Product> GetMidPriceProduct(
			List<com.gecpp.p.product.domain.Product> plist)
	{
		List<com.gecpp.p.product.domain.Product> midProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		
		Map<String, List<com.gecpp.p.product.domain.Product>> uniqPn = new HashMap<String, List<com.gecpp.p.product.domain.Product>>();
    	
    	String product_id = "";
		
		for(com.gecpp.p.product.domain.Product product : plist)
    	{
    		String pn = "";
    		String mfs = "";
    		try
    		{
    			pn = product.getPn().trim();
    		}
    		catch(Exception e){}
    		
    		try
    		{
    			mfs = product.getMfs().trim();
    		}
    		catch(Exception e){}
    		
    		if(!product_id.equalsIgnoreCase(pn + mfs))
    		{
    			product_id = pn + mfs;
    			
    			List<com.gecpp.p.product.domain.Product> id_product = new ArrayList<com.gecpp.p.product.domain.Product>();
    			id_product.add(product);
    			
    			uniqPn.put(product_id, id_product);

    		}
    		else
    		{
    			List<com.gecpp.p.product.domain.Product> id_product = uniqPn.get(product_id);
    			
    			id_product.add(product);
    			
    			uniqPn.put(product_id, id_product);
    		}
    	}
		
		for (Map.Entry<String, List<com.gecpp.p.product.domain.Product>> entry : uniqPn.entrySet())
    	{
			List<com.gecpp.p.product.domain.Product> id_product = entry.getValue();
			
			List<Float> thousand_price = new ArrayList<Float>();
			List<Float> ten_thousand_price = new ArrayList<Float>();
			
			for(com.gecpp.p.product.domain.Product pro : id_product)
			{
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
			                	
			                	// 20170623
			                	//officalPriceList = AddSudoPrice(officalPriceList);
			                	//String plist = GetSudoPrice(officalPriceList);

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
			              
			                        }
			                    }


			                }
			            }

			            // 算standStepPrice
			            Map standStepPrice = PriceHelper.getStandStepPrice(priceList);
			            String thousand = (String) standStepPrice.get("p1000");
			            String ten_thousand = (String) standStepPrice.get("p10000");
			            float fThousand = 0.0f;
			            float fTenThousand = 0.0f;
			            
			            try{
			            	fThousand = Float.parseFloat(thousand);
			            }catch(Exception e){}
			            
			            try{
			            	fTenThousand = Float.parseFloat(ten_thousand);
			            }catch(Exception e){}
			            
			            thousand_price.add(fThousand);
			            ten_thousand_price.add(fTenThousand);

			        }
			    }
			}
        	
			int nTake = ChooseMid(thousand_price, ten_thousand_price);
			
			if(nTake == -1)
				nTake = 0;
			
			com.gecpp.p.product.domain.Product pro = id_product.get(nTake);
			
			com.gecpp.p.product.domain.Supplier sup = pro.getSupplier();
			sup.setId(0);
			sup.setName("国内供应商");
			sup.setAbbreviation("国内供应商");
			sup.setLocalName("国内供应商");
			
			pro.setSupplier(sup);
			
			midProductList.add(pro);
    	}
		
		return midProductList;

	}

	private static int ChooseMid(List<Float> thousand_price,
			List<Float> ten_thousand_price) {
		// TODO Auto-generated method stub
		
		int level = 0;
		
		double [] thousand = new double[thousand_price.size()];
		int i=0;
		for(float p : thousand_price)
		{
			thousand[i++] = p;
		}
		
		double[] ten_thousand = new double[ten_thousand_price.size()];
		i=0;
		for(float p : ten_thousand_price)
		{
			ten_thousand[i++] = p;
		}
		
		float thousand_value = (float) CommonUtil.getMedian(thousand);
		float ten_thousand_value = (float)  CommonUtil.getMedian(ten_thousand);
		
		if(thousand_value == 0.0 && ten_thousand_value != 0.0)
			level = ten_thousand_price.indexOf(ten_thousand_value);
		else
			level = thousand_price.indexOf(thousand_value);
		
		return level;
	}

	public static List<com.gecpp.p.product.domain.Product> getPriceByProductListDetail(
			List<com.gecpp.p.product.domain.Product> plist, int amount,
			List<String> currencies, List<Integer> catalog_ids) {

		List<com.gecpp.p.product.domain.Product> newProductList = new ArrayList<com.gecpp.p.product.domain.Product>();
		
		for (com.gecpp.p.product.domain.Product pro : plist) {
			
            int catalog_id = 0;
            String currency = "";
            
            boolean bAddToNewList = false;
            
            try
            {
            	catalog_id = pro.getCatalogId();
            }
            catch(Exception e)
            {}
            
            try
            {
            	currency = pro.getStoreList().get(0).getCurrency();
            }
            catch(Exception e)
            {}
   
            // catalog
            boolean bFound = false;
            if(catalog_ids != null)
            {
	            if(catalog_ids.size() == 0)
	        		bFound = true;
	        	for(Integer id:catalog_ids)
	        	{
	        		if(catalog_id == id)
	        		{
	        			bFound = true;
	        			break;
	        		}
	        	}
            }
            else
            	bFound = true;
        	
        	if(bFound == true)
        		bAddToNewList = true;
            
            // currency
        	if(currencies != null)
        	{
	        	if(currencies.size() == 0)
	        		bFound = true;
	        	else
	        		bFound = false;
	        	
	        	for(String cur:currencies)
	        	{
	        		if(cur.equalsIgnoreCase(currency))
	        		{
	        			bFound = true;
	        			break;
	        		}
	        	}
        	}
        	
        	if(bFound == true)
        		bAddToNewList = true;
            
            
            if(bAddToNewList == true)
            	newProductList.add(pro);
        }
		
		
		return newProductList;
	}
	
	public static int GetMiddleCatalogById(int childId)
	{
		int parentid = 0;
		
		String strSql = "select parent_id from pm_standar_catalog where id in(" + childId + ")";

		List<Integer> sList = new ArrayList<>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectPm();

			try {
				stmt = conn.prepareStatement(strSql);

				rs = stmt.executeQuery();
				while (rs.next())
					sList.add(rs.getInt(1));
	
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
		
		if(sList.size()>0)
			parentid = sList.get(0);
		
		return parentid;
	}
	
	public static int GetParentCatalogById(int childId)
	{
		int parentid = 0;
		
		String strSql = "select qegoo_catagory_id, qegoo_catagory.qegoo_catagory from pm_standar_catalog inner join qegoo_catagory on pm_standar_catalog.parent_id = qegoo_catagory.id and pm_standar_catalog.id in(" + childId + ")";

		List<Integer> sList = new ArrayList<>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectPm();

			try {
				stmt = conn.prepareStatement(strSql);

				rs = stmt.executeQuery();
				while (rs.next())
					sList.add(rs.getInt(1));
	
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
		

		if(sList.size()>0)
			parentid = sList.get(0);
		else // is in qegoo_catagory?
		{
	
		
			strSql = "select qegoo_catagory_id from qegoo_catagory where id = " + childId + "  ";
	
			sList = new ArrayList<>();
			
			conn = null;
			stmt = null;
			rs = null;
	
			try {
	
				conn = DbHelper.connectPm();
	
				try {
					stmt = conn.prepareStatement(strSql);
	
					rs = stmt.executeQuery();
					while (rs.next())
						sList.add(rs.getInt(1));
		
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
		}
		
		return parentid;
	}
	
	
	public static List<String> Catalog(List<Integer> catalog_ids) {
		// TODO Auto-generated method stub
		// 回傳值

        // catalog的結果
        List<String> redisResult = new ArrayList<String>();

   
    	try
    	{
    		redisResult = getCatalogSearchId(catalog_ids);
    	}
    	catch(Exception e)
    	{
    		List<String> sErr = new ArrayList<String>();
    		sErr.add(e.getMessage());
    		
    	}
    	
    	if(redisResult.size() == 0)
    	{
    		redisResult = getCatalogSearch(catalog_ids);
    	}
   
 
        
        return redisResult;
	}
	
	public static String getFormatedId(List<Integer> pns)
	{
		String pnSql = "";
		
		if(pns == null)
			return pnSql;

        int pnsCount = pns.size();

        if (pns != null && pnsCount > 0) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < pnsCount; i++) {
            	Integer s = pns.get(i);
                stringBuilder.append(s).append(",");
            }
            pnSql = stringBuilder.substring(0, stringBuilder.length() - 1);
        } else {
            pnSql = "";
        }

        return pnSql;
	}
	
	
	public static Map<Integer, Integer> getFakeCatalogById(List<Integer> catalog)
	{
		List<Integer> realCatalogIds = getRealCatalogId(catalog);
		
		if(realCatalogIds.size() == 0)
			realCatalogIds.addAll(catalog);
        
        Map<Integer, Integer> catalogIds = getFakeCatalogListByParentId(realCatalogIds, 500);
    
        return catalogIds;
	}
	
	private static List<String> getCatalogSearchId(List<Integer> catalog)
	{
		List<Integer> realCatalogIds = getRealCatalogId(catalog);
		
		if(realCatalogIds.size() == 0)
			realCatalogIds.addAll(catalog);
        
        List<Integer> catalogIds = getCatalogListByParentId(realCatalogIds, 20);
    
		List<String> resultPn = new ArrayList<>();
		
		String ids = getFormatedId(catalogIds);
		
		if("".equals(ids))
			ids = getFormatedId(catalog);
		
		String strSql = "select pn from ez_catalog_count where id in(" + ids + ") order by count desc limit 500";
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectFm();

			
					stmt = conn.createStatement();
					rs = stmt.executeQuery(strSql);
					while (rs.next())
					{
						resultPn.add(rs.getString(1));
					}
	

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {

			DbHelper.attemptClose(rs);
			DbHelper.attemptClose(stmt);
			DbHelper.attemptClose(conn);
		}

        return resultPn;
	}
	
	
	public static List<Integer> retSupplier(String keyword)
	{
		refreshCacheSupplier();
		
		List<Integer> realSupplierIds = new ArrayList<Integer>();
		
		if("".equals(keyword.trim()))
				return realSupplierIds;
		
		if(!cachedSupplier.contains(keyword.toUpperCase()))
			return realSupplierIds;
		
		String strSql = "select id from fm_product where supplier = '" + keyword.toUpperCase() + "' order by id desc limit 500";
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			conn = DbHelper.connectFm();

			stmt = conn.createStatement();
			rs = stmt.executeQuery(strSql);
			while (rs.next())
			{
				realSupplierIds.add(rs.getInt(1));
			}
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {

			DbHelper.attemptClose(rs);
			DbHelper.attemptClose(stmt);
			DbHelper.attemptClose(conn);
		}

        return realSupplierIds;
	}
	
	
	private static List<String> getCatalogSearch(List<Integer> catalog)
	{
		
    	
		List<String> resultPn = new ArrayList<>();
		
		String ids = "";
		
		ids = getFormatedId(catalog);
		
		String strSql = "select distinct pn from pm_product where catalog_id in(" + ids + ") limit 200";
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectPm();

			try {
					stmt = conn.createStatement();
					rs = stmt.executeQuery(strSql);
					while (rs.next())
					{
						resultPn.add(rs.getString(1));
					}
	
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

        return resultPn;
	}
	
	private static List<Integer> getRealCatalogId(List<Integer> catalog) {
		
		String strSql = "select id from qegoo_catagory where qegoo_catagory_id in(" + getFormatedId(catalog) + ")";

		List<Integer> sList = new ArrayList<>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectPm();

			try {
				stmt = conn.prepareStatement(strSql);

				rs = stmt.executeQuery();
				while (rs.next())
					sList.add(rs.getInt(1));
	
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
	
	private static List<Integer> getParentId(List<Integer>catalog)
	{
		String strSql = "select distinct parent_id from pm_standar_catalog where parent_id in(" + getFormatedId(catalog) + ") and  is_valid='1'";
		List<Integer> sList = new ArrayList<>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectPm();

			try {
				stmt = conn.prepareStatement(strSql);

				rs = stmt.executeQuery();
				while (rs.next())
					sList.add(rs.getInt(1));
	
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
	
	private static List<Integer> getCatalogListByParentId(List<Integer> catalog, int limit) {
        //保证result<=limit.
		List<Integer> thirdId = new ArrayList<>(catalog);
		
		thirdId.removeAll(getParentId(catalog));
		
        List<Integer> result = new ArrayList<>();

        //获取所有的二级分类
        List<Integer> secondCatalogList = getCatalog(getParentId(catalog));
        

        //遍历二级分类，至多拿到7个二级分类即可。
        for (int i = 0; i < secondCatalogList.size(); i++) {
        	Integer secondCatalog =  secondCatalogList.get(i);

            //拿到三级分类
            List<Integer> thirdCatalogList = getCatalog(secondCatalog);
            //遍历三级分类,保证取到的三级分类个数不超过limit
            if (thirdCatalogList.size()==0){
                result.add(secondCatalog);
            }else {
                for (int j = 0;j< thirdCatalogList.size() && j < limit;j++){
                	Integer thirdCatalog = thirdCatalogList.get(j);
                    result.add(thirdCatalog);
                    if (result.size() < limit){
                        continue;
                    }else {
                        break;
                    }
                }
            }
            if (result.size() < limit){
                continue;
            }else {
                break;
            }
        }
        
        // sort result
        result.addAll(thirdId);
        
        result = sortCatalog(result, 10);
        
        return result;
    }
	
	
	private static Map<Integer, Integer> getFakeCatalogListByParentId(List<Integer> catalog, int limit) {
        //保证result<=limit.
		List<Integer> thirdId = new ArrayList<>(catalog);
		
		thirdId.removeAll(getParentId(catalog));
		
        List<Integer> result = new ArrayList<>();

        //获取所有的二级分类
        List<Integer> secondCatalogList = getCatalog(getParentId(catalog));
        

        //遍历二级分类，至多拿到7个二级分类即可。
        for (int i = 0; i < secondCatalogList.size(); i++) {
        	Integer secondCatalog =  secondCatalogList.get(i);

        	result.add(secondCatalog);
        	/*
            //拿到三级分类
            List<Integer> thirdCatalogList = getCatalog(secondCatalog);
            //遍历三级分类,保证取到的三级分类个数不超过limit
            if (thirdCatalogList.size()==0){
                result.add(secondCatalog);
            }else {
                for (int j = 0;j< thirdCatalogList.size() && j < limit;j++){
                	Integer thirdCatalog = thirdCatalogList.get(j);
                    result.add(thirdCatalog);
                    if (result.size() < limit){
                        continue;
                    }else {
                        break;
                    }
                }
            }
            */
            if (result.size() < limit){
                continue;
            }else {
                break;
            }
        }
        
        // sort result
        result.addAll(thirdId);
        
        Map<Integer, Integer> ret = fakeCatalog(result, limit);
        
        return ret;
    }
	
	private static Map<Integer, Integer> fakeCatalog(List<Integer> catalogId, int limit) {
		
		String strSql = "select id, cnt from ez_catalog_simple_count where id in (" + getFormatedId(catalogId) + ") order by cnt desc limit " + limit;

		Map<Integer, Integer> sList = new HashMap<Integer, Integer>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectFm();

			
				stmt = conn.prepareStatement(strSql);

				rs = stmt.executeQuery();
				while (rs.next())
					sList.put(rs.getInt(1), rs.getInt(2));
	
		

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
	



private static List<String> getFuzzyPn(String alphabat, int limit) {
		
		String strSql = "select pn from auto_cache_pn where pn like ? limit ? ";

		List<String> sList = new ArrayList<>();
		
		// 避免非正常料號的查詢
		//if(alphabat.length() > 16)
			return sList;
		/*
		Connection conn = null;
		PreparedStatement  pstmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectFm();

				
				pstmt = conn.prepareStatement(strSql);
				pstmt.setString(1, alphabat + '%');
				pstmt.setInt(2, limit);
				
				rs = pstmt.executeQuery();

				while (rs.next())
					sList.add(rs.getString(1));
	

		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {

			DbHelper.attemptClose(rs);
			DbHelper.attemptClose(pstmt);
			DbHelper.attemptClose(conn);
		}
		

		return sList;
		*/
    }
	
private static List<Integer> sortCatalog(List<Integer> catalogId, int limit) {
		
		String strSql = "select id from ez_catalog_simple_count where id in (" + getFormatedId(catalogId) + ") order by cnt desc limit " + limit;

		List<Integer> sList = new ArrayList<>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectFm();

				stmt = conn.prepareStatement(strSql);

				rs = stmt.executeQuery();
				while (rs.next())
					sList.add(rs.getInt(1));
	

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
	
	private static List<Integer> getCatalog(int parentId) {
		
		String strSql = "select id from pm_standar_catalog where parent_id=? and  is_valid='1'";

		List<Integer> sList = new ArrayList<>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectPm();

			try {
				stmt = conn.prepareStatement(strSql);
				stmt.setInt(1, parentId);
				rs = stmt.executeQuery();
				while (rs.next())
					sList.add(rs.getInt(1));
	
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

	
	private static List<Integer> getCatalog(List<Integer> catalog) {
		
		List<Integer> sList = new ArrayList<>();
		
		// 20170907 避免空catalog
		if(catalog.size() == 0)
		{
			return sList;
		}
			
		String strSql = "select id from pm_standar_catalog where parent_id in(" + getFormatedId(catalog) + ") and  is_valid='1'";
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			conn = DbHelper.connectPm();

			try {
				stmt = conn.prepareStatement(strSql);

				rs = stmt.executeQuery();
				while (rs.next())
					sList.add(rs.getInt(1));
	
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

	public static List<com.gecpp.p.product.domain.Product> findProductsBySupplier(String idsSql) {
		// TODO Auto-generated method stub
		List<com.gecpp.p.product.domain.Product> returnList = new ArrayList<com.gecpp.p.product.domain.Product>();
		/*	
			if(!idsSql.equalsIgnoreCase(""))
	        {
				// 2017/04/28 for 分表
				List<Map<String, Object>> proMapList = null;
				try {
					proMapList = fetchFromAllTables(idsSql);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
	            returnList = formatToProductList(proMapList);
	        } 
	        
	        */

	        if(!idsSql.equalsIgnoreCase(""))
	        {
	        	List<Map<String, Object>> proMapList = null;
	        	//20170919 build cache system 
	    		long cacheid = KeywordLogic.getMd5(idsSql);
	    		/*
	    		//20170919 build cache system 
	    		List<Map<String, Object>> plist = ReadFromCache(cacheid);
	    		if(plist == null)
	    		{
		        	// 20161029 for 分表
		        	String selectProductListById = getAllInfoDetailByPn_head_Multi.replace("*******", idsSql);
		        	
		        	selectProductListById = selectProductListById.replace("(c.status in ('1', '2') OR c.status  IS NULL)", " 1=1 ");
		            // String selectProductListById = getAllInfoDetailByPn_head + idsSql + getAllInfoDetailByPn_foot;
		            proMapList = OrderManagerModel.queryForList(selectProductListById);
	    		}
	    		else
	    			proMapList = plist;
	    		
	    		if(plist == null)
	    			WriteToCache(cacheid, proMapList);
	    			*/
	    		
	    		
	    		
	    		// 20161029 for 分表
	        	String selectProductListById = getAllInfoDetailBySupplier_head_Multi.replace("*******", idsSql);
	        	
	        	selectProductListById = selectProductListById.replace("(c.status in ('1', '2') OR c.status  IS NULL)", " 1=1 ");
	            // String selectProductListById = getAllInfoDetailByPn_head + idsSql + getAllInfoDetailByPn_foot;
	            proMapList = OrderManagerModel.queryForList(selectProductListById);
	            
	            
	            
	            
	            returnList = formatToProductList(proMapList);
	        } 

			
			return returnList;
	}

}
