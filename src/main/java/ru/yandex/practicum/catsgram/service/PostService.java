package ru.yandex.practicum.catsgram.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.catsgram.dal.PostRepository;
import ru.yandex.practicum.catsgram.exception.ConditionsNotMetException;
import ru.yandex.practicum.catsgram.exception.NotFoundException;
import ru.yandex.practicum.catsgram.model.Post;
import ru.yandex.practicum.catsgram.model.SortOrder;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;

    public PostService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository;
        this.userService = userService;
    }

    public Collection<Post> findAll(int from, int size, SortOrder sort) {
        // PostgreSQL уже сортирует по post_date DESC в запросе
        // Для ASC нужно будет изменить запрос в репозитории или сортировать здесь
        return postRepository.findAll(size, from);
    }

    public Optional<Post> findById(long postId) {
        return postRepository.findById(postId);
    }

    public Post create(Post post) {
        if (post.getDescription() == null || post.getDescription().isBlank()) {
            throw new ConditionsNotMetException("Описание не может быть пустым");
        }

        Optional<ru.yandex.practicum.catsgram.model.User> author = userService.findUserById(post.getAuthorId());
        if (author.isEmpty()) {
            throw new ConditionsNotMetException("Автор с id = " + post.getAuthorId() + " не найден");
        }

        post.setPostDate(Instant.now());
        return postRepository.save(post);
    }

    public Post update(Post newPost) {
        if (newPost.getId() == null) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }

        Post existingPost = postRepository.findById(newPost.getId())
                .orElseThrow(() -> new NotFoundException("Пост с id = " + newPost.getId() + " не найден"));

        if (newPost.getDescription() == null || newPost.getDescription().isBlank()) {
            throw new ConditionsNotMetException("Описание не может быть пустым");
        }

        existingPost.setDescription(newPost.getDescription());
        return postRepository.update(existingPost);
    }
}