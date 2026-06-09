package com.topnivo.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    public String fileBtyeToGenerateFileUrl(byte[] content) {
        String prefix = "data:image/png;base64,";
        return prefix + Base64.getEncoder().encodeToString(content);
    }
}
