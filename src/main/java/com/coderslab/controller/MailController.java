package com.coderslab.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
public class MailController {

	private static final String EML_FILE = "/home/zubayer/A-WORKSPACE/savedmailfile/mail.eml";

	@Autowired
	Environment env;

	@GetMapping
	public String loadMainPage(Model model) {

		return "index";
	}

	private Session getMailSession() {
		Properties props = System.getProperties();
		try {
			props.load(new FileInputStream(new File("src/main/resources/mail-settings.properties")));
		} catch (IOException e) {
			e.printStackTrace();
		}

		Session session = Session.getDefaultInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(props.getProperty("mail.user"), props.getProperty("mail.passwd"));
			}
		});

		return session;
	}

	@GetMapping("/mail2")
	public String sendMailFromEMLFile(RedirectAttributes redirect) throws FileNotFoundException, MessagingException {
		String to = "zubayer.ahamed@metafour.com";
		String from = "zubayer.ahamed@metafour.com";

		// Mail session
		Session session = getMailSession();
		// Mime message
		MimeMessage message = getMimeMessage(session);

		File emlFile = new File(EML_FILE);
		InputStream source = new FileInputStream(emlFile);
		BodyPart htmlPart = new MimeBodyPart(source);

		// Add Attachment 
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(htmlPart);
		for(Map.Entry<String, String> attachment : getFiles().entrySet()){
			BodyPart mailBody = new MimeBodyPart();
			DataSource dSource = new FileDataSource(attachment.getValue());
			mailBody.setDataHandler(new DataHandler(dSource));
			mailBody.setFileName(attachment.getKey());
			multipart.addBodyPart(mailBody);
		}

		message.setContent(multipart);

		// send mail
		Transport.send(message);

		redirect.addFlashAttribute("sm", "Email send successfull");

		return "redirect:/";
	}

	private Map<String, String> getContextData(){
		Map<String, String> map = new HashMap<>();
		map.put("fname", "Zubayer");
		map.put("lname", "Ahamed");
		return map;
	}

	private Map<String, String> getFiles(){
		Map<String, String> map = new HashMap<>();
		map.put("attachment.csv", "src/main/resources/static/mail-attachment-template.csv");
		map.put("attachment.pdf", "src/main/resources/static/zubayer_cv.pdf");
		return map;
	}

	private MimeMessage getMimeMessage(Session session) throws AddressException, MessagingException {
		String from = "zubayer.ahamed@metafour.com";
		String to = "zubayer.ahamed@metafour.com";
		String cc = "";
		String bcc = "";
		String replyTo = "";

		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		if(!cc.isEmpty()) message.setRecipient(Message.RecipientType.CC, new InternetAddress(cc));
		if(!bcc.isEmpty()) message.setRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
		if(!replyTo.isEmpty()) message.setReplyTo(InternetAddress.parse(replyTo));
		message.setSubject("Test mail through simple java API");
		
		return message;
	}

	@GetMapping("/mail")
	public String sendMail(RedirectAttributes redirect) throws AddressException, MessagingException, FileNotFoundException, IOException {

		// Mail session
		Session session = getMailSession();

		// Creating default MIME message object
		MimeMessage message = getMimeMessage(session);


		// Mail Body
		BodyPart mailBody = new MimeBodyPart();

		// Velocity engine
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.init();

		// create context and add data
		VelocityContext context = new VelocityContext();
		getContextData().entrySet().stream().forEach(d -> {
			context.put(d.getKey(), d.getValue());
		});

		/* now render the template into a StringWriter */
		// Get velocity template
		Template velocityTemplate = velocityEngine.getTemplate("src/main/resources/static/mail-body-template.vm");
		StringWriter bodyWriter = new StringWriter();
		velocityTemplate.merge(context, bodyWriter);

		// Save mail file before send
		File file = new File(EML_FILE);
		if(!file.exists()) {
			file.createNewFile();
		}
		message.setContent(bodyWriter.toString(), "text/html;charset=UTF-8");
		message.writeTo(new FileOutputStream(file));

		// Set template data to mail body
		mailBody.setContent(bodyWriter.toString(), "text/html");

		// Add Attachment 
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(mailBody);
		for(Map.Entry<String, String> attachment : getFiles().entrySet()){
			BodyPart multipartBody = new MimeBodyPart();
			DataSource source = new FileDataSource(attachment.getValue());
			multipartBody.setDataHandler(new DataHandler(source));
			multipartBody.setFileName(attachment.getKey());
			multipart.addBodyPart(multipartBody);
		}

		message.setContent(multipart);

		// send mail
		Transport.send(message);

		redirect.addFlashAttribute("sm", "Email send successfull");

		return "redirect:/";
	}
}
