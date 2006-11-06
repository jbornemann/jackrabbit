/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.cluster;

import java.util.List;
import org.apache.jackrabbit.core.state.ChangeLog;
import javax.jcr.RepositoryException;

/**
 * Interface used to receive information about incoming, external update events.
 */
public interface UpdateEventListener {

    /**
     * Handle an external update.
     *
     * @param changes external changes containing only node and property ids.
     * @param events events to deliver
     * @throws RepositoryException if the update cannot be processed
     */
    public void externalUpdate(ChangeLog changes, List events) throws RepositoryException;
}
