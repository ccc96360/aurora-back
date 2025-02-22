package com.center.aurora.repository.post;

import com.center.aurora.domain.post.Post;
import com.center.aurora.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long>{
    Page<Post> findAll(Pageable pageable);
    Page<Post> findAllByWriter(Pageable pageable, User writer);
}