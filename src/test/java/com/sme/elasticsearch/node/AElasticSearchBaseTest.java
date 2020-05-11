package com.sme.elasticsearch.node;

import static org.elasticsearch.cluster.coordination.ClusterBootstrapService.INITIAL_MASTER_NODES_SETTING;
import static org.elasticsearch.discovery.SettingsBasedSeedHostsProvider.DISCOVERY_SEED_HOSTS_SETTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.util.TimeUnits;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.routing.allocation.DiskThresholdSettings;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.core.internal.io.IOUtils;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.indices.breaker.HierarchyCircuitBreakerService;
import org.elasticsearch.node.MockNode;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.MockHttpTransport;
import org.elasticsearch.transport.TransportSettings;
import org.elasticsearch.transport.nio.MockNioTransportPlugin;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.carrotsearch.randomizedtesting.RandomizedContext;
import com.carrotsearch.randomizedtesting.RandomizedRunner;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.annotations.TimeoutSuite;

/**
 * The implementation based on {@link ESSingleNodeTestCase} logic.
 */
@RunWith(RandomizedRunner.class)
@ThreadLeakScope(Scope.SUITE)
@ThreadLeakLingering(linger = 5000)
@TimeoutSuite(millis = 20 * TimeUnits.MINUTE)
public abstract class AElasticSearchBaseTest extends Assert
{
    private static Node NODE = null;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        Policy.setPolicy(new Policy()
        {
            @Override
            public boolean implies(ProtectionDomain domain, Permission permission)
            {
                return true;
            }
        });
        System.setSecurityManager(new SecurityManager());

        stopNode();
    }

    @AfterClass
    public static void afterClass() throws IOException, InterruptedException
    {
        stopNode();
    }

    @Before
    public void setUp() throws Exception
    {
        if (NODE == null)
        {
            startNode(new Random().nextLong());
        }
    }

    private void startNode(long seed) throws Exception
    {
        NODE = RandomizedContext.current().runWithPrivateRandomness(seed, this::newNode);

        ClusterHealthResponse clusterHealthResponse = client().admin().cluster().prepareHealth().setWaitForGreenStatus().get();
        assertFalse(clusterHealthResponse.isTimedOut());
        client().admin()
                .indices()
                .preparePutTemplate("one_shard_index_template")
                .setPatterns(Collections.singletonList("*"))
                .setOrder(0)
                .setSettings(Settings.builder()
                        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0))
                .get();
        client().admin()
                .indices()
                .preparePutTemplate("random-soft-deletes-template")
                .setPatterns(Collections.singletonList("*"))
                .setOrder(0)
                .setSettings(Settings.builder()
                        .put(IndexSettings.INDEX_SOFT_DELETES_SETTING.getKey(), new Random().nextBoolean())
                        .put(IndexSettings.INDEX_SOFT_DELETES_RETENTION_OPERATIONS_SETTING.getKey(),
                                new Random().nextBoolean() ? IndexSettings.INDEX_SOFT_DELETES_RETENTION_OPERATIONS_SETTING.get(Settings.EMPTY) : ThreadLocalRandom.current().nextInt(0, 1000)))
                .get();
    }

    private Node newNode()
    {
        new File("target/elasticsearch").mkdir();
        final Path tempDir = new File("target/elasticsearch").toPath();
        final String nodeName = nodeSettings().get(Node.NODE_NAME_SETTING.getKey(), "node_s_0");

        Settings settings = Settings.builder()
                .put(ClusterName.CLUSTER_NAME_SETTING.getKey(), "LOCAL_TEST_CLUSTER")
                .put(Environment.PATH_HOME_SETTING.getKey(), tempDir)
                .put(Environment.PATH_REPO_SETTING.getKey(), tempDir.resolve("repo"))
                .put(Environment.PATH_SHARED_DATA_SETTING.getKey(), tempDir.getParent())
                .put(Node.NODE_NAME_SETTING.getKey(), nodeName)
                .put(ScriptService.SCRIPT_MAX_COMPILATIONS_RATE.getKey(), "1000/1m")
                .put(EsExecutors.NODE_PROCESSORS_SETTING.getKey(), 1)
                .put("transport.type", MockNioTransportPlugin.MOCK_NIO_TRANSPORT_NAME)
                .put(TransportSettings.PORT.getKey(), ESTestCase.getPortRange())
                .put(Node.NODE_DATA_SETTING.getKey(), true)
                .put(NodeEnvironment.NODE_ID_SEED_SETTING.getKey(), new Random().nextLong())
                .put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING.getKey(), "1b")
                .put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING.getKey(), "1b")
                .put(DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_DISK_FLOOD_STAGE_WATERMARK_SETTING.getKey(), "1b")
                .put(HierarchyCircuitBreakerService.USE_REAL_MEMORY_USAGE_SETTING.getKey(), false)
                .putList(DISCOVERY_SEED_HOSTS_SETTING.getKey())
                .putList(INITIAL_MASTER_NODES_SETTING.getKey(), nodeName)
                .put(nodeSettings())
                .build();

        Collection<Class<? extends Plugin>> plugins = getPlugins();
        if (plugins.contains(MockNioTransportPlugin.class) == false)
        {
            plugins = new ArrayList<>(plugins);
            plugins.add(MockNioTransportPlugin.class);
        }
        if (addMockHttpTransport())
        {
            plugins.add(MockHttpTransport.TestPlugin.class);
        }
        Node node = new MockNode(settings, plugins, forbidPrivateIndexSettings());
        try
        {
            node.start();
        }
        catch (NodeValidationException e)
        {
            throw new RuntimeException(e);
        }
        return node;
    }

    protected Client client()
    {
        return NODE.client();
    }

    protected Settings nodeSettings()
    {
        return Settings.EMPTY;
    }

    protected Collection<Class<? extends Plugin>> getPlugins()
    {
        return Collections.emptyList();
    }

    protected boolean addMockHttpTransport()
    {
        return true;
    }

    protected boolean forbidPrivateIndexSettings()
    {
        return true;
    }

    private static void stopNode() throws IOException, InterruptedException
    {
        Node node = NODE;
        NODE = null;
        IOUtils.close(node);
        if (node != null && node.awaitClose(10, TimeUnit.SECONDS) == false)
        {
            throw new AssertionError("Node couldn't close within 10 seconds.");
        }

        FileSystemUtils.deleteSubDirectories(new File("target/elasticsearch").toPath());
    }
}
