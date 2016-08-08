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

import org.emaginniss.agni.Configuration;
import org.emaginniss.agni.Node;
import org.emaginniss.agni.annotations.Subscribe;
import org.emaginniss.agni.grid.model.Instance;
import org.emaginniss.agni.messages.StopRouting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class JDBCGridPersistence implements GridPersistence {

    private static final String INSTANCE_COLUMNS = "uuid, instance_id, group_name, created, last_checkin, status";

    protected Connection connection;

    public JDBCGridPersistence(Node node, Configuration configuration) {

        String driverClass = configuration.getString("driverClass", null);
        String url = configuration.getString("url", null);
        String username = configuration.getString("username", null);
        String password = configuration.getString("password", null);

        try {
            Class.forName(driverClass);
            connection = DriverManager.getConnection(url, username, password);
            checkAndBuildTables();

            node.register(new Object() {
                @Subscribe
                public void handleStopRouting(StopRouting sr) {
                    if (sr.getNodeUuid().equals(node.getUuid())) {
                        try {
                            connection.close();
                        } catch (Exception e) {
                            //do nothing
                        }
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error while initializing persistence for grid server", e);
        }
    }

    protected abstract void checkAndBuildTables() throws SQLException;

    private Instance buildInstance(ResultSet rset) throws SQLException {
        return new Instance(rset.getString(1), rset.getString(2), rset.getString(3), rset.getLong(4), rset.getLong(5), Instance.Status.valueOf(rset.getString(6)));
    }

    @NotNull
    @Override
    public Collection<Instance> getInstances() {
        Set<Instance> out = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement("select " + INSTANCE_COLUMNS + " from instance")) {
            loadInstances(stmt, out);
        } catch (SQLException e) {
            throw new RuntimeException("Error in getInstances", e);
        }
        return out;
    }

    public void loadInstances(PreparedStatement stmt, Set<Instance> out) throws SQLException {
        try (ResultSet rset = stmt.executeQuery()) {
            while (rset.next()) {
                out.add(buildInstance(rset));
            }
        }
    }

    @NotNull
    @Override
    public Collection<Instance> getInstances(Instance.Status status) {
        Set<Instance> out = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement("select " + INSTANCE_COLUMNS + " from instance where status = ?")) {
            stmt.setString(1, status.name());
            loadInstances(stmt, out);
        } catch (SQLException e) {
            throw new RuntimeException("Error in getInstances", e);
        }
        return out;
    }

    @NotNull
    @Override
    public Collection<Instance> getInstances(String groupName) {
        Set<Instance> out = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement("select " + INSTANCE_COLUMNS + " from instance where group_name = ?")) {
            stmt.setString(1, groupName);
            loadInstances(stmt, out);
        } catch (SQLException e) {
            throw new RuntimeException("Error in getInstances", e);
        }
        return out;
    }

    @Nullable
    @Override
    public Instance getInstanceByUuid(String uuid) {
        try (PreparedStatement stmt = connection.prepareStatement("select " + INSTANCE_COLUMNS + " from instance where uuid = ?")) {
            stmt.setString(1, uuid);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return buildInstance(rset);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in getInstanceByUuid", e);
        }
        return null;
    }

    @Nullable
    @Override
    public Instance getInstanceByInstanceId(String instanceId) {
        try (PreparedStatement stmt = connection.prepareStatement("select " + INSTANCE_COLUMNS + " from instance where instance_id = ?")) {
            stmt.setString(1, instanceId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return buildInstance(rset);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in getInstanceByInstanceId", e);
        }
        return null;
    }

    @Override
    public void createInstance(Instance instance) {
        try (PreparedStatement stmt = connection.prepareStatement("insert into instance (" + INSTANCE_COLUMNS + ") values (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, instance.getUuid());
            stmt.setString(2, instance.getInstanceId());
            stmt.setString(3, instance.getGroupName());
            stmt.setLong(4, instance.getCreated());
            stmt.setLong(5, instance.getLastCheckin());
            stmt.setString(6, instance.getStatus().name());
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Error in createInstance", e);
        }
    }

    @Override
    public void updateInstance(Instance instance) {
        try (PreparedStatement stmt = connection.prepareStatement("update instance set last_checkin = ?, status = ? where uuid = ?")) {
            stmt.setLong(1, instance.getLastCheckin());
            stmt.setString(2, instance.getStatus().name());
            stmt.setString(3, instance.getUuid());
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Error in updateInstance", e);
        }
    }

    @Override
    public void deleteInstance(Instance instance) {
        try (PreparedStatement stmt = connection.prepareStatement("delete from instance where uuid = ?")) {
            stmt.setString(1, instance.getUuid());
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Error in deleteInstance", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
