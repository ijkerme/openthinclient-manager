package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.event.DashboardEvent.BrowserResizeEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.event.DashboardEvent.UserLoggedOutEvent;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.event.PackageManagerTaskActivatedEvent;
import org.openthinclient.web.ui.event.PackageManagerTaskFinalizedEvent;
import org.openthinclient.web.view.MainView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.security.VaadinSecurity;
import org.vaadin.spring.sidebar.components.ValoSideBar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

@Theme("dashboard")
@Title("openthinclient.org")
@SpringUI(path = "/")
public final class DashboardUI extends UI {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 4314279050575370517L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardUI.class);
    private final DashboardEventBus dashboardEventbus = new DashboardEventBus();

    @Autowired
    VaadinSecurity vaadinSecurity;
    @Autowired
    SpringViewProvider viewProvider;
    @Autowired
    ValoSideBar sideBar;
    @Autowired
    PackageManagerExecutionEngine packageManagerExecutionEngine;
    @Autowired
    private EventBus.SessionEventBus eventBus;
    private PackageManagerExecutionEngine.Registration taskFinalizedRegistration;
    private PackageManagerExecutionEngine.Registration taskActivatedRegistration;
    
    @Autowired
    private RememberMeServices rememberMeServices;

    public static DashboardEventBus getDashboardEventbus() {
        return ((DashboardUI) getCurrent()).dashboardEventbus;
    }

    protected void onPackageManagerTaskFinalized(ListenableProgressFuture<?> listenableProgressFuture) {
        eventBus.publish(this, new PackageManagerTaskFinalizedEvent(packageManagerExecutionEngine));
    }

    protected void onPackageManagerTaskActivated(ListenableProgressFuture<?> listenableProgressFuture) {
        eventBus.publish(this, new PackageManagerTaskActivatedEvent(packageManagerExecutionEngine));
    }

    @Override
    protected void init(final VaadinRequest request) {

        setLocale(LocaleUtil.getLocaleForMessages(ConsoleWebMessages.class, UI.getCurrent().getLocale()));

        DashboardEventBus.register(this);
        Responsive.makeResponsive(this);
        addStyleName(ValoTheme.UI_WITH_MENU);

        updateContent();

        // Some views need to be aware of browser resize events so a
        // BrowserResizeEvent gets fired to the event bus on every occasion.
        Page.getCurrent().addBrowserWindowResizeListener(event -> DashboardEventBus.post(new BrowserResizeEvent()));

        IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
        Page.getCurrent().setTitle(mc.getMessage(ConsoleWebMessages.UI_PAGE_TITLE));

        taskActivatedRegistration = packageManagerExecutionEngine.addTaskActivatedHandler(this::onPackageManagerTaskActivated);
        taskFinalizedRegistration = packageManagerExecutionEngine.addTaskFinalizedHandler(this::onPackageManagerTaskFinalized);

    }

    @Override
    public void close() {
        super.close();
    }

    /**
     * Updates the correct content for this UI based on the current user status. If the user is
     * logged in with appropriate privileges, main view is shown. Otherwise login view is shown.
     */
    private void updateContent() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Collection<? extends GrantedAuthority> authentication = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        if (principal instanceof UserDetails && authentication != null && authentication.contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATORS"))) {
            // Authenticated user with ROLE_ADMINISTRATORS
            setContent(new MainView(viewProvider, sideBar));
            removeStyleName("loginview");
            Navigator navigator = getNavigator();
            if (navigator.getState().isEmpty()) {
                navigator.navigateTo("dashboard");
            }
        } else {
            setContent(new LoginView(eventBus));
            addStyleName("loginview");
        }
    }

    @EventBusListenerMethod
    public void userLoginRequested(final DashboardEvent.UserLoginRequestedEvent event) {
       
        final IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
        try {
            final Authentication authentication = vaadinSecurity.login(event.getUserName(), event.getPassword());
            LOGGER.debug("Received UserLoginRequestedEvent for ", authentication.getPrincipal());
            
            if (event.isRememberMe()) {
              HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
              HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
              request.setAttribute(AbstractRememberMeServices.DEFAULT_PARAMETER, event.isRememberMe());
              rememberMeServices.loginSuccess(request, response, authentication);
            }
            
            updateContent();
        } catch (AuthenticationException ex) {
            Notification.show(mc.getMessage(ConsoleWebMessages.UI_DASHBOARDUI_LOGIN_FAILED), ex.getMessage(), Notification.Type.ERROR_MESSAGE);
        } catch (Exception ex) {
            Notification.show(mc.getMessage(ConsoleWebMessages.UI_DASHBOARDUI_LOGIN_UNEXPECTED_ERROR), ex.getMessage(), Notification.Type.ERROR_MESSAGE);
            LOGGER.error("Unexpected error while logging in", ex);
        }
    }

    @EventBusListenerMethod
    public void userLoggedOut(final UserLoggedOutEvent event) {

        LOGGER.debug("Received UserLoggedOutEvent for ", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        // When the user logs out, current VaadinSession gets closed and the
        // page gets reloaded on the login screen. Do notice the this doesn't
        // invalidate the current HttpSession.
        VaadinSession.getCurrent().close();
        SecurityContextHolder.getContext().setAuthentication(null);
        vaadinSecurity.logout();
        Page.getCurrent().reload();
    }

    @Subscribe
    public void closeOpenWindows(final CloseOpenWindowsEvent event) {
        for (Window window : getWindows()) {
            window.close();
        }
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.subscribe(this);
    }

    @Override
    public void detach() {
        taskActivatedRegistration.unregister();
        taskFinalizedRegistration.unregister();
        eventBus.unsubscribe(this);
        super.detach();
    }
}
