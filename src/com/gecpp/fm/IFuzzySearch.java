package com.gecpp.fm;

import java.util.List;

public interface IFuzzySearch {
	
	/**
	 * DeleteFuzzyRecord�A�R����ID������
	 * @param pid
	 * @return 1 success, 0 fail
	 */
	public int DeleteFuzzyRecord(int pid);
	
	/**
	 * InsertFuzzyRecord�A�s�W�Χ�s�ǤJID������
	 * @param pid
	 * @param pn
	 * @param mfs
	 * @param catalog
	 * @param description
	 * @param param
	 * @return 1 success, 0 fail
	 * �p�GFuzzyDB���w���ӵ�pid��ơA�h�|��s
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
	 * �ª�zzySearch�d�߬d��
	 * @param fuzzyString
	 * @return
	 */
	public List<String> QueryFuzzyRecord(String fuzzyString);
	
	
	/**
	 * QueryFuzzyRecordByListPage�A�O�̷�om�ƧǹL�A�B�i�H�^�Ǥ����L�᪺�Ƹ���T
	 */
	public OrderResult QueryFuzzyRecordByListPage(String fuzzyString, int currentPage, int pageSize);
	
	/** 
	 * GetMaxIndexID�A�i�H�d�ߥثe���ޫإ̫᪺߳ID
	 * @return  �̫᪺ID
	 */
	public int GetMaxIndexID();
	
	/** GetIndexIDStatus�A�i�H�d�߸ӯ���ID�إߪ����A(�w��OR����) 1 ���w�� , 0 ������
	 * 
	 * @param pid
	 * @return  1 ���w�� , 0 ������
	 */
	public int GetIndexIDStatus(int pid);
	
	
	
}


