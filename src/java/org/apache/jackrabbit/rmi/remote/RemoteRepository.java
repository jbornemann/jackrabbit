/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

/**
 * Remote version of the JCR {@link javax.jcr.Repository Repository} interface.
 * Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerRepository ServerRepository}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientRepository ClientRepository}
 * adapters to provide transparent RMI access to remote repositories.
* <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding Repository method. The remote object will simply
 * forward the method call to the underlying Repository instance.
 * {@link javax.jcr.Session Session} objects are returned as remote references
 * to the {@link org.apache.jackrabbit.rmi.remote.RemoteSession RemoteSession}
 * interface. Simple return values and possible exceptions are simply copied
 * over the network to the client. RMI errors are signalled with
 * RemoteExceptions.
 *
 * @author Jukka Zitting
 * @see javax.jcr.Repository
 * @see org.apache.jackrabbit.rmi.client.ClientRepository
 * @see org.apache.jackrabbit.rmi.server.ServerRepository
 */
public interface RemoteRepository extends Remote {
    
    /**
     * @see javax.jcr.Repository#getDescriptor(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    public String getDescriptor(String name) throws RemoteException;
    
    /**
     * @see javax.jcr.Repository#getDescriptorKeys()
     * @throws RemoteException on RMI errors
     */
    public String[] getDescriptorKeys() throws RemoteException;
    
    /**
     * @see javax.jcr.Repository#login()
     * @throws RemoteException on RMI errors
     */
    public RemoteSession login() throws LoginException,
        NoSuchWorkspaceException, RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Repository#login(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    public RemoteSession login(String workspace) throws LoginException,
        NoSuchWorkspaceException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Repository#login(javax.jcr.Credentials)
     * @throws RemoteException on RMI errors
     */
    public RemoteSession login(Credentials credentials) throws LoginException,
        NoSuchWorkspaceException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Repository#login(javax.jcr.Credentials, java.lang.String)
     * @throws RemoteException on RMI errors
     */
    public RemoteSession login(Credentials credentials, String workspace)
        throws LoginException, NoSuchWorkspaceException, RepositoryException,
        RemoteException;
        
}
