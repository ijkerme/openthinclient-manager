package org.openthinclient.pkgmgr;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = UpdateDatabaseTest.Config.class)
public class UpdateDatabaseTest {
    private static DebianTestRepositoryServer testRepositoryServer;
    @Autowired
    PackageRepository packageRepository;
    @Autowired
    SourceRepository sourceRepository;
    @Autowired
    PackageManagerConfiguration configuration;

    @BeforeClass
    public static void startRepoServer() {
        testRepositoryServer = new DebianTestRepositoryServer();
        testRepositoryServer.start();
    }

    @AfterClass
    public static void shutdownRepoServer() {
        testRepositoryServer.stop();
        testRepositoryServer = null;
    }

    @Test
    public void testUpdatePackages() throws Exception {

        UpdateDatabase updater = new UpdateDatabase(configuration, getSourcesList(), packageRepository);

        updater.doUpdate(null, configuration.getProxyConfiguration());

        // we'r expecting four packages to exist at this point in time
        assertEquals(4, packageRepository.count());

        // running another update should not add new packages
        updater = new UpdateDatabase(configuration, getSourcesList(), packageRepository);

        updater.doUpdate(null, configuration.getProxyConfiguration());
        assertEquals(4, packageRepository.count());

    }

    @Before
    public void configureSource() {
        final Source source = new Source();
        source.setUrl(testRepositoryServer.getServerUrl());
        source.setEnabled(true);
        sourceRepository.saveAndFlush(source);
    }

    public SourcesList getSourcesList() {

        final SourcesList sourcesList = new SourcesList();
        sourcesList.getSources().addAll(sourceRepository.findAll());
        return sourcesList;

    }

    @Configuration()
    @Import({SimpleTargetDirectoryPackageManagerConfiguration.class, PackageManagerInMemoryDatabaseConfiguration.class})
    public static class Config {

    }


}