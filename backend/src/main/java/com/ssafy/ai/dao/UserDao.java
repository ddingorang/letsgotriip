package com.ssafy.ai.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;

import com.ssafy.ai.domain.GroupCountRow;
import com.ssafy.ai.domain.UserDto;
import com.ssafy.ai.domain.UserSearchCriteria;

@Mapper
public interface UserDao {

    /**
     * 공통 컬럼 정의 (Snake Case 원본 컬럼명 사용)
     */
    String COLUMNS = " user_id, name, email, role, job_group, region, birthday, joined_at ";

    /**
     * 회원 정보 매핑을 위한 공통 ResultMap 정의
     */
    @Results(id = "UserMap", value = {
        @Result(property = "userId", column = "user_id", id = true),
        @Result(property = "name", column = "name"),
        @Result(property = "email", column = "email"),
        @Result(property = "role", column = "role"),
        @Result(property = "jobGroup", column = "job_group"),
        @Result(property = "region", column = "region"),
        @Result(property = "birthday", column = "birthday"),
        @Result(property = "joinedAt", column = "joined_at")
    })
    @Select("SELECT " + COLUMNS + " FROM users ORDER BY joined_at DESC LIMIT #{limit}")
    List<UserDto> selectRecentUsers(@Param("limit") int limit);

    @ResultMap("UserMap")
    @Select("""
        <script>
        SELECT """ + COLUMNS + """
        FROM users
        <where>
          <if test='c.role != null and c.role != ""'> role = #{c.role} </if>
          <if test='c.jobGroup != null and c.jobGroup != ""'> AND job_group = #{c.jobGroup} </if>
          <if test='c.region != null and c.region != ""'> AND region = #{c.region} </if>
          <if test='c.birthFrom != null'> AND YEAR(birthday) &gt;= #{c.birthFrom} </if>
          <if test='c.birthTo != null'> AND YEAR(birthday) &lt;= #{c.birthTo} </if>
        </where>
        ORDER BY joined_at DESC LIMIT #{c.limit}
        </script>
        """)
    List<UserDto> selectUsersByCriteria(@Param("c") UserSearchCriteria c);

    @Select("""
        <script>
        SELECT COUNT(*) FROM users
        <where>
          <if test='c.role != null and c.role != ""'> role = #{c.role} </if>
          <if test='c.jobGroup != null and c.jobGroup != ""'> AND job_group = #{c.jobGroup} </if>
          <if test='c.region != null and c.region != ""'> AND region = #{c.region} </if>
          <if test='c.birthFrom != null'> AND YEAR(birthday) &gt;= #{c.birthFrom} </if>
          <if test='c.birthTo != null'> AND YEAR(birthday) &lt;= #{c.birthTo} </if>
        </where>
        </script>
        """)
    int countUsersByCriteria(@Param("c") UserSearchCriteria c);

    @Select("""
        <script>
        SELECT
          <choose>
            <when test="groupBy == 'region'"> IFNULL(region, '(NULL)') </when>
            <when test="groupBy == 'jobGroup'"> IFNULL(job_group, '(NULL)') </when>
            <when test="groupBy == 'role'"> IFNULL(role, '(NULL)') </when>
            <when test="groupBy == 'birthDecade'">
              CASE WHEN birthday IS NULL THEN '(NULL)' ELSE CONCAT(FLOOR(YEAR(birthday)/10)*10, 's') END
            </when>
            <otherwise> IFNULL(region, '(NULL)') </otherwise>
          </choose> AS groupKey,
          COUNT(*) AS cnt
        FROM users
        <where>
          <if test='c.role != null and c.role != ""'> role = #{c.role} </if>
          <if test='c.jobGroup != null and c.jobGroup != ""'> AND job_group = #{c.jobGroup} </if>
          <if test='c.region != null and c.region != ""'> AND region = #{c.region} </if>
          <if test='c.birthFrom != null'> AND YEAR(birthday) &gt;= #{c.birthFrom} </if>
          <if test='c.birthTo != null'> AND YEAR(birthday) &lt;= #{c.birthTo} </if>
        </where>
        GROUP BY groupKey
        ORDER BY cnt DESC, groupKey ASC
        LIMIT 50
        </script>
        """)
    List<GroupCountRow> selectGroupCounts(@Param("c") UserSearchCriteria c, @Param("groupBy") String groupBy);
}
