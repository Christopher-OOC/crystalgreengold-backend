package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.UnknownException;
import com.topnivo.backend.model.constant.EmailTemplates;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Order;
import com.topnivo.backend.model.response.OrderItemEmailResponse;
import com.topnivo.backend.repository.OrderRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final OrderRepository orderRepository;

    @Async
    public void sendAccountCreationEmail(
            String memberName,
            String destinationEmail,
            String username,
            String password
    ) throws MessagingException {
//        MimeMessage mimeMessage = mailSender.createMimeMessage();
//        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_RELATED, StandardCharsets.UTF_8.name());
//        mimeMessageHelper.setFrom(fromEmail);
//        final String templateName = EmailTemplates.ACCOUNT_CREATION_CONFIRMATION.getTemplate();
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("memberName", memberName);
//        variables.put("username", username);
//        variables.put("password", password);
//
//        Context context = new Context();
//        context.setVariables(variables);
//        mimeMessageHelper.setSubject(EmailTemplates.ACCOUNT_CREATION_CONFIRMATION.getSubject());
//
//        try {
//            String htmlTemplate = templateEngine.process(templateName, context);
//            mimeMessageHelper.setText(htmlTemplate, true);
//            mimeMessageHelper.setTo(destinationEmail);
//
//            mailSender.send(mimeMessage);
//        }
//        catch (MessagingException ex) {
//            throw new BadRequestException(ErrorMessages.INVALID_EMAIL);
//        }
    }

    @Async
    public void sendForgotPasswordEmail(String memberName, String email, String newPassword) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_RELATED, StandardCharsets.UTF_8.name());
        mimeMessageHelper.setFrom(fromEmail);
        final String templateName = EmailTemplates.FORGOT_PASSWORD_EMAIL.getTemplate();
        Map<String, Object> variables = new HashMap<>();
        variables.put("memberName", memberName);
        variables.put("password", newPassword);

        Context context = new Context();
        context.setVariables(variables);
        mimeMessageHelper.setSubject(EmailTemplates.FORGOT_PASSWORD_EMAIL.getSubject());

        try {
            String htmlTemplate = templateEngine.process(templateName, context);
            mimeMessageHelper.setText(htmlTemplate, true);
            mimeMessageHelper.setTo(email);

            mailSender.send(mimeMessage);
        }
        catch (MessagingException ex) {
            throw new BadRequestException(ErrorMessages.INVALID_EMAIL);
        }
    }

    @Async
    public void sendOrderConfirmationEmail(
            Member member,
            Order order,
            String storeName,
            String referenceId,
            double totalBoughtBv,
            double totalBoughtPv,
            double totalPrice) throws MessagingException {
//        MimeMessage mimeMessage = mailSender.createMimeMessage();
//        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_RELATED, StandardCharsets.UTF_8.name());
//        mimeMessageHelper.setFrom(fromEmail);
//        final String templateName = EmailTemplates.ORDER_CONFIRMATION.getTemplate();
//
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("storeName", storeName);
//        variables.put("orderNumber", order.getOrderId());
//        variables.put("referenceId", referenceId);
//        variables.put("orderDate", order.getOrderDate().toString());
//        variables.put("contact", member.getPhoneNumber());
//
//        List<OrderItemEmailResponse> itemEmailResponses = new ArrayList<>();
//        order.getOrderItems().forEach(orderItem -> {
//            OrderItemEmailResponse itemEmailResponse = new OrderItemEmailResponse();
//            itemEmailResponse.setName(String.format("%s (%f)(%f)", orderItem.getName(),
//                    toOneDecimalPoint(orderItem.getPv()),
//                    toOneDecimalPoint(orderItem.getBv())));
//            itemEmailResponse.setQuantity(orderItem.getQuantity());
//            itemEmailResponse.setTotalPv(orderItem.getPv() * orderItem.getQuantity());
//            itemEmailResponse.setTotalBv(orderItem.getBv() * orderItem.getQuantity());
//            itemEmailResponse.setPrice(orderItem.getPrice());
//            itemEmailResponse.setTotalPrice(orderItem.getPrice() * orderItem.getQuantity());
//
//            itemEmailResponses.add(itemEmailResponse);
//        });
//
//        variables.put("productItems", itemEmailResponses);
//        variables.put("totalPv", totalBoughtPv);
//        variables.put("totalBv", totalBoughtBv);
//        variables.put("totalAmount", totalPrice);
//
//        Context context = new Context();
//        context.setVariables(variables);
//        mimeMessageHelper.setSubject(EmailTemplates.ORDER_CONFIRMATION.getSubject());
//
//        try {
//            String htmlTemplate = templateEngine.process(templateName, context);
//
//            order.setPdfText(htmlTemplate);
//            orderRepository.save(order);
//
//            mimeMessageHelper.setText(htmlTemplate, true);
//            mimeMessageHelper.setTo(member.getEmail());
//
//            mailSender.send(mimeMessage);
//        }
//        catch (MessagingException ex) {
//            throw new UnknownException(ErrorMessages.UNKNOWN_ERROR);
//        }
    }

    private double toOneDecimalPoint(double value) {
        return ((int)(value * 10)) / 10.0;
    }
}