package com.gecpp.fm;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gecpp.p.product.domain.Mfs;
import com.gecpp.p.product.domain.Product;
import com.gecpp.p.product.domain.Supplier;
import com.gecpp.p.product.domain.Catalog;

public class OrderResultDetail implements Serializable{
	
	/*
	public int getTotalCount() {
		return TotalCount;
	}
	public void setTotalCount(int totalCount) {
		TotalCount = totalCount;
	}
	public String getHighLight() {
		return HighLight;
	}
	public void setHighLight(String highLight) {
		HighLight = highLight;
	}
	public LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> getPidList() {
		return PidList;
	}
	public void setPidList(LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> pidList) {
		PidList = pidList;
	}
	public List<Integer> getIds() {
		return ids;
	}
	public void setIds(List<Integer> ids) {
		this.ids = ids;
	}
	public String[] getPns() {
		return pns;
	}
	public void setPns(String[] pns) {
		this.pns = pns;
	}

	public String [] getPkg() {
		return pkg;
	}

	public void setPkg(String [] pkg) {
		this.pkg = pkg;
	}

	public String [] getSupplier() {
		return supplier;
	}

	public void setSupplier(String [] supplier) {
		this.supplier = supplier;
	}


	private int TotalCount;			// 全部資料量
	
	private String HighLight;		// 關鍵字，以逗號區分(TI, OP, LM358)
	
	private LinkedHashMap<String, LinkedHashMap<String,List<Integer>>>  PidList;	// 結構  (料號, Map<供應商, List<PID>>)
	
	private List<Integer> ids;		// 当前页所有产品的id列表(List<PID>)
	
	private String [] pns;			// 当前页所有产品的pn列表(String[])
	
	private String [] pkg;			// 当前页所有产品的pkg列表(String[])
	
	private String [] supplier;			// 当前页所有产品的pkg列表(String[])
	
	*/
	
    /**
     * 匹配料号
     * TODO fm需实现该属性值
     */
    private String[] pns;
    
    private Map<String, Double> pns_similarity;

    /**
     * 页面展示的产品
     * TODO fm需实现该属性值
     * 形如：{ {pn:{mfs:[]}}, {pn:{mfs:[]}} }
     */
    private LinkedHashMap<String, Map<String, List<Product>>> productList;
    
    private LinkedHashMap<String, Map<String, List<Integer>>> countList;
    
    private List<String> mfsList;

    /**
     * 总记录数
     * TODO fm需实现该属性值
     */
    private long totalCount;

    /**
     * 在深度搜索需要展示出来的标准制造商列表
     * TODO fm需实现该属性值
     */
    private List<Mfs> mfsStandard;

    /**
     * 在深度搜索需要展示出来的供应商列表（只包含打广告的供应商）
     * TODO fm需实现该属性值
     */
    private List<Supplier> suppliers;
    
    
    
    private Map<Mfs, Integer> mfsStandard_count;			// 製造商含count

    private Map<Supplier, Integer> suppliers_count;			// 供應商含count
    
    private List<String> currencies;						// 幣別
    
    private Map<String, Integer> status_count;		// (hasStock，noStock，hasPrice，hasInquery) 含count
    
    private Map<Catalog, Integer> catalogs_count;		// 分類ID 含count
    
    private Map<Catalog, Integer> middle_catalogs_count; // 中分類ID 含count
    
    private Map<Catalog, Integer> parent_catalogs_count; // 主分類ID 含count
    
    private LinkedHashMap<Catalog, Map<Catalog, Map<Catalog, Integer>>> catalogList;
    
    private Map<Mfs, List<String>>  mfsPnDescription;	// 2018/03/14 Map<製造商, List<PN>>  mfsPnDescription;
    
    private Map<String, List<ProductNews>> pn_news;		// 2018/08/03 Map<料號, List<新聞>> 
    
    private Map<String, List<ProductDesign>> pn_Design;	// 2018/08/03 Map<料號, List<應用>>
    
    private float rate;									// RATE
    

	/**
	 * @return the totalCount
	 */
	public long getTotalCount() {
		return totalCount;
	}

	/**
	 * @param totalCount the totalCount to set
	 */
	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}

	/**
	 * @return the suppliers
	 */
	public List<Supplier> getSuppliers() {
		return suppliers;
	}

	/**
	 * @param suppliers the suppliers to set
	 */
	public void setSuppliers(List<Supplier> suppliers) {
		this.suppliers = suppliers;
	}

	/**
	 * @return the mfsStandard
	 */
	public List<Mfs> getMfsStandard() {
		return mfsStandard;
	}

	/**
	 * @param mfsStandard the mfsStandard to set
	 */
	public void setMfsStandard(List<Mfs> mfsStandard) {
		this.mfsStandard = mfsStandard;
	}

	/**
	 * @return the mfsList
	 */
	public List<String> getMfsList() {
		return mfsList;
	}

	/**
	 * @param mfsList the mfsList to set
	 */
	public void setMfsList(List<String> mfsList) {
		this.mfsList = mfsList;
	}

	/**
	 * @return the pns
	 */
	public String[] getPns() {
		return pns;
	}

	/**
	 * @param pns the pns to set
	 */
	public void setPns(String[] pns) {
		this.pns = pns;
	}

	/**
	 * @return the productList
	 */
	public LinkedHashMap<String, Map<String, List<Product>>> getProductList() {
		return productList;
	}

	/**
	 * @param productList the productList to set
	 */
	public void setProductList(LinkedHashMap<String, Map<String, List<Product>>> productList) {
		this.productList = productList;
	}


	/**
	 * @return the currencies
	 */
	public List<String> getCurrencies() {
		return currencies;
	}

	/**
	 * @param currencies the currencies to set
	 */
	public void setCurrencies(List<String> currencies) {
		this.currencies = currencies;
	}

	/**
	 * @return the mfsStandard_count
	 */
	public Map<Mfs, Integer> getMfsStandard_count() {
		return mfsStandard_count;
	}

	/**
	 * @param mfsStandard_count the mfsStandard_count to set
	 */
	public void setMfsStandard_count(Map<Mfs, Integer> mfsStandard_count) {
		this.mfsStandard_count = mfsStandard_count;
	}

	/**
	 * @return the suppliers_count
	 */
	public Map<Supplier, Integer> getSuppliers_count() {
		return suppliers_count;
	}

	/**
	 * @param suppliers_count the suppliers_count to set
	 */
	public void setSuppliers_count(Map<Supplier, Integer> suppliers_count) {
		this.suppliers_count = suppliers_count;
	}

	/**
	 * @return the status_count
	 */
	public Map<String, Integer> getStatus_count() {
		return status_count;
	}

	/**
	 * @param status_count the status_count to set
	 */
	public void setStatus_count(Map<String, Integer> status_count) {
		this.status_count = status_count;
	}

	/**
	 * @return the catalogs_count
	 */
	public Map<Catalog, Integer> getCatalogs_count() {
		return catalogs_count;
	}

	/**
	 * @param catalogs_count the catalogs_count to set
	 */
	public void setCatalogs_count(Map<Catalog, Integer> catalogs_count) {
		this.catalogs_count = catalogs_count;
	}

	/**
	 * @return the rate
	 */
	public float getRate() {
		return rate;
	}

	/**
	 * @param rate the rate to set
	 */
	public void setRate(float rate) {
		this.rate = rate;
	}

	/**
	 * @return the parent_catalogs_count
	 */
	public Map<Catalog, Integer> getParent_catalogs_count() {
		return parent_catalogs_count;
	}

	/**
	 * @param parent_catalogs_count the parent_catalogs_count to set
	 */
	public void setParent_catalogs_count(Map<Catalog, Integer> parent_catalogs_count) {
		this.parent_catalogs_count = parent_catalogs_count;
	}

	/**
	 * @return the middle_catalogs_count
	 */
	public Map<Catalog, Integer> getMiddle_catalogs_count() {
		return middle_catalogs_count;
	}

	/**
	 * @param middle_catalogs_count the middle_catalogs_count to set
	 */
	public void setMiddle_catalogs_count(Map<Catalog, Integer> middle_catalogs_count) {
		this.middle_catalogs_count = middle_catalogs_count;
	}

	/**
	 * @return the catalogList
	 */
	public LinkedHashMap<Catalog, Map<Catalog, Map<Catalog, Integer>>> getCatalogList() {
		return catalogList;
	}

	/**
	 * @param catalogList the catalogList to set
	 */
	public void setCatalogList(LinkedHashMap<Catalog, Map<Catalog, Map<Catalog, Integer>>> catalogList) {
		this.catalogList = catalogList;
	}

	/**
	 * @return the countList
	 */
	public LinkedHashMap<String, Map<String, List<Integer>>> getCountList() {
		return countList;
	}

	/**
	 * @param countList the countList to set
	 */
	public void setCountList(LinkedHashMap<String, Map<String, List<Integer>>> countList) {
		this.countList = countList;
	}

	/**
	 * @return the mfsPnDescription
	 */
	public Map<Mfs, List<String>> getMfsPnDescription() {
		return mfsPnDescription;
	}

	/**
	 * @param mfsPnDescription the mfsPnDescription to set
	 */
	public void setMfsPnDescription(Map<Mfs, List<String>> mfsPnDescription) {
		this.mfsPnDescription = mfsPnDescription;
	}

	/**
	 * @return the pn_news
	 */
	public Map<String, List<ProductNews>> getPn_news() {
		return pn_news;
	}

	/**
	 * @param pn_news the pn_news to set
	 */
	public void setPn_news(Map<String, List<ProductNews>> pn_news) {
		this.pn_news = pn_news;
	}

	/**
	 * @return the pn_Design
	 */
	public Map<String, List<ProductDesign>> getPn_Design() {
		return pn_Design;
	}

	/**
	 * @param pn_Design the pn_Design to set
	 */
	public void setPn_Design(Map<String, List<ProductDesign>> pn_Design) {
		this.pn_Design = pn_Design;
	}

	/**
	 * @return the pns_similarity
	 */
	public Map<String, Double> getPns_similarity() {
		return pns_similarity;
	}

	/**
	 * @param pns_similarity the pns_similarity to set
	 */
	public void setPns_similarity(Map<String, Double> pns_similarity) {
		this.pns_similarity = pns_similarity;
	}


	
}
