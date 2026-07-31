package com.topnivo.backend.controller;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.topnivo.backend.model.entity.Order;
import com.topnivo.backend.repository.OrderRepository;
import com.topnivo.backend.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping(value = "/api/v1/resources")
@RequiredArgsConstructor
public class FileController {

    private final OrderRepository orderRepository;
    private final PayrollService payrollService;

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

    @GetMapping(value = "/payroll/report", produces = "text/csv")
    public ResponseEntity<Resource> payrollReport() throws IOException {
        ByteArrayResource resource = payrollService.generatePayrollReportCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=payroll-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @GetMapping(value = "/payroll/flutterwave-csv", produces = "text/csv")
    public ResponseEntity<Resource> generateFlutterwavePayrollCsv() throws IOException {
        ByteArrayResource resource = payrollService.generateFlutterwavePayrollCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=flutterwave-payroll.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
