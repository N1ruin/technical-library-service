package by.niruin.library.controller;

import by.niruin.library.service.FileService;
import org.springframework.stereotype.Controller;

@Controller
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }
}
