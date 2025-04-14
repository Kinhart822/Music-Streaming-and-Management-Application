package com.spring.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;

public class JavaFileToMultipartFile implements MultipartFile {
    private final File file;

    public JavaFileToMultipartFile(File file) {
        this.file = file;
    }

    @NotNull
    @Override
    public String getName() {
        return file.getName();
    }

    @NotNull
    @Override
    public String getOriginalFilename() {
        return file.getName();
    }

    @NotNull
    @Override
    public String getContentType() {
        try {
            return Files.probeContentType(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error while extracting MIME type of file", e);
        }
    }

    @Override
    public boolean isEmpty() {
        return file.length() == 0;
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @NotNull
    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    @NotNull
    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.copy(file.toPath(), dest.toPath());
    }
}
