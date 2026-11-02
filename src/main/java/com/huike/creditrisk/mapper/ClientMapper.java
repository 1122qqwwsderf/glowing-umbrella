package com.huike.creditrisk.mapper;

import com.huike.creditrisk.pojo.Client;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ClientMapper {

    List<Client> selectPage(@Param("sex") Integer sex,
                            @Param("education") Integer education,
                            @Param("minAge") Integer minAge,
                            @Param("maxAge") Integer maxAge,
                            @Param("minDelay") Integer minDelay,
                            @Param("defaultFlag") Integer defaultFlag,
                            @Param("offset") int offset,
                            @Param("size") int size);

    long countPage(@Param("sex") Integer sex,
                   @Param("education") Integer education,
                   @Param("minAge") Integer minAge,
                   @Param("maxAge") Integer maxAge,
                   @Param("minDelay") Integer minDelay,
                   @Param("defaultFlag") Integer defaultFlag);

    Client selectById(@Param("id") Integer id);

    int deleteById(@Param("id") Integer id);

    Map<String, Object> summary();

    List<Map<String, Object>> byMaxDelay();

    List<Map<String, Object>> byDelayMonths();

    List<Map<String, Object>> bySex();

    List<Map<String, Object>> byAgeGroup();

    List<Map<String, Object>> byPay0();
}
