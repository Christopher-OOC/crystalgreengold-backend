package com.topnivo.backend.controller;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.topnivo.backend.model.entity.Order;
import com.topnivo.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping(value = "/api/v1/resources")
@RequiredArgsConstructor
public class FileController {

    private final OrderRepository orderRepository;

    @GetMapping("/orders/{id}")
    public ResponseEntity<ByteArrayResource> downloadPdf(@PathVariable("id") String id) throws Exception {
        Order order = orderRepository.findByOrderId(id);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(order.getPdfText(), null);
        builder.toStream(baos);
        builder.run();

        byte[] pdfBytes = baos.toByteArray();
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=order.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }
}
