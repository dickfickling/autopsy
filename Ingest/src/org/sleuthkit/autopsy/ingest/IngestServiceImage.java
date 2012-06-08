/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.ingest;

import org.sleuthkit.datamodel.Image;

/**
 * 
 * Ingest service that acts on entire image 
 * Image ingest services run in own thread in the background
 * in parallel to the file processing ingest pipeline and other image ingest modules
 */
public interface IngestServiceImage extends IngestServiceAbstract {

    
    /**
     * Entry point to process the image by the service.
     * 
     * Service does all the processing work in this method.
     * It is responsible for extracting content of interest from the image (i.e. using DataModel API) and processing it.
     * Results of processing, such as extracted data or analysis results, should be posted to the blackboard. 
     * 
     * The service notifies the ingest inbox of interesting events (data, errors, warnings, infos) 
     * by posting ingest messages
     * The service notifies data viewers by firing events using IngestManager.fireServiceDataEvent
     * 
     * The service is responsible for posting progress to controller
     * And to periodically check controller if it should break out of the processing loop because task has been cancelled
     * 
     * @param image to process
     * @param controller to post progress to and to use for checking if cancellation has occurred
     */
    public void process(Image image, IngestImageWorkerController controller);
}