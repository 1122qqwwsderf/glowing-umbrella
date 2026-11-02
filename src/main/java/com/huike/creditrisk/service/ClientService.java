package com.huike.creditrisk.service;

import com.huike.creditrisk.pojo.ClientRisk;
import com.huike.creditrisk.pojo.PageResult;

import java.util.Map;

public interface ClientService {

    PageResult<ClientRisk> page(int page, int size,
                                Integer sex, Integer education,
                                Integer minAge, Integer maxAge,
                                Integer minDelay, Integer defaultFlag);

    ClientRisk detail(Integer id);

    Map<String, Object> stats();
}
