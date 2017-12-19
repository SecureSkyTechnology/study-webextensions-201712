package com.secureskytech.scdemosrv.swingui;

import java.nio.charset.StandardCharsets;

import javax.swing.JTextArea;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * @see https://logback.qos.ch/manual/appenders.html#WriteYourOwnAppender
 * @see https://logback.qos.ch/manual/appenders_ja.html#WriteYourOwnAppender
 * @see http://oct.im/how-to-create-logback-loggers-dynamicallypragmatically.html
 */
public class LogbackSwingTextareaAppender extends AppenderBase<ILoggingEvent> {

    private final JTextArea textarea;
    private PatternLayoutEncoder encoder;

    public LogbackSwingTextareaAppender(JTextArea ta) {
        this.textarea = ta;
    }

    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        byte[] logdata = this.encoder.encode(eventObject);
        String logline = new String(logdata, StandardCharsets.UTF_8);
        String log = textarea.getText();
        log += logline;
        textarea.setText(log);
    }

    public static void addToRootLogger(JTextArea textarea) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{ISO8601} [%thread] %marker %level %logger - %msg%n");
        encoder.start();
        LogbackSwingTextareaAppender newAppender = new LogbackSwingTextareaAppender(textarea);
        newAppender.setContext(loggerContext);
        newAppender.setEncoder(encoder);
        newAppender.start();
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(newAppender);
    }
}
