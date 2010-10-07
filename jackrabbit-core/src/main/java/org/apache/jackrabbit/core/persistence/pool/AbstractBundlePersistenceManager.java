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
package org.apache.jackrabbit.core.persistence.pool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.PropertyType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.core.cache.ConcurrentCache;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.CachingPersistenceManager;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.FileBasedIndex;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.util.StringIndex;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * The <code>AbstractBundlePersistenceManager</code> acts as base for all
 * persistence managers that store the state in a {@link NodePropBundle}.
 * <p/>
 * The state and all property states of one node are stored together in one
 * record. Property values of a certain size can be store outside of the bundle.
 * This currently only works for binary properties. NodeReferences are not
 * included in the bundle since they are addressed by the target id.
 * <p/>
 * Some strings like namespaces and local names are additionally managed by
 * separate indexes. only the index number is serialized to the records which
 * reduces the amount of memory used.
 * <p/>
 * Special treatment is performed for the properties "jcr:uuid", "jcr:primaryType"
 * and "jcr:mixinTypes". As they are also stored in the node state they are not
 * included in the bundle but generated when required.
 * <p/>
 * In order to increase performance, there are 2 caches maintained. One is the
 * bundle cache that caches already loaded bundles. The other is the
 * {@link LRUNodeIdCache} that caches non-existent bundles. This is useful
 * because a lot of {@link #exists(NodeId)} calls are issued that would result
 * in a useless SQL execution if the desired bundle does not exist.
 * <p/>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/>
 * </ul>
 */
public abstract class AbstractBundlePersistenceManager implements
    PersistenceManager, CachingPersistenceManager, IterablePersistenceManager {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(AbstractBundlePersistenceManager.class);

    /** the prefix of a node file */
    protected static final String NODEFILENAME = "n";

    /** the prefix of a node references file */
    protected static final String NODEREFSFILENAME = "r";

    /** the name of the names-index resource */
    protected static final String RES_NAME_INDEX = "/names.properties";

    /** the name of the namespace-index resource */
    protected static final String RES_NS_INDEX = "/namespaces.properties";

    /** Sentinel instance used to mark a non-existent bundle in the cache */
    private static final NodePropBundle MISSING =
        new NodePropBundle(new NodeId());

    /** the index for namespaces */
    private StringIndex nsIndex;

    /** the index for local names */
    private StringIndex nameIndex;

    /** the cache of loaded bundles */
    private ConcurrentCache<NodeId, NodePropBundle> bundles;

    /** the persistence manager context */
    protected PMContext context;

    /** default size of the bundle cache */
    private long bundleCacheSize = 8 * 1024 * 1024;

    /**
     * Returns the size of the bundle cache in megabytes.
     * @return the size of the bundle cache in megabytes.
     */
    public String getBundleCacheSize() {
        return String.valueOf(bundleCacheSize / (1024 * 1024));
    }

    /**
     * Sets the size of the bundle cache in megabytes.
     * the default is 8.
     *
     * @param bundleCacheSize the bundle cache size in megabytes.
     */
    public void setBundleCacheSize(String bundleCacheSize) {
        this.bundleCacheSize = Long.parseLong(bundleCacheSize) * 1024 * 1024;
    }

    /**
     * Creates the folder path for the given node id that is suitable for
     * storing states in a filesystem.
     *
     * @param buf buffer to append to or <code>null</code>
     * @param id the id of the node
     * @return the buffer with the appended data.
     */
    protected StringBuffer buildNodeFolderPath(StringBuffer buf, NodeId id) {
        if (buf == null) {
            buf = new StringBuffer();
        }
        char[] chars = id.toString().toCharArray();
        int cnt = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '-') {
                continue;
            }
            //if (cnt > 0 && cnt % 4 == 0) {
            if (cnt == 2 || cnt == 4) {
                buf.append(FileSystem.SEPARATOR_CHAR);
            }
            buf.append(chars[i]);
            cnt++;
        }
        return buf;
    }

    /**
     * Creates the folder path for the given property id that is suitable for
     * storing states in a filesystem.
     *
     * @param buf buffer to append to or <code>null</code>
     * @param id the id of the property
     * @return the buffer with the appended data.
     */
    protected StringBuffer buildPropFilePath(StringBuffer buf, PropertyId id) {
        if (buf == null) {
            buf = new StringBuffer();
        }
        buildNodeFolderPath(buf, id.getParentId());
        buf.append(FileSystem.SEPARATOR);
        buf.append(getNsIndex().stringToIndex(id.getName().getNamespaceURI()));
        buf.append('.');
        buf.append(getNameIndex().stringToIndex(id.getName().getLocalName()));
        return buf;
    }

    /**
     * Creates the file path for the given property id and value index that is
     * suitable for storing property values in a filesystem.
     *
     * @param buf buffer to append to or <code>null</code>
     * @param id the id of the property
     * @param i the index of the property value
     * @return the buffer with the appended data.
     */
    protected StringBuffer buildBlobFilePath(StringBuffer buf, PropertyId id,
                                             int i) {
        if (buf == null) {
            buf = new StringBuffer();
        }
        buildPropFilePath(buf, id);
        buf.append('.');
        buf.append(i);
        return buf;
    }

    /**
     * Creates the file path for the given node id that is
     * suitable for storing node states in a filesystem.
     *
     * @param buf buffer to append to or <code>null</code>
     * @param id the id of the node
     * @return the buffer with the appended data.
     */
    protected StringBuffer buildNodeFilePath(StringBuffer buf, NodeId id) {
        if (buf == null) {
            buf = new StringBuffer();
        }
        buildNodeFolderPath(buf, id);
        buf.append(FileSystem.SEPARATOR);
        buf.append(NODEFILENAME);
        return buf;
    }

    /**
     * Creates the file path for the given references id that is
     * suitable for storing reference states in a filesystem.
     *
     * @param buf buffer to append to or <code>null</code>
     * @param id the id of the node
     * @return the buffer with the appended data.
     */
    protected StringBuffer buildNodeReferencesFilePath(
            StringBuffer buf, NodeId id) {
        if (buf == null) {
            buf = new StringBuffer();
        }
        buildNodeFolderPath(buf, id);
        buf.append(FileSystem.SEPARATOR);
        buf.append(NODEREFSFILENAME);
        return buf;
    }

    /**
     * Returns the namespace index
     * @return the namespace index
     * @throws IllegalStateException if an error occurs.
     */
    public StringIndex getNsIndex() {
        try {
            if (nsIndex == null) {
                // load name and ns index
                FileSystemResource nsFile = new FileSystemResource(context.getFileSystem(), RES_NS_INDEX);
                if (nsFile.exists()) {
                    nsIndex = new FileBasedIndex(nsFile);
                } else {
                    nsIndex = (StringIndex) context.getNamespaceRegistry();
                }
            }
            return nsIndex;
        } catch (Exception e) {
            IllegalStateException e2 = new IllegalStateException("Unable to create nsIndex.");
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Returns the local name index
     * @return the local name index
     * @throws IllegalStateException if an error occurs.
     */
    public StringIndex getNameIndex() {
        try {
            if (nameIndex == null) {
                nameIndex = new FileBasedIndex(new FileSystemResource(
                        context.getFileSystem(), RES_NAME_INDEX));
            }
            return nameIndex;
        } catch (Exception e) {
            IllegalStateException e2 = new IllegalStateException("Unable to create nameIndex.");
            e2.initCause(e);
            throw e2;
        }
    }

    //-----------------------------------------< CacheablePersistenceManager >--

    /**
     * {@inheritDoc}
     */
    public synchronized void onExternalUpdate(ChangeLog changes) {
        for (ItemState state : changes.modifiedStates()) {
            bundles.remove(getBundleId(state));
        }
        for (ItemState state : changes.deletedStates()) {
            bundles.remove(getBundleId(state));
        }
        for (ItemState state : changes.addedStates()) {
            // There may have been a cache miss entry
            bundles.remove(getBundleId(state));
        }
    }

    private NodeId getBundleId(ItemState state) {
        if (state.isNode()) {
            return (NodeId) state.getId();
        } else {
            return state.getParentId();
        }
    }

    //----------------------------------------------------------------< spi >---

    /**
     * Loads a bundle from the underlying system.
     *
     * @param id the node id of the bundle
     * @return the loaded bundle or <code>null</code> if the bundle does not
     *         exist.
     * @throws ItemStateException if an error while loading occurs.
     */
    protected abstract NodePropBundle loadBundle(NodeId id)
            throws ItemStateException;

    /**
     * Stores a bundle to the underlying system.
     *
     * @param bundle the bundle to store
     * @throws ItemStateException if an error while storing occurs.
     */
    protected abstract void storeBundle(NodePropBundle bundle)
            throws ItemStateException;

    /**
     * Deletes the bundle from the underlying system.
     *
     * @param bundle the bundle to destroy
     *
     * @throws ItemStateException if an error while destroying occurs.
     */
    protected abstract void destroyBundle(NodePropBundle bundle)
            throws ItemStateException;

    /**
     * Deletes the node references from the underlying system.
     *
     * @param refs the node references to destroy.
     * @throws ItemStateException if an error while destroying occurs.
     */
    protected abstract void destroy(NodeReferences refs)
            throws ItemStateException;

    /**
     * Stores a node references to the underlying system.
     *
     * @param refs the node references to store.
     * @throws ItemStateException if an error while storing occurs.
     */
    protected abstract void store(NodeReferences refs)
            throws ItemStateException;

    /**
     * Returns the BLOB store used by this persistence manager.
     *
     * @return BLOB store
     */
    protected abstract BLOBStore getBlobStore();

    //-------------------------------------------------< PersistenceManager >---

    /**
     * {@inheritDoc}
     *
     * Initializes the internal structures of this abstract persistence manager.
     */
    public void init(PMContext context) throws Exception {
        this.context = context;
        // init bundle cache
        bundles = new ConcurrentCache<NodeId, NodePropBundle>();
        bundles.setMaxMemorySize(bundleCacheSize);
    }

    /**
     * {@inheritDoc}
     *
     *  Closes the persistence manager, release acquired resources.
     */
    public void close() throws Exception {
        // clear caches
        bundles.clear();
    }

    /**
     * {@inheritDoc}
     *
     * Loads the state via the appropriate NodePropBundle.
     */
    public NodeState load(NodeId id) throws NoSuchItemStateException, ItemStateException {
        NodePropBundle bundle = getBundle(id);
        if (bundle == null) {
            throw new NoSuchItemStateException(id.toString());
        }
        return bundle.createNodeState(this);
    }

    /**
     * {@inheritDoc}
     *
     * Loads the state via the appropriate NodePropBundle.
     */
    public PropertyState load(PropertyId id) throws NoSuchItemStateException, ItemStateException {
        NodePropBundle bundle = getBundle(id.getParentId());
        if (bundle == null) {
            throw new NoSuchItemStateException(id.toString());
        }
        PropertyState state = bundle.createPropertyState(this, id.getName());
        if (state == null) {
            // check if autocreated property state
            if (id.getName().equals(NameConstants.JCR_UUID)) {
                state = createNew(id);
                state.setType(PropertyType.STRING);
                state.setMultiValued(false);
                state.setValues(new InternalValue[]{InternalValue.create(id.getParentId().toString())});
            } else if (id.getName().equals(NameConstants.JCR_PRIMARYTYPE)) {
                state = createNew(id);
                state.setType(PropertyType.NAME);
                state.setMultiValued(false);
                state.setValues(new InternalValue[]{InternalValue.create(bundle.getNodeTypeName())});
            } else if (id.getName().equals(NameConstants.JCR_MIXINTYPES)) {
                Set<Name> mixins = bundle.getMixinTypeNames();
                state = createNew(id);
                state.setType(PropertyType.NAME);
                state.setMultiValued(true);
                state.setValues(InternalValue.create(mixins.toArray(new Name[mixins.size()])));
            } else {
                throw new NoSuchItemStateException(id.toString());
            }
            bundle.addProperty(state, getBlobStore());
        }
        return state;
    }

    /**
     * {@inheritDoc}
     *
     * Loads the state via the appropriate NodePropBundle.
     */
    public boolean exists(PropertyId id) throws ItemStateException {
        NodePropBundle bundle = getBundle(id.getParentId());
        return bundle != null && bundle.hasProperty(id.getName());
    }

    /**
     * {@inheritDoc}
     *
     * Checks the existence via the appropriate NodePropBundle.
     */
    public boolean exists(NodeId id) throws ItemStateException {
        // anticipating a load followed by a exists
        return getBundle(id) != null;
    }

    /**
     * {@inheritDoc}
     */
    public NodeState createNew(NodeId id) {
        return new NodeState(id, null, null, NodeState.STATUS_NEW, false);
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState createNew(PropertyId id) {
        return new PropertyState(id, PropertyState.STATUS_NEW, false);
    }

    /**
     * Right now, this iterates over all items in the changelog and
     * calls the individual methods that handle single item states
     * or node references objects. Properly implemented, this method
     * should ensure that changes are either written completely to
     * the underlying persistence layer, or not at all.
     *
     * {@inheritDoc}
     */
    public synchronized void store(ChangeLog changeLog)
            throws ItemStateException {
        boolean success = false;
        try {
            storeInternal(changeLog);
            success = true;
        } finally {
            if (!success) {
                bundles.clear();
            }
        }
    }

    /**
     * Stores the given changelog and updates the bundle cache.
     *
     * @param changeLog the changelog to store
     * @throws ItemStateException on failure
     */
    private void storeInternal(ChangeLog changeLog)
            throws ItemStateException {
        // delete bundles
        HashSet<ItemId> deleted = new HashSet<ItemId>();
        for (ItemState state : changeLog.deletedStates()) {
            if (state.isNode()) {
                NodePropBundle bundle = getBundle((NodeId) state.getId());
                if (bundle == null) {
                    throw new NoSuchItemStateException(state.getId().toString());
                }
                deleteBundle(bundle);
                deleted.add(state.getId());
            }
        }
        // gather added node states
        HashMap<ItemId, NodePropBundle> modified = new HashMap<ItemId, NodePropBundle>();
        for (ItemState state : changeLog.addedStates()) {
            if (state.isNode()) {
                NodePropBundle bundle = new NodePropBundle((NodeState) state);
                modified.put(state.getId(), bundle);
            }
        }
        // gather modified node states
        for (ItemState state : changeLog.modifiedStates()) {
            if (state.isNode()) {
                NodeId nodeId = (NodeId) state.getId();
                NodePropBundle bundle = modified.get(nodeId);
                if (bundle == null) {
                    bundle = getBundle(nodeId);
                    if (bundle == null) {
                        throw new NoSuchItemStateException(nodeId.toString());
                    }
                    modified.put(nodeId, bundle);
                }
                bundle.update((NodeState) state);
            } else {
                PropertyId id = (PropertyId) state.getId();
                // skip redundant primaryType, mixinTypes and uuid properties
                if (id.getName().equals(NameConstants.JCR_PRIMARYTYPE)
                    || id.getName().equals(NameConstants.JCR_MIXINTYPES)
                    || id.getName().equals(NameConstants.JCR_UUID)) {
                    continue;
                }
                NodeId nodeId = id.getParentId();
                NodePropBundle bundle = modified.get(nodeId);
                if (bundle == null) {
                    bundle = getBundle(nodeId);
                    if (bundle == null) {
                        throw new NoSuchItemStateException(nodeId.toString());
                    }
                    modified.put(nodeId, bundle);
                }
                bundle.addProperty((PropertyState) state, getBlobStore());
            }
        }
        // add removed properties
        for (ItemState state : changeLog.deletedStates()) {
            if (state.isNode()) {
                // check consistency
                NodeId parentId = state.getParentId();
                if (!modified.containsKey(parentId) && !deleted.contains(parentId)) {
                    log.warn("Deleted node state's parent is not modified or deleted: " + parentId + "/" + state.getId());
                }
            } else {
                PropertyId id = (PropertyId) state.getId();
                NodeId nodeId = id.getParentId();
                if (!deleted.contains(nodeId)) {
                    NodePropBundle bundle = modified.get(nodeId);
                    if (bundle == null) {
                        // should actually not happen
                        log.warn("deleted property state's parent not modified!");
                        bundle = getBundle(nodeId);
                        if (bundle == null) {
                            throw new NoSuchItemStateException(nodeId.toString());
                        }
                        modified.put(nodeId, bundle);
                    }
                    bundle.removeProperty(id.getName(), getBlobStore());
                }
            }
        }
        // add added properties
        for (ItemState state : changeLog.addedStates()) {
            if (!state.isNode()) {
                PropertyId id = (PropertyId) state.getId();
                // skip primaryType pr mixinTypes properties
                if (id.getName().equals(NameConstants.JCR_PRIMARYTYPE)
                    || id.getName().equals(NameConstants.JCR_MIXINTYPES)
                    || id.getName().equals(NameConstants.JCR_UUID)) {
                    continue;
                }
                NodeId nodeId = id.getParentId();
                NodePropBundle bundle = modified.get(nodeId);
                if (bundle == null) {
                    // should actually not happen
                    log.warn("added property state's parent not modified!");
                    bundle = getBundle(nodeId);
                    if (bundle == null) {
                        throw new NoSuchItemStateException(nodeId.toString());
                    }
                    modified.put(nodeId, bundle);
                }
                bundle.addProperty((PropertyState) state, getBlobStore());
            }
        }

        // now store all modified bundles
        for (NodePropBundle bundle : modified.values()) {
            putBundle(bundle);
        }

        // store the refs
        for (NodeReferences refs : changeLog.modifiedRefs()) {
            if (refs.hasReferences()) {
                store(refs);
            } else {
                destroy(refs);
            }
        }
    }

    /**
     * Gets the bundle for the given node id. Read/write synchronization
     * happens higher up at the SISM level, so we don't need to worry about
     * conflicts here.
     *
     * @param id the id of the bundle to retrieve.
     * @return the bundle or <code>null</code> if the bundle does not exist
     *
     * @throws ItemStateException if an error occurs.
     */
    private NodePropBundle getBundle(NodeId id) throws ItemStateException {
        NodePropBundle bundle = bundles.get(id);
        if (bundle == MISSING) {
            return null;
        } else if (bundle == null) {
            bundle = loadBundle(id);
            if (bundle != null) {
                bundle.markOld();
                bundles.put(id, bundle, bundle.getSize());
            } else {
                bundles.put(id, MISSING, 16);
            }
        }
        return bundle;
    }

    /**
     * Deletes the bundle
     *
     * @param bundle the bundle to delete
     * @throws ItemStateException if an error occurs
     */
    private void deleteBundle(NodePropBundle bundle) throws ItemStateException {
        destroyBundle(bundle);
        bundle.removeAllProperties(getBlobStore());
        bundles.put(bundle.getId(), MISSING, 16);
    }

    /**
     * Stores the bundle and puts it to the cache.
     *
     * @param bundle the bundle to store
     * @throws ItemStateException if an error occurs
     */
    private void putBundle(NodePropBundle bundle) throws ItemStateException {
        storeBundle(bundle);
        bundle.markOld();
        log.debug("stored bundle {}", bundle.getId());

        // only put to cache if already exists. this is to ensure proper
        // overwrite and not creating big contention during bulk loads
        if (bundles.containsKey(bundle.getId())) {
            bundles.put(bundle.getId(), bundle, bundle.getSize());
        }
    }

    /**
     * This implementation does nothing.
     *
     * {@inheritDoc}
     */
    public void checkConsistency(String[] uuids, boolean recursive, boolean fix) {
    }

    /**
     * Evicts the bundle with <code>id</code> from the bundle cache.
     *
     * @param id the id of the bundle.
     */
    protected void evictBundle(NodeId id) {
        bundles.remove(id);
    }

}