/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * Remote version of the JCR
 * {@link javax.jcr.nodetype.NodeTypeManager NodeTypeManager} interface.
 * Used by the 
 * {@link org.apache.jackrabbit.rmi.server.ServerNodeTypeManager ServerNodeTypeManager}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientNodeTypeManager ClientNodeTypeManager}
 * adapters to provide transparent RMI access to remote node type managers.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding NodeTypeManager method. The remote object will
 * simply forward the method call to the underlying NodeTypeManager instance.
 * Arguments and possible exceptions are copied over the network. Complex
 * {@link javax.jcr.nodetype.NodeType NodeType} values are returned as
 * remote references to the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeType RemoteNodeType}
 * interface. Iterator values are transmitted as object arrays. RMI errors
 * are signalled with RemoteExceptions.
 *
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.NodeTypeManager
 * @see org.apache.jackrabbit.rmi.client.ClientNodeTypeManager
 * @see org.apache.jackrabbit.rmi.server.ServerNodeTypeManager
 */
public interface RemoteNodeTypeManager extends Remote {

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#getNodeType(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    public RemoteNodeType getNodeType(String name)
        throws NoSuchNodeTypeException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#getAllNodeTypes()
     * @throws RemoteException on RMI errors
     */
    public RemoteNodeType[] getAllNodeTypes()
        throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#getPrimaryNodeTypes()
     * @throws RemoteException on RMI errors
     */
    public RemoteNodeType[] getPrimaryNodeTypes()
        throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#getMixinNodeTypes()
     * @throws RemoteException on RMI errors
     */
    public RemoteNodeType[] getMixinNodeTypes()
        throws RepositoryException, RemoteException;

}
