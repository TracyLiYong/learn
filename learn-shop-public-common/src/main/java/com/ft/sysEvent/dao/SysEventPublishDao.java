package com.ft.sysEvent.dao;

import com.ft.sysEvent.model.expand.SysEventPublishDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author liuyongtao
 * @create 2018-03-01 9:25
 */
public interface SysEventPublishDao extends JpaRepository<SysEventPublishDto, String> {

    List<SysEventPublishDto> findByStatus(String status);
}