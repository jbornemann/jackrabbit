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
package org.apache.jackrabbit.rmi.client;

import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.BinaryValue;
import javax.jcr.BooleanValue;
import javax.jcr.DateValue;
import javax.jcr.DoubleValue;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.LongValue;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferenceValue;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.StringValue;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.SerialValue;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNode RemoteNode}
 * inteface. This class makes a remote node locally available using
 * the JCR {@link javax.jcr.Node Node} interface.
 * 
 * @author Jukka Zitting
 * @see javax.jcr.Node
 * @see org.apache.jackrabbit.rmi.remote.RemoteNode
 */
public class ClientNode extends ClientItem implements Node {

    /** The adapted remote node. */
    private RemoteNode remote;
    
    /**
     * Creates a local adapter for the given remote node.
     * 
     * @param session current session
     * @param remote  remote node
     * @param factory local adapter factory
     */
    public ClientNode(Session session, RemoteNode remote,
            LocalAdapterFactory factory) {
        super(session, remote, factory);
        this.remote = remote;
    }
    
    /**
     * Returns <code>true</code> without contacting the remote node.
     *   
     * {@inheritDoc}
     */
    public boolean isNode() {
        return true;
    }

    /**
     * Calls the {@link ItemVisitor#visit(Node) ItemVisitor.visit(Node)}
     * method of the given visitor. Does not contact the remote node, but
     * the visitor may invoke other methods that do contact the remote node.
     * 
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }
    
    /** {@inheritDoc} */
    public Node addNode(String path) throws ItemExistsException,
            PathNotFoundException, ConstraintViolationException,
            RepositoryException {
        try {
            return factory.getNode(session, remote.addNode(path));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node addNode(String path, String type) throws ItemExistsException,
            PathNotFoundException, NoSuchNodeTypeException,
            ConstraintViolationException, RepositoryException {
        try {
            return factory.getNode(session, remote.addNode(path, type));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void orderBefore(String src, String dst) throws
            UnsupportedRepositoryOperationException,
            ConstraintViolationException, ItemNotFoundException,
            RepositoryException {
        try {
            remote.orderBefore(src, dst);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value value)
            throws ValueFormatException, RepositoryException {
        try {
            return factory.getProperty(
                    session, remote.setProperty(name, new SerialValue(value)));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value[] values)
            throws ValueFormatException, RepositoryException {
        try {
            Value[] serials = SerialValue.makeSerialValueArray(values);
            RemoteProperty property = remote.setProperty(name, serials);
            return factory.getProperty(session, property);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String[] strings)
            throws ValueFormatException, RepositoryException {
        Value[] values = new Value[strings.length];
        for (int i = 0; i < strings.length; i++) {
            values[i] = new StringValue(strings[i]);
        }
        return setProperty(name, values);
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String value)
            throws ValueFormatException, RepositoryException {
        return setProperty(name, new StringValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, InputStream value)
            throws ValueFormatException, RepositoryException {
        return setProperty(name, new BinaryValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, boolean value)
            throws ValueFormatException, RepositoryException {
        return setProperty(name, new BooleanValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, double value)
            throws ValueFormatException, RepositoryException {
        return setProperty(name, new DoubleValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, long value)
            throws ValueFormatException, RepositoryException {
        return setProperty(name, new LongValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Calendar value)
            throws ValueFormatException, RepositoryException {
        return setProperty(name, new DateValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Node value)
            throws ValueFormatException, RepositoryException {
        return setProperty(name, new ReferenceValue(value));
    }

    /** {@inheritDoc} */
    public Node getNode(String path) throws PathNotFoundException,
            RepositoryException {
        try {
            return factory.getNode(session, remote.getNode(path));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getNodes() throws RepositoryException {
        try {
            return getNodeIterator(session, remote.getNodes());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getNodes(String pattern) throws RepositoryException {
        try {
            return getNodeIterator(session, remote.getNodes(pattern));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property getProperty(String path) throws PathNotFoundException,
            RepositoryException {
        try {
            return factory.getProperty(session, remote.getProperty(path));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getProperties() throws RepositoryException {
        try {
            return getPropertyIterator(session, remote.getProperties());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getProperties(String pattern) throws
            RepositoryException {
        try {
            return getPropertyIterator(session, remote.getProperties(pattern));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Item getPrimaryItem() throws ItemNotFoundException,
            RepositoryException {
        try {
            return factory.getItem(session, remote.getPrimaryItem());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getUUID() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        try {
            return remote.getUUID();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getReferences() throws RepositoryException {
        try {
            return getPropertyIterator(session, remote.getReferences());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasNode(String path) throws RepositoryException {
        try {
            return remote.hasNode(path);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasProperty(String path) throws RepositoryException {
        try {
            return remote.hasProperty(path);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasNodes() throws RepositoryException {
        try {
            return remote.hasNodes();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasProperties() throws RepositoryException {
        try {
            return remote.hasProperties();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        try {
            return factory.getNodeType(remote.getPrimaryNodeType());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        try {
            return getNodeTypeArray(remote.getMixinNodeTypes());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isNodeType(String type) throws RepositoryException {
        try {
            return remote.isNodeType(type);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void addMixin(String name) throws NoSuchNodeTypeException,
            ConstraintViolationException, RepositoryException {
        try {
            remote.addMixin(name);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeMixin(String name) throws NoSuchNodeTypeException,
            ConstraintViolationException, RepositoryException {
        try {
            remote.removeMixin(name);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean canAddMixin(String name) throws RepositoryException {
        try {
            return remote.canAddMixin(name);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeDef getDefinition() throws RepositoryException {
        try {
            return factory.getNodeDef(remote.getDefinition());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public Version checkin() throws VersionException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void checkout() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        try {
            remote.checkout();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void update(String workspace) throws NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException {
        try {
            remote.update(workspace);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void merge(String workspace, boolean bestEffort) throws
            UnsupportedRepositoryOperationException, NoSuchWorkspaceException,
            AccessDeniedException, MergeException, RepositoryException {
        try {
            remote.merge(workspace, bestEffort);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void cancelMerge(Version version) throws VersionException,
            InvalidItemStateException, UnsupportedRepositoryOperationException,
            RepositoryException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
    
    /** {@inheritDoc} */
    public void doneMerge(Version version) throws VersionException,
            InvalidItemStateException, UnsupportedRepositoryOperationException,
            RepositoryException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
    
    /** {@inheritDoc} */
    public String getCorrespondingNodePath(String workspace) throws
            ItemNotFoundException, NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException {
        try {
            return remote.getCorrespondingNodePath(workspace);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public int getIndex() throws RepositoryException {
        try {
            return remote.getIndex();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restore(String version, boolean removeExisting) throws
            VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        try {
            remote.restore(version, removeExisting);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restore(Version version, boolean removeExisting) throws
            VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            RepositoryException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void restore(Version version, String path, boolean removeExisting)
            throws PathNotFoundException, ItemExistsException,
            VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void restoreByLabel(String label, boolean removeExisting) throws
            VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        try {
            remote.restoreByLabel(label, removeExisting);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String[] strings, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        Value[] values = new Value[strings.length];
        for (int i = 0; i < strings.length; i++) {
            values[i] = new StringValue(strings[i]);
        }
        return setProperty(name, values, type);
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        try {
            Value[] serials = SerialValue.makeSerialValueArray(values);
            RemoteProperty property = remote.setProperty(name, serials, type);
            return factory.getProperty(session, property);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isCheckedOut() throws
            UnsupportedRepositoryOperationException, RepositoryException {
        try {
            return remote.isCheckedOut();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public VersionHistory getVersionHistory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public Version getBaseVersion()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public Lock lock(boolean isDeep, boolean isSessionScoped)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, RepositoryException {
        try {
            return factory.getLock(this, remote.lock(isDeep, isSessionScoped));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Lock getLock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException {
        try {
            return factory.getLock(this, remote.getLock());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void unlock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException {
        try {
            remote.unlock();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean holdsLock() throws RepositoryException {
        try {
            return remote.holdsLock();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
     }

    /** {@inheritDoc} */
    public boolean isLocked() throws RepositoryException {
        try {
            return remote.isLocked();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
