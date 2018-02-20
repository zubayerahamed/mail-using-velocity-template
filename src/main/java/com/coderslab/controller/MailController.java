package com.coderslab.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
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

	@Autowired
	Environment env;

	@GetMapping
	public String loadMainPage(Model model) {

		return "index";
	}

	@GetMapping("/mail")
	public String sendMail(RedirectAttributes redirect)
			throws AddressException, MessagingException, FileNotFoundException, IOException {
		String to = "zubayer.ahamed@metafour.com";
		String from = "cyclingbd007@gmail.com";

		Properties props = System.getProperties();
		props.load(new FileInputStream(new File("src/main/resources/mail-settings.properties")));

		Session session = Session.getDefaultInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(props.getProperty("mail.user"), props.getProperty("mail.passwd"));
			}
		});

		// Creating default MIME message object
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		message.setSubject("Test mail through simple java API");

		BodyPart body = new MimeBodyPart();

		// velocity stuff.
		// Initialize velocity
		VelocityEngine ve = new VelocityEngine();
		ve.init();

		// get the template
		Template t = ve.getTemplate("src/main/resources/static/mail-body-template.vm");

		// create context and add data
		VelocityContext context = new VelocityContext();
		context.put("fname", "Zubayer");
		context.put("lname", "Ahamed");
		context.put("proprietor", "coderslab.com");

		/* now render the template into a StringWriter */
		StringWriter out = new StringWriter();
		t.merge(context, out);

		// velocity stuff end

		body.setContent(out.toString(), "text/html");

		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(body);

		body = new MimeBodyPart();

		String filename = "src/main/resources/static/mail-attachment-template.csv";
		DataSource source = new FileDataSource(filename);
		body.setDataHandler(new DataHandler(source));
		body.setFileName("attachment.csv");
		multipart.addBodyPart(body);

		message.setContent(multipart, "text/html");

		// send mail
		Transport.send(message);

		redirect.addFlashAttribute("sm", "Email send successfull");

		return "redirect:/";
	}
}
