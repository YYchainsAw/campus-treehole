package xyz.yychainsaw.campus_treehole.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.yychainsaw.campus_treehole.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByUserId(long userId, Pageable pageable);

    @Modifying
    @Query("delete from Post p where p.id = :id and p.userId = :userId")
    int deleteOwnedPost(@Param("id") long id, @Param("userId") long userId);
}
