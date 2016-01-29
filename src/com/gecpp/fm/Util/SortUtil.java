package com.gecpp.fm.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import java.util.TreeMap;

import com.gecpp.fm.Dao.*;


public class SortUtil {
	
	public static boolean ASC = true;
    public static boolean DESC = false;

	
	
	private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order)
    {

        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Integer>>()
        {
            public int compare(Entry<String, Integer> o1,
                    Entry<String, Integer> o2)
            {
                if (order)
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else
                {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
	
	public static List<IndexResult> RegroupIndexResult(List<IndexResult> list1, List<IndexResult> list2)
	{
		// 料號與權重對照表
		HashMap<String, Integer> hashPnWeight = new HashMap<String, Integer>();
		// 涵蓋關鍵字次數表
		HashMap<String, Integer> hashIndexCount = new HashMap<String, Integer>();
		
		for(IndexResult tuple : list1)
		{
			hashPnWeight.put(tuple.getPn(), tuple.getWeight());
			hashIndexCount.put(tuple.getPn(), tuple.getCount());
		}
		
		for(IndexResult tuple : list2)
		{
			hashPnWeight.put(tuple.getPn(), tuple.getWeight());
			hashIndexCount.put(tuple.getPn(), tuple.getCount());
		}
		
		return SortIndexResult(hashIndexCount, hashPnWeight);
	}
	
	public static List<IndexResult> SortIndexResultSimple(HashMap<String, Integer> pnWeight, int nCount)
	{
    
        // 準備好依照次數以權重排序的map
        Map<String, Integer> groupMapByCount = sortByComparator(pnWeight, DESC);
        
        // 準備好要回傳的List
        List<IndexResult> retList = new ArrayList<IndexResult>();
    
        
		for(Map.Entry<String,Integer> group_entry : groupMapByCount.entrySet())
		{
			IndexResult tuple = new IndexResult();
			tuple.setCount(nCount);
			tuple.setPn(group_entry.getKey());
			tuple.setWeight(group_entry.getValue());
			
			retList.add(tuple);
		}
		
	
        
		return retList;
	}
    
    public static List<IndexResult> SortIndexResult(HashMap<String, Integer> pnCount, HashMap<String, Integer> pnWeight)
    {
    	int nCount = 0;
    	// 先依照次數排序
        Map<String,Integer> ord_map =  sortByComparator(pnCount, DESC);
        
        // 準備好依照次數以權重排序的map
        HashMap<String, Integer> groupMapByCount = new HashMap<String, Integer>();
        
        // 準備好要回傳的List
        List<IndexResult> retList = new ArrayList<IndexResult>();
    
        for(Map.Entry<String,Integer> entry : ord_map.entrySet()) {
        	if(entry.getValue() != nCount)
        	{
        		Map<String,Integer> ord_group = sortByComparator(groupMapByCount, DESC);
        		
        		for(Map.Entry<String,Integer> group_entry : ord_group.entrySet())
        		{
        			IndexResult tuple = new IndexResult();
        			tuple.setCount(nCount);
        			tuple.setPn(group_entry.getKey());
        			tuple.setWeight(group_entry.getValue());
        			
        			retList.add(tuple);
        		}
        		
        		nCount = entry.getValue();
        		groupMapByCount.clear();
        		groupMapByCount.put(entry.getKey(),  pnWeight.get(entry.getKey()));
        	}
        	else
        	{
        		groupMapByCount.put(entry.getKey(),  pnWeight.get(entry.getKey()));
        	}
        	
        }
        
        // 最後的一次
        Map<String,Integer> ord_group = sortByComparator(groupMapByCount, DESC);
		
		for(Map.Entry<String,Integer> group_entry : ord_group.entrySet())
		{
			IndexResult tuple = new IndexResult();
			tuple.setCount(nCount);
			tuple.setPn(group_entry.getKey());
			tuple.setWeight(group_entry.getValue());
			
			retList.add(tuple);
		}
	
        
		return retList;
    }
    
    
}
