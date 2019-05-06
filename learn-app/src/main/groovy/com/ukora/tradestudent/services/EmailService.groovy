package com.ukora.tradestudent.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Service
class EmailService {

    @Autowired
    SimulationResultService simulationResultService

    @Scheduled(cron = '0 59 */6 * * *')
    def sendBestSimulation() {
        ObjectMapper objectMapper = new ObjectMapper()
        def r = simulationResultService.getTopPerformingSimulation()
        sendEmail('top performing simulation', objectMapper.writeValueAsString(r), 'baskuis1@gmail.com')
    }

    @Async
    def sendEmail(String subject, String message, String email) {
        try {
            def props = new Properties()
            props.put("mail.smtp.user", 'b@ukora.com')
            props.put("mail.smtp.host", 'smtp.gmail.com')
            props.put("mail.smtp.port", 465)
            props.put("mail.smtp.starttls.enable", "true")
            props.put("mail.smtp.debug", "false")
            props.put("mail.smtp.auth", "true")
            props.put("mail.smtp.socketFactory.port", 465)
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            props.put("mail.smtp.socketFactory.fallback", "false")

            def auth = new SMTPAuthenticator()
            def session = Session.getInstance(props, auth)
            session.setDebug(true)

            def msg = new MimeMessage(session)
            msg.setText(message)
            msg.setSubject(subject)
            msg.setFrom(new InternetAddress('b@ukora.com'))
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email))

            Transport transport = session.getTransport("smtps")
            transport.connect('smtp.gmail.com', 465, 'b@ukora.com', 'mp5d2FkQm0n')
            transport.sendMessage(msg, msg.getAllRecipients())
            transport.close()
        } catch (Exception e) {
            Logger.log('Unable to send email. Info: ' + e.message)
        }
    }

    class SMTPAuthenticator extends Authenticator {
        PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication('b@ukora.com', 'mp5d2FkQm0n')
        }
    }

}
