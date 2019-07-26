package com.vaadin.flow.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;
import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8;
import elemental.json.Json;

public class IndexHtmlRequestHandler extends SynchronizedRequestHandler {
    private static final String DEFER_ATTRIBUTE = "defer";
    private static final String SCRIPT_TAG = "script";

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        Document indexDocument = getIndexHtmlDocument(request);
        if (indexDocument == null) {
            return false;
        }
        prependBaseHref(request, indexDocument);
        appendNpmBundle(indexDocument.head(), request.getService());
        response.setContentType(CONTENT_TYPE_TEXT_HTML_UTF_8);
        try {
            writeStream(response.getOutputStream(), indexDocument.html());
        } catch (IOException e) {
            getLogger().debug("Error writing index.html file", e);
        }
        return true;
    }

    private void prependBaseHref(VaadinRequest request,
            Document indexDocument) {
        Elements base = indexDocument.head().getElementsByTag("base");
        if (base.isEmpty()) {
            indexDocument.head().prependElement("base")
                    .attr("href", getServiceUrl(request));
        }
    }

    private Document getIndexHtmlDocument(VaadinRequest request) {
        try {
            String index = FrontendUtils
                    .getIndexHtmlContent(request.getService());
            return index != null ? Jsoup.parse(index) : null;
        } catch (IOException e) {
            getLogger().error("Can't read index.html", e);
        }
        return null;
    }

    protected void appendNpmBundle(Element head, VaadinService service)
            throws IOException {
        String content = FrontendUtils.getStatsContent(service);
        if (content == null) {
            throw new IOException(
                    "The stats file from webpack (stats.json) was not found.\n" +
                            "This typically mean that you have started the application without executing the 'prepare-frontend' Maven target.\n" +
                            "If you are using Spring Boot and are launching the Application class directly, " +
                            "you need to run \"mvn install\" once first or launch the application using \"mvn spring-boot:run\"");
        }
        elemental.json.JsonObject chunks = Json.parse(content)
                .getObject("assetsByChunkName");
        for (String key : chunks.keys()) {
            Element script = createJavaScriptElement(
                    "./" + VAADIN_MAPPING + chunks.getString(key));
            if (key.endsWith(".es5")) {
                head.appendChild(script.attr("nomodule", true));
            } else {
                head.appendChild(script.attr("type", "module"));
            }
        }
    }

    private Element createJavaScriptElement(String sourceUrl, boolean defer) {
        return createJavaScriptElement(sourceUrl, defer, "text/javascript");
    }

    private Element createJavaScriptElement(String sourceUrl, boolean defer,
            String type) {
        Element jsElement = new Element(Tag.valueOf(SCRIPT_TAG), "")
                .attr("type", type).attr(DEFER_ATTRIBUTE, defer);
        if (sourceUrl != null) {
            jsElement = jsElement.attr("src", sourceUrl);
        }
        return jsElement;
    }

    private Element createJavaScriptElement(String sourceUrl) {
        return createJavaScriptElement(sourceUrl, true);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(IndexHtmlRequestHandler.class);
    }

    private void writeStream(OutputStream outputStream, String indexHtml)
            throws IOException {
        InputStream inputStream = IOUtils
                .toInputStream(indexHtml, StandardCharsets.UTF_8);
        final byte[] buffer = new byte[32 * 1024];
        int bytes;
        while ((bytes = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, bytes);
        }
    }

    /**
     * Gets the service URL as a URL relative to the request URI.
     *
     * @param vaadinRequest
     *         the request
     * @return the relative service URL
     */
    protected static String getServiceUrl(VaadinRequest vaadinRequest) {
        String pathInfo = vaadinRequest.getPathInfo();
        if (pathInfo == null) {
            return ".";
        } else {
            /*
             * Make a relative URL to the servlet by adding one ../ for each
             * path segment in pathInfo (i.e. the part of the requested path
             * that comes after the servlet mapping)
             */
            return ServletHelper.getCancelingRelativePath(pathInfo);
        }
    }
}
