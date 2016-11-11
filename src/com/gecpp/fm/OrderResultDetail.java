package com.gecpp.fm;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gecpp.p.product.domain.Mfs;
import com.gecpp.p.product.domain.Product;
import com.gecpp.p.product.domain.Supplier;

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

    /**
     * 页面展示的产品
     * TODO fm需实现该属性值
     * 形如：{ {pn:{mfs:[]}}, {pn:{mfs:[]}} }
     */
    private LinkedHashMap<String, Map<String, List<Product>>> productList;
    
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

}
