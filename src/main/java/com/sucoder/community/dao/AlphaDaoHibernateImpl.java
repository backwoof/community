package com.sucoder.community.dao;

import org.springframework.stereotype.Repository;

@Repository
public class AlphaDaoHibernateImpl implements AlphaDao{
    @Override
    public String select() {
        return "Hibernate";
    }
}
