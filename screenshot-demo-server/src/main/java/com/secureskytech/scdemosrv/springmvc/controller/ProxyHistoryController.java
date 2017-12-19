package com.secureskytech.scdemosrv.springmvc.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import com.secureskytech.scdemosrv.AppContext;
import com.secureskytech.scdemosrv.model.ProxyHistory;
import com.secureskytech.scdemosrv.model.ScreenshotHistory;

@Controller
public class ProxyHistoryController {

    @Autowired
    ServletContext servletContext;

    @Autowired
    HttpServletRequest servletRequest;

    @Autowired
    HttpServletResponse servletResponse;

    @GetMapping("/")
    public String list(@RequestAttribute Connection dbconn, Model model) throws SQLException {
        model.addAttribute("proxyHistories", ProxyHistory.getList(dbconn));
        return "index";
    }

    @GetMapping("/proxy-history/{logContext}/{messageRef}")
    public String detail(@RequestAttribute Connection dbconn, Model model, @PathVariable String logContext,
            @PathVariable int messageRef) throws SQLException {
        model.addAttribute("logContext", logContext);
        model.addAttribute("messageRef", messageRef);
        ProxyHistory proxyHistory = ProxyHistory.getOne(dbconn, logContext, messageRef);
        if (Objects.isNull(proxyHistory)) {
            servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "proxy-history-not-found";
        }
        model.addAttribute("proxyHistory", ProxyHistory.getOne(dbconn, logContext, messageRef));
        model.addAttribute("charsetNames", AppContext.getAvailableCharsetNames());
        model.addAttribute("screenshots", ScreenshotHistory.getList(dbconn, logContext, messageRef));
        return "proxy-history";
    }

    @PostMapping("/proxy-history/{logContext}/{messageRef}")
    public String updateCharset(@RequestAttribute Connection dbconn, Model model, @PathVariable String logContext,
            @PathVariable int messageRef, @RequestParam String requestCharset, @RequestParam String responseCharset)
            throws SQLException {
        ProxyHistory.updateCharset(dbconn, logContext, messageRef, requestCharset, responseCharset);
        // TODO affected row check
        return "redirect:/proxy-history/{logContext}/{messageRef}";
    }

    @GetMapping("/attach-screenshot-form")
    public String attachScreenshotForm(Model model) {
        return "attach-screenshot-form";
    }

    @PostMapping("/attach-screenshot-upload")
    public String attachScreenshotUpload(@RequestAttribute Connection dbconn, Model model,
            @RequestParam(required = true) String url, @RequestParam(required = true) String dataSchemedImage)
            throws SQLException {
        final AppContext appContext = (AppContext) servletContext.getAttribute("appContext");
        final String logContext = appContext.getLogContext();
        final long newScreenshotId = appContext.getNewScreenshotId();
        final Timestamp tsnow = new Timestamp(appContext.now());
        try {
            long foundmref =
                ProxyHistory
                    .attachScreenshot(dbconn, logContext, newScreenshotId, url.trim(), dataSchemedImage.trim(), tsnow);
            if (foundmref == 0) {
                throw new IllegalStateException("ProxyHistory.attachScreenshot() returns 0");
            }
            model.addAttribute("logContext", logContext);
            model.addAttribute("messageRef", foundmref);
            model.addAttribute("scid", newScreenshotId);
            return "redirect:/proxy-history/{logContext}/{messageRef}#sc-{scid}";
        } catch (IllegalArgumentException matchedUrlNotFound) {
            model.addAttribute("errormsg", "URLにマッチするHTTPログが見つからなかったため、添付できませんでした。");
            return "attach-screenshot-form";
        }
    }

}
