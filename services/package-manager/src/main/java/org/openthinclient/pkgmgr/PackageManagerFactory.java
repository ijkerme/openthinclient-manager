/*******************************************************************************
 * openthinclient.org ThinClient suite <p/> Copyright (C) 2004, 2007 levigo holding GmbH. All Rights
 * Reserved. <p/> <p/> This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. <p/> This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details. <p/> You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.util.dpkg.DPKGPackageManager;

public class PackageManagerFactory {

    private final PackageManagerDatabase packageManagerDatabase;
    private final PackageManagerExecutionEngine executionEngine;

    public PackageManagerFactory(PackageManagerDatabase packageManagerDatabase, PackageManagerExecutionEngine executionEngine) {
        this.packageManagerDatabase = packageManagerDatabase;
        this.executionEngine = executionEngine;
    }

    /**
     * @return a new created instance of the DPKGPackageManager
     */
    public PackageManager createPackageManager(PackageManagerConfiguration configuration) {

        PackageManagerTaskSummary taskSummary = new PackageManagerTaskSummary();

        DPKGPackageManager dpkgPackageManager = new DPKGPackageManager(configuration, packageManagerDatabase, executionEngine);
        dpkgPackageManager.setTaskSummary(taskSummary);
        return dpkgPackageManager;
    }

}
