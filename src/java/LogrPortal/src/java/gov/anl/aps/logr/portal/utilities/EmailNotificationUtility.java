/*
 * Copyright (c) UChicago Argonne, LLC. All rights reserved.
 * See LICENSE file.
 */
package gov.anl.aps.logr.portal.utilities;

import gov.anl.aps.logr.portal.controllers.SystemPropertyController;
import gov.anl.aps.logr.portal.model.db.entities.Log;
import gov.anl.aps.logr.portal.model.db.entities.LogTopic;
import gov.anl.aps.logr.portal.model.db.entities.UserInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Sends email notifications to a category's email list when a new log entry
 * is created. Configuration is read from cdb.portal.properties.
 *
 * To enable, set cdb.portal.email.smtp.host to your SMTP server hostname.
 */
public class EmailNotificationUtility {

    private static final Logger logger = LogManager.getLogger(EmailNotificationUtility.class.getName());

    private static final String PROP_SMTP_HOST     = "cdb.portal.email.smtp.host";
    private static final String PROP_SMTP_PORT     = "cdb.portal.email.smtp.port";
    private static final String PROP_SMTP_STARTTLS = "cdb.portal.email.smtp.starttls";
    private static final String PROP_SMTP_AUTH     = "cdb.portal.email.smtp.auth";
    private static final String PROP_SMTP_USERNAME = "cdb.portal.email.smtp.username";
    private static final String PROP_SMTP_PASSWORD = "cdb.portal.email.smtp.password";
    private static final String PROP_FROM          = "cdb.portal.email.from";
    private static final String PROP_BASE_URL      = "cdb.portal.email.baseUrl";

    /**
     * Sends a notification email to all addresses in the log entry's category
     * email list. Silently skips if SMTP is not configured or the category has
     * no email list. All exceptions are caught and logged — email is a
     * non-critical path.
     *
     * @param log the newly-created log entry
     */
    public static void sendLogEntryNotification(Log log) {
        List<LogTopic> topics = log.getLogTopicList();
        if (topics == null || topics.isEmpty()) {
            return;
        }

        String smtpHost = ConfigurationUtility.getPortalProperty(PROP_SMTP_HOST);
        if (smtpHost == null || smtpHost.trim().isEmpty()) {
            logger.debug("Email notifications disabled (cdb.portal.email.smtp.host not set). "
                    + "Skipping notification for log entry {}.", log.getId());
            return;
        }

        // Collect unique recipient addresses across all categories
        Set<String> seenAddresses = new LinkedHashSet<>();
        List<InternetAddress> recipients = new ArrayList<>();
        List<String> categoryNames = new ArrayList<>();

        for (LogTopic topic : topics) {
            String emailListRaw = topic.getEmailList();
            if (emailListRaw == null || emailListRaw.trim().isEmpty()) {
                continue;
            }
            categoryNames.add(topic.getName());
            for (InternetAddress addr : parseEmailList(emailListRaw)) {
                String canonical = addr.getAddress().toLowerCase();
                if (seenAddresses.add(canonical)) {
                    recipients.add(addr);
                }
            }
        }

        if (recipients.isEmpty()) {
            return;
        }

        try {
            Session mailSession = buildMailSession(smtpHost);
            MimeMessage msg = buildMessage(mailSession, log, recipients, categoryNames);
            Transport.send(msg);
            logger.info("Sent category email notification for log entry {} (categories: {}) to {} recipient(s).",
                    log.getId(), categoryNames, recipients.size());
        } catch (MessagingException ex) {
            logger.error("Failed to send email notification for log entry {}: {}",
                    log.getId(), ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Session buildMailSession(String smtpHost) {
        String smtpPort     = ConfigurationUtility.getPortalProperty(PROP_SMTP_PORT,     "25");
        String startTls     = ConfigurationUtility.getPortalProperty(PROP_SMTP_STARTTLS, "false");
        String auth         = ConfigurationUtility.getPortalProperty(PROP_SMTP_AUTH,     "false");
        String smtpUsername = ConfigurationUtility.getPortalProperty(PROP_SMTP_USERNAME, "");
        String smtpPassword = ConfigurationUtility.getPortalProperty(PROP_SMTP_PASSWORD, "");

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost.trim());
        props.put("mail.smtp.port", smtpPort.trim());
        props.put("mail.smtp.starttls.enable", startTls.trim());
        props.put("mail.smtp.auth", auth.trim());

        boolean useAuth = Boolean.parseBoolean(auth.trim());
        if (useAuth) {
            final String user = smtpUsername.trim();
            final String pass = smtpPassword.trim();
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
        }

        return Session.getInstance(props);
    }

    private static MimeMessage buildMessage(Session session, Log log, List<InternetAddress> recipients,
            List<String> categoryNames) throws MessagingException {

        String fromAddress = ConfigurationUtility.getPortalProperty(PROP_FROM, "bely-noreply@localhost");
        String baseUrl     = resolveBaseUrl();

        String categoriesDisplay = String.join(", ", categoryNames);

        UserInfo author = log.getEnteredByUser();
        String authorName = (author != null) ? author.getUsername() : "Unknown";

        String entryText = (log.getText() != null) ? log.getText() : "";
        Date enteredOn   = log.getEnteredOnDateTime();

        // Subject: use first category name for brevity; show all if multiple
        String subjectCategory = categoryNames.size() == 1
                ? categoryNames.get(0)
                : categoryNames.get(0) + " (+" + (categoryNames.size() - 1) + " more)";
        String subject = "[BELY] [" + subjectCategory + "] New log entry by " + authorName;

        // Plain-text body
        StringBuilder body = new StringBuilder();
        body.append("A new log entry has been posted.\n");
        body.append("\n");
        body.append("Categories: ").append(categoriesDisplay).append("\n");
        body.append("Author    : ").append(authorName).append("\n");
        body.append("Posted    : ").append(enteredOn != null ? enteredOn.toString() : "unknown").append("\n");
        if (!baseUrl.isEmpty() && log.getId() != null) {
            String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            body.append("View      : ").append(trimmed).append("/views/log/view.xhtml?id=").append(log.getId()).append("\n");
        }
        body.append("\n");
        body.append("--- Log Entry ---\n");
        body.append(entryText).append("\n");
        body.append("-----------------\n");

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(fromAddress));
        msg.setRecipients(Message.RecipientType.TO,
                recipients.toArray(new InternetAddress[0]));
        msg.setSubject(subject);
        msg.setText(body.toString(), "UTF-8");
        msg.setSentDate(new Date());

        return msg;
    }

    private static String resolveBaseUrl() {
        try {
            SystemPropertyController ctrl = SystemPropertyController.getInstance();
            if (ctrl != null) {
                String url = ctrl.getPermalinkBaseUrl();
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }
        } catch (Exception ex) {
            logger.debug("Could not resolve permalink base URL from application context: {}", ex.getMessage());
        }
        // Fallback to properties file (used when FacesContext is unavailable or DB not configured)
        return ConfigurationUtility.getPortalProperty(PROP_BASE_URL, "");
    }

    /**
     * Parses a comma- or whitespace-separated string of email addresses,
     * silently skipping any that are malformed.
     */
    private static List<InternetAddress> parseEmailList(String emailListRaw) {
        List<InternetAddress> result = new ArrayList<>();
        for (String token : emailListRaw.split("[,\\s]+")) {
            String addr = token.trim();
            if (addr.isEmpty()) {
                continue;
            }
            try {
                result.add(new InternetAddress(addr, true));
            } catch (AddressException ex) {
                logger.warn("Skipping invalid email address '{}': {}", addr, ex.getMessage());
            }
        }
        return result;
    }

}
