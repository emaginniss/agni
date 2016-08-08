/*
 * Copyright (c) 2015-2016, Eric A Maginniss
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ERIC A MAGINNISS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.emaginniss.agni.grid.persistence;

import org.emaginniss.agni.Agni;
import org.emaginniss.agni.Configuration;
import org.emaginniss.agni.grid.model.Instance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HSQLDBGridPersistenceTest {

    private static int counter = 0;

    private GridPersistence gp;

    @Before
    public void setup() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getObject().addProperty("type", "hsqldb");
        configuration.getObject().addProperty("driverClass", "org.hsqldb.jdbc.JDBCDriver");
        configuration.getObject().addProperty("url", "jdbc:hsqldb:mem:db" + counter++);
        configuration.getObject().addProperty("username", "SA");
        configuration.getObject().addProperty("password", "");

        Agni.initialize();

        gp = new HSQLDBGridPersistence(Agni.getNode(), configuration);
    }

    @After
    public void teardown() throws Exception {
        Agni.shutdown();
        gp.close();
    }

    @Test
    public void testAll() throws Exception {
        assertEquals(0, gp.getInstances().size());
        assertEquals(0, gp.getInstances(Instance.Status.starting).size());
        assertEquals(0, gp.getInstances("group1").size());

        gp.createInstance(new Instance("uuid1", "instanceId1", "group1", 1000, 0, Instance.Status.starting));
        assertEquals(1, gp.getInstances().size());
        Instance i = gp.getInstances().iterator().next();
        assertEquals("uuid1", i.getUuid());
        assertEquals("instanceId1", i.getInstanceId());
        assertEquals("group1", i.getGroupName());
        assertEquals(1000, i.getCreated());
        assertEquals(0, i.getLastCheckin());
        assertEquals(Instance.Status.starting, i.getStatus());
        assertEquals(1, gp.getInstances(Instance.Status.starting).size());
        assertEquals(0, gp.getInstances(Instance.Status.up).size());
        assertEquals(1, gp.getInstances("group1").size());
        assertEquals(0, gp.getInstances("group2").size());
        assertNotNull(gp.getInstanceByInstanceId("instanceId1"));
        assertNull(gp.getInstanceByInstanceId("instanceId2"));
        assertNotNull(gp.getInstanceByUuid("uuid1"));
        assertNull(gp.getInstanceByUuid("uuid2"));

        gp.createInstance(new Instance("uuid2", "instanceId2", "group1", 1000, 0, Instance.Status.up));
        assertEquals(2, gp.getInstances().size());
        assertEquals(1, gp.getInstances(Instance.Status.starting).size());
        assertEquals(1, gp.getInstances(Instance.Status.up).size());
        assertEquals(2, gp.getInstances("group1").size());
        assertEquals(0, gp.getInstances("group2").size());
        assertNotNull(gp.getInstanceByInstanceId("instanceId1"));
        assertNotNull(gp.getInstanceByInstanceId("instanceId2"));
        assertNotNull(gp.getInstanceByUuid("uuid1"));
        assertNotNull(gp.getInstanceByUuid("uuid2"));

        gp.updateInstance(new Instance("uuid1", null, null, 0, 9999, Instance.Status.up));
        i = gp.getInstanceByUuid("uuid1");
        assertNotNull(i);
        assertEquals("uuid1", i.getUuid());
        assertEquals("instanceId1", i.getInstanceId());
        assertEquals("group1", i.getGroupName());
        assertEquals(1000, i.getCreated());
        assertEquals(9999, i.getLastCheckin());
        assertEquals(Instance.Status.up, i.getStatus());
        assertEquals(0, gp.getInstances(Instance.Status.starting).size());
        assertEquals(2, gp.getInstances(Instance.Status.up).size());

        gp.deleteInstance(new Instance("uuid1", null, null, 0, 0, null));
        i = gp.getInstanceByUuid("uuid1");
        assertNull(i);
        assertEquals(1, gp.getInstances().size());
    }

}
