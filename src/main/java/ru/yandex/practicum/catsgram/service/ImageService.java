package ru.yandex.practicum.catsgram.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.catsgram.dal.ImageRepository;
import ru.yandex.practicum.catsgram.exception.ConditionsNotMetException;
import ru.yandex.practicum.catsgram.exception.ImageFileException;
import ru.yandex.practicum.catsgram.exception.NotFoundException;
import ru.yandex.practicum.catsgram.model.Image;
import ru.yandex.practicum.catsgram.model.ImageData;
import ru.yandex.practicum.catsgram.model.Post;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final PostService postService;
    private final ImageRepository imageRepository;

    @Value("${catsgram.image-directory}")
    private String imageDirectory;

    public List<Image> getPostImages(long postId) {
        return imageRepository.findByPostId(postId);
    }

    public List<Image> saveImages(long postId, List<MultipartFile> files) {
        return files.stream().map(file -> saveImage(postId, file)).collect(Collectors.toList());
    }

    public ImageData getImageData(long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Изображение с id = " + imageId + " не найдено"));

        byte[] data = loadFile(image);
        return new ImageData(data, image.getOriginalFileName());
    }

    private byte[] loadFile(Image image) {
        Path path = Paths.get(image.getFilePath());
        if (Files.exists(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                throw new ImageFileException("Ошибка чтения файла. Id: " + image.getId()
                        + ", name: " + image.getOriginalFileName(), e);
            }
        } else {
            throw new ImageFileException("Файл не найден. Id: " + image.getId()
                    + ", name: " + image.getOriginalFileName());
        }
    }

    private Image saveImage(long postId, MultipartFile file) {
        Post post = postService.findById(postId)
                .orElseThrow(() -> new ConditionsNotMetException("Указанный пост не найден"));

        Path directory = Paths.get(imageDirectory);
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            throw new ImageFileException("Не удалось создать директорию для хранения изображений", e);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        if (originalFilename.contains("..")) {
            throw new ImageFileException("Некорректное имя файла: " + originalFilename);
        }

        String filename = Instant.now().toEpochMilli() + "_" + originalFilename;
        Path filePath = directory.resolve(filename);

        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new ImageFileException("Не удалось сохранить файл: " + originalFilename, e);
        }

        Image image = new Image();
        image.setPostId(postId);
        image.setOriginalFileName(originalFilename);
        image.setFilePath(filePath.toString());

        return imageRepository.save(image);
    }
}