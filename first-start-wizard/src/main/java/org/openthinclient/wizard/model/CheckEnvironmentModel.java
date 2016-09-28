package org.openthinclient.wizard.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckExecutionResult;
import org.openthinclient.advisor.check.CheckFilesystemFreeSpace;
import org.openthinclient.advisor.check.CheckNetworkInferfaces;
import org.openthinclient.advisor.inventory.SystemInventory;

import com.vaadin.ui.UI;

public class CheckEnvironmentModel {

  private final CheckExecutionEngine checkExecutionEngine;
  private final List<CheckStatus> checkStates;
  
  /** ManagerHomeModel to get current directory value */
  private ManagerHomeModel managerHomeModel;

  public CheckEnvironmentModel(SystemInventory systemInventory, CheckExecutionEngine checkExecutionEngine, ManagerHomeModel managerHomeModel, int installationFreespaceMinimum) {
    this.checkExecutionEngine = checkExecutionEngine;
    this.managerHomeModel = managerHomeModel;
    
    Locale locale = UI.getCurrent().getLocale();
    
    checkStates = new ArrayList<>();
    checkStates.add(new CheckStatus(new CheckNetworkInferfaces(locale, systemInventory)));
    checkStates.add(new CheckStatus(new CheckFilesystemFreeSpace(locale, this::getManagerHome, installationFreespaceMinimum)));
  }

  public List<CheckStatus> getCheckStates() {
    return checkStates;
  }

  protected Path getManagerHome() {
    return Paths.get(managerHomeModel.getManagerHomePathProperty().getValue());
  }  
  
  public void runChecks() {
    checkStates.forEach(check -> check.executeOn(checkExecutionEngine));
  }

  public boolean isRunning() {
    return checkStates.stream().anyMatch(CheckStatus::isRunning);
  }

  public boolean isAcceptable() {
    return checkStates.stream().allMatch(checkStatus -> !checkStatus.isRunning() && checkStatus.getResultType() != null && checkStatus.getResultType() != CheckExecutionResult.CheckResultType.FAILED);
  }

  public boolean allChecksRunned() {
    return checkStates.stream().allMatch(CheckStatus::isFinished);
  }

//  @Override
//  public String getStringParam(String key) {
//    
//    if (key != null && key.equals(CheckFilesystemFreeSpace.MANAGER_HOME_DIRECTORY)) {
//      return managerHomeModel.getManagerHomePathProperty().getValue();
//    }
//    
//    return null;
//  }

}
