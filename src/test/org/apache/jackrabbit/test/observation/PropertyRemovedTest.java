/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.test.observation;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.EventType;
import javax.jcr.observation.Event;

/**
 * Test cases for {@link javax.jcr.observation.EventType#PROPERTY_REMOVED
 * PROPERTY_REMOVED} events.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class PropertyRemovedTest extends AbstractObservationTest {

    public void testSinglePropertyRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
	addEventListener(result, EventType.PROPERTY_REMOVED);
	Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
	foo.setProperty("prop1", new String[] { "foo" });
	foo.setProperty("prop2", new String[] { "bar" });
	testRoot.save();
	foo.remove("prop1");
	testRoot.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
	checkPropertyRemoved(events, new String[] { "foo/prop1" });
    }

    public void testMultiPropertyRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
	addEventListener(result, EventType.PROPERTY_REMOVED);
	Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
	foo.setProperty("prop1", new String[] { "foo" });
	foo.setProperty("prop2", new String[] { "bar" });
	testRoot.save();
	foo.remove("prop1");
	foo.remove("prop2");
	testRoot.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
	checkPropertyRemoved(events, new String[] { "foo/prop1", "foo/prop2" });
    }

}
