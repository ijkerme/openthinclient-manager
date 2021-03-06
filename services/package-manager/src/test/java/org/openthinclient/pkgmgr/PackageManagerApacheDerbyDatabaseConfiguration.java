package org.openthinclient.pkgmgr;

import javax.sql.DataSource;

import org.openthinclient.db.DatabaseConfiguration.DatabaseType;
import org.openthinclient.pkgmgr.spring.PackageManagerDatabaseConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerRepositoryConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({PackageManagerRepositoryConfiguration.class, //
        HibernateJpaAutoConfiguration.class, //
        PackageManagerDatabaseConfiguration.class
})
public class PackageManagerApacheDerbyDatabaseConfiguration {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create() //
                .driverClassName(DatabaseType.APACHE_DERBY.getDriverClassName()) //
                .url("jdbc:derby:memory:pkgmngr-test-" + System.currentTimeMillis() + ";create=true")
                .build();
    }

}
