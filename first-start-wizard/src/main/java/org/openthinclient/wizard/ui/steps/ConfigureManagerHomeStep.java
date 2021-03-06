package org.openthinclient.wizard.ui.steps;

import java.util.Collections;
import java.util.List;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;
import org.openthinclient.wizard.model.CheckStatus;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;

import com.vaadin.data.Validator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ConfigureManagerHomeStep extends AbstractCheckExecutingStep {

    private final SystemSetupModel systemSetupModel;
    private final VerticalLayout content;
    private final TextField homeDirectoryTextField;
    private CheckEnvironmentStep.CheckStatusLabel checkStatusLabel;
    private volatile boolean validatedProceed;

    public ConfigureManagerHomeStep(Wizard wizard, SystemSetupModel systemSetupModel) {
        super(wizard);
        
        this.systemSetupModel = systemSetupModel;

        homeDirectoryTextField = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_LABEL), systemSetupModel.getManagerHomeModel().getManagerHomePathProperty());
        homeDirectoryTextField.setWidth(100, Sizeable.Unit.PERCENTAGE);
        homeDirectoryTextField.setStyleName(ValoTheme.TEXTFIELD_LARGE);
        homeDirectoryTextField.addValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_VALIDATOR_DIRECTORYNAME), 3, null, false));
        homeDirectoryTextField.setEnabled(systemSetupModel.getManagerHomeModel().isManagerHomeChangeable());
        content = new VerticalLayout(

                createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_HEADLINE)),
                createLabelHuge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_TEXT)),

                homeDirectoryTextField

        );

        content.setSpacing(true);
        content.setMargin(true);
        setContent(content);

    }

    @Override
    public String getCaption() {
        return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREMANAGERHOMESTEP_TITLE);
    }

    @Override
    public boolean onAdvance() {

        // Once the checks have been executed and the result is valid, immediately proceed to the next wizard step.
        if (validatedProceed) {
            validatedProceed = true;
            return true;
        }

        if (systemSetupModel.getManagerHomeModel().isManagerHomeChangeable())
            try {
                homeDirectoryTextField.commit();
            } catch (Validator.InvalidValueException e) {
                return false;
            }

        if (systemSetupModel.getManagerHomeModel().isManagerHomeSpecified() && !systemSetupModel.getManagerHomeModel().isManagerHomeValidated()) {
            runChecks();

            return false;
        }

        return systemSetupModel.getManagerHomeModel().isManagerHomeValid();
    }

    @Override
    public boolean onBack() {
        return true;
    }

    @Override
    protected List<CheckStatusLabel> getStatusLabels() {
        if (checkStatusLabel != null) {
            return Collections.singletonList(checkStatusLabel);
        }

        return Collections.emptyList();
    }

    @Override
    protected void onRunChecks() {

        // execute the manager home validation
        final CheckStatus checkStatus = systemSetupModel.getManagerHomeModel().runCheck();

        if (checkStatusLabel != null) {
            content.removeComponent(checkStatusLabel);
        }
        checkStatusLabel = new CheckEnvironmentStep.CheckStatusLabel(checkStatus);
        content.addComponent(checkStatusLabel);
    }

    @Override
    protected void onChecksFinished() {
        if (systemSetupModel.getManagerHomeModel().isManagerHomeValid()) {
            // advance the wizard to the next step
            // specifying validatedProceed to ensure that onAdvance will immediately proceed without any further checks
            validatedProceed = true;
            wizard.next();
        }
    }

    @Override
    protected boolean isChecksFinished() {
        return systemSetupModel.getManagerHomeModel().isManagerHomeValidated();
    }
}
