/*
#
# Copyright 2012 The Trustees of Indiana University
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
# File:  CombinePageVolumeZipMaker.java
# Description:  
#
# -----------------------------------------------------------------
# 
*/



/**
 * 
 */
package edu.indiana.d2i.htrc.access.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import edu.indiana.d2i.htrc.access.VolumeReader;
import edu.indiana.d2i.htrc.access.VolumeReader.PageReader;
import edu.indiana.d2i.htrc.access.VolumeRetriever;
import edu.indiana.d2i.htrc.access.ZipMaker;
import edu.indiana.d2i.htrc.access.exception.KeyNotFoundException;
import edu.indiana.d2i.htrc.access.exception.PolicyViolationException;
import edu.indiana.d2i.htrc.access.exception.RepositoryException;
import edu.indiana.d2i.htrc.audit.Auditor;

/**
 * @author Yiming Sun
 *
 */
public class CombinePageVolumeZipMaker implements ZipMaker {
    
    private static Logger log = Logger.getLogger(CombinePageVolumeZipMaker.class);
    protected static final String ACCESSED_ACTION = "ACCESSED";
    protected static final int DEFAULT_PAGE_SEQUENCE_ARRAY_SIZE = 400;
    protected final Auditor auditor;

    CombinePageVolumeZipMaker(Auditor auditor) {
        this.auditor = auditor;
    }

    /**
     * @see edu.indiana.d2i.htrc.access.ZipMaker#makeZipFile(java.io.OutputStream, java.lang.String, edu.indiana.d2i.htrc.access.VolumeRetriever)
     */
    @Override
    public void makeZipFile(OutputStream outputStream, VolumeRetriever volumeRetriever) throws IOException {
        
        boolean entryOpen = false;
        
        String currentVolumeID = null;
        List<String> currentPageSequences = null;
        
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
        
        ZipEntry zipEntry = null;
        
        List<Exception> exceptionList = new LinkedList<Exception>();
        
        while (volumeRetriever.hasMoreVolumes()) {
            try {
                VolumeReader volumeReader = volumeRetriever.nextVolume();
                
                if (volumeReader != null) {
                    String volumeID = volumeReader.getVolumeID();
                    
                    if (!volumeID.equals(currentVolumeID)) {
                        if (currentVolumeID != null) {
                            auditor.audit(ACCESSED_ACTION,  currentVolumeID,  currentPageSequences.toArray(new String[0]));
                        }
                    
                        if (entryOpen) {
                            zipOutputStream.closeEntry();
                            entryOpen = false;
                        }
                        
                        currentVolumeID = volumeID;
                        currentPageSequences = new ArrayList<String>(DEFAULT_PAGE_SEQUENCE_ARRAY_SIZE);
                        
                        String entryName = volumeReader.getPairtreeCleanedVolumeID() + ".txt";
                        zipEntry = new ZipEntry(entryName);
    
                        zipOutputStream.putNextEntry(zipEntry);
                        entryOpen = true;
                    }
                    
                    while (volumeReader.hasMorePages()) {
                        PageReader pageReader = volumeReader.nextPage();
                        byte[] pageContent = pageReader.getPageContent();
    
                        zipOutputStream.write(pageContent);
                        
                        currentPageSequences.add(pageReader.getPageSequence());
                    }
                }
            } catch (KeyNotFoundException e) {
                log.error("KeyNotFoundException", e);
                exceptionList.add(e);
            } catch (PolicyViolationException e) {
                log.error("PolicyViolationException", e);
                exceptionList.add(e);
            } catch (RepositoryException e) {
                log.error("RepositoryException", e);
                exceptionList.add(e);
            } 
        }

        if (currentVolumeID != null) {
            auditor.audit(ACCESSED_ACTION, currentVolumeID, currentPageSequences.toArray(new String[0]));
        }
        if (entryOpen) {
            zipOutputStream.closeEntry();
            entryOpen = false;
        }
        
        if (!exceptionList.isEmpty()) {
            ZipMakerFactory.Helper.injectErrorEntry(zipOutputStream, entryOpen, exceptionList);
        }
        zipOutputStream.close();
    }

}

