package com.gecpp.fm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderResult {
	
	
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
	
	public LinkedHashMap<String, Map<String, List<Integer>>> getPidList() {
		return PidList;
	}
	public void setPidList(LinkedHashMap<String, Map<String, List<Integer>>> pidList) {
		PidList = pidList;
	}
	
	

	private int TotalCount;			// �`��
	
	private String HighLight;		// ����r�A�H�r���Ϥ�
	
	private LinkedHashMap<String, Map<String,List<Integer>>>  PidList;	// ���c  (�Ƹ�, Map<������, List<PID>>)


}
