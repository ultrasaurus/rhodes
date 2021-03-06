package com.rho.sync;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

class JSONArrayIterator
{
    JSONArray m_array;
    int    m_nCurItem;
	
	JSONArrayIterator(String szData)throws JSONException
	{
	    m_array = new JSONArray(szData);
	    m_nCurItem = 0;
	}

	boolean isEnd()
	{
	    return !(m_array != null && m_nCurItem < m_array.length());
	}

	void  next()
	{
	    m_nCurItem++;
	}

	JSONEntry getCurItem()throws JSONException
	{
	    return new JSONEntry( isEnd() ? null : (JSONObject) m_array.get(m_nCurItem) );
	}
}
