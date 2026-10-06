package com.example.mapper;

import com.example.entity.Message;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 站内消息。所有读写都带「接收人 ID + 角色」条件：一个人只能看到、标记、删除发给自己的消息。
 */
public interface MessageMapper {

    @Insert("insert into message (user_id, role, type, title, content, link, is_read, create_time)"
            + " values (#{userId}, #{role}, #{type}, #{title}, #{content}, #{link}, 0, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    @Select("<script>select * from message where user_id = #{userId} and role = #{role}"
            + "<if test='unreadOnly'> and is_read = 0</if> order by id desc</script>")
    List<Message> selectByReceiver(@Param("userId") Integer userId, @Param("role") String role,
                                   @Param("unreadOnly") boolean unreadOnly);

    @Select("select count(*) from message where user_id = #{userId} and role = #{role} and is_read = 0")
    int countUnread(@Param("userId") Integer userId, @Param("role") String role);

    @Update("update message set is_read = 1 where id = #{id} and user_id = #{userId} and role = #{role}")
    int markRead(@Param("id") Integer id, @Param("userId") Integer userId, @Param("role") String role);

    @Update("update message set is_read = 1 where user_id = #{userId} and role = #{role} and is_read = 0")
    int markAllRead(@Param("userId") Integer userId, @Param("role") String role);

    @Delete("delete from message where id = #{id} and user_id = #{userId} and role = #{role}")
    int deleteOwn(@Param("id") Integer id, @Param("userId") Integer userId, @Param("role") String role);
}
