package org.openthinclient.wizard.model;

import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.inventory.SystemInventory;
import org.springframework.core.task.AsyncListenableTaskExecutor;

public class SystemSetupModel {
  private final NetworkConfigurationModel networkConfigurationModel;
  private final CheckEnvironmentModel checkEnvironmentModel;
  private final ManagerHomeModel managerHomeModel;
  private final InstallModel installModel;

  public SystemSetupModel(SystemInventory systemInventory, CheckExecutionEngine checkExecutionEngine, AsyncListenableTaskExecutor taskExecutor) {

    this.networkConfigurationModel = new NetworkConfigurationModel();
    this.checkEnvironmentModel = new CheckEnvironmentModel(systemInventory, checkExecutionEngine);
    this.managerHomeModel = new ManagerHomeModel(checkExecutionEngine);
    this.installModel = new InstallModel(taskExecutor);
  }

  public NetworkConfigurationModel getNetworkConfigurationModel() {
    return networkConfigurationModel;
  }

  public CheckEnvironmentModel getCheckEnvironmentModel() {
    return checkEnvironmentModel;
  }

  public ManagerHomeModel getManagerHomeModel() {
    return managerHomeModel;
  }

  public InstallModel getInstallModel() {
    return installModel;
  }
}
