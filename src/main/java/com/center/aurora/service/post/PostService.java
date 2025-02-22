package com.center.aurora.service.post;

import com.center.aurora.domain.post.Mood;
import com.center.aurora.domain.user.User;
import com.center.aurora.exception.UserAuthException;
import com.center.aurora.repository.user.UserRepository;
import com.center.aurora.service.post.dto.PostResponse;
import com.center.aurora.service.post.dto.PostUserDto;
import com.center.aurora.utils.S3Uploader;
import com.center.aurora.domain.post.Image;
import com.center.aurora.repository.post.ImageRepository;
import com.center.aurora.service.post.dto.PostCreateRequestDto;
import com.center.aurora.domain.post.Post;
import com.center.aurora.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public List<PostResponse> getAllPost(Pageable pageable) {
        Page<Post> list = postRepository.findAll(pageable);
        return fetchPosts(list);
    }

    @Transactional
    public List<PostResponse> getPost(Long user_id, Pageable pageable) {
        User user = userRepository.findById(user_id).get();
        Page<Post> list = postRepository.findAllByWriter(pageable,user);
        return fetchPosts(list);
    }

    @Transactional
    public PostResponse createPost(Long user_id, PostCreateRequestDto postDto) throws IOException {
        User user = userRepository.findById(user_id).get();
        Post post = Post.builder()
                .writer(user)
                .mood(postDto.getMood())
                .content(postDto.getContent())
                .build();
        postRepository.save(post);

        List<String> images = new ArrayList<>();

        if(postDto.getImages()!=null){
            for (MultipartFile imageValue : postDto.getImages()) {
                images.add(s3Uploader.upload(imageValue, "aurora"));
            }
        }

        for (String imageValue : images) {
            Image image = Image.builder()
                    .post(post)
                    .image(imageValue)
                    .build();

            imageRepository.save(image);
        }

        return fetchPost(post, images);
    }

    @Transactional
    public PostResponse updatePost(Long user_id, Long post_id, PostCreateRequestDto postDto) throws IOException {
        Post post = postRepository.findById(post_id).get();
        List<String> images = new ArrayList<>();
        if(post.getWriter().getId() == user_id) {
            Mood mood;
            String content;

            if (postDto.getMood() != null) {
                mood = postDto.getMood();
            } else {
                mood = post.getMood();
            }
            if (postDto.getContent() != null) {
                content = postDto.getContent();
            } else {
                content = post.getContent();
            }

            post.update(mood, content);



            if (postDto.getImages() != null) {
                imageRepository.deleteAllByPostId(post);
                for (MultipartFile imageValue : postDto.getImages()) {
                    images.add(s3Uploader.upload(imageValue, "aurora"));
                }
                for (String imageValue : images) {
                    Image image = Image.builder()
                            .post(post)
                            .image(imageValue)
                            .build();
                    imageRepository.save(image);
                }
            } else {
                images = imageRepository.findAllImageByPostId(post);
            }
        }else{
            throw new UserAuthException("유저 권한이 없습니다.");
        }
        return fetchPost(post, images);
    }

    @Transactional
    public String deletePost(Long user_id, Long post_id){
        String message = "";
        Post post = postRepository.findById(post_id).get();

        if(post.getWriter().getId() == user_id){
            imageRepository.deleteAllByPostId(post);
            postRepository.deleteById(post_id);
            message = "게시물이 정상적으로 삭제되었습니다";
        }else{
            message = "유저 권한이 없습니다.";
        }

        return message;
    }

    public List<PostResponse> fetchPosts(Page<Post> list){
        List<PostResponse> posts = new ArrayList<>();

        for (Post post : list.getContent()){
            List<String> images = imageRepository.findAllImageByPostId(post);
            posts.add(fetchPost(post,images));
        }

        return posts;
    }
    public PostResponse fetchPost(Post post, List<String> images){
        PostUserDto PostUser = PostUserDto.builder()
                .id(post.getWriter().getId())
                .name(post.getWriter().getName())
                .avatar(post.getWriter().getImage())
                .build();

        PostResponse postResponse = PostResponse.builder()
                .id(post.getId())
                .getAllPostUser(PostUser)
                .mood(post.getMood())
                .content(post.getContent())
                .images(images)
                .build();

        return postResponse;
    }
}
