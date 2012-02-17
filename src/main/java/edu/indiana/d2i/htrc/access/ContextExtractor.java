/*
#
# Copyright 2007 The Trustees of Indiana University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or areed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# -----------------------------------------------------------------
#
# Project: data-api
# File:  ContextExtractor.java
# Description:  
#
# -----------------------------------------------------------------
# 
*/



/**
 * 
 */
package edu.indiana.d2i.htrc.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;

/**
 * @author Yiming Sun
 *
 */
public class ContextExtractor {
    
    private static Logger log = Logger.getLogger(ContextExtractor.class);
    
    protected final Map<String, List<String>> contextMap; 
    
    public ContextExtractor(HttpServletRequest httpServletRequest, HttpHeaders httpHeaders) {
        contextMap = new HashMap<String, List<String>>();
        extractFromRequest(contextMap, httpServletRequest);
        extractFromHeaders(contextMap, httpHeaders);
    }
    
    public List<String> getContext(String key) {
        List<String> list = null;
        List<String> list2 = contextMap.get(key.toLowerCase());
        if (list2 != null) {
            list = Collections.unmodifiableList(list2);
        }
        return list;
    }
    
    protected void extractFromRequest(Map<String, List<String>> map, HttpServletRequest httpServletRequest) {
        String remoteAddr = httpServletRequest.getRemoteAddr();
        putIntoMap(map, "remoteaddr", remoteAddr);
        if (log.isDebugEnabled()) log.debug("remoteAddr: " + remoteAddr);
        
        String remoteHost = httpServletRequest.getRemoteHost();
        putIntoMap(map, "remotehost", remoteHost);
        if (log.isDebugEnabled()) log.debug("remoteHost: " + remoteHost);
        
        int remotePort = httpServletRequest.getRemotePort();
        putIntoMap(map, "remoteport", Integer.toString(remotePort));
        if (log.isDebugEnabled()) log.debug("remotePort: " + remotePort);
        
        String remoteUser = httpServletRequest.getRemoteUser();
        putIntoMap(map, "remoteuser", remoteUser);
        if (log.isDebugEnabled()) log.debug("remoteUser: " + remoteUser);
        
        String requestURI = httpServletRequest.getRequestURI();
        putIntoMap(map, "requesturi", requestURI);
        if (log.isDebugEnabled()) log.debug("requestURI: " + requestURI);
        
    }
    
    protected void putIntoMap(Map<String, List<String>> map, String name, String value) {
        if (name != null && value != null) {
            if (!"".equals(name) && !"".equals(value)) {
                List<String> list = map.get(name);
                if (list == null) {
                    list = new ArrayList<String>();
                    map.put(name, list);
                }
                list.add(value);
            }
        }
    }
    
    
    protected void extractFromHeaders(Map<String, List<String>> map, HttpHeaders httpHeaders) {
        MultivaluedMap<String, String> requestHeaders = httpHeaders.getRequestHeaders();
        Set<String> keySet = requestHeaders.keySet();
        for (String key : keySet) {
            List<String> list = requestHeaders.get(key.toLowerCase());
            if (list != null && !list.isEmpty()) {
                if (log.isDebugEnabled()) {
                    StringBuilder builder = new StringBuilder(key);
                    builder.append(":");
                    for (String string : list) {
                        builder.append(string).append(" ");
                    }
                    log.debug(builder.toString());
                }
                
                List<String> list2 = map.get(key);
                if (list2 == null) {
                    list2 = new ArrayList<String>(list);
                    map.put(key.toLowerCase(), list2);
                } else {
                    list2.addAll(list);
                }
            } else {
                if (log.isDebugEnabled()) log.debug(key + ": null | empty");
            }
        }
        
    }

}
