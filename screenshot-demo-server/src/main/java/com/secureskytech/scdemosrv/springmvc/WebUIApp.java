package com.secureskytech.scdemosrv.springmvc;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;

import com.secureskytech.scdemosrv.AppContext;

public class WebUIApp {
    private static final Logger LOG = LoggerFactory.getLogger(WebUIApp.class);

    private Server server;

    public WebUIApp(final AppContext appContext) throws IOException {
        server = new Server();

        AnnotationConfigWebApplicationContext springContext = new AnnotationConfigWebApplicationContext();
        springContext.setConfigLocation("com.secureskytech.scdemosrv.springmvc.config");

        DispatcherServlet dispatcherServlet = new DispatcherServlet(springContext);
        ServletHolder springServletHolder = new ServletHolder("mvc-dispatcher", dispatcherServlet);

        // ref: https://github.com/bkielczewski/example-spring-mvc-jetty
        // ref: https://github.com/fernandospr/spring-jetty-example
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);

        /* 以下を設定せずにスクリーンショット画像添付のform POST を実際のWebExtensionsから呼ぶと
         * java.lang.IllegalStateException: Form too large: 202666 > 200000
         * が発生した。さすがスクリーンショット画像の data scheme url (base64), かなり大きい。
         * 以下を参考に、とりあえず「まずこれを超えることはないだろう」ということで、
         * DoS対策は一旦気にせずに100MBに設定した。
         * http://www.eclipse.org/jetty/documentation/current/setting-form-size.html
         * http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/server/handler/ContextHandler.html#setMaxFormContentSize-int-
         * なお、stackoverflowなどググった結果出てきた記事のいくつかには、0や-1を設定すると無制限になる、
         * みたいなことも書かれていたが、少なくとも上記のJetty公式ドキュメントにはそのようなことは書かれていなかった。
         * そのため 0 / -1 などによる無制限設定はしていない。
         */
        contextHandler.setMaxFormContentSize(100 * 1024 * 1024);

        contextHandler.setErrorHandler(null);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(springServletHolder, "/*");
        contextHandler.addEventListener(new ContextLoaderListener(springContext));
        contextHandler.setResourceBase(new ClassPathResource("/webui", WebUIApp.class).getURI().toString());

        contextHandler.setAttribute("appContext", appContext);

        CharacterEncodingFilter utf8Filter = new CharacterEncodingFilter();
        utf8Filter.setEncoding("UTF-8");
        utf8Filter.setForceEncoding(true);
        FilterHolder filterHolder = new FilterHolder(utf8Filter);
        EnumSet<DispatcherType> allDispatcher = EnumSet.of(
            DispatcherType.ASYNC,
            DispatcherType.ERROR,
            DispatcherType.FORWARD,
            DispatcherType.INCLUDE,
            DispatcherType.REQUEST);
        contextHandler.addFilter(filterHolder, "/*", allDispatcher);

        server.setHandler(contextHandler);
    }

    public void start(int listenPort) throws Exception {
        if (Objects.nonNull(server) && server.isRunning()) {
            LOG.info("ineternal webui already running at port [" + listenPort + "].");
            throw new Exception("already running at port[" + listenPort + "]");
        }
        // remove old connectors
        Connector[] oldConnectors = server.getConnectors();
        if (Objects.nonNull(oldConnectors)) {
            for (Connector oldc : oldConnectors) {
                server.removeConnector(oldc);
            }
        }
        // add new connector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(listenPort);
        server.setConnectors(new Connector[] { connector });
        server.start();
        LOG.info("internal webui server started with listening port [" + listenPort + "].");
    }

    public void stop() throws Exception {
        if (Objects.isNull(server)) {
            LOG.info("internal webui server is not initialized.");
            return;
        }
        if (!server.isStarted()) {
            LOG.info("internal webui server is not started.");
            return;
        }
        server.stop();
        LOG.info("internal webui server stopped.");
    }
}
